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
package org.gbif.ws.server.interceptor;

import org.gbif.api.annotation.Trim;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.registry.Dataset;

import java.io.IOException;
import java.lang.reflect.Type;

import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.WrapDynaBean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * An interceptor that will trim all possible strings of a bean.
 * All top level string properties are handled, as are those of nested objects that are in the GBIF registry model
 * package.
 * This will recurse only 5 levels deep, to guard against potential circular looping.
 */
@SuppressWarnings("NullableProblems")
@ControllerAdvice
public class StringTrimInterceptor implements RequestBodyAdvice {

  private static final Logger LOG = LoggerFactory.getLogger(StringTrimInterceptor.class);

  // only goes 5 levels deep to stop potential circular loops
  private static final int MAX_RECURSION = 5;
  private static final String REGEX_INVISIBLE_CONTROL_CHARS = "\\p{C}";

  @Override
  public boolean supports(
      MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
    return methodParameter.getMethodAnnotation(Trim.class) != null
        || methodParameter.getParameterAnnotation(Trim.class) != null;
  }

  @Override
  public HttpInputMessage beforeBodyRead(
      HttpInputMessage httpInputMessage,
      MethodParameter methodParameter,
      Type type,
      Class<? extends HttpMessageConverter<?>> aClass)
      throws IOException {
    return httpInputMessage;
  }

  @Override
  public Object afterBodyRead(
      Object o,
      HttpInputMessage httpInputMessage,
      MethodParameter methodParameter,
      Type type,
      Class<? extends HttpMessageConverter<?>> aClass) {
    trimStringsOf(o);
    return o;
  }

  @Override
  public Object handleEmptyBody(
      Object o,
      HttpInputMessage httpInputMessage,
      MethodParameter methodParameter,
      Type type,
      Class<? extends HttpMessageConverter<?>> aClass) {
    return o;
  }

  @VisibleForTesting
  void trimStringsOf(Object target) {
    trimStringsOf(target, 0);
  }

  private void trimStringsOf(Object target, int level) {
    if (target != null && level <= MAX_RECURSION) {
      LOG.debug("Trimming class: {}", target.getClass());

      WrapDynaBean wrapped = new WrapDynaBean(target);
      DynaClass dynaClass = wrapped.getDynaClass();
      for (DynaProperty dynaProp : dynaClass.getDynaProperties()) {
        if (String.class.isAssignableFrom(dynaProp.getType())) {
          String prop = dynaProp.getName();
          String orig = (String) wrapped.get(prop);
          if (orig != null) {
            String trimmed = Strings.emptyToNull(orig.trim());
            String withoutControlChars =
                StringUtils.removeAll(trimmed, REGEX_INVISIBLE_CONTROL_CHARS);
            if (!Objects.equal(orig, withoutControlChars)) {
              LOG.debug(
                  "Overriding value of [{}] from [{}] to [{}]", prop, orig, withoutControlChars);
              wrapped.set(prop, withoutControlChars);
            }
          }
        } else {
          try {
            // trim everything in the registry model package (assume that Dataset resides in the
            // correct package here)
            Object property = wrapped.get(dynaProp.getName());
            if (property != null
                && (Dataset.class.getPackage() == property.getClass().getPackage()
                    || Collection.class.getPackage() == property.getClass().getPackage())) {
              trimStringsOf(property, level + 1);
            }

          } catch (IllegalArgumentException e) {
            // expected for non accessible properties
          }
        }
      }
    }
  }
}
