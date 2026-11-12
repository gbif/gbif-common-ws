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

import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME_PREFIX;
import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_DATE;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;
import static org.gbif.ws.util.SecurityConstants.HEADER_ORIGINAL_REQUEST_METHOD;
import static org.gbif.ws.util.SecurityConstants.HEADER_ORIGINAL_REQUEST_URL;

/**
 * The GBIF authentication scheme is modelled after the Amazon scheme on how to sign REST HTTP
 * requests using a private key. It uses the standard HTTP Authorization header to transport the
 * following information: Authorization: GBIF applicationKey:signature
 *
 * <p><br>
 * The header starts with the authentication scheme (GBIF), followed by the plain applicationKey
 * (the public key) and a unique signature for the very request which is generated using a fixed set
 * of request attributes which are then encrypted by a standard HMAC-SHA256 algorithm.
 *
 * <p><br>
 * A POST request with a GBIF header would look like this:
 *
 * <pre>
 * POST /dataset HTTP/1.1
 * Host: api.gbif.org
 * x-gbif-date: Mon, 26 Mar 2007 19:37:58 GMT
 * x-gbif-user: trobertson
 * Content-MD5: LiFThEP4Pj2TODQXa/oFPg==
 * Authorization: GBIF gbif.portal:frJIUN8DYpKDtOLCwo//yllqDzg=
 * </pre>
 *
 * <p>When signing an HTTP request in addition to the Authorization header some additional custom
 * headers are added which are used to sign and digest the message. <br> x-gbif-user is added to
 * transport a proxied user in which the application is acting. <br> x-gbif-date is the RFC 1123 date
 * of the request, requests outside the allowed clock skew are rejected to limit replays. <br>
 * Content-MD5 is added if a body entity exists and is verified against the actual body when the
 * request content is wrapped. See Content-MD5 header specs: http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.15
 *
 * <p>The x-url and x-method headers are only honoured as-is on the forwarded authentication paths
 * (e.g. the registry's /user/auth/app used by remote authentication). On any other path x-method
 * must match the real method and x-url must refer to the real path.
 */
