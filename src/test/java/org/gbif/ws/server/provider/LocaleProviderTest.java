package org.gbif.ws.server.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.core.Context;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocaleProviderTest {

  private final LocaleProvider provider = new LocaleProvider();

  @Test
  public void testGetInjectable() {
    ComponentContext mockComponentContext = mock(ComponentContext.class);
    Context mockContext = mock(Context.class);

    Injectable<Locale> injectable = provider.getInjectable(mockComponentContext, mockContext, Locale.class);
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
    List<Locale> locales = new ArrayList<Locale>();
    when(req.getAcceptableLanguages()).thenReturn(locales);

    // test with no language requested
    assertNull(provider.getValue(ctx));

    // test with one language requested
    locales.add(Locale.FRENCH);
    assertEquals(Locale.FRENCH, provider.getValue(ctx));

    // test with two languages requested
    locales.add(Locale.GERMANY);
    assertEquals(Locale.FRENCH, provider.getValue(ctx));

    // test with any language locale
    locales.clear();
    locales.add(new Locale("*"));
    assertNull(provider.getValue(ctx));

    // test with bad languages
    locales.clear();
    locales.add(new Locale("German"));
    assertNull(provider.getValue(ctx));

    // test with upper case language
    locales.clear();
    locales.add(new Locale("DE"));
    assertEquals(Locale.GERMAN, provider.getValue(ctx));
  }
}
