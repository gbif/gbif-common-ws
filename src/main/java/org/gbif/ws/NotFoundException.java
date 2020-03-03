package org.gbif.ws;

import java.net.URI;
import org.springframework.http.HttpStatus;

/**
 * A HTTP 404 (Not Found) exception.
 */
public class NotFoundException extends WebApplicationException {

  /**
   * The URI that cannot be found. Required.
   */
  private final URI notFoundUri;

  /**
   * Create a HTTP 404 (Not Found) exception.
   *
   * @param message     the String that is the entity of the 404 response.
   * @param notFoundUri the URI that cannot be found.
   */
  public NotFoundException(String message, URI notFoundUri) {
    super(message + " for uri: " + notFoundUri, HttpStatus.NOT_FOUND);
    this.notFoundUri = notFoundUri;
  }

  /**
   * Get the URI that is not found.
   */
  public URI getNotFoundUri() {
    return notFoundUri;
  }
}
