package org.gbif.ws.client.guice;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * Module adding a configured number of Named bindings with dynamic URL values for testing local webapps.
 * Used in client integration tests to connect to the webservices.
 */
public class UrlBindingModule extends AbstractModule {

  private final String url;
  private String[] names;

  /**
   * Creates a module with URI value bindings.
   * @param url the URI value to bind
   * @param name the list of names to bind the URI value to
   */
  public UrlBindingModule(String url, String ... name) {
    this.url = url;
    this.names = name;
  }

  /**
   * Configures a {@link com.google.inject.Binder} via the exposed methods.
   */
  @Override
  protected void configure() {
    for (String n : names){
      bindConstant().annotatedWith(Names.named(n)).to(url);
    }
  }
}
