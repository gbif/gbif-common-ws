package org.gbif.ws.server.filter;

import java.io.IOException;
import java.util.Objects;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.GbifAuthenticationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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

  private GbifAuthenticationManager authenticationManager;

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
    }
  }
}
