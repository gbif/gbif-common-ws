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
import org.gbif.ws.server.provider.SearchRequestProviderTest.TestSearchParameter;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.inject.Injectable;
import org.junit.Test;
import org.mockito.Mockito;

import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACETS_ONLY;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MULTISELECT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class FacetedSearchRequestProviderTest {

  private final FacetedSearchRequestProvider provider =
    new FacetedSearchRequestProvider(FacetedSearchRequest.class, TestSearchParameter.class);

  @Test
  public void testGetInjectable() {
    ComponentContext mockComponentContext = Mockito.mock(ComponentContext.class);
    Context mockContext = Mockito.mock(Context.class);

    Injectable<FacetedSearchRequest> injectable =
      provider.getInjectable(mockComponentContext, mockContext, FacetedSearchRequest.class);
    assertNotNull(injectable);

    injectable = provider.getInjectable(mockComponentContext, mockContext, String.class);
    assertNull(injectable);
  }

  @Test
  public void testGetValue() {
    // mock context and request
    HttpContext ctx = Mockito.mock(HttpContext.class);
    HttpRequestContext req = Mockito.mock(HttpRequestContext.class);
    Mockito.when(ctx.getRequest()).thenReturn(req);

    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    Mockito.when(req.getQueryParameters()).thenReturn(params);

    // test with real facets
    params.clear();
    params.add(PARAM_FACET, TestSearchParameter.FIRSTNAME.name());
    params.add(PARAM_FACET, TestSearchParameter.LASTNAME.name());
    params.add(PARAM_FACETS_ONLY, Boolean.TRUE.toString());
    params.add(PARAM_FACET_MULTISELECT, Boolean.TRUE.toString());
    FacetedSearchRequest<TestSearchParameter> x = (FacetedSearchRequest<TestSearchParameter>) provider.getValue(ctx);
    assertTrue(x.getFacets().contains(TestSearchParameter.FIRSTNAME));
    assertTrue(x.getFacets().contains(TestSearchParameter.LASTNAME));
    assertTrue(x.isFacetsOnly());
    assertTrue(x.isMultiSelectFacets());

    // test with real facets, case insensitive
    params.clear();
    params.add(PARAM_FACET, "firstname");
    params.add(PARAM_FACET, "LastName");
    x = (FacetedSearchRequest<TestSearchParameter>) provider.getValue(ctx);
    assertTrue(x.getFacets().contains(TestSearchParameter.FIRSTNAME));
    assertTrue(x.getFacets().contains(TestSearchParameter.LASTNAME));

    // test with wrong facet names
    params.clear();
    params.add(PARAM_FACET, "noname");
    params.add(PARAM_FACET, "bisrtname");
    params.add(PARAM_FACETS_ONLY, Boolean.TRUE.toString());
    x = (FacetedSearchRequest<TestSearchParameter>) provider.getValue(ctx);
    assertTrue(x.getFacets().isEmpty());

  }

  @Test
  public void testScope() {
    assertEquals(ComponentScope.PerRequest, provider.getScope());
  }
}
