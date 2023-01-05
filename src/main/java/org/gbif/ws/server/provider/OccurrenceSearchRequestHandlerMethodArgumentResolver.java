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
package org.gbif.ws.server.provider;

import java.util.Optional;

import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;

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
  private static final String SHUFFLE_PARAM = "shuffle";
  private static final String SHUFFLE_SEED_PARAM = "shuffleSeed";

  public OccurrenceSearchRequestHandlerMethodArgumentResolver() {
    super(OccurrenceSearchRequest.class, OccurrenceSearchParameter.class);
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return OccurrenceSearchRequest.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    return getValue(webRequest);
  }

  @Override
  protected OccurrenceSearchRequest getSearchRequest(
      WebRequest webRequest, OccurrenceSearchRequest searchRequest) {
    OccurrenceSearchRequest occurrenceSearchRequest =
        super.getSearchRequest(webRequest, searchRequest);
    Optional.ofNullable(webRequest.getParameter(MATCH_CASE_PARAM))
        .ifPresent(
            matchVerbatim ->
                occurrenceSearchRequest.setMatchCase(Boolean.parseBoolean(matchVerbatim)));

    String shuffleParam = webRequest.getParameter(SHUFFLE_PARAM);
    if (shuffleParam != null && !shuffleParam.isEmpty()) {
      Boolean shuffle = Boolean.parseBoolean(shuffleParam);
      occurrenceSearchRequest.setShuffle(shuffle);

      if (Boolean.TRUE.equals(shuffle)) {
        String shuffleSeedParam = webRequest.getParameter(SHUFFLE_SEED_PARAM);
        if (shuffleSeedParam != null
            && shuffleSeedParam.length() < 6) {
          throw new IllegalArgumentException("Shuffle seed must have at least 6 characters");
        }

        if (occurrenceSearchRequest.getOffset() > 0 && shuffleSeedParam == null) {
          throw new IllegalArgumentException("Shuffle requests with paging must specify a shuffle seed");
        }

        occurrenceSearchRequest.setShuffleSeed(shuffleSeedParam);
      }
    }

    return occurrenceSearchRequest;
  }
}
