package org.gbif.ws.security;

import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;

import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RunWith(MockitoJUnitRunner.class)
public class GbifAuthServiceTest {

  public static final String APPKEY = "appKey";

  private URI testUri;
  private GbifAuthService service;

  @Before
  public void setUp() throws Exception {
    testUri = new URI("http://api.gbif.org/v1/dataset");
    service = prepareGbifAuthService();
  }

  public static GbifAuthService prepareGbifAuthService() throws Exception {
    URL resource = GbifAuthServiceTest.class.getClassLoader().getResource("appkeys.properties");
    String stringPath = Paths.get(resource.toURI()).toAbsolutePath().toString();

    AppkeysConfigurationProperties mockAppkeysConfiguration = mock(AppkeysConfigurationProperties.class);
    when(mockAppkeysConfiguration.getFile()).thenReturn(stringPath);

    return new GbifAuthServiceImpl(
        new AppKeySigningService(new FileSystemKeyStore(mockAppkeysConfiguration)),
        new Md5EncodeServiceImpl(JacksonJsonObjectMapperProvider.getObjectMapper()),
        () -> APPKEY
    );
  }

  @Test
  public void testSignRequest() {
    // given
    GbifHttpServletRequestWrapper requestObjectMock = mock(GbifHttpServletRequestWrapper.class);
    HttpHeaders headers = new HttpHeaders();
    when(requestObjectMock.getRequestURI()).thenReturn(testUri.toString());
    when(requestObjectMock.getHttpHeaders()).thenReturn(headers);
    when(requestObjectMock.getMethod()).thenReturn("POST");
    when(requestObjectMock.getContent()).thenReturn("Simsalabim");

    // when
    service.signRequest("heinz", requestObjectMock);

    // then
    assertNotNull(headers.getFirst(HEADER_CONTENT_MD5));
    assertEquals("heinz", headers.getFirst(HEADER_GBIF_USER));
    assertNotNull(headers.getFirst(AUTHORIZATION));
    assertTrue(headers.getFirst(AUTHORIZATION).startsWith("GBIF appKey:"));
  }

  @Test
  public void testIsValid() {
    // given
    GbifHttpServletRequestWrapper requestObjectMock = mock(GbifHttpServletRequestWrapper.class);
    HttpHeaders headers = new HttpHeaders();
    when(requestObjectMock.getRequestURI()).thenReturn(testUri.toString());
    when(requestObjectMock.getHttpHeaders()).thenReturn(headers);
    when(requestObjectMock.getMethod()).thenReturn("POST");
    when(requestObjectMock.getContent()).thenReturn("Simsalabim");
    when(requestObjectMock.getHeader(AUTHORIZATION)).thenCallRealMethod();
    service.signRequest("heinz", requestObjectMock);

    // when
    boolean isRequestValidCorrectContentActual = service.isValidRequest(requestObjectMock);
    headers.set(HEADER_CONTENT_MD5, "73");
    boolean isRequestValidWrongContentActual = service.isValidRequest(requestObjectMock);

    // then
    assertTrue(isRequestValidCorrectContentActual);
    assertFalse(service.isValidRequest(requestObjectMock));
  }

  @Test
  public void testGetAppKeyFromRequest() {
    // given
    GbifHttpServletRequestWrapper requestObjectMock = mock(GbifHttpServletRequestWrapper.class);
    HttpHeaders headers = new HttpHeaders();
    when(requestObjectMock.getRequestURI()).thenReturn(testUri.toString());
    when(requestObjectMock.getHttpHeaders()).thenReturn(headers);
    when(requestObjectMock.getMethod()).thenReturn("POST");
    when(requestObjectMock.getContent()).thenReturn("Simsalabim");
    when(requestObjectMock.getHeader(AUTHORIZATION)).thenCallRealMethod();

    // when
    service.signRequest("heinz", requestObjectMock);
    String actualAppKey = GbifAuthUtils.getAppKeyFromRequest(requestObjectMock.getHeader(AUTHORIZATION));

    // then
    assertEquals(APPKEY, actualAppKey);
  }
}
