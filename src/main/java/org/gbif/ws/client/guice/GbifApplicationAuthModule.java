package org.gbif.ws.client.guice;

import org.gbif.ws.client.filter.HttpGbifAuthFilter;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.GbifAuthService;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * An authentication module for trusted GBIF applications.
 * It requires an application key and can then act on behalf of any username.
 *
 * To set the proxied username you can either set it directly on the module instance
 * or inject the SimplePrincipalProvider into your code anywhere to set the current user:
 *
 * <pre>
 * {@code
 * GbifApplicationAuthModule authMod = new GbifApplicationAuthModule();
 * authMod.setPrincipal("admin");
 *
 * // set user via SimplePrincipalProvider:
 *
 * {@literal @}Inject
 * SimplePrincipalProvider pp;
 * pp.setPrincipal("admin");
 * }
 * </pre>
 *
 * Note that the users role still depends on the roles defined in the user account in Drupal and will not default
 * to ADMIN. The user also has to exist in Drupal for the webservice authorization to work.
 *
 */
// TODO: Make the application key configurable (Requires significant changes in the webservice AuthModule)
public class GbifApplicationAuthModule extends AbstractModule {

  public static final String PROPERTY_APP_KEY = "application.key";
  public static final String PROPERTY_APP_SECRET = "application.secret";

  private final SimplePrincipalProvider pp = new SimplePrincipalProvider();
  private final GbifAuthService authService;

  /**
   * Creates a new authentication guice module for a trusted application with its public and secret key.
   * The proxied user will be set to match the application key.
   *
   * @param appKey       the trusted application public key
   * @param appSecretKey the secret key issued for this application
   */
  public GbifApplicationAuthModule(String appKey, String appSecretKey) {
    authService = GbifAuthService.singleKeyAuthService(appKey, appSecretKey);
    pp.setPrincipal(appKey);
  }

  /**
   * Creates a new authentication guice module for a trusted application with its id and secret key given as
   * properties.
   * The proxied user will be set to match the application.key.
   *
   * @param properties that holds an entry for application.key and application.secret
   */
  public GbifApplicationAuthModule(Properties properties) {
    this(properties.getProperty(PROPERTY_APP_KEY), properties.getProperty(PROPERTY_APP_SECRET));
  }

  @Override
  protected void configure() {
    bind(SimplePrincipalProvider.class).toInstance(pp);
    bind(GbifAuthService.class).toInstance(authService);
    HttpGbifAuthFilter authFilter = new HttpGbifAuthFilter(authService, pp);
    bind(ClientFilter.class).toInstance(authFilter);
  }

  public void setPrincipal(String username) {
    pp.setPrincipal(username);
  }
}
