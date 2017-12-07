package org.gbif.ws.server.provider;


import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.inject.Injectable;
import org.gbif.api.model.common.paging.Pageable;
import org.junit.Test;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import static org.gbif.api.model.common.paging.PagingConstants.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PageableProviderTest {

  private final PageableProvider provider = new PageableProvider();

  @Test
  public void testGetInjectable() {
    ComponentContext mockComponentContext = mock(ComponentContext.class);
    Context mockContext = mock(Context.class);

    Injectable<Pageable> injectable = provider.getInjectable(mockComponentContext, mockContext, Pageable.class);
    assertNotNull(injectable);

    injectable = provider.getInjectable(mockComponentContext, mockContext, String.class);
    assertNull(injectable);
  }

  @Test
  public void testGetValue() {
    // mock context and request
    HttpContext ctx = mock(HttpContext.class);
    HttpRequestContext req = mock(HttpRequestContext.class);
    when(ctx.getRequest()).thenReturn(req);

    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    when(req.getQueryParameters()).thenReturn(params);

    // test with no params
    assertEquals(DEFAULT_PARAM_OFFSET, provider.getValue(ctx).getOffset());
    assertEquals(DEFAULT_PARAM_LIMIT, provider.getValue(ctx).getLimit());

    // test with params
    params.add(PARAM_OFFSET, "200");
    params.add(PARAM_LIMIT, "10");
    assertEquals(200, provider.getValue(ctx).getOffset());
    assertEquals(10, provider.getValue(ctx).getLimit());

    // test with bad content
    params = new MultivaluedMapImpl();
    when(req.getQueryParameters()).thenReturn(params);
    params.add(PARAM_OFFSET, "NOT VALID");
    params.add(PARAM_LIMIT, "-20");
    assertEquals(DEFAULT_PARAM_OFFSET, provider.getValue(ctx).getOffset());
    assertEquals(DEFAULT_PARAM_LIMIT, provider.getValue(ctx).getLimit());

    // try to use insane limits
    params.putSingle(PARAM_LIMIT, "20000");
    assertEquals(DEFAULT_PARAM_OFFSET, provider.getValue(ctx).getOffset());
    assertEquals(PageableProvider.LIMIT_CAP, provider.getValue(ctx).getLimit());
  }

  @Test
  public void testScope() {
    assertEquals(ComponentScope.PerRequest, provider.getScope());
  }
}
