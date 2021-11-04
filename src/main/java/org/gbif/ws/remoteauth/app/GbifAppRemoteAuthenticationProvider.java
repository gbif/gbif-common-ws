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
package org.gbif.ws.remoteauth.app;

import java.util.ArrayList;
import java.util.Collection;

import org.gbif.api.vocabulary.AppRole;
import org.gbif.ws.remoteauth.AbstractRemoteAuthenticationProvider;
import org.gbif.ws.remoteauth.LoggedUser;
import org.gbif.ws.remoteauth.RemoteAuthClient;
import org.gbif.ws.security.AppPrincipal;
import org.gbif.ws.security.GbifAuthenticationToken;
import org.gbif.ws.security.GbifUserPrincipal;
import org.gbif.ws.util.SecurityConstants;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.extern.slf4j.Slf4j;

import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME;

/**
 * GBIF APP authentication against the registry.
 */
@Slf4j
public class GbifAppRemoteAuthenticationProvider
    extends AbstractRemoteAuthenticationProvider<GbifAppAuthentication> {

  private static final String AUTH_PATH = "/user/auth/app";

  public GbifAppRemoteAuthenticationProvider(RemoteAuthClient remoteAuthClient) {
    super(GbifAppAuthentication.class, AUTH_PATH, remoteAuthClient);
  }

  @Override
  public HttpHeaders createHttpHeaders(Authentication authentication) {
    HttpHeaders headers = new HttpHeaders();
    GbifAppAuthentication gbifAppAuthentication = (GbifAppAuthentication) authentication;
    headers.add(HttpHeaders.AUTHORIZATION, gbifAppAuthentication.getGbifScheme());
    headers.add(SecurityConstants.HEADER_CONTENT_MD5, gbifAppAuthentication.getContentMd5());
    if (authentication.getPrincipal() != null) {
      headers.add(SecurityConstants.HEADER_GBIF_USER, authentication.getPrincipal().toString());
    }
    if (gbifAppAuthentication.getOriginalRequestUrl() != null
        && !gbifAppAuthentication.getOriginalRequestUrl().isEmpty()) {
      headers.add(
          SecurityConstants.HEADER_ORIGINAL_REQUEST_URL,
          gbifAppAuthentication.getOriginalRequestUrl());
    }

    log.info("Headers Gbif APP: {}", headers);

    return headers;
  }

  @Override
  protected Authentication createSuccessAuthentication(
      ResponseEntity<String> response, Authentication authentication) {
    LoggedUser loggedUser = readUserFromResponse(response);
    Collection<SimpleGrantedAuthority> authorities = extractRoles(loggedUser);

    UserDetails userDetails = null;
    if (loggedUser.getRoles().contains(AppRole.APP.name())) {
      userDetails = new AppPrincipal(
          ((GbifAppAuthentication) authentication).getAppKey(), new ArrayList<>(authorities));
    } else {
      userDetails = new GbifUserPrincipal(loggedUser.toGbifUser());
    }

    return new GbifAuthenticationToken(
        userDetails,
        GBIF_SCHEME,
        authorities);
  }
}
