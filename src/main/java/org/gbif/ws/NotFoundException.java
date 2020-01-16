package org.gbif.ws;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URI;

/**
 * A HTTP 404 (Not Found) exception.
 */
public class NotFoundException extends WebApplicationException {

  private final URI notFoundUri;

  /**
   * Create a HTTP 404 (Not Found) exception.
   */
  public NotFoundException() {
    this((URI)null);
  }

  /**
   * Create a HTTP 404 (Not Found) exception.
   *
   * @param notFoundUri the URI that cannot be found.
   */
  public NotFoundException(URI notFoundUri) {
    super(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    this.notFoundUri = notFoundUri;
  }

  /**
   * Create a HTTP 404 (Not Found) exception.
   *
   * @param message the String that is the entity of the 404 response.
   */
  public NotFoundException(String message) {
    this(message, null);
  }

  /**
   * Create a HTTP 404 (Not Found) exception.
   *
   * @param message the String that is the entity of the 404 response.
   * @param notFoundUri the URI that cannot be found.
   */
  public NotFoundException(String message, URI notFoundUri) {
    super(ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body(message));

    this.notFoundUri = notFoundUri;
  }

  /**
   * Get the URI that is not found.
   *
   * @return the URI that is not found.
   */
  public URI getNotFoundUri() {
    return notFoundUri;
  }

  @Override
  public String getMessage() {
    return super.getMessage() + " for uri: " + notFoundUri;
  }
}
