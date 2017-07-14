package org.gbif.ws.server.filter;

import org.gbif.api.model.common.ExtendedPrincipal;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.api.model.common.User;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.ws.security.GbifAuthService;

import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Future replacement for common-ws AuthFilter
 *
 * Server filter that looks for a http BasicAuthentication with user accounts based on a {@link IdentityAccessService}
 * or GBIF trusted application schema to impersonate a user and populates the security context.
 *
 * As we have another custom authorization filter in the registry that understands a registry internal authentication,
 * all Basic authentication requests that have a UUID as the username are simply passed through and passwords are not
 * evaluated.
 */
public class IdentityFilter implements ContainerRequestFilter {

  private static class Authorizer implements SecurityContext {

    private final ExtendedPrincipal principal;
    private final String authenticationScheme;
    private final boolean isSecure;

    private Authorizer(ExtendedPrincipal principal, String authenticationScheme, boolean isSecure) {
      this.principal = principal;
      this.authenticationScheme = authenticationScheme;
      this.isSecure = isSecure;
    }

    /**
     * Get an {@link Authorizer} for a specific {@link User}.
     *
     * @param user
     * @param authenticationScheme
     * @param isSecure
     *
     * @return
     */
    static Authorizer getAuthorizer(GbifUser user, String authenticationScheme, boolean isSecure) {
      return new Authorizer(new GbifUserPrincipal(user), authenticationScheme, isSecure);
    }

    /**
     * Get an anonymous {@link Authorizer}.
     * Anonymous users do not have {@link Principal}.
     *
     * @param isSecure
     *
     * @return
     */
    static Authorizer getAnonymous(boolean isSecure) {
      return new Authorizer(null, "", isSecure);
    }

    @Override
    public String getAuthenticationScheme() {
      return authenticationScheme;
    }

    @Override
    public Principal getUserPrincipal() {
      return principal;
    }

    @Override
    public boolean isSecure() {
      return isSecure;
    }

    @Override
    public boolean isUserInRole(String role) {
      return principal != null && principal.hasRole(role);
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(IdentityFilter.class);

  private static final Pattern COLON_PATTERN = Pattern.compile(":");
  private final IdentityAccessService identityAccessService;
  private final GbifAuthService authService;

  private static final String GBIF_SCHEME_PREFIX = GbifAuthService.GBIF_SCHEME + " ";
  private static final String BASIC_SCHEME_PREFIX = "Basic ";

  /**
   * IdentityFilter constructor
   * In case {@link GbifAuthService} is not provided, this class will reject all authentications
   * on the GBIF scheme prefix.
   *
   * @param identityAccessService
   * @param authService     nullable GbifAuthService
   */
  @Inject
  public IdentityFilter(@NotNull IdentityAccessService identityAccessService, @Nullable GbifAuthService authService) {
    Objects.requireNonNull(identityAccessService, "identityAccessService shall be provided");
    this.identityAccessService = identityAccessService;
    this.authService = authService;
  }

  @Override
  public ContainerRequest filter(ContainerRequest request) {

    // authenticates the HTTP method, but ignores legacy UUID user names
    Authorizer authorizer = authenticate(request);

    if (authorizer == null) {
      // default is the anonymous user
      authorizer = Authorizer.getAnonymous(request.isSecure());
    }
    request.setSecurityContext(authorizer);
    return request;
  }

  private Authorizer authenticate(ContainerRequest request) {
    // Extract authentication credentials
    String authentication = request.getHeaderValue(ContainerRequest.AUTHORIZATION);
    if (authentication != null) {
      if (authentication.startsWith(BASIC_SCHEME_PREFIX)) {
        return basicAuthentication(authentication.substring(BASIC_SCHEME_PREFIX.length()), request.isSecure());
      } else if (authentication.startsWith(GBIF_SCHEME_PREFIX)) {
        return gbifAuthentication(request);
      }
    }
    return Authorizer.getAnonymous(request.isSecure());
  }

  private Authorizer basicAuthentication(String authentication, boolean isSecure) {
    String[] values = COLON_PATTERN.split(Base64.base64Decode(authentication));
    if (values.length < 2) {
      LOG.warn("Invalid syntax for username and password: {}", authentication);
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    String username = values[0];
    String password = values[1];
    if (username == null || password == null) {
      LOG.warn("Missing basic authentication username or password: {}", authentication);
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    // ignore usernames which are UUIDs - these are registry legacy IPT calls and handled by a special security filter
    try {
      UUID.fromString(username);
      return null;
    } catch (IllegalArgumentException e) {
      // no UUID, continue with regular drupal authentication
    }

    GbifUser user = identityAccessService.authenticate(username, password);
    if (user == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    LOG.debug("Authenticating user {} via scheme {}", username, SecurityContext.BASIC_AUTH);
    return Authorizer.getAuthorizer(user, SecurityContext.BASIC_AUTH, isSecure);
  }

  private Authorizer gbifAuthentication(ContainerRequest request) {
    String username = request.getHeaderValue(GbifAuthService.HEADER_GBIF_USER);
    if (Strings.isNullOrEmpty(username)) {
      LOG.warn("Missing gbif username header {}", GbifAuthService.HEADER_GBIF_USER);
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    if (authService == null) {
      LOG.warn("No GbifAuthService defined.");
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    if (!authService.isValidRequest(request)) {
      LOG.warn("Invalid GBIF authenticated request");
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    LOG.debug("Authenticating user {} via scheme {}", username, GbifAuthService.GBIF_SCHEME);
    if (identityAccessService == null) {
      LOG.debug("No identityService configured! No roles assigned, using anonymous user instead.");
      return Authorizer.getAnonymous(request.isSecure());
    }

    //check if we have a request that impersonates a user
    //Note: using an Anonymous Authorizer is probably not the best thing to do here
    //we should consider
    GbifUser user = identityAccessService.get(username);
    return user == null ? Authorizer.getAnonymous(request.isSecure())
            : Authorizer.getAuthorizer(user, GbifAuthService.GBIF_SCHEME, request.isSecure());
  }

}
