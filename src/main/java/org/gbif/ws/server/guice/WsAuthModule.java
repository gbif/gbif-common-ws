package org.gbif.ws.server.guice;

import org.gbif.ws.security.GbifAppAuthService;
import org.gbif.ws.util.PropertiesUtil;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Maps;
import com.google.inject.PrivateModule;

/**
 * Server authentication related bindings providing primarily the AuthFilter.
 * Requires an externally bound UserService instance.
 */
public class WsAuthModule extends PrivateModule {

  public static final String PROPERTY_APPKEY_FILEPATH = "appkeys.file";
  private final GbifAppAuthService authService;

  /**
   * @param keys map of applicaiton keys to secrets
   */
  public WsAuthModule(Map<String, String> keys) {
    this.authService = new GbifAppAuthService(keys);
  }

  /**
   * @param appKeyStoreFilePath the property file containing application keys and secrets only
   */
  public WsAuthModule(String appKeyStoreFilePath) {
    try {
      Properties props = PropertiesUtil.readFromFile(appKeyStoreFilePath);
      authService = new GbifAppAuthService(Maps.fromProperties(props));
    } catch (IOException e) {
      throw new IllegalArgumentException(
        "Property file path to application keys does not exist: " + appKeyStoreFilePath, e);
    }
  }

  /**
   * @param properties config properties to find the standard property for the appKey file
   */
  public WsAuthModule(Properties properties) {
    this(properties.getProperty(PROPERTY_APPKEY_FILEPATH));
  }

  @Override
  protected void configure() {
    bind(GbifAppAuthService.class).toInstance(authService);
    expose(GbifAppAuthService.class);
  }
}