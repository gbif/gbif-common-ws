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

import org.gbif.api.vocabulary.AppRole;
import org.gbif.ws.security.AnonymousUserPrincipal;
import org.gbif.ws.security.AppPrincipal;
import org.gbif.ws.security.AppkeysConfigurationProperties;
import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.security.GbifAuthUtils;
import org.gbif.ws.security.GbifAuthenticationToken;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;
import org.gbif.ws.util.SecurityConstants;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A filter that allows an application to identify itself as an application (as opposed to an
 * application impersonating a user). In order to identify itself an application shall provide its
 * appKey in the header x-gbif-user and sign the request accordingly. If the application can be
 * authenticated AND its appKey is in the whitelist, the {@link Principal#getName()} will return the
 * appKey and the role {@link AppRole#APP} will be assigned to it.
 *
 * <p>We use an appKey whitelist to control which app should be allowed to have the {@link
 * AppRole#APP} while letting the user impersonation available in {@link IdentityFilter}. If at some
 * point multiple {@link AppRole} should be supported the whitelist should simply be changed for
 * something more structured.
 *
 * <p>This filter must run AFTER {@link IdentityFilter} if user impersonation using appKey is
 * required. This filter will be skipped if the request already has a {@link Principal} attached.
 * This filter operates on {@link SecurityConstants#GBIF_SCHEME} only. If the appKeyWhitelist list
 * is not provided no apps will be authenticated by this filter.
 */
@Component
public class AppIdentityFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(AppIdentityFilter.class);

  private final GbifAuthService authService;
  private final List<String> appKeyWhitelist;

  public AppIdentityFilter(
      @NotNull GbifAuthService authService, AppkeysConfigurationProperties appkeysConfiguration) {
    this.authService = authService;
    // defensive copy or creation
    this.appKeyWhitelist =
        appkeysConfiguration.getWhitelist() != null
            ? new ArrayList<>(appkeysConfiguration.getWhitelist())
            : new ArrayList<>();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // Only try if no user principal is already there
    if (authentication == null
        || authentication.getPrincipal() == null
        || authentication.getPrincipal() instanceof AnonymousUserPrincipal) {
      String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (StringUtils.startsWith(authorization, SecurityConstants.GBIF_SCHEME_PREFIX)) {
        if (authService.isValidRequest(
            request instanceof GbifHttpServletRequestWrapper
                ? (GbifHttpServletRequestWrapper) request
                : new GbifHttpServletRequestWrapper(request))) {
          String username = request.getHeader(SecurityConstants.HEADER_GBIF_USER);
          String appKey = GbifAuthUtils.getAppKeyFromRequest(authorization);

          // check if it's an app by ensuring the appkey used to sign the request is the one used as
          // x-gbif-user
          if (StringUtils.equals(appKey, username) && appKeyWhitelist.contains(appKey)) {
            final List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(AppRole.APP.name()));
            final AppPrincipal principal = new AppPrincipal(appKey, authorities);
            final Authentication newAuthentication =
                new GbifAuthenticationToken(principal, SecurityConstants.GBIF_SCHEME, authorities);

            SecurityContextHolder.getContext().setAuthentication(newAuthentication);
          }
        } else {
          LOG.warn("Invalid GBIF authenticated request");
          response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}
