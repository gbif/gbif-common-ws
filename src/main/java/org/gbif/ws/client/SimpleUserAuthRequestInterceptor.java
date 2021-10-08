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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * A small request interceptor for a single, fixed user. Request Interceptor adding HTTP Basic Authentication header
 * to the HTTP request. Analogue of Jersey's HTTPBasicAuthFilter.
 *
 * If application need to write to webservices, e.g the registry, you need to make sure the user has admin rights.
 */
public class SimpleUserAuthRequestInterceptor implements RequestInterceptor {

  private final String authentication;
  private static final Charset CHARACTER_SET = StandardCharsets.ISO_8859_1;

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

    authentication =
        "Basic "
            + new String(Base64.getEncoder().encode(usernamePassword), StandardCharsets.US_ASCII);
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
