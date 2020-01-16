package org.gbif.ws.server.provider;

import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class DatasetSearchRequestHandlerMethodArgumentResolver
    extends FacetedSearchRequestProvider<DatasetSearchRequest, DatasetSearchParameter>
    implements HandlerMethodArgumentResolver {

  public DatasetSearchRequestHandlerMethodArgumentResolver() {
    super(DatasetSearchRequest.class, DatasetSearchParameter.class);
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return DatasetSearchRequest.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
    return getValue(webRequest);
  }
}
