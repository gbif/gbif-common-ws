package org.gbif.ws.server.provider;

import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.inject.Injectable;
import junit.framework.TestCase;
import org.junit.Test;
import org.mockito.Mockito;

import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET;
import static org.gbif.ws.util.WebserviceParameter.PARAM_HIGHLIGHT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_HIGHLIGHT_CONTEXT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_HIGHLIGHT_FIELD;
import static org.gbif.ws.util.WebserviceParameter.PARAM_QUERY_FIELD;

/**
 *
 */
public class NameUsageSearchRequestProviderTest extends TestCase {

  private final NameUsageSearchRequestProvider provider = new NameUsageSearchRequestProvider();

  @Test
  public void testGetInjectable() {
    ComponentContext mockComponentContext = Mockito.mock(ComponentContext.class);
    Context mockContext = Mockito.mock(Context.class);

    Injectable<NameUsageSearchRequest> injectable = provider.getInjectable(mockComponentContext, mockContext, NameUsageSearchRequest.class);
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
    params.add(PARAM_FACET, NameUsageSearchParameter.HABITAT.name());
    params.add(PARAM_FACET, NameUsageSearchParameter.DATASET_KEY.name());
    params.add(PARAM_HIGHLIGHT, Boolean.TRUE.toString());
    params.add(PARAM_HIGHLIGHT_CONTEXT, String.valueOf(250));
    params.add(PARAM_HIGHLIGHT_FIELD, NameUsageSearchRequest.QueryField.DESCRIPTION.name());
    params.add(PARAM_HIGHLIGHT_FIELD, NameUsageSearchRequest.QueryField.VERNACULAR.name());
    params.add(PARAM_QUERY_FIELD, NameUsageSearchRequest.QueryField.VERNACULAR.name());

    NameUsageSearchRequest x = provider.getValue(ctx);
    assertTrue(x.getFacets().contains(NameUsageSearchParameter.HABITAT));
    assertTrue(x.getFacets().contains(NameUsageSearchParameter.DATASET_KEY));
    assertEquals(2, x.getFacets().size());

    assertTrue(x.isHighlight());
    assertEquals((Integer)250, x.getHighlightContext());

    assertEquals(2, x.getHighlightFields().size());
    assertTrue(x.getHighlightFields().contains(NameUsageSearchRequest.QueryField.DESCRIPTION));
    assertTrue(x.getHighlightFields().contains(NameUsageSearchRequest.QueryField.VERNACULAR));

    assertEquals(1, x.getQueryFields().size());
    assertTrue(x.getQueryFields().contains(NameUsageSearchRequest.QueryField.VERNACULAR));
  }

  @Test
  public void testScope() {
    assertEquals(ComponentScope.PerRequest, provider.getScope());
  }
}
