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
package org.gbif.ws.remoteauth.jwt;

import java.util.Objects;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * JWT {@link org.springframework.security.core.Authentication} that contains a jwt token and
 * information about the user.
 */
public class JwtAuthentication extends AbstractAuthenticationToken {

  private String username;
  private String token;

  public JwtAuthentication(String token) {
    super(null);
    this.token = token;
    super.setAuthenticated(false);
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return username;
  }

  public String getToken() {
    return token;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JwtAuthentication)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    JwtAuthentication that = (JwtAuthentication) o;
    return Objects.equals(username, that.username) && Objects.equals(token, that.token);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), username, token);
  }
}
