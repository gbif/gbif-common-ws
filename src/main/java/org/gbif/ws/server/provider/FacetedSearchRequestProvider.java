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

import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;

import java.lang.reflect.Type;
import java.util.List;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.base.Strings;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.spi.inject.InjectableProvider;

import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACETS_ONLY;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MINCOUNT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MULTISELECT;

/**
 * Provider class that transforms a set of HTTP parameters into a FacetedSearchRequest class instance.
 * This assumes the existence of the following parameters in the HTTP request:
 * 'page_size', 'offset', 'facet', 'q' and any of the search parameter enum member names case insensitively.
 */
public class FacetedSearchRequestProvider<RT extends FacetedSearchRequest<P>, P extends Enum<?> & SearchParameter>
  extends SearchRequestProvider<RT, P> implements InjectableProvider<Context, Type> {

  public FacetedSearchRequestProvider(Class<RT> requestType, Class<P> searchParameterClass) {
    super(requestType, searchParameterClass);
  }

  @Override
  protected RT getSearchRequest(HttpContext context, RT searchRequest) {
    RT request = super.getSearchRequest(context, searchRequest);

    final MultivaluedMap<String, String> params = context.getRequest().getQueryParameters();

    final String facetsOnlyValue = params.getFirst(PARAM_FACETS_ONLY);
    if (!Strings.isNullOrEmpty(facetsOnlyValue)) {
      searchRequest.setFacetsOnly(Boolean.parseBoolean(facetsOnlyValue));
    }

    final String facetMultiSelectValue = params.getFirst(PARAM_FACET_MULTISELECT);
    if (!Strings.isNullOrEmpty(facetMultiSelectValue)) {
      searchRequest.setMultiSelectFacets(Boolean.parseBoolean(facetMultiSelectValue));
    }

    final String facetMinCountValue = params.getFirst(PARAM_FACET_MINCOUNT);
    if (!Strings.isNullOrEmpty(facetMinCountValue)) {
      searchRequest.setFacetMinCount(Integer.parseInt(facetMinCountValue));
    }

    final List<String> facets = params.get(PARAM_FACET);
    if (facets != null && !facets.isEmpty()) {
      for (String f : facets) {
        P p = findSearchParam(f);
        if (p != null) {
          searchRequest.addFacets(p);
        }
      }
    }

    return request;
  }
}
