package org.gbif.ws.client.guice;

import com.google.inject.AbstractModule;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * A small module that installs a {@link com.sun.jersey.api.client.filter.HTTPBasicAuthFilter}
 * for a single, fixed user.
 *
 * If application need to write to webservices, e.g the registry, you need to make sure the user
 * has admin rights in drupal.
 *
 * @see <a href="http://staging.gbif.org/drupal/user/register">register a new user in drupal</a>
 */
public class SingleUserAuthModule extends AbstractModule {

  private final HTTPBasicAuthFilter filter;

  public SingleUserAuthModule(String user, String password) {
    filter = new HTTPBasicAuthFilter(user, password);
  }

  @Override
  protected void configure() {
    bind(ClientFilter.class).toInstance(filter);
  }
}
