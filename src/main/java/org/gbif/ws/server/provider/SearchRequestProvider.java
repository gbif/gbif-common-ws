/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

import static org.gbif.ws.util.WebserviceParameter.PARAM_HIGHLIGHT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_QUERY_STRING;

/**
 * Provider class that transforms a set of HTTP parameters into a SearchRequest class instance.
 * This assumes the existence of the following parameters in the HTTP request:
 * 'page_size', 'offset', 'q' and any of the search parameter enum member names case insensitively.
 */
public class SearchRequestProvider<RT extends SearchRequest<P>, P extends Enum<?> & SearchParameter>
  extends AbstractHttpContextInjectable<RT> implements InjectableProvider<Context, Type> {

  private final Class<P> searchParameterClass;
  private final Class<RT> requestType;

  public SearchRequestProvider(Class<RT> requestType, Class<P> searchParameterClass) {
    this.requestType = requestType;
    this.searchParameterClass = searchParameterClass;
  }

  /**
   * Get an injectable.
   * 
   * @param ic the injectable context
   * @param context the annotation instance
   * @param type the context instance
   * @return an Injectable instance, otherwise null if an instance cannot
   *         be created.
   */
  @Override
  public Injectable<RT> getInjectable(ComponentContext ic, Context context, Type type) {
    if (type.equals(requestType)) {
      return this;
    }
    return null;
  }

  /**
   * Get the scope of the injectable provider.
   * 
   * @return the scope.
   */
  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  public RT getValue(HttpContext context) {
    try {
      RT req = requestType.newInstance();
      return getSearchRequest(context, req);
    } catch (InstantiationException e) {
    } catch (IllegalAccessException e) {
    }
    return null;
  }

  protected P findSearchParam(String name) {
    try {
      return (P) VocabularyUtils.lookupEnum(name, searchParameterClass);
    } catch (IllegalArgumentException e) {
      // we have all params here, not only the enum ones, so this is ok to end up here a few times
    }
    return null;
  }

  protected RT getSearchRequest(HttpContext context, RT searchRequest) {
    searchRequest.copyPagingValues(PageableProvider.getPagingRequest(context));

    final MultivaluedMap<String, String> params = context.getRequest().getQueryParameters();
    final String q = params.getFirst(PARAM_QUERY_STRING);
    final String highlightValue = params.getFirst(PARAM_HIGHLIGHT);

    if (!Strings.isNullOrEmpty(q)) {
      searchRequest.setQ(q);
    }
    if (!Strings.isNullOrEmpty(highlightValue)) {
      searchRequest.setHighlight(Boolean.parseBoolean(highlightValue));
    }
    // find search parameter enum based filters
    setSearchParams(searchRequest, params);

    return searchRequest;
  }

  /**
   * Removes all empty and null parameters from the list.
   * Each value is trimmed(String.trim()) in order to remove all sizes of empty parameters.
   */
  private List<String> removeEmptyParameters(List<String> parameters) {
    List<String> cleanParameters = Lists.newArrayList();
    for (String param : parameters) {
      final String cleanParam = Strings.nullToEmpty(param).trim();
      if (cleanParam.length() > 0) {
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
  private void setSearchParams(RT searchRequest, MultivaluedMap<String, String> params) {
    for (Entry<String, List<String>> entry : params.entrySet()) {
      P p = findSearchParam(entry.getKey());
      if (p != null) {
        for (String val : removeEmptyParameters(entry.getValue())) {
          // validate value for certain types
          SearchTypeValidator.validate(p, val);
          searchRequest.addParameter(p, val);
        }
      }
    }
  }
}
