package org.gbif.ws.client.guice;

import com.google.inject.AbstractModule;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * A small module that installs a dummy {@link com.sun.jersey.api.client.filter.ClientFilter} that doesn't do anything.
 * It can be used as an anonymous authentication filter, leaving the http headers untouched.
 */
public class AnonymousAuthModule extends AbstractModule {

  /**
   * Dummy client filter that doesn't do anything.
   * Can be used as anonymous authentication for a required filter.
   */
  private static class AnonymousAuthFilter extends ClientFilter {

    @Override
    public ClientResponse handle(final ClientRequest cr) throws ClientHandlerException {
      return getNext().handle(cr);
    }
  }

  @Override
  protected void configure() {
    bind(ClientFilter.class).to(AnonymousAuthFilter.class).asEagerSingleton();
  }
}
