package org.gbif.ws.security;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * HTTP 401 jersey exception when a request requires a valid authentication.
 */
public class NotAuthenticatedException extends WebApplicationException {

     public NotAuthenticatedException() {
         super(Response.status(Response.Status.UNAUTHORIZED).build());
     }

     /**
      * @param message the String that is the entity of the 401 response.
      */
     public NotAuthenticatedException(String message) {
         super(Response.status(Response.Status.UNAUTHORIZED).entity(message).type("text/plain").build());
     }

}