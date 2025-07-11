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
package org.gbif.ws.server.filter;

import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.GbifAuthenticationManager;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Override a built-in spring filter because of legacy behaviour.
 *
 * <p>Replacement for AuthFilter (legacy gbif-common-ws).
 *
 * <p>Server filter that looks for a http BasicAuthentication with user accounts based on a {@link
 * IdentityAccessService} or GBIF trusted application schema to impersonate a user and populates the
 * security context.
 *
 * <p>As we have another custom authorization filter in the registry that understands a registry
 * internal authentication, all Basic authentication requests that have a UUID as the username are
 * simply passed through and passwords are not evaluated.
 */
@Component
public class IdentityFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(IdentityFilter.class);

  private final GbifAuthenticationManager authenticationManager;

  public IdentityFilter(GbifAuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Objects.requireNonNull(request, "Can't filter null request");
    Objects.requireNonNull(response, "Can't filter null response");

    // authenticates the HTTP method, but ignores legacy UUID user names
    try {
      final Authentication authentication = authenticationManager.authenticate(request);
      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);
    } catch (final WebApplicationException e) {
      LOG.debug("Exception while authentication in IdentityFilter: {}", e.getMessage());
      response.setStatus(e.getStatus());
      response.getOutputStream().println(e.getMessage());
    }
  }
}
