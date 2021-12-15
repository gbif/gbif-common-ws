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
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.PrivateKeyNotFoundException;
import org.gbif.ws.security.RequestDataToSign;
import org.gbif.ws.security.SigningService;

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

  public GbifAuthRequestInterceptor(
      String username,
      String appKey,
      String secretKey,
      SigningService signingService,
      Md5EncodeService md5EncodeService) {
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
    requestDataToSign.setUrl(removeQueryParameters(template.url()));
    requestDataToSign.setUser(username);

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

      template.header("Content-MD5", contentMd5);
    }

    LOG.debug("Client data to sign: {}", requestDataToSign.stringToSign());

    try {
      String signature = signingService.buildSignature(requestDataToSign, secretKey);

      template.header("x-gbif-user", username);
      template.header("Authorization", "GBIF " + appKey + ":" + signature);
    } catch (PrivateKeyNotFoundException e) {
      LOG.debug("Private key was not found for the application {}", appKey);
      throw new WebApplicationException(
          "Private key was not found for the application " + appKey, HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Remove query parameters from the URL.
   */
  private String removeQueryParameters(String url) {
    return url.split("\\?")[0];
  }
}
