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
package org.gbif.ws.server.mapper;

import org.gbif.ws.server.advice.AuthResponseFilter;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Maps access denied to 401 for requests without credentials and to 403 for authenticated users
 * without the required role.
 *
 * <p>The WWW-Authenticate challenge of the 401 is added by {@link AuthResponseFilter}, which must
 * be registered too.
 */
@ControllerAdvice
public class AccessDeniedExceptionMapper {

  private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Object> toResponse(AccessDeniedException exception) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return ResponseEntity.status(
            trustResolver.isAuthenticated(authentication)
                ? HttpStatus.FORBIDDEN
                : HttpStatus.UNAUTHORIZED)
        .contentType(MediaType.TEXT_PLAIN)
        .body(exception.getMessage());
  }
}