@Service
public class GbifAuthServiceImpl implements GbifAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(GbifAuthServiceImpl.class);

  private static final Pattern COLON_PATTERN = Pattern.compile(":");

  public static final long DEFAULT_MAX_CLOCK_SKEW_SECONDS = 15 * 60;
  public static final String DEFAULT_FORWARDED_AUTH_PATH = "/user/auth/app";

  private final SigningService signingService;
  private final Md5EncodeService md5EncodeService;
  private final AppKeyProvider appKeyProvider;
  private final Duration maxClockSkew;
  private final List<String> forwardedAuthPaths;
  private final Clock clock;

  public GbifAuthServiceImpl(
      SigningService signingService,
      Md5EncodeService md5EncodeService,
      AppKeyProvider appKeyProvider) {
    this(
        signingService,
        md5EncodeService,
        appKeyProvider,
        DEFAULT_MAX_CLOCK_SKEW_SECONDS,
        new String[] {DEFAULT_FORWARDED_AUTH_PATH});
  }

  @Autowired
  public GbifAuthServiceImpl(
      SigningService signingService,
      Md5EncodeService md5EncodeService,
      @Autowired(required = false) AppKeyProvider appKeyProvider,
      @Value("${gbif.ws.security.gbifAuth.maxClockSkewSeconds:" + DEFAULT_MAX_CLOCK_SKEW_SECONDS + "}")
          long maxClockSkewSeconds,
      @Value("${gbif.ws.security.gbifAuth.forwardedAuthPaths:" + DEFAULT_FORWARDED_AUTH_PATH + "}")
          String[] forwardedAuthPaths) {
    this(
        signingService,
        md5EncodeService,
        appKeyProvider,
        Duration.ofSeconds(maxClockSkewSeconds),
        Arrays.asList(forwardedAuthPaths),
        Clock.systemUTC());
  }

  GbifAuthServiceImpl(
      SigningService signingService,
      Md5EncodeService md5EncodeService,
      AppKeyProvider appKeyProvider,
      Duration maxClockSkew,
      List<String> forwardedAuthPaths,
      Clock clock) {
    this.signingService = signingService;
    this.md5EncodeService = md5EncodeService;
    this.appKeyProvider = appKeyProvider;
    this.maxClockSkew = maxClockSkew;
    this.forwardedAuthPaths = forwardedAuthPaths;
    this.clock = clock;
  }

  @Override
  public boolean isValidRequest(final GbifHttpServletRequestWrapper request) {
    // parse auth header
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith(GBIF_SCHEME_PREFIX)) {
      LOG.info("{} header is no GBIF scheme", HttpHeaders.AUTHORIZATION);
      return false;
    }

    String[] values = COLON_PATTERN.split(authHeader.substring(5), 2);
    if (values.length < 2) {
      LOG.warn("Invalid syntax for application key and signature");
      return false;
    }

    final String appKey = values[0];
    final String signatureFound = values[1];
    if (appKey == null || signatureFound == null) {
      LOG.warn("Authentication header missing applicationKey or signature");
      return false;
    }

    final HttpHeaders headers = request.getHttpHeaders();
    if (!GbifAuthUtils.isDateWithinWindow(headers.getFirst(HEADER_GBIF_DATE), clock, maxClockSkew)) {
      LOG.info(
          "Missing, invalid or expired {} header: {}", HEADER_GBIF_DATE, headers.getFirst(HEADER_GBIF_DATE));
      return false;
    }

    final RequestDataToSign requestDataToSign;
    try {
      boolean forwarded = isForwardedAuthRequest(request);
      if (!forwarded
          && !(isOriginalRequestValid(request, headers) && isContentValid(request, headers))) {
        return false;
      }
      requestDataToSign = buildRequestDataToSign(request, forwarded);
    } catch (IllegalArgumentException e) {
      LOG.info("Invalid request URL or query string: {}", e.getMessage());
      return false;
    }
    LOG.debug("Request data to sign: {}", requestDataToSign.stringToSign());
    // sign
    final String signature;
    try {
      signature = signingService.buildSignature(requestDataToSign, appKey);
    } catch (PrivateKeyNotFoundException e) {
      LOG.debug("Private key was not found for app key {}", appKey);
      return false;
    }
    // compare signatures; use MessageDigest.isEquals to avoid timing attacks
    if (MessageDigest.isEqual(signatureFound.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
      LOG.debug("Trusted application with matching signatures");
      return true;
    }
    LOG.info("Invalid signature for app key {}", appKey);

    return false;
  }

  /**
   * Forwarded authentication requests (remote auth) carry the method and URL of the original
   * request in the x-method and x-url headers, the forwarding service is responsible to bind them
   * to the original request.
   */
  private boolean isForwardedAuthRequest(final GbifHttpServletRequestWrapper request) {
    String path = GbifAuthUtils.getCanonicalizedPath(request.getRequestURI());
    return path != null && forwardedAuthPaths.stream().anyMatch(path::endsWith);
  }

  /**
   * The x-method and x-url headers must refer to the real request, otherwise any signed request
   * could be replayed against any other path or method.
   */
  private boolean isOriginalRequestValid(
      final GbifHttpServletRequestWrapper request, final HttpHeaders headers) {
    String originalMethod = headers.getFirst(HEADER_ORIGINAL_REQUEST_METHOD);
    if (originalMethod != null && !originalMethod.equalsIgnoreCase(request.getMethod())) {
      LOG.info(
          "{} header {} does not match request method {}",
          HEADER_ORIGINAL_REQUEST_METHOD,
          originalMethod,
          request.getMethod());
      return false;
    }
    String originalUrl = headers.getFirst(HEADER_ORIGINAL_REQUEST_URL);
    String requestPath = GbifAuthUtils.getCanonicalizedPath(request.getRequestURI());
    if (originalUrl != null && !GbifAuthUtils.isOriginalUrlValid(originalUrl, requestPath)) {
      LOG.info(
          "{} header {} does not match request path {}",
          HEADER_ORIGINAL_REQUEST_URL,
          originalUrl,
          requestPath);
      return false;
    }
    return true;
  }

  /**
   * Verifies the Content-MD5 header against the actual body, so the body can't be swapped under a
   * valid signature. Only possible when the request content is wrapped.
   */
  private boolean isContentValid(
      final GbifHttpServletRequestWrapper request, final HttpHeaders headers) {
    byte[] body = request.getContentBytes();
    if (body == null) {
      LOG.debug("Request content not wrapped, {} can't be verified", HEADER_CONTENT_MD5);
      return true;
    }
    String contentMd5 = headers.getFirst(HEADER_CONTENT_MD5);
    if (body.length == 0 && contentMd5 == null) {
      return true;
    }
    if (!GbifAuthUtils.isContentMd5Valid(contentMd5, body, md5EncodeService)) {
      LOG.info("{} header {} does not match the request body", HEADER_CONTENT_MD5, contentMd5);
      return false;
    }
    return true;
  }

  /**
   * The signed URL is the path, or the original path from x-url when present (the proxy can strip
   * a prefix), followed by the canonical query string of the request. Forwarded authentication
   * requests carry the original query string in x-url, the real one belongs to the forwarding call.
   */
  private RequestDataToSign buildRequestDataToSign(
      final GbifHttpServletRequestWrapper request, boolean forwarded) {
    final HttpHeaders headers = request.getHttpHeaders();
    final RequestDataToSign dataToSign = new RequestDataToSign();

    // custom header to keep the original method in the case the request is forwarded as the remote
    // auth does
    if (headers.containsKey(HEADER_ORIGINAL_REQUEST_METHOD)) {
      dataToSign.setMethod(headers.getFirst(HEADER_ORIGINAL_REQUEST_METHOD));
    } else {
      dataToSign.setMethod(request.getMethod());
    }

    String originalUrl = headers.getFirst(HEADER_ORIGINAL_REQUEST_URL);
    if (originalUrl == null) {
      dataToSign.setUrl(
          GbifAuthUtils.getCanonicalizedUrl(
              GbifAuthUtils.getCanonicalizedPath(request.getRequestURI()),
              request.getQueryString()));
    } else {
      dataToSign.setUrl(
          GbifAuthUtils.getCanonicalizedUrl(
              GbifAuthUtils.getCanonicalizedPath(originalUrl),
              forwarded ? URI.create(originalUrl).getRawQuery() : request.getQueryString()));
    }
    dataToSign.setContentType(headers.getFirst(HttpHeaders.CONTENT_TYPE));
    dataToSign.setContentTypeMd5(headers.getFirst(HEADER_CONTENT_MD5));
    dataToSign.setUser(headers.getFirst(HEADER_GBIF_USER));
    dataToSign.setDate(headers.getFirst(HEADER_GBIF_DATE));

    return dataToSign;
  }

  /**
   * Signs a request by adding a Content-MD5 and Authorization header. For PUT/POST requests that
   * contain a body entity the Content-MD5 header is created using the same JSON mapper for
   * serialization as the clients use.
   *
   * <p>Other formats than JSON are not supported currently !!!
   */
  @Override
  public GbifHttpServletRequestWrapper signRequest(
      final String username, final GbifHttpServletRequestWrapper request) {
    String appKey = appKeyProvider.get();
    Objects.requireNonNull(appKey, "To sign the request a single application key is required");
    // first add custom GBIF headers so we can use them to build the string to sign
    // the proxied username
    request.getHttpHeaders().add(HEADER_GBIF_USER, username);
    request.getHttpHeaders().add(HEADER_GBIF_DATE, GbifAuthUtils.formatDate(clock));

    // the canonical path header
    request
        .getHttpHeaders()
        .add(
            HEADER_ORIGINAL_REQUEST_URL,
            GbifAuthUtils.getCanonicalizedPath(request.getRequestURI()));

    // adds content md5, encoded the same way as the GbifAuthRequestInterceptor does
    byte[] content = request.getContentBytes();
    if (content != null && content.length > 0) {
      request.getHttpHeaders().add(HEADER_CONTENT_MD5, md5EncodeService.encode(content));
    }

    // build the unique request data object to sign
    final RequestDataToSign requestDataToSign =
        buildRequestDataToSign(request, isForwardedAuthRequest(request));

    // sign
    final String signature;
    try {
      signature = signingService.buildSignature(requestDataToSign, appKey);
    } catch (PrivateKeyNotFoundException e) {
      LOG.warn("Skip signing request with unknown application key: {}", appKey);
      return request;
    }

    // build authorization header string
    final String header = buildAuthHeader(appKey, signature);
    // add authorization header
    LOG.debug(
        "Adding authentication header to request {} for proxied user {} with app key {}",
        request.getRequestURI(),
        username,
        appKey);
    request.getHttpHeaders().add(HttpHeaders.AUTHORIZATION, header);

    return request;
  }

  private static String buildAuthHeader(String applicationKey, String signature) {
    return GBIF_SCHEME_PREFIX + applicationKey + ':' + signature;
  }
}
