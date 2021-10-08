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

import org.gbif.api.vocabulary.AppRole;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Class providing temporary authorization for legacy web service requests (GBRDS/IPT).
 */
public class LegacyRequestAuthorization implements Authentication {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyRequestAuthorization.class);

  private boolean authenticated = false;
  private final UUID userKey;
  private final UUID organizationKey;
  private final Collection<GrantedAuthority> authorities;

  public LegacyRequestAuthorization(UUID userKey, UUID organizationKey) {
    this.userKey = userKey;
    this.organizationKey = organizationKey;
    this.authorities = Collections.singleton(new SimpleGrantedAuthority(AppRole.IPT.name()));
    setAuthenticated(true);
  }

  public UUID getUserKey() {
    return userKey;
  }

  public UUID getOrganizationKey() {
    return organizationKey;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public Object getCredentials() {
    LOG.warn("LegacyRequestAuthorization#getCredentials is not used");
    return null;
  }

  @Override
  public Object getDetails() {
    LOG.warn("LegacyRequestAuthorization#getDetails is not used");
    return null;
  }

  @Override
  public Object getPrincipal() {
    return new BasicUserPrincipal(userKey.toString());
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
    return userKey.toString();
  }
}
