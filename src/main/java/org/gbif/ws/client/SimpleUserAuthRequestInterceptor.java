package org.gbif.ws.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A small request interceptor for a single, fixed user. Request Interceptor adding HTTP Basic Authentication header
 * to the HTTP request. Analogue of Jersey's HTTPBasicAuthFilter.
 *
 * If application need to write to webservices, e.g the registry, you need to make sure the user has admin rights.
 */
public class SimpleUserAuthRequestInterceptor implements RequestInterceptor {

  private final String authentication;
  static private final Charset CHARACTER_SET = StandardCharsets.ISO_8859_1;

  /**
   * Creates a new request interceptor using provided username
   * and password credentials. This constructor allows you to avoid storing
   * plain password value in a String variable.
   */
  public SimpleUserAuthRequestInterceptor(final String username, final byte[] password) {
    final byte[] prefix = (username + ":").getBytes(CHARACTER_SET);
    final byte[] usernamePassword = new byte[prefix.length + password.length];

    System.arraycopy(prefix, 0, usernamePassword, 0, prefix.length);
    System.arraycopy(password, 0, usernamePassword, prefix.length, password.length);

    authentication = "Basic " + new String(Base64.getEncoder().encode(usernamePassword), StandardCharsets.US_ASCII);
  }

  /**
   * Creates a new request interceptor using provided username
   * and password credentials.
   */
  public SimpleUserAuthRequestInterceptor(final String username, final String password) {
    this(username, password.getBytes(CHARACTER_SET));
  }

  @Override
  public void apply(RequestTemplate template) {
    template.header("Authorization", authentication);
  }
}
