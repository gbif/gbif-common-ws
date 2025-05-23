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

import org.gbif.ws.security.GbifAuthenticationToken;
import org.gbif.ws.util.SecurityConstants;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Intercepts all requests to look for a JWT token. */
public class JwtRequestFilter extends OncePerRequestFilter {

  private final AuthenticationManager authenticationManager;

  public JwtRequestFilter(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  /**
   * Performs the authentication, only if JWT token is found.
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Optional<String> token = JwtUtils.findTokenInRequest(request);
    if (token.isPresent()) {
      try {
        GbifAuthenticationToken authentication =
            (GbifAuthenticationToken)
                authenticationManager.authenticate(new JwtAuthentication(token.get()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // set the new token in the response
        response.setHeader(SecurityConstants.HEADER_TOKEN, authentication.getJwtToken());
        response.addHeader(
            HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, SecurityConstants.HEADER_TOKEN);
      } catch (AuthenticationException exc) {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}
