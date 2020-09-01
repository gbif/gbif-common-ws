/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class GbifAuthenticationToken implements GbifAuthentication {

  private static final Logger LOG = LoggerFactory.getLogger(GbifAuthenticationToken.class);

  private boolean authenticated = false;

  /**
   * User authorities (e.g. REGISTRY_ADMIN or APP).
   */
  private final Collection<? extends GrantedAuthority> authorities;

  /**
   * User information (can be either {@link GbifUserPrincipal} or {@link AppPrincipal}).
   */
  private final UserDetails principal;

  /**
   * Authentication scheme (e.g. 'GBIF').
   */
  private final String authenticationScheme;

  public GbifAuthenticationToken(UserDetails principal) {
    this.principal = principal;
    this.authenticationScheme = "";
    this.authorities = Collections.emptyList();
    setAuthenticated(true);
  }

  public GbifAuthenticationToken(UserDetails principal, String authenticationScheme) {
    this.principal = principal;
    this.authenticationScheme = authenticationScheme;
    this.authorities = Collections.emptyList();
    setAuthenticated(true);
  }

  public GbifAuthenticationToken(
      UserDetails principal, Collection<? extends GrantedAuthority> authorities) {
    this.principal = principal;
    this.authenticationScheme = "";
    this.authorities = authorities;
    setAuthenticated(true);
  }

  public GbifAuthenticationToken(
      UserDetails principal,
      String authenticationScheme,
      Collection<? extends GrantedAuthority> authorities) {
    this.principal = principal;
    this.authenticationScheme = authenticationScheme;
    this.authorities = authorities;
    setAuthenticated(true);
  }

  public static GbifAuthentication anonymous() {
    return new GbifAuthenticationToken(new AnonymousUserPrincipal());
  }

  @Override
  public String getAuthenticationScheme() {
    return authenticationScheme;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public Object getCredentials() {
    LOG.warn("GbifAuthenticationToken#getCredentials is not used");
    return null;
  }

  @Override
  public Object getDetails() {
    LOG.warn("GbifAuthenticationToken#getDetails is not used");
    return null;
  }

  @Override
  public UserDetails getPrincipal() {
    return principal;
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) {
    this.authenticated = isAuthenticated;
  }

  @Override
  public String getName() {
    return principal.getUsername();
  }
}
