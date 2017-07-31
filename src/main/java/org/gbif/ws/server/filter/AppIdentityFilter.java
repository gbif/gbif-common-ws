package org.gbif.ws.server.filter;

import org.gbif.api.model.common.AppPrincipal;
import org.gbif.api.model.common.ExtendedPrincipal;
import org.gbif.api.vocabulary.AppRole;
import org.gbif.ws.security.GbifAuthService;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A filter that allows an application to identify itself as an application (as opposed to an application
 * impersonating a user).
 * In order to identify itself an application shall provide its appKey in the header x-gbif-user and sign the request
 * accordingly.
 * If the application can be authenticated AND its appKey is in the whitelist, the {@link Principal#getName()} will return the appKey and the
 * role {@link AppRole#APP} will be assigned to it.
 *
 * We use an appKey whitelist to control which app should be allowed to have the {@link AppRole#APP} while letting
 * the user impersonation available in {@link IdentityFilter}. If at some point multiple {@link AppRole} should be
 * supported the whitelist should simply be changed for something more structured.
 *
 * This filter must run AFTER {@link IdentityFilter} if user impersonation using appKey is required.
 * This filter will be skipped if the {@link ContainerRequest} already has a {@link Principal} attached.
 * This filter operates on {@link GbifAuthService#GBIF_SCHEME} only.
 * If the appKeyWhitelist list is not provided no apps will be authenticated by this filter.
 *
 */
public class AppIdentityFilter implements ContainerRequestFilter {

  public static final  String APPKEYS_WHITELIST = "identity.appkeys.whitelist";

  //FIXME should probably have its own scheme but that would requires to change {@link GbifAuthService)
  private static final String GBIF_SCHEME_PREFIX = GbifAuthService.GBIF_SCHEME + " ";
  private static final Logger LOG = LoggerFactory.getLogger(AppIdentityFilter.class);

  private final GbifAuthService authService;
  private final List<String> appKeyWhitelist;

  @Inject
  public AppIdentityFilter(@NotNull GbifAuthService authService, @Nullable @Named(APPKEYS_WHITELIST) List<String> appKeyWhitelist) {
    this.authService = authService;
    //defensive copy or creation
    this.appKeyWhitelist = appKeyWhitelist != null ? new ArrayList<>(appKeyWhitelist) : new ArrayList<>();
  }

  @Override
  public ContainerRequest filter(final ContainerRequest containerRequest) {

    // Only try if no user principal is already there
    if (containerRequest.getUserPrincipal() != null) {
      return containerRequest;
    }

    String authorization = containerRequest.getHeaderValue(ContainerRequest.AUTHORIZATION);
    if (StringUtils.startsWith(authorization, GBIF_SCHEME_PREFIX)) {
      if (!authService.isValidRequest(containerRequest)) {
        LOG.warn("Invalid GBIF authenticated request");
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
      }

      String username = containerRequest.getHeaderValue(GbifAuthService.HEADER_GBIF_USER);
      String appKey = GbifAuthService.getAppKeyFromRequest(containerRequest::getHeaderValue);

      //check if it's an app by ensuring the appkey used to sign the request is the one used as x-gbif-user
      if (StringUtils.equals(appKey, username) && appKeyWhitelist.contains(appKey)) {
        containerRequest.setSecurityContext(new SecurityContext() {
          private final ExtendedPrincipal principal = new AppPrincipal(appKey, AppRole.APP.name());

          @Override
          public Principal getUserPrincipal() {
            return principal;
          }

          @Override
          public boolean isUserInRole(String s) {
            return principal.hasRole(s);
          }

          @Override
          public boolean isSecure() {
            return containerRequest.isSecure();
          }

          @Override
          public String getAuthenticationScheme() {
            return GbifAuthService.GBIF_SCHEME;
          }
        });
      }
    }
    return containerRequest;
  }
}
