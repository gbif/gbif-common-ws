package org.gbif.ws.security;

import org.gbif.api.model.common.User;
import org.gbif.ws.WebApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * HTTP 403 exception when a request has a valid authentication, but the authenticated user is not allowed
 * to execute the resource.
 */
public class NotAllowedException extends WebApplicationException {

  public NotAllowedException() {
    super(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
  }

  /**
   * @param message the String that is the entity of the 403 response.
   */
  public NotAllowedException(String message) {
    super(ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.TEXT_PLAIN).body(message));
  }

  /**
   * @param user that is authenticated
   */
  public NotAllowedException(User user) {
    this(user.getUserName() + " is not allowed to access this resource.");
  }

}