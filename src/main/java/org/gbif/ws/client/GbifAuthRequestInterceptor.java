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
package org.gbif.ws.client;

import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.GbifAuthUtils;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.PrivateKeyNotFoundException;
import org.gbif.ws.security.RequestDataToSign;
import org.gbif.ws.security.SigningService;

import java.time.Clock;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import static org.gbif.ws.client.ClientUtils.isPostOrPutRequest;
import static org.gbif.ws.client.ClientUtils.isRequestBodyNotEmpty;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_DATE;

/**
 * An authentication request interceptor for trusted GBIF applications.
 * It requires an application key and can then act on behalf of any username.
 *
 * Request interceptor adding an HTTP Authentication header to the HTTP request using the custom GBIF schema
 * for trusted applications.
 * In addition to the Authentication this request interceptor will add these headers to the request:
 * <ul>
 * <li>Content-MD5: the MD5 hash for the request body</li>
 * <li>x-gbif-user: the username of the proxied user</li>
 * <li>x-gbif-date: the RFC 1123 date of the request, servers reject signatures outside a time window</li>
 * </ul>
 *
 * Note that the users role still depends on the roles defined in the user account and will not default
 * to ADMIN. The user also has to exist for the webservice authorization to work.
 */
public class GbifAuthRequestInterceptor implements RequestInterceptor {

  private static final Logger LOG = LoggerFactory.getLogger(GbifAuthRequestInterceptor.class);

  private SigningService signingService;
  private Md5EncodeService md5EncodeService;
  private String username;
  private String appKey;
  private String secretKey;
  private Clock clock;

  public GbifAuthRequestInterceptor(
      String username,
      String appKey,
      String secretKey,
      SigningService signingService,
      Md5EncodeService md5EncodeService) {
    this(username, appKey, secretKey, signingService, md5EncodeService, Clock.systemUTC());
  }

  public GbifAuthRequestInterceptor(
      String username,
      String appKey,
      String secretKey,
      SigningService signingService,
      Md5EncodeService md5EncodeService,
      Clock clock) {
    this.clock = clock;
    this.signingService = signingService;
    this.md5EncodeService = md5EncodeService;
    this.username = username;
    this.appKey = appKey;
    this.secretKey = secretKey;
  }

  @Override
  public void apply(RequestTemplate template) {
    RequestDataToSign requestDataToSign = new RequestDataToSign();
    requestDataToSign.setMethod(template.method());
    requestDataToSign.setUrl(canonicalizeUrl(template.url()));
    requestDataToSign.setUser(username);
    String date = GbifAuthUtils.formatDate(clock);
    requestDataToSign.setDate(date);

    if (isPostOrPutRequest(template) && isRequestBodyNotEmpty(template)) {
      Map<String, Collection<String>> headers = template.headers();

      Collection<String> contentTypeHeaders = headers.get(HttpHeaders.CONTENT_TYPE);

      String contentType =
          (contentTypeHeaders != null && !contentTypeHeaders.isEmpty())
              ? contentTypeHeaders.iterator().next()
              : "application/json";
      requestDataToSign.setContentType(contentType);

      String contentMd5 = md5EncodeService.encode(template.body());
      requestDataToSign.setContentTypeMd5(contentMd5);

      template.removeHeader("Content-MD5");
      template.header("Content-MD5", contentMd5);
    }

    LOG.debug("Client data to sign: {}", requestDataToSign.stringToSign());

    try {
      String signature = signingService.buildSignature(requestDataToSign, secretKey);

      // replace rather than append, interceptors are applied again on retries
      template.removeHeader("x-gbif-user");
      template.removeHeader(HEADER_GBIF_DATE);
      template.removeHeader("Authorization");
      template.header("x-gbif-user", username);
      template.header(HEADER_GBIF_DATE, date);
      template.header("Authorization", "GBIF " + appKey + ":" + signature);
    } catch (PrivateKeyNotFoundException e) {
      LOG.debug("Private key was not found for the application {}", appKey);
      throw new WebApplicationException(
          "Private key was not found for the application " + appKey, HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * The path and the canonical query string, see {@link GbifAuthUtils#getCanonicalizedQuery}.
   */
  private String canonicalizeUrl(String url) {
    int idx = url.indexOf('?');
    return idx < 0
        ? url
        : GbifAuthUtils.getCanonicalizedUrl(url.substring(0, idx), url.substring(idx + 1));
  }
}
