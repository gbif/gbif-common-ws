package org.gbif.ws.server.provider;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.google.inject.Singleton;

/**
 * Converts a UnsupportedOperationException into a http 501 not implemented.
 */
@Provider
@Singleton
public class UnsupportedOperationExceptionMapper implements ExceptionMapper<UnsupportedOperationException> {

  @Override
  public Response toResponse(UnsupportedOperationException e) {
    return Response.status(501).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
  }
}
