package org.gbif.ws.util;

import org.gbif.api.exception.ServiceUnavailableException;

import java.io.InputStream;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * Utility class that validates response statuses and extract the input stream from a HTTP response.
 */
public class InputStreamUtils {

  private InputStreamUtils() {
    //empty constructor
  }

  /**
   * Only allow status 200 responses and throw NotFound or ServiceUnavailable exceptions otherwise.
   * @return the http entities input stream if http status=200 or null for 404
   * @throws org.gbif.api.exception.ServiceUnavailableException in all other cases
   */
  public static InputStream wrapStream(WebResource resource) {

    final ClientResponse response = resource.get(ClientResponse.class);

    if (response.getStatus() == ClientResponse.Status.NOT_FOUND.getStatusCode()) {
      response.close();
      return null;
    }

    if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
      response.close();
      throw new ServiceUnavailableException("HTTP " + response.getStatus() + ": " +
                                            response.getStatusInfo().getReasonPhrase());
    }

    return response.getEntityInputStream();
  }
}
