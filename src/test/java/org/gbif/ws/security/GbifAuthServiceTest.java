/*
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
package org.gbif.ws.security;

import org.gbif.ws.client.GbifAuthRequestInterceptor;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import feign.Request.HttpMethod;
import feign.RequestTemplate;

import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_DATE;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;
import static org.gbif.ws.util.SecurityConstants.HEADER_ORIGINAL_REQUEST_METHOD;
import static org.gbif.ws.util.SecurityConstants.HEADER_ORIGINAL_REQUEST_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class GbifAuthServiceTest {

  public static final String APPKEY = "appKey";
  private static final String SECRET = "fghj56g66b676DFG";
  private static final String USER = "heinz";
  private static final String BODY = "{\"title\":\"Simsalabim\"}";
  private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

  private GbifAuthServiceImpl service;
  private SigningService signingService;
  private Md5EncodeService md5EncodeService;

  @BeforeEach
  public void setUp() throws Exception {
    signingService = prepareSigningService();
    md5EncodeService = new Md5EncodeServiceImpl(JacksonJsonObjectMapperProvider.getObjectMapper());
    service = serviceAt(NOW);
  }

  public static GbifAuthService prepareGbifAuthService() throws Exception {
    return new GbifAuthServiceImpl(
        prepareSigningService(),
        new Md5EncodeServiceImpl(JacksonJsonObjectMapperProvider.getObjectMapper()),
        () -> APPKEY);
  }

  private static SigningService prepareSigningService() throws Exception {
    URL resource = GbifAuthServiceTest.class.getClassLoader().getResource("appkeys.properties");
    String stringPath = Paths.get(resource.toURI()).toAbsolutePath().toString();

    AppkeysConfigurationProperties mockAppkeysConfiguration =
        mock(AppkeysConfigurationProperties.class);
    when(mockAppkeysConfiguration.getFile()).thenReturn(stringPath);

    return new AppKeySigningService(new FileSystemKeyStore(mockAppkeysConfiguration));
  }

  private GbifAuthServiceImpl serviceAt(Instant instant) {
    return new GbifAuthServiceImpl(
        signingService,
        md5EncodeService,
        () -> APPKEY,
        Duration.ofMinutes(15),
        Collections.singletonList(GbifAuthServiceImpl.DEFAULT_FORWARDED_AUTH_PATH),
        Clock.fixed(instant, ZoneOffset.UTC));
  }

  private static GbifHttpServletRequestWrapper wrap(
      String method, String uri, String body, Map<String, String> headers) {
    int idx = uri.indexOf('?');
    MockHttpServletRequest request =
        new MockHttpServletRequest(method, idx < 0 ? uri : uri.substring(0, idx));
    if (idx >= 0) {
      request.setQueryString(uri.substring(idx + 1));
    }
    if (body != null) {
      request.setContent(body.getBytes(StandardCharsets.UTF_8));
      request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    }
    headers.forEach(request::addHeader);
    return new GbifHttpServletRequestWrapper(request, true);
  }

  /** Signs the request the same way GbifAuthRequestInterceptor does. */
  private Map<String, String> sign(
      String method, String url, String body, Instant instant, String user) {
    RequestDataToSign data = new RequestDataToSign();
    data.setMethod(method);
    data.setUrl(url);
    data.setUser(user);
    String date = GbifAuthUtils.formatDate(Clock.fixed(instant, ZoneOffset.UTC));
    data.setDate(date);
    HttpHeaders headers = new HttpHeaders();
    if (body != null) {
      String md5 = md5EncodeService.encode(body.getBytes(StandardCharsets.UTF_8));
      data.setContentType("application/json");
      data.setContentTypeMd5(md5);
      headers.set(HEADER_CONTENT_MD5, md5);
    }
    headers.set(HEADER_GBIF_USER, user);
    headers.set(HEADER_GBIF_DATE, date);
    headers.set(AUTHORIZATION, "GBIF " + APPKEY + ":" + signingService.buildSignature(data, APPKEY));
    return headers.toSingleValueMap();
  }

  @Test
  public void testSignRequest() {
    GbifHttpServletRequestWrapper request =
        wrap("POST", "/v1/dataset", BODY, Collections.emptyMap());

    service.signRequest(USER, request);

    HttpHeaders headers = request.getHttpHeaders();
    assertNotNull(headers.getFirst(HEADER_CONTENT_MD5));
    assertNotNull(headers.getFirst(HEADER_GBIF_DATE));
    assertEquals(USER, headers.getFirst(HEADER_GBIF_USER));
    assertTrue(headers.getFirst(AUTHORIZATION).startsWith("GBIF appKey:"));
  }

  @Test
  public void testIsValid() {
    GbifHttpServletRequestWrapper request =
        wrap("POST", "/v1/dataset", BODY, Collections.emptyMap());
    service.signRequest(USER, request);
    assertTrue(service.isValidRequest(request));

    // tampered Content-MD5 header
    request.getHttpHeaders().set(HEADER_CONTENT_MD5, "73");
    assertFalse(service.isValidRequest(request));
  }

  @Test
  public void testClientSignatureIsValid() {
    assertTrue(service.isValidRequest(wrap("GET", "/dataset", null, sign("GET", "/dataset", null, NOW, USER))));
    assertTrue(service.isValidRequest(wrap("POST", "/dataset", BODY, sign("POST", "/dataset", BODY, NOW, USER))));
  }

  @Test
  public void testRawBodyMd5IsAccepted() {
    // non Java clients hash the raw body
    String rawMd5 = Base64.getEncoder().encodeToString(DigestUtils.md5(BODY.getBytes(StandardCharsets.UTF_8)));
    RequestDataToSign data = new RequestDataToSign();
    data.setMethod("POST");
    data.setUrl("/dataset");
    data.setContentType("application/json");
    data.setContentTypeMd5(rawMd5);
    data.setUser(USER);
    data.setDate(GbifAuthUtils.formatDate(Clock.fixed(NOW, ZoneOffset.UTC)));
    HttpHeaders headers = new HttpHeaders();
    headers.set(HEADER_CONTENT_MD5, rawMd5);
    headers.set(HEADER_GBIF_USER, USER);
    headers.set(HEADER_GBIF_DATE, data.getDate());
    headers.set(AUTHORIZATION, "GBIF " + APPKEY + ":" + signingService.buildSignature(data, APPKEY));

    assertTrue(service.isValidRequest(wrap("POST", "/dataset", BODY, headers.toSingleValueMap())));
  }

  @Test
  public void testReplayOutsideWindowIsRejected() {
    Map<String, String> headers = sign("GET", "/dataset", null, NOW, USER);

    assertTrue(serviceAt(NOW.plus(Duration.ofMinutes(14))).isValidRequest(wrap("GET", "/dataset", null, headers)));
    assertFalse(serviceAt(NOW.plus(Duration.ofMinutes(16))).isValidRequest(wrap("GET", "/dataset", null, headers)));
    assertFalse(serviceAt(NOW.minus(Duration.ofMinutes(16))).isValidRequest(wrap("GET", "/dataset", null, headers)));
  }

  @Test
  public void testMissingOrTamperedDateIsRejected() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAll(sign("GET", "/dataset", null, NOW, USER));

    headers.remove(HEADER_GBIF_DATE);
    assertFalse(service.isValidRequest(wrap("GET", "/dataset", null, headers.toSingleValueMap())));

    // a date within the window but not the signed one
    headers.set(HEADER_GBIF_DATE, GbifAuthUtils.formatDate(Clock.fixed(NOW.plusSeconds(60), ZoneOffset.UTC)));
    assertFalse(service.isValidRequest(wrap("GET", "/dataset", null, headers.toSingleValueMap())));
  }

  @Test
  public void testSwappedBodyIsRejected() {
    Map<String, String> headers = sign("POST", "/dataset", BODY, NOW, USER);
    assertFalse(service.isValidRequest(wrap("POST", "/dataset", "{\"title\":\"evil\"}", headers)));
    // body removed
    assertFalse(service.isValidRequest(wrap("POST", "/dataset", "", headers)));
  }

  @Test
  public void testBodyAddedToBodylessSignatureIsRejected() {
    Map<String, String> headers = sign("POST", "/dataset/1/endorse", null, NOW, USER);
    assertTrue(service.isValidRequest(wrap("POST", "/dataset/1/endorse", null, headers)));
    assertFalse(service.isValidRequest(wrap("POST", "/dataset/1/endorse", BODY, headers)));
  }

  @Test
  public void testSpoofedOriginalUrlAndMethodAreRejected() {
    // signature captured for GET /dataset, replayed with x-url/x-method against another resource
    HttpHeaders headers = new HttpHeaders();
    headers.setAll(sign("GET", "/dataset", null, NOW, USER));
    headers.set(HEADER_ORIGINAL_REQUEST_URL, "/dataset");
    headers.set(HEADER_ORIGINAL_REQUEST_METHOD, "GET");

    assertTrue(service.isValidRequest(wrap("GET", "/dataset", null, headers.toSingleValueMap())));
    assertFalse(service.isValidRequest(wrap("DELETE", "/dataset", null, headers.toSingleValueMap())));
    assertFalse(service.isValidRequest(wrap("GET", "/user/admin", null, headers.toSingleValueMap())));
  }

  @Test
  public void testOriginalUrlWithProxyPrefixIsAccepted() {
    // the proxy strips /v1, clients like the portal send the full public URL in x-url and sign its
    // canonical form: the path and the sorted query string
    HttpHeaders headers = new HttpHeaders();
    headers.setAll(sign("GET", "/v1/dataset?limit=1", null, NOW, USER));
    headers.set(HEADER_ORIGINAL_REQUEST_URL, "https://api.gbif.org/v1/dataset?limit=1");

    assertTrue(service.isValidRequest(wrap("GET", "/dataset?limit=1", null, headers.toSingleValueMap())));
    assertFalse(service.isValidRequest(wrap("GET", "/set?limit=1", null, headers.toSingleValueMap())));
  }

  @Test
  public void testForwardedAuthRequestHonoursOriginalUrlAndMethod() {
    // remote auth forwards the original method and url to the registry
    HttpHeaders headers = new HttpHeaders();
    headers.setAll(sign("PUT", "/dataset/1", BODY, NOW, USER));
    headers.set(HEADER_ORIGINAL_REQUEST_URL, "/dataset/1");
    headers.set(HEADER_ORIGINAL_REQUEST_METHOD, "PUT");
    headers.set(HttpHeaders.CONTENT_TYPE, "application/json");

    assertTrue(service.isValidRequest(wrap("POST", "/user/auth/app", null, headers.toSingleValueMap())));
  }

  @Test
  public void testIsOriginalUrlValid() {
    assertTrue(GbifAuthUtils.isOriginalUrlValid("/dataset", "/dataset"));
    assertTrue(GbifAuthUtils.isOriginalUrlValid("/dataset/", "/dataset"));
    assertTrue(GbifAuthUtils.isOriginalUrlValid("/v1/dataset", "/dataset"));
    assertTrue(GbifAuthUtils.isOriginalUrlValid("http://api.gbif.org/v1/directory/person/1", "/person/1"));
    assertTrue(GbifAuthUtils.isOriginalUrlValid("admin/user/x", "/admin/user/x"));
    assertFalse(GbifAuthUtils.isOriginalUrlValid("min/user/x", "/admin/user/x"));
    assertFalse(GbifAuthUtils.isOriginalUrlValid("/v1/mydataset", "/dataset"));
    assertFalse(GbifAuthUtils.isOriginalUrlValid("/dataset", "/v1/dataset"));
    assertFalse(GbifAuthUtils.isOriginalUrlValid("/dataset/../user", "/dataset"));
    assertFalse(GbifAuthUtils.isOriginalUrlValid("/dataset", "/"));
  }

  /** Signs with the Feign client interceptor, returning the headers it set. */
  private Map<String, String> clientSign(String method, String url, String body, Instant instant) {
    RequestTemplate template = new RequestTemplate();
    template.method(HttpMethod.valueOf(method));
    template.uri(url);
    if (body != null) {
      template.header(HttpHeaders.CONTENT_TYPE, "application/json");
      template.body(body);
    }
    new GbifAuthRequestInterceptor(
            USER,
            APPKEY,
            SECRET,
            new SecretKeySigningService(),
            md5EncodeService,
            Clock.fixed(instant, ZoneOffset.UTC))
        .apply(template);
    HttpHeaders headers = new HttpHeaders();
    template.headers().forEach((name, values) -> headers.addAll(name, List.copyOf(values)));
    return headers.toSingleValueMap();
  }

  @Test
  public void testClientInterceptorSignatureIsValid() {
    assertTrue(
        service.isValidRequest(
            wrap("GET", "/dataset?q=a b&limit=1", null, clientSign("GET", "/dataset?q=a%20b&limit=1", null, NOW))));
    assertTrue(
        service.isValidRequest(wrap("POST", "/dataset", BODY, clientSign("POST", "/dataset", BODY, NOW))));
    assertFalse(
        service.isValidRequest(
            wrap("POST", "/dataset", "{}", clientSign("POST", "/dataset", BODY, NOW))));
  }

  @Test
  public void testClientInterceptorRetryReplacesHeaders() {
    RequestTemplate template = new RequestTemplate();
    template.method(HttpMethod.GET);
    template.uri("/dataset");
    GbifAuthRequestInterceptor interceptor =
        new GbifAuthRequestInterceptor(
            USER, APPKEY, SECRET, new SecretKeySigningService(), md5EncodeService);
    interceptor.apply(template);
    interceptor.apply(template);

    assertEquals(1, template.headers().get(AUTHORIZATION).size());
    assertEquals(1, template.headers().get(HEADER_GBIF_DATE).size());
  }

  @Test
  public void testQueryStringIsSigned() {
    Map<String, String> headers = clientSign("GET", "/occurrence/search?taxonKey=1&country=DK", null, NOW);

    assertTrue(service.isValidRequest(wrap("GET", "/occurrence/search?taxonKey=1&country=DK", null, headers)));
    // sorted by the proxy and encoded differently
    assertTrue(service.isValidRequest(wrap("GET", "/occurrence/search?country=%44K&taxonKey=1", null, headers)));
    // tampered, added or removed parameters
    assertFalse(service.isValidRequest(wrap("GET", "/occurrence/search?taxonKey=2&country=DK", null, headers)));
    assertFalse(service.isValidRequest(wrap("GET", "/occurrence/search?taxonKey=1&country=DK&limit=300", null, headers)));
    assertFalse(service.isValidRequest(wrap("GET", "/occurrence/search?taxonKey=1", null, headers)));
    assertFalse(service.isValidRequest(wrap("GET", "/occurrence/search", null, headers)));
    // malformed query string
    assertFalse(service.isValidRequest(wrap("GET", "/occurrence/search?taxonKey=%zz", null, headers)));
  }

  @Test
  public void testQueryStringIsSignedWithOriginalUrl() {
    // the query string in x-url is not trusted, the real one is signed
    Map<String, String> signed = clientSign("GET", "/v1/dataset?limit=1", null, NOW);
    HttpHeaders headers = new HttpHeaders();
    headers.setAll(signed);
    headers.set(HEADER_ORIGINAL_REQUEST_URL, "https://api.gbif.org/v1/dataset?limit=1");

    assertTrue(service.isValidRequest(wrap("GET", "/dataset?limit=1", null, headers.toSingleValueMap())));
    assertFalse(service.isValidRequest(wrap("GET", "/dataset?limit=1000", null, headers.toSingleValueMap())));
  }

  @Test
  public void testForwardedAuthRequestSignsOriginalQueryString() {
    Map<String, String> signed = clientSign("GET", "/dataset?limit=1", null, NOW);
    HttpHeaders headers = new HttpHeaders();
    headers.setAll(signed);
    headers.set(HEADER_ORIGINAL_REQUEST_METHOD, "GET");

    headers.set(HEADER_ORIGINAL_REQUEST_URL, "/dataset?limit=1");
    assertTrue(service.isValidRequest(wrap("POST", "/user/auth/app", null, headers.toSingleValueMap())));
    headers.set(HEADER_ORIGINAL_REQUEST_URL, "/dataset?limit=1000");
    assertFalse(service.isValidRequest(wrap("POST", "/user/auth/app", null, headers.toSingleValueMap())));
  }

  @Test
  public void testCanonicalizedQuery() {
    assertEquals("", GbifAuthUtils.getCanonicalizedQuery(null));
    assertEquals("", GbifAuthUtils.getCanonicalizedQuery(""));
    assertEquals("a=0&a=1&b=2&flag=", GbifAuthUtils.getCanonicalizedQuery("b=2&a=1&flag&a=0"));
    assertEquals("q=a%20b", GbifAuthUtils.getCanonicalizedQuery("q=a+b"));
    assertEquals("q=a%20b", GbifAuthUtils.getCanonicalizedQuery("q=a%20b"));
    assertEquals("q=%26~%2A", GbifAuthUtils.getCanonicalizedQuery("q=%26%7E*"));
    assertEquals("/dataset", GbifAuthUtils.getCanonicalizedUrl("/dataset", "&"));
  }

  @Test
  public void testGetAppKeyFromRequest() {
    GbifHttpServletRequestWrapper request =
        wrap("POST", "/v1/dataset", BODY, Collections.emptyMap());

    service.signRequest(USER, request);

    assertEquals(APPKEY, GbifAuthUtils.getAppKeyFromRequest(request.getHeader(AUTHORIZATION)));
  }
}
