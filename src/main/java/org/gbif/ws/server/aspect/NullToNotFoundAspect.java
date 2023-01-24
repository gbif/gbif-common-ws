/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.ws.server.aspect;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.ws.NotFoundException;

import java.net.URI;
import java.util.Optional;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This aspect throws a {@link NotFoundException} for every {@code null} return value of a method.
 */
@Component
@Aspect
public class NullToNotFoundAspect {

  private static final Logger LOG = LoggerFactory.getLogger(NullToNotFoundAspect.class);

  @AfterReturning(
      pointcut = "@annotation(org.gbif.api.annotation.NullToNotFound)",
      returning = "retVal")
  public void afterReturningAdvice(JoinPoint jp, Object retVal) {
    if (retVal == null) {
      NullToNotFound nullToNotFound = getAnnotation(jp);

      // replace pat variables in URI with values
      URI uri = getTargetUrl(jp, nullToNotFound);

      throw new NotFoundException("Entity not found", uri);
    }
  }

  /**
   * Gets the NullToNotFound annotation.
   */
  private static NullToNotFound getAnnotation(JoinPoint jp) {
    return ((MethodSignature) jp.getSignature()).getMethod().getAnnotation(NullToNotFound.class);
  }

  /**
   * Builds the URL invoked by the request.
   */
  private static URI getTargetUrl(JoinPoint jp, NullToNotFound nullToNotFound) {
    if (nullToNotFound.useUrlMapping()) {
      return UriComponentsBuilder.newInstance()
          .path(getResourceUrl(jp))
          .path(getMethodResourceUrl(jp))
          .build(jp.getArgs());
    } else {
      return UriComponentsBuilder.newInstance().path(nullToNotFound.value()).build(jp.getArgs());
    }
  }

  /**
   * Ensures the url is surrounded by '/'.
   */
  private static String addSurroundingSlashes(String url) {
    String resultUrl = url.endsWith("/") ? url : url + '/';
    return resultUrl.startsWith("/") ? resultUrl : '/' + resultUrl;
  }

  /**
   * Gets the Resource URL from the RequestMapping annotation if it exists.
   */
  private static String getResourceUrl(JoinPoint jp) {
    return Optional.ofNullable(jp.getTarget().getClass().getAnnotation(RequestMapping.class))
        .map(rm -> rm.value()[0])
        .map(NullToNotFoundAspect::addSurroundingSlashes)
        .orElse("");
  }

  /**
   * Gets the value of the GetMapping annotation.
   */
  private static String getMethodResourceUrl(JoinPoint jp) {
    return Optional.ofNullable(
            ((MethodSignature) jp.getSignature()).getMethod().getAnnotation(GetMapping.class))
        .map(gm -> gm.value()[0])
        .orElse("");
  }
}
