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
package org.gbif.ws.client;

import org.gbif.api.annotation.PartialDate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;

import feign.MethodMetadata;
import feign.Util;

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

    Util.checkState(Util.emptyToNull(name) != null, "PartialDate.value() was null");

    context.setParameterName(name);

    data.indexToExpander().put(parameterIndex, partialDateExpander);
    Collection<String> query =
        context.setTemplateParameter(name, data.template().queries().get(name));
    data.template().query(name, query);

    return true;
  }
}
