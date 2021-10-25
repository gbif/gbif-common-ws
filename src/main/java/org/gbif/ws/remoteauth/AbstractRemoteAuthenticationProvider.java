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
package org.gbif.ws.remoteauth;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.gbif.api.vocabulary.UserRole;
import org.gbif.ws.security.identity.model.LoggedUser;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for authentication against remote end-points.
 *
 * @param <T> supported authentication type.
 */
@Slf4j
@Data
public abstract class AbstractRemoteAuthenticationProvider<T extends Authentication> implements AuthenticationProvider {

  protected static final ObjectReader OBJECT_READER =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).reader();

  private final RemoteAuthClient remoteAuthClient;

  private final Class<T> authClass;

  private final String authWsPath;

  public AbstractRemoteAuthenticationProvider(
      Class<T> authClass, String authWsPath, RemoteAuthClient remoteAuthClient) {
    this.authClass = authClass;
    this.authWsPath = authWsPath;
    this.remoteAuthClient = remoteAuthClient;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    return createSuccessAuthentication(tryLogin((T) authentication), authentication);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return authClass.isAssignableFrom(authentication);
  }

  /** Performs the remote call to the login service. */
  @Retryable(value = RuntimeException.class,
      maxAttempts = 5, backoff = @Backoff(delay = 300))
  protected ResponseEntity<String> tryLogin(T authentication) {
    return remoteAuthClient.remoteAuth(authWsPath, createHttpHeaders(authentication));
  }

  public abstract HttpHeaders createHttpHeaders(Authentication authentication);

  /** Creates an UsernamePasswordAuthenticationToken from the supplied parameters. */
  protected abstract Authentication createSuccessAuthentication(ResponseEntity<String> response,
      Authentication authentication);

  /** Maps User roles to a list SimpleGrantedAuthority. */
  protected Collection<SimpleGrantedAuthority> extractRoles(LoggedUser loggedUser) {
    Objects.requireNonNull(loggedUser);
    return Optional.ofNullable(loggedUser.getRoles())
        .map(roles -> roles.stream()
            .map(r -> new SimpleGrantedAuthority(UserRole.valueOf(r).name()))
            .collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }

  @SneakyThrows
  protected LoggedUser readUserFromResponse(ResponseEntity<String> response) {
    return OBJECT_READER.readValue(response.getBody(), LoggedUser.class);
  }

}
