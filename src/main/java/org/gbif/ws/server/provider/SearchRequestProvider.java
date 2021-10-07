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
package org.gbif.ws.server.provider;

import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.util.SearchTypeValidator;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.ws.CommonRuntimeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.WebRequest;

import static org.gbif.ws.util.CommonWsUtils.getFirst;
import static org.gbif.ws.util.WebserviceParameter.PARAM_HIGHLIGHT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_QUERY_STRING;
import static org.gbif.ws.util.WebserviceParameter.PARAM_SPELLCHECK;
import static org.gbif.ws.util.WebserviceParameter.PARAM_SPELLCHECK_COUNT;

/**
 * Provider class that transforms a set of HTTP parameters into a SearchRequest class instance.
 * This assumes the existence of the following parameters in the HTTP request:
 * 'page_size', 'offset', 'q' and any of the search parameter enum member names case insensitively.
 */
public class SearchRequestProvider<RT extends SearchRequest<P>, P extends Enum<?> & SearchParameter>
    implements ContextProvider<RT> {

  private static final int MAX_PAGE_SIZE = 1000;
  private static final int NON_SPELL_CHECK_COUNT = -1;

  private final Class<P> searchParameterClass;
  private final Class<RT> requestType;
  private final Integer maxPageSize;

  public SearchRequestProvider(Class<RT> requestType, Class<P> searchParameterClass) {
    this.requestType = requestType;
    this.searchParameterClass = searchParameterClass;
    this.maxPageSize = MAX_PAGE_SIZE;
  }

  public SearchRequestProvider(
      Class<RT> requestType, Class<P> searchParameterClass, Integer maxPageSize) {
    this.requestType = requestType;
    this.searchParameterClass = searchParameterClass;
    this.maxPageSize = maxPageSize;
  }

  @Override
  public RT getValue(WebRequest webRequest) {
    try {
      RT req = requestType.newInstance();
      return getSearchRequest(webRequest, req);
    } catch (InstantiationException | IllegalAccessException e) {
      // should never happen
      throw new CommonRuntimeException(e);
    }
  }

  protected P findSearchParam(String name) {
    try {
      return VocabularyUtils.lookupEnum(name, searchParameterClass);
    } catch (IllegalArgumentException e) {
      // we have all params here, not only the enum ones, so this is ok to end up here a few times
    }
    return null;
  }

  protected RT getSearchRequest(WebRequest webRequest, RT searchRequest) {
    searchRequest.copyPagingValues(PageableProvider.getPagingRequest(webRequest, maxPageSize));

    final Map<String, String[]> params = webRequest.getParameterMap();

    getSearchRequestFromQueryParams(searchRequest, params);

    return searchRequest;
  }

  /**
   * Override this method for populating specific search/suggest requests
   */
  protected void getSearchRequestFromQueryParams(
      RT searchRequest, final Map<String, String[]> params) {
    final String q = getFirst(params, PARAM_QUERY_STRING);
    final String highlightValue = getFirst(params, PARAM_HIGHLIGHT);
    final String spellCheck = getFirst(params, PARAM_SPELLCHECK);
    final String spellCheckCount = getFirst(params, PARAM_SPELLCHECK_COUNT);

    if (StringUtils.isNotEmpty(q)) {
      searchRequest.setQ(q);
    }

    if (StringUtils.isNotEmpty(highlightValue)) {
      searchRequest.setHighlight(Boolean.parseBoolean(highlightValue));
    }

    if (StringUtils.isNotEmpty(spellCheck)) {
      searchRequest.setSpellCheck(Boolean.parseBoolean(spellCheck));
    }

    if (StringUtils.isNotEmpty(spellCheckCount)) {
      searchRequest.setSpellCheckCount(Integer.parseInt(spellCheckCount));
    } else {
      searchRequest.setSpellCheckCount(NON_SPELL_CHECK_COUNT);
    }

    // find search parameter enum based filters
    setSearchParams(searchRequest, params);
  }

  /**
   * Removes all empty and null parameters from the list.
   * Each value is trimmed(String.trim()) in order to remove all sizes of empty parameters.
   */
  private static List<String> removeEmptyParameters(List<String> parameters) {
    List<String> cleanParameters = new ArrayList<>(parameters.size());
    for (String param : parameters) {
      String cleanParam = StringUtils.trimToEmpty(param);
      if (!cleanParam.isEmpty()) {
        cleanParameters.add(cleanParam);
      }
    }
    return cleanParameters;
  }

  /**
   * Iterates over the params map and adds to the search request the recognized parameters (i.e.: those that have a
   * correspondent value in the P generic parameter).
   * Empty (of all size) and null parameters are discarded.
   */
  private void setSearchParams(RT searchRequest, Map<String, String[]> params) {
    for (Entry<String, String[]> entry : params.entrySet()) {
      P p = findSearchParam(entry.getKey());
      if (p != null) {
        final List<String> list =
            entry.getValue() != null ? Arrays.asList(entry.getValue()) : Collections.emptyList();
        for (String val : removeEmptyParameters(list)) {
          // validate value for certain types
          SearchTypeValidator.validate(p, val);
          searchRequest.addParameter(p, val);
        }
      }
    }
  }
}
