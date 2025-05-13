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
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME_PREFIX;
import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
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
 * of request attributes which are then encrypted by a standard HMAC-SHA1 algorithm.
 *
 * <p><br>
 * A POST request with a GBIF header would look like this:
 *
 * <pre>
 * POST /dataset HTTP/1.1
 * Host: api.gbif.org
 * Date: Mon, 26 Mar 2007 19:37:58 +0000
 * x-gbif-user: trobertson
 * Content-MD5: LiFThEP4Pj2TODQXa/oFPg==
 * Authorization: GBIF gbif.portal:frJIUN8DYpKDtOLCwo//yllqDzg=
 * </pre>
 *
 * <p>When signing an HTTP request in addition to the Authorization header some additional custom
 * headers are added which are used to sign and digest the message. <br> x-gbif-user is added to
 * transport a proxied user in which the application is acting. <br> Content-MD5 is added if a body
 * entity exists. See Content-MD5 header specs: http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.15
 */
@Service
public class GbifAuthServiceImpl implements GbifAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(GbifAuthServiceImpl.class);

  private static final Pattern COLON_PATTERN = Pattern.compile(":");

  private final SigningService signingService;
  private final Md5EncodeService md5EncodeService;
  private final AppKeyProvider appKeyProvider;

  public GbifAuthServiceImpl(
      SigningService signingService,
      Md5EncodeService md5EncodeService,
      @Autowired(required = false) AppKeyProvider appKeyProvider) {
    this.signingService = signingService;
    this.md5EncodeService = md5EncodeService;
    this.appKeyProvider = appKeyProvider;
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
      LOG.warn("Invalid syntax for application key and signature: {}", authHeader);
      return false;
    }

    final String appKey = values[0];
    final String signatureFound = values[1];
    if (appKey == null || signatureFound == null) {
      LOG.warn("Authentication header missing applicationKey or signature: {}", authHeader);
      return false;
    }

    final RequestDataToSign requestDataToSign = buildRequestDataToSign(request);
    LOG.debug("Request data to sign: {}", requestDataToSign.stringToSign());
    // sign
    final String signature;
    try {
      signature = signingService.buildSignature(requestDataToSign, appKey);
    } catch (PrivateKeyNotFoundException e) {
      LOG.debug("Private key was not found for app key {}", appKey);
      return false;
    }
    // compare signatures
    if (signatureFound.equals(signature)) {
      LOG.debug("Trusted application with matching signatures");
      return true;
    }
    LOG.info("Invalid signature: {}", authHeader);

    return false;
  }

  private RequestDataToSign buildRequestDataToSign(final GbifHttpServletRequestWrapper request) {
    final HttpHeaders headers = request.getHttpHeaders();
    final RequestDataToSign dataToSign = new RequestDataToSign();

    // custom header to keep the original method in the case the request is forwarded as the remote
    // auth does
    if (headers.containsKey(HEADER_ORIGINAL_REQUEST_METHOD)) {
      dataToSign.setMethod(headers.getFirst(HEADER_ORIGINAL_REQUEST_METHOD));
    } else {
      dataToSign.setMethod(request.getMethod());
    }

    // custom header set by varnish overrides real URI
    // see http://dev.gbif.org/issues/browse/GBIFCOM-137
    if (headers.containsKey(HEADER_ORIGINAL_REQUEST_URL)) {
      dataToSign.setUrl(headers.getFirst(HEADER_ORIGINAL_REQUEST_URL));
    } else {
      dataToSign.setUrl(getCanonicalizedPath(request.getRequestURI()));
    }

    String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
    String contentMd5 = headers.getFirst(HEADER_CONTENT_MD5);
    String user = headers.getFirst(HEADER_GBIF_USER);

    dataToSign.setContentType(contentType);
    dataToSign.setContentTypeMd5(contentMd5);
    dataToSign.setUser(user);

    LOG.debug("Content type {}, MD5 {}", contentType, contentMd5);

    return dataToSign;
  }

  /**
   * @return an absolute uri of the resource path alone, excluding host, scheme and query parameters
   */
  private String getCanonicalizedPath(final String strUri) {
    return URI.create(strUri).normalize().getPath();
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

    // the canonical path header
    request
        .getHttpHeaders()
        .add(HEADER_ORIGINAL_REQUEST_URL, getCanonicalizedPath(request.getRequestURI()));

    String content = null;
    if (request.getContent() != null) {
      content = request.getContent();
    }

    // adds content md5
    if (StringUtils.isNotEmpty(content)) {
      request.getHttpHeaders().add(HEADER_CONTENT_MD5, md5EncodeService.encode(content));
    }

    // build the unique request data object to sign
    final RequestDataToSign requestDataToSign = buildRequestDataToSign(request);
    LOG.debug(
        "Request data to sign: {}",
        requestDataToSign.stringToSign());
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
        "Adding authentication header to request {} for proxied user {} : {}",
        request.getRequestURI(),
        username,
        header);
    request.getHttpHeaders().add(HttpHeaders.AUTHORIZATION, header);

    return request;
  }

  private static String buildAuthHeader(String applicationKey, String signature) {
    return GBIF_SCHEME_PREFIX + applicationKey + ':' + signature;
  }
}
