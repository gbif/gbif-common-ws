package org.gbif.ws.security;


import org.gbif.ws.WebApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * HTTP 401 jersey exception when a request requires a valid authentication.
 */
public class NotAuthenticatedException extends WebApplicationException {

  public NotAuthenticatedException() {
    super(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  /**
   * @param message the String that is the entity of the 401 response.
   */
  public NotAuthenticatedException(String message) {
    super(ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.TEXT_PLAIN).body(message));
  }
}