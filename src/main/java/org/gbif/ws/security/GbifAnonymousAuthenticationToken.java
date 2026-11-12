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

import java.util.Collections;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Authentication of a request without (or with ignored) credentials.
 *
 * <p>It is an {@link AnonymousAuthenticationToken}, so Spring Security's {@code
 * AuthenticationTrustResolver} recognises it as anonymous: {@code .authenticated()}, {@code
 * isAuthenticated()} and {@code isFullyAuthenticated()} are not satisfied by it, and access denied
 * to it is handled by the authentication entry point. The principal is an {@link
 * AnonymousUserPrincipal}.
 */
public class GbifAnonymousAuthenticationToken extends AnonymousAuthenticationToken
    implements GbifAuthentication {

  public static final String ANONYMOUS_ROLE = "ROLE_ANONYMOUS";

  // only used by Spring's AnonymousAuthenticationProvider, which GBIF services do not configure
  private static final String KEY = GbifAnonymousAuthenticationToken.class.getName();

  public GbifAnonymousAuthenticationToken() {
    super(
        KEY,
        new AnonymousUserPrincipal(),
        Collections.singletonList(new SimpleGrantedAuthority(ANONYMOUS_ROLE)));
  }

  @Override
  public String getAuthenticationScheme() {
    return "";
  }

  @Override
  public UserDetails getPrincipal() {
    return (UserDetails) super.getPrincipal();
  }
}
