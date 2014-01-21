package org.gbif.ws.client.filter;

import org.gbif.ws.security.GbifAppAuthService;
import org.gbif.ws.security.GbifAppAuthServiceTest;

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
import org.mockito.runners.MockitoJUnitRunner;

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
  public void setupMock() throws URISyntaxException {
    headers = new OutBoundHeaders();
    entity = "Simsalabim";
    uri = new URI("http://localhost/dataset");
    when(mockRequest.getURI()).thenReturn(uri);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getEntity()).thenReturn(entity);
    // we instantiate a real request here, because there is no implementation of MultivaluedMap<String, Object>
    when(mockRequest.getHeaders()).thenReturn(headers);
    when(mockRequest.getAdapter()).thenReturn(origAdapter);
  }

  @Test
  public void testHandleWithoutPrincipal() throws Exception {
    pp.setPrincipal(null);

    execute();

    // no principal set, expect no headers
    assertNull(headers.getFirst(GbifAppAuthService.HEADER_GBIF_CONTENT_HASH));
    assertNull(headers.getFirst(GbifAppAuthService.HEADER_GBIF_DATE));
    assertNull(headers.getFirst(GbifAppAuthService.HEADER_GBIF_USER));
    assertNull(headers.getFirst(GbifAppAuthService.HEADER_AUTHORIZATION));
  }

  // we need to run the filter but also the added adapter!
  private void execute() throws IOException {
    GbifAppAuthService authService = new GbifAppAuthService(GbifAppAuthServiceTest.buildAppKeyMap());
    HttpGbifAuthFilter filter = new HttpGbifAuthFilter(GbifAppAuthServiceTest.APPKEY, authService, pp);
    try {
      filter.handle(mockRequest);
    } catch (NullPointerException e) {
      // expected as the filter is not initialised with a client setting the next filter head
    }
  }

  @Test
  public void testHandleWithPrincipal() throws Exception {
    pp.setPrincipal("Søren");

    execute();

    // principal set, expect headers
    assertNotNull(headers.getFirst(GbifAppAuthService.HEADER_GBIF_CONTENT_HASH));
    assertNotNull(headers.getFirst(GbifAppAuthService.HEADER_GBIF_DATE));
    assertNotNull(headers.getFirst(GbifAppAuthService.HEADER_GBIF_USER));
    assertNotNull(headers.getFirst(GbifAppAuthService.HEADER_AUTHORIZATION));

    assertEquals("Søren", headers.getFirst(GbifAppAuthService.HEADER_GBIF_USER));
    assertTrue(headers.getFirst(GbifAppAuthService.HEADER_AUTHORIZATION).toString()
      .startsWith("GBIF " + GbifAppAuthServiceTest.APPKEY));
  }

}
