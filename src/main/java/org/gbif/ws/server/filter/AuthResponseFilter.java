package org.gbif.ws.server.filter;

import javax.ws.rs.core.Response;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * Response filter that adds a WWW-Authenticate header if response status is Unauthenticated,
 * indicating a Basic Authentication scheme to be used.
 */
public class AuthResponseFilter implements ContainerResponseFilter {

  private final String realm;

  /**
   * Default constructor for the GBIF realm.
   */
  public AuthResponseFilter() {
    this.realm = "GBIF";
  }

  public AuthResponseFilter(String realm) {
    this.realm = realm;
  }

  @Override
  public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
    if (Response.Status.UNAUTHORIZED.getStatusCode() == response.getStatus()) {
      response.getHttpHeaders().putSingle("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
    }

    return response;
  }
}
