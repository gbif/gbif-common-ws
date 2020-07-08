package org.gbif.ws;

import org.springframework.http.HttpStatus;

public class MethodNotAllowedException extends WebApplicationException {

  /**
   * Create a HTTP 405 (Method not allowed) exception.
   *
   * @param message     the String that is the entity of the 404 response.
   */
  public MethodNotAllowedException(String message) {
    super(message, HttpStatus.METHOD_NOT_ALLOWED);
  }

}
