/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
      String uri =
          ((MethodSignature) jp.getSignature())
              .getMethod()
              .getAnnotation(NullToNotFound.class)
              .value();

      String[] parameterNames = ((MethodSignature) jp.getSignature()).getParameterNames();
      Object[] parameterValues = jp.getArgs();

      // replace pat variables in URI with values
      for (int i = 0; i < parameterNames.length; i++) {
        if (uri.contains('{' + parameterNames[i] + '}')) {
          uri = uri.replace('{' + parameterNames[i] + '}', parameterValues[i].toString());
        }
      }

      if (uri.contains("{") || uri.contains("}")) {
        LOG.warn("URI was not processed properly and contains special characters {}", uri);
      }

      throw new NotFoundException("Entity not found", URI.create(uri));
    }
  }
}
