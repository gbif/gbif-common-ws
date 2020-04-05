package org.gbif.ws.client;

import feign.MethodMetadata;
import feign.Util;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import org.gbif.api.annotation.PartialDate;
import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;

/**
 * Process method arguments annotated with annotation {@link PartialDate}.
 * Should be used instead of {@link org.springframework.web.bind.annotation.RequestParam}.
 * Only for date parameters!
 */
public class PartialDateParameterProcessor implements AnnotatedParameterProcessor {

  private static final Class<PartialDate> ANNOTATION = PartialDate.class;

  private PartialDateExpander partialDateExpander = new PartialDateExpander();

  @Override
  public Class<? extends Annotation> getAnnotationType() {
    return ANNOTATION;
  }

  @Override
  public boolean processArgument(
      AnnotatedParameterContext context, Annotation annotation, Method method) {
    int parameterIndex = context.getParameterIndex();
    MethodMetadata data = context.getMethodMetadata();
    PartialDate requestParam = ANNOTATION.cast(annotation);
    String name = requestParam.value();

    Util.checkState(Util.emptyToNull(name) != null,
        "PartialDate.value() was null");

    context.setParameterName(name);

    data.indexToExpander().put(parameterIndex, partialDateExpander);
    Collection<String> query =
        context.setTemplateParameter(name, data.template().queries().get(name));
    data.template().query(name, query);

    return true;
  }
}
