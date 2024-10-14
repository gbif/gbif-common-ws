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
package org.gbif.ws.remoteauth.basic;

import org.gbif.ws.util.SecurityConstants;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Intercepts all requests that use basic authentication. */
public class BasicAuthRequestFilter extends OncePerRequestFilter {

  private final AuthenticationManager authenticationManager;
  private final BasicAuthenticationConverter authenticationConverter =
      new BasicAuthenticationConverter();

  public BasicAuthRequestFilter(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  /**
   * Performs the authentication, only if the basic auth header is found.
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null
        && StringUtils.startsWith(authorization, SecurityConstants.BASIC_SCHEME_PREFIX)) {
      try {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
            authenticationConverter.convert(request);
        SecurityContextHolder.getContext()
            .setAuthentication(
                authenticationManager.authenticate(usernamePasswordAuthenticationToken));
      } catch (AuthenticationException exc) {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}
