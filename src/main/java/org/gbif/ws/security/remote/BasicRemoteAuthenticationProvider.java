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

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import lombok.extern.slf4j.Slf4j;

/**
 * Basic authentication against the user/login service.
 */
@Slf4j
public class BasicRemoteAuthenticationProvider extends AbstractRemoteAuthenticationProvider<UsernamePasswordAuthenticationToken> {

  public BasicRemoteAuthenticationProvider(IdentityServiceClient identityServiceClient) {
    super(identityServiceClient, UsernamePasswordAuthenticationToken.class);
  }

  /** Performs the remote call to the login service.*/
  @Retryable( value = RuntimeException.class,
    maxAttempts = 5, backoff = @Backoff(delay = 300))
  @Override
  protected LoggedUser login(UsernamePasswordAuthenticationToken authentication) {
    return getIdentityServiceClient().login("Basic " + HttpHeaders.encodeBasicAuth(authentication.getName(), (String)authentication.getCredentials(), StandardCharsets.UTF_8));
  }

}
