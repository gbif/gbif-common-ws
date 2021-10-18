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
package org.gbif.ws.server.filter.jwt;

import org.gbif.api.vocabulary.UserRole;
import org.gbif.ws.security.identity.model.LoggedUser;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

/** Intercepts all requests to look for a JWT token. */
public class JwtFilter extends OncePerRequestFilter {

  private final RestTemplate restTemplate;

  private final String gbifApiLoginUrl;

  private static final String LOGIN_PATH = "user/login/jwt";

  public JwtFilter(RestTemplate restTemplate, String gbifApiUrl) {
    this.restTemplate = restTemplate;
    this.gbifApiLoginUrl = gbifApiUrl + LOGIN_PATH;
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {
    JwtUtils.findTokenInRequest(request)
      .ifPresent(
        token -> {
          try {
            ResponseEntity<LoggedUser> authResponse = jwtLogin(token);
            if (authResponse.getStatusCode().isError()) {
              unAuthorized(response);
            }
            authorized(authResponse);
          } catch (Exception exc) {
            unAuthorized(response);
          }
        });
    filterChain.doFilter(request, response);
  }

  /** Performs the remote call to the login/jwt service.*/
  private ResponseEntity<LoggedUser> jwtLogin(String token) {
    return restTemplate.postForEntity(gbifApiLoginUrl, new HttpEntity<>(jwtHeaders(token)), LoggedUser.class);
  }

  /** Gets the user data from the response and stores it in the SecurityContextHolder.*/
  private void authorized(ResponseEntity<LoggedUser> authResponse) {
    LoggedUser loggedUser = authResponse.getBody();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthentication(loggedUser.getUserName(), loggedUser.getToken(), extractRoles(
      loggedUser)));
  }

  /** Clears the context and sets the status to unauthorized.*/
  private void unAuthorized(HttpServletResponse response) {
    SecurityContextHolder.clearContext();
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  /** Maps User roles to a list SimpleGrantedAuthority. */
  protected Collection<SimpleGrantedAuthority> extractRoles(LoggedUser loggedUser) {
    return Optional.ofNullable(loggedUser.getRoles())
            .map(roles -> roles.stream()
                            .map(r -> new SimpleGrantedAuthority(UserRole.valueOf(r).name()))
                            .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
  }

  /** Creates HttpHeaders for JWT authentication request.*/
  private HttpHeaders jwtHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }
}
