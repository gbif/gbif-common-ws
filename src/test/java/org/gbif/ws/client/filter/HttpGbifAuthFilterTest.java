package org.gbif.ws.client.filter;

import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.security.GbifAuthServiceTest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientRequestAdapter;
import com.sun.jersey.core.header.OutBoundHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpGbifAuthFilterTest {

  SimplePrincipalProvider pp = new SimplePrincipalProvider();
  Object entity;
  OutBoundHeaders headers;
  URI uri;

  @Mock
  ClientRequestAdapter origAdapter;
  @Mock
  ClientRequest mockRequest;
  ArgumentCaptor<ClientRequestAdapter> captor = ArgumentCaptor.forClass(ClientRequestAdapter.class);

  @Before
  public void setUp() throws URISyntaxException {
    headers = new OutBoundHeaders();
    entity = "Simsalabim";
    uri = new URI("http://localhost/dataset");
  }

  @Test
  public void testHandleWithoutPrincipal() throws Exception {
    pp.setPrincipal(null);

    execute();

    // no principal set, expect no headers
    assertNull(headers.getFirst(GbifAuthService.HEADER_CONTENT_MD5));
    assertNull(headers.getFirst(GbifAuthService.HEADER_GBIF_USER));
    assertNull(headers.getFirst(GbifAuthService.HEADER_AUTHORIZATION));
  }

  // we need to run the filter but also the added adapter!
  private void execute() throws IOException {
    GbifAuthService authService = GbifAuthService.singleKeyAuthService(GbifAuthServiceTest.APPKEY, GbifAuthServiceTest.APPSECRET);
    HttpGbifAuthFilter filter = new HttpGbifAuthFilter(authService, pp);
    try {
      filter.handle(mockRequest);
    } catch (NullPointerException e) {
      // expected as the filter is not initialised with a client setting the next filter head
    }
  }

  @Test
  public void testHandleWithPrincipal() throws Exception {
    when(mockRequest.getURI()).thenReturn(uri);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getEntity()).thenReturn(entity);
    // we instantiate a real request here, because there is no implementation of MultivaluedMap<String, Object>
    when(mockRequest.getHeaders()).thenReturn(headers);

    pp.setPrincipal("Søren");

    execute();

    // principal set, expect headers
    assertNotNull(headers.getFirst(GbifAuthService.HEADER_GBIF_USER));
    assertNotNull(headers.getFirst(GbifAuthService.HEADER_AUTHORIZATION));
    assertNotNull(headers.getFirst(GbifAuthService.HEADER_CONTENT_MD5));

    assertEquals("Søren", headers.getFirst(GbifAuthService.HEADER_GBIF_USER));
    assertTrue(headers.getFirst(GbifAuthService.HEADER_AUTHORIZATION).toString()
      .startsWith("GBIF " + GbifAuthServiceTest.APPKEY));
  }

}
