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
import org.gbif.api.vocabulary.Country;

import java.util.UUID;
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

import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_LIMIT;
import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_OFFSET;
import static org.gbif.api.model.common.paging.PagingConstants.PARAM_LIMIT;
import static org.gbif.api.model.common.paging.PagingConstants.PARAM_OFFSET;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACETS_ONLY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SearchRequestProviderTest {

  public enum TestSearchParameter implements SearchParameter {
    FIRSTNAME (String.class),
    LASTNAME (String.class),
    DATASET_KEY (UUID.class),
    COUNTRY (Country.class);

    private final Class<?> type;

    TestSearchParameter(Class<?> type) {
      this.type = type;
    }

    public Class<?> type() {
      return type;
    }
  }

  private final SearchRequestProvider provider =
    new SearchRequestProvider(SearchRequest.class, TestSearchParameter.class);

  /**
   * Tests that empty parameters are excluded by the SearchRequestProvider.
   */
  @Test
  public void testEmptyParameter() {
    // mock context and request
    HttpContext ctx = Mockito.mock(HttpContext.class);
    HttpRequestContext req = Mockito.mock(HttpRequestContext.class);
    Mockito.when(ctx.getRequest()).thenReturn(req);
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    Mockito.when(req.getQueryParameters()).thenReturn(params);
    params.add(TestSearchParameter.FIRSTNAME.name(), "");
    params.add(TestSearchParameter.FIRSTNAME.name(), null);
    assertTrue(provider.getValue(ctx).getParameters().get(TestSearchParameter.FIRSTNAME).isEmpty());
  }

  @Test
  public void testTypedParameterValidation() {
    // mock context and request
    HttpContext ctx = Mockito.mock(HttpContext.class);
    HttpRequestContext req = Mockito.mock(HttpRequestContext.class);
    Mockito.when(ctx.getRequest()).thenReturn(req);
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    Mockito.when(req.getQueryParameters()).thenReturn(params);
    params.add(TestSearchParameter.COUNTRY.name(), "FR");
    final UUID uuid = UUID.randomUUID();
    params.add(TestSearchParameter.DATASET_KEY.name(), uuid.toString());

    SearchRequest<TestSearchParameter> sr = provider.getValue(ctx);

    assertEquals(1, sr.getParameters().get(TestSearchParameter.COUNTRY).size());
    assertEquals(Country.FRANCE.getIso2LetterCode(), sr.getParameters().get(TestSearchParameter.COUNTRY).iterator().next());

    assertEquals(1, sr.getParameters().get(TestSearchParameter.DATASET_KEY).size());
    assertEquals(uuid.toString(), sr.getParameters().get(TestSearchParameter.DATASET_KEY).iterator().next());
  }

  /**
   * Tests iso codes for country params
   */
  @Test(expected = IllegalArgumentException.class)
  public void testTypedParameterValidation2() {
    // mock context and request
    HttpContext ctx = Mockito.mock(HttpContext.class);
    HttpRequestContext req = Mockito.mock(HttpRequestContext.class);
    Mockito.when(ctx.getRequest()).thenReturn(req);
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    Mockito.when(req.getQueryParameters()).thenReturn(params);
    params.add(TestSearchParameter.COUNTRY.name(), "FRANCE");
    SearchRequest<TestSearchParameter> sr = provider.getValue(ctx);
  }

  @Test
  public void testGetInjectable() {
    ComponentContext mockComponentContext = Mockito.mock(ComponentContext.class);
    Context mockContext = Mockito.mock(Context.class);

    Injectable<SearchRequest> injectable =
      provider.getInjectable(mockComponentContext, mockContext, SearchRequest.class);
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

    // test with no params
    assertEquals(DEFAULT_PARAM_OFFSET, provider.getValue(ctx).getOffset());
    assertEquals(DEFAULT_PARAM_LIMIT, provider.getValue(ctx).getLimit());

    // test with params
    params.add(PARAM_OFFSET, "200");
    params.add(PARAM_LIMIT, "10");
    params.add(TestSearchParameter.FIRSTNAME.name(), "Carla");
    assertEquals(200, provider.getValue(ctx).getOffset());
    assertEquals(10, provider.getValue(ctx).getLimit());
    assertEquals("Carla", provider.getValue(ctx).getParameters().get(TestSearchParameter.FIRSTNAME).iterator().next());
    assertTrue(provider.getValue(ctx).getParameters().get(TestSearchParameter.LASTNAME).isEmpty());

    // test case insensitive parameter names
    params.clear();
    params.add("firstName", "Carla");
    assertEquals("Carla", provider.getValue(ctx).getParameters().get(TestSearchParameter.FIRSTNAME).iterator().next());
    assertTrue(provider.getValue(ctx).getParameters().get(TestSearchParameter.LASTNAME).isEmpty());

    // test with wrong parameter names
    params.clear();
    params.add("coolname", "Carla");
    assertTrue(provider.getValue(ctx).getParameters().isEmpty());
    assertTrue(provider.getValue(ctx).getParameters().get(TestSearchParameter.FIRSTNAME).isEmpty());
    assertTrue(provider.getValue(ctx).getParameters().get(TestSearchParameter.LASTNAME).isEmpty());

    // test with wrong paging params
    params.clear();
    params.add(PARAM_OFFSET, "twenty");
    params.add(PARAM_LIMIT, "-10");
    params.add(PARAM_FACET, "f1");
    params.add(PARAM_FACET, "f2");
    params.add(PARAM_FACETS_ONLY, Boolean.TRUE.toString());
    assertEquals(DEFAULT_PARAM_OFFSET, provider.getValue(ctx).getOffset());
    assertEquals(DEFAULT_PARAM_LIMIT, provider.getValue(ctx).getLimit());
  }

  @Test
  public void testScope() {
    assertEquals(ComponentScope.PerRequest, provider.getScope());
  }
}
