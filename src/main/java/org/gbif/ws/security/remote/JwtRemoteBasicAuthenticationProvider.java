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
package org.gbif.ws.security.remote;

import org.gbif.ws.security.identity.model.LoggedUser;
import org.gbif.ws.server.filter.jwt.JwtAuthentication;

import lombok.extern.slf4j.Slf4j;

/**
 * JWT Remote authentication against the user/login/jwt service.
 */
@Slf4j
public class JwtRemoteBasicAuthenticationProvider extends AbstractRemoteAuthenticationProvider<JwtAuthentication> {

  public JwtRemoteBasicAuthenticationProvider(IdentityServiceClient identityServiceClient) {
    super(identityServiceClient, JwtAuthentication.class);
  }

  /** Performs the remote call to the login/jwt service.*/
  @Override
  protected LoggedUser login(JwtAuthentication jwtAuthentication) {
    return getIdentityServiceClient().loginJwt("Bearer " + jwtAuthentication.getToken());
  }

}
