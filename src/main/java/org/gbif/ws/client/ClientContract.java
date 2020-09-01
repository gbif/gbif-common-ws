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
package org.gbif.ws.client;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.openfeign.annotation.PathVariableParameterProcessor;
import org.springframework.cloud.openfeign.annotation.QueryMapParameterProcessor;
import org.springframework.cloud.openfeign.annotation.RequestHeaderParameterProcessor;
import org.springframework.cloud.openfeign.annotation.RequestParamParameterProcessor;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.web.bind.annotation.RequestMapping;

import feign.MethodMetadata;
import feign.Util;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

public class ClientContract extends SpringMvcContract {

  public ClientContract() {
    super(
        Arrays.asList(
            new PartialDateParameterProcessor(),
            new PathVariableParameterProcessor(),
            new RequestParamParameterProcessor(),
            new RequestHeaderParameterProcessor(),
            new QueryMapParameterProcessor()));
  }

  @Override
  public List<MethodMetadata> parseAndValidatateMetadata(final Class<?> targetType) {
    checkState(
        targetType.getTypeParameters().length == 0,
        "Parameterized types unsupported: %s",
        targetType.getSimpleName());
    final Map<String, MethodMetadata> result = new LinkedHashMap<>();

    for (final Method method : targetType.getMethods()) {
      if (method.getDeclaringClass() == Object.class
          || (method.getModifiers() & Modifier.STATIC) != 0
          || Util.isDefault(method)
          // skip default methods which related to generic inheritance
          // also default methods are considered as "unsupported operations"
          || method.toString().startsWith("public default")
          // skip not annotated methods (consider as "not implemented")
          || method.getAnnotations().length == 0) {
        continue;
      }
      final MethodMetadata metadata = this.parseAndValidateMetadata(targetType, method);
      checkState(
          !result.containsKey(metadata.configKey()),
          "Overrides unsupported: %s",
          metadata.configKey());
      result.put(metadata.configKey(), metadata);
    }

    return new ArrayList<>(result.values());
  }

  @Override
  protected void processAnnotationOnClass(MethodMetadata data, Class<?> clz) {
    RequestMapping classAnnotation = findMergedAnnotation(clz, RequestMapping.class);
    if (classAnnotation != null) {
      // Prepend path from class annotation if specified
      if (classAnnotation.value().length > 0) {
        String pathValue = emptyToNull(classAnnotation.value()[0]);
        data.template().uri(StringUtils.prependIfMissing(pathValue, "/"));
      }
    }
  }
}
