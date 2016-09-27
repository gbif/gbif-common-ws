/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
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

import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.util.VocabularyUtils;

import java.lang.reflect.Type;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import com.google.common.base.Strings;
import com.google.inject.Singleton;
import com.sun.jersey.spi.inject.InjectableProvider;

import static org.gbif.ws.util.WebserviceParameter.PARAM_EXTENDED;
import static org.gbif.ws.util.WebserviceParameter.PARAM_HIGHLIGHT_CONTEXT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_HIGHLIGHT_FIELD;
import static org.gbif.ws.util.WebserviceParameter.PARAM_QUERY_FIELD;

@Provider
@Singleton
public class NameUsageSearchRequestProvider extends FacetedSearchRequestProvider<NameUsageSearchRequest, NameUsageSearchParameter>
  implements InjectableProvider<Context, Type> {

  public NameUsageSearchRequestProvider() {
    super(NameUsageSearchRequest.class, NameUsageSearchParameter.class);
  }

  @Override
  protected void getSearchRequestFromQueryParams(NameUsageSearchRequest request, MultivaluedMap<String, String> params) {
    super.getSearchRequestFromQueryParams(request, params);

    String p = params.getFirst(PARAM_EXTENDED);
    if (!Strings.isNullOrEmpty(p)) {
      request.setExtended(Boolean.parseBoolean(p));
    }

    p = params.getFirst(PARAM_QUERY_FIELD);
    if (!Strings.isNullOrEmpty(p)) {
      request.getQueryFields().clear();
      request.getQueryFields().add(VocabularyUtils.lookupEnum(p, NameUsageSearchRequest.QueryField.class));
    }

    p = params.getFirst(PARAM_HIGHLIGHT_CONTEXT);
    if (!Strings.isNullOrEmpty(p)) {
      request.setHighlightContext(Integer.parseInt(p));
    }

    if (params.containsKey(PARAM_HIGHLIGHT_FIELD)) {
      request.getHighlightFields().clear();
      for (String val : params.get(PARAM_HIGHLIGHT_FIELD)) {
        if (!Strings.isNullOrEmpty(val)) {
          request.getHighlightFields().add(VocabularyUtils.lookupEnum(val, NameUsageSearchRequest.QueryField.class));
        }
      }
    }
  }
}
