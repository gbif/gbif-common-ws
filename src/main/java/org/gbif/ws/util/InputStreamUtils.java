package org.gbif.ws.util;

import org.gbif.api.exception.ServiceUnavailableException;

import java.io.InputStream;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class InputStreamUtils {

  private InputStreamUtils() {

  }

  /**
   * Only allow status 200 responses and throw NotFound or ServiceUnavailable exceptions otherwise.
   * @return the http entities input stream if http status=200 or null for 404
   * @throws org.gbif.api.exception.ServiceUnavailableException in all other cases
   */
  public static InputStream wrapStream(WebResource resource) {

    final ClientResponse response = resource.get(ClientResponse.class);

    if (response.getStatus() == 404) {
      response.close();
      return null;
    }

    if (response.getStatus() != 200) {
      response.close();
      throw new ServiceUnavailableException("HTTP " + response.getStatus() + ": " + response.getClientResponseStatus().getReasonPhrase());
    }

    return response.getEntityInputStream();
  }
}
