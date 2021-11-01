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
package org.gbif.ws.remoteauth.app;

import java.io.IOException;

import org.gbif.ws.util.SecurityConstants;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Intercepts all requests to look for a JWT token. */
public class GbifAppRequestFilter extends OncePerRequestFilter {

  private final AuthenticationManager authenticationManager;

  public GbifAppRequestFilter(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  /**
   * Performs the authentication, only if JWT token is found.
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null
        && StringUtils.startsWith(authorization, SecurityConstants.GBIF_SCHEME_PREFIX)) {
      String gbifUser = request.getHeader(SecurityConstants.HEADER_GBIF_USER);
      String contentMd5 = request.getHeader(SecurityConstants.HEADER_CONTENT_MD5);
      String originalRequestUrl = request.getHeader(SecurityConstants.HEADER_ORIGINAL_REQUEST_URL);

      try {
        SecurityContextHolder.getContext()
            .setAuthentication(
                authenticationManager.authenticate(
                    new GbifAppAuthentication(
                        authorization, gbifUser, contentMd5, originalRequestUrl)));
      } catch (AuthenticationException exc) {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}
