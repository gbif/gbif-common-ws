package org.gbif.ws.client;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

import feign.MethodMetadata;
import feign.Util;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.web.bind.annotation.RequestMapping;

public class ClientContract extends SpringMvcContract {

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
