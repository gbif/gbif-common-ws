package org.gbif.ws.server.filter;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.UUID;
import javax.ws.rs.core.Response;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * Filter that updates http headers when a new resource is successfully created via a POST request.
 * The following headers are added or replaced if they existed:
 * <ul>
 *   <li>Http response code 201</li>
 *   <li>Location header is set accordingly based on returned key</li>
 * </ul>
 */
public class CreatedResponseFilter implements ContainerResponseFilter {

  @Override
  public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {

    if (request.getMethod() != null && request.getMethod().equalsIgnoreCase("post") && response.getStatusType() != null
        && response.getStatusType().getFamily() == Response.Status.Family.SUCCESSFUL) {

      response.setStatus(HttpURLConnection.HTTP_CREATED);
      // if response contains the key, also set Location
      if (response.getEntity() != null) {
        Object key = response.getEntity();
        // we use POSTs also for non Create method which can return large objects, e.g. a list of parsed names
        // only set the location header if the object is one of the following simple primary key data types
        if (key instanceof Number || key instanceof UUID || key instanceof String) {
          // allow POSTing to resource with or without trailing slash
          URI location = request.getRequestUriBuilder().path(key.toString()).build();
          response.getHttpHeaders().putSingle("Location", location.toString());
        }

      }
    }

    return response;
  }
}
