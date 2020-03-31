package org.gbif.ws.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Collection;
import java.util.Map;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.PrivateKeyNotFoundException;
import org.gbif.ws.security.RequestDataToSign;
import org.gbif.ws.security.SigningService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public class GbifAuthRequestInterceptor implements RequestInterceptor {

  private SigningService signingService;
  private Md5EncodeService md5EncodeService;
  private String appKey;
  private String secretKey;

  public GbifAuthRequestInterceptor(
      String appKey,
      String secretKey,
      SigningService signingService,
      Md5EncodeService md5EncodeService) {
    this.signingService = signingService;
    this.md5EncodeService = md5EncodeService;
    this.appKey = appKey;
    this.secretKey = secretKey;
  }

  @Override
  public void apply(RequestTemplate template) {
    RequestDataToSign requestDataToSign = new RequestDataToSign();
    requestDataToSign.setMethod(template.method());
    requestDataToSign.setUrl(removeQueryParameters(template.url()));
    requestDataToSign.setUser(appKey);

    if ("POST".equals(template.method()) || "PUT".equals(template.method())) {
      Map<String, Collection<String>> headers = template.headers();

      Collection<String> contentTypeHeaders = headers.get(HttpHeaders.CONTENT_TYPE);

      String contentType =
          (contentTypeHeaders != null && !contentTypeHeaders.isEmpty())
              ? contentTypeHeaders.iterator().next()
              : "application/json";
      requestDataToSign.setContentType(contentType);

      String contentMd5 = md5EncodeService.encode(template.requestBody().asString());
      requestDataToSign.setContentTypeMd5(contentMd5);

      template.header("Content-MD5", contentMd5);
    }

    try {
      String signature = signingService.buildSignature(requestDataToSign, secretKey);

      template.header("x-gbif-user", appKey);
      template.header("Authorization", "GBIF " + appKey + ":" + signature);
    } catch (PrivateKeyNotFoundException e) {
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
