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

import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.WebRequest;

import static org.gbif.ws.util.CommonWsUtils.getFirst;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_LIMIT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MINCOUNT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MULTISELECT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_OFFSET;

/**
 * Provider class that transforms a set of HTTP parameters into a FacetedSearchRequest class instance.
 * This assumes the existence of the following parameters in the HTTP request:
 * 'page_size', 'offset', 'facet', 'q' and any of the search parameter enum member names case insensitively.
 */
public class FacetedSearchRequestProvider<
        RT extends FacetedSearchRequest<P>, P extends SearchParameter>
    extends SearchRequestProvider<RT, P> {

  private static final int DEFAULT_FACET_LIMIT = 10;
  public static final int DEFAULT_MAX_FACET_LIMIT = 1000;
  public static final int DEFAULT_MAX_FACET_OFFSET = 10000;

  private final int maxFacetLimit;
  private final int maxFacetOffset;

  public FacetedSearchRequestProvider(Class<RT> requestType, Class<P> searchParameterClass) {
    super(requestType, searchParameterClass);
    this.maxFacetLimit = DEFAULT_MAX_FACET_LIMIT;
    this.maxFacetOffset = DEFAULT_MAX_FACET_OFFSET;
  }

  public FacetedSearchRequestProvider(
      Class<RT> requestType, Class<P> searchParameterClass, Integer maxPageSize) {
    this(
        requestType,
        searchParameterClass,
        maxPageSize,
        DEFAULT_MAX_FACET_LIMIT,
        DEFAULT_MAX_FACET_OFFSET);
  }

  /**
   * @param maxFacetLimit  facet limits above it are lowered to it, as limit is for page sizes
   * @param maxFacetOffset facet offsets above it are rejected (400)
   */
  public FacetedSearchRequestProvider(
      Class<RT> requestType,
      Class<P> searchParameterClass,
      Integer maxPageSize,
      int maxFacetLimit,
      int maxFacetOffset) {
    super(requestType, searchParameterClass, maxPageSize);
    this.maxFacetLimit = maxFacetLimit;
    this.maxFacetOffset = maxFacetOffset;
  }

  @Override
  protected RT getSearchRequest(WebRequest webRequest, RT searchRequest) {
    RT request = super.getSearchRequest(webRequest, searchRequest);

    final Map<String, String[]> params = webRequest.getParameterMap();

    final String facetMultiSelectValue = getFirstIgnoringCase(PARAM_FACET_MULTISELECT, params);
    if (facetMultiSelectValue != null) {
      searchRequest.setFacetMultiSelect(Boolean.parseBoolean(facetMultiSelectValue));
    }

    final String facetMinCountValue = getFirstIgnoringCase(PARAM_FACET_MINCOUNT, params);
    if (facetMinCountValue != null) {
      searchRequest.setFacetMinCount(
          parseNonNegativeInt(PARAM_FACET_MINCOUNT, facetMinCountValue));
    }

    final String facetLimit = getFirstIgnoringCase(PARAM_FACET_LIMIT, params);
    if (facetLimit != null) {
      searchRequest.setFacetLimit(parseFacetLimit(PARAM_FACET_LIMIT, facetLimit));
    } else if (searchRequest.getFacetLimit() != null
        && searchRequest.getFacetLimit() > maxFacetLimit) {
      // the request's own default limit must not exceed a lower maximum either
      searchRequest.setFacetLimit(maxFacetLimit);
    }

    final String facetOffset = getFirstIgnoringCase(PARAM_FACET_OFFSET, params);
    if (facetOffset != null) {
      searchRequest.setFacetOffset(parseFacetOffset(PARAM_FACET_OFFSET, facetOffset));
    }

    final List<String> facets =
        params.get(PARAM_FACET) != null
            ? Arrays.asList(params.get(PARAM_FACET))
            : Collections.emptyList();
    if (!facets.isEmpty()) {
      for (String f : facets) {
        P p = findSearchParam(f);
        if (p != null) {
          searchRequest.addFacets(p);
          String pFacetOffsetParam = f + '.' + PARAM_FACET_OFFSET;
          String pFacetLimitParam = f + '.' + PARAM_FACET_LIMIT;
          String pFacetOffset = getFirstIgnoringCase(pFacetOffsetParam, params);
          String pFacetLimit = getFirstIgnoringCase(pFacetLimitParam, params);
          if (pFacetLimit != null || pFacetOffset != null) {
            searchRequest.addFacetPage(
                p,
                pFacetOffset != null ? parseFacetOffset(pFacetOffsetParam, pFacetOffset) : 0,
                pFacetLimit != null
                    ? parseFacetLimit(pFacetLimitParam, pFacetLimit)
                    : Math.min(DEFAULT_FACET_LIMIT, maxFacetLimit));
          }
        }
      }
    }

    return request;
  }

  private int parseFacetLimit(String parameter, String value) {
    return Math.min(parseNonNegativeInt(parameter, value), maxFacetLimit);
  }

  private int parseFacetOffset(String parameter, String value) {
    int offset = parseNonNegativeInt(parameter, value);
    if (offset > maxFacetOffset) {
      throw new IllegalArgumentException(
          "Parameter " + parameter + " must not be greater than " + maxFacetOffset);
    }
    return offset;
  }

  /**
   * Get the first parameter value, the parameter is searched in a case-insensitive manner.
   * First tries with the exact match, then the lowercase and finally the uppercase value of the parameter.
   */
  protected static String getFirstIgnoringCase(String parameter, Map<String, String[]> params) {
    String value = getFirst(params, parameter);
    if (StringUtils.isNotEmpty(value)) {
      return value;
    }
    value = getFirst(params, parameter.toLowerCase());
    if (StringUtils.isNotEmpty(value)) {
      return value;
    }
    value = getFirst(params, parameter.toUpperCase());
    if (StringUtils.isNotEmpty(value)) {
      return value;
    }
    if (!params.isEmpty()) {
      // try with proper case insensitive search
      Map<String, String> loweredParams = params.keySet().stream()
          .collect(Collectors.toMap(
              String::toLowerCase,
              k -> k,
              (existing, replacement) -> existing // keep first
          ));
      if (loweredParams.containsKey(parameter.toLowerCase())) {
        value = getFirst(params, loweredParams.get(parameter.toLowerCase()));
        if (StringUtils.isNotEmpty(value)) {
          return value;
        }
      }
    }

    return null;
  }
}
