package org.gbif.ws.security;

import org.gbif.api.model.common.User;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * HTTP 403 jersey exception when a request has a valid authentication, but the authenticated user is not allowed
 * to execute the resource.
 */
public class NotAllowedException extends WebApplicationException {

     public NotAllowedException() {
         super(Response.status(Response.Status.FORBIDDEN).build());
     }

     /**
      * @param message the String that is the entity of the 403 response.
      */
     public NotAllowedException(String message) {
         super(Response.status(Response.Status.FORBIDDEN).entity(message).type("text/plain").build());
     }

  /**
   * @param user that is authenticated
   */
  public NotAllowedException(User user) {
    this (user.getUserName() + " is not allowed to access this resource.");
  }

}