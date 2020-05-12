package org.gbif.ws.client;

import feign.RequestTemplate;

public final class ClientUtils {

  private ClientUtils() {}

  public static boolean isPostOrPutRequest(RequestTemplate template) {
    return "POST".equals(template.method()) || "PUT".equals(template.method());
  }

  public static boolean isRequestBodyNotEmpty(RequestTemplate template) {
    return template.requestBody().length() != 0;
  }
}
