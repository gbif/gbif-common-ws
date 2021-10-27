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
package org.gbif.ws.remoteauth.basic;

import org.gbif.ws.remoteauth.AbstractRemoteAuthenticationProvider;
import org.gbif.ws.remoteauth.RemoteAuthClient;
import org.gbif.ws.security.GbifAuthenticationToken;
import org.gbif.ws.security.GbifUserPrincipal;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import lombok.extern.slf4j.Slf4j;

/**
 * Basic authentication against the registry.
 */
@Slf4j
public class BasicRemoteAuthenticationProvider
    extends AbstractRemoteAuthenticationProvider<UsernamePasswordAuthenticationToken> {

  private static final String AUTH_PATH = "/user/auth/basic";

  public BasicRemoteAuthenticationProvider(RemoteAuthClient remoteAuthClient) {
    super(UsernamePasswordAuthenticationToken.class, AUTH_PATH, remoteAuthClient);
  }

  @Override
  public HttpHeaders createHttpHeaders(Authentication authentication) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth(authentication.getName(), authentication.getCredentials().toString());
    return headers;
  }

  @Override
  protected Authentication createSuccessAuthentication(
      ResponseEntity<String> response, Authentication authentication) {
    GbifUserPrincipal gbifUserPrincipal =
        new GbifUserPrincipal(readUserFromResponse(response).toGbifUser());
    return new GbifAuthenticationToken(gbifUserPrincipal, gbifUserPrincipal.getAuthorities());
  }
}
