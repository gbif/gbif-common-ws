package org.gbif.ws.server.provider;

import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class OccurrenceSearchRequestHandlerMethodArgumentResolver
    extends FacetedSearchRequestProvider<OccurrenceSearchRequest, OccurrenceSearchParameter>
    implements HandlerMethodArgumentResolver {

  private static final String MATCH_CASE_PARAM = "matchCase";

  public OccurrenceSearchRequestHandlerMethodArgumentResolver() {
    super(OccurrenceSearchRequest.class, OccurrenceSearchParameter.class);
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return OccurrenceSearchRequest.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
    return getValue(webRequest);
  }

  @Override
  protected OccurrenceSearchRequest getSearchRequest(WebRequest webRequest, OccurrenceSearchRequest searchRequest) {
    OccurrenceSearchRequest occurrenceSearchRequest = super.getSearchRequest(webRequest, searchRequest);
    Optional.ofNullable(webRequest.getParameter(MATCH_CASE_PARAM))
      .ifPresent(matchVerbatim -> occurrenceSearchRequest.setMatchCase(Boolean.parseBoolean(matchVerbatim)));
    return occurrenceSearchRequest;
  }
}
