package org.gbif.ws.server.provider;

import org.gbif.api.vocabulary.Country;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.inject.Injectable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CountryProviderTest {

  private final CountryProvider provider = new CountryProvider();

  @Test
  public void testGetInjectable() {
    ComponentContext mockComponentContext = mock(ComponentContext.class);
    Context mockContext = mock(Context.class);

    Injectable<Country> injectable = provider.getInjectable(mockComponentContext, mockContext, Country.class);
    assertNotNull(injectable);

    injectable = provider.getInjectable(mockComponentContext, mockContext, String.class);
    assertNull(injectable);
  }

  @Test
  public void testScope() {
    assertEquals(ComponentScope.PerRequest, provider.getScope());
  }

  @Test
  public void testGetValue() {
    // mock context and request
    HttpContext ctx = mock(HttpContext.class);
    HttpRequestContext req = mock(HttpRequestContext.class);
    when(ctx.getRequest()).thenReturn(req);

    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    when(req.getQueryParameters()).thenReturn(params);

    // test with no country requested
    assertNull(provider.getValue(ctx));

    // test with one ico2 code requested
    params.putSingle("country", Country.FRANCE.getIso2LetterCode());
    assertEquals(Country.FRANCE, provider.getValue(ctx));

    // test with an ico3 code requested
    params.putSingle("country", Country.FRANCE.getIso3LetterCode());
    assertEquals(Country.FRANCE, provider.getValue(ctx));

    // test with enum name requested
    params.putSingle("country", Country.FRANCE.name());
    assertEquals(Country.FRANCE, provider.getValue(ctx));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadValue() {
    // mock context and request
    HttpContext ctx = mock(HttpContext.class);
    HttpRequestContext req = mock(HttpRequestContext.class);
    when(ctx.getRequest()).thenReturn(req);

    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    when(req.getQueryParameters()).thenReturn(params);

    // test with bad values requested
    params.putSingle("country", "xyz");
    provider.getValue(ctx);
  }
}
