package org.gbif.ws.server.provider;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.google.inject.Singleton;
import org.apache.http.HttpStatus;

/**
 * Converts a UnsupportedOperationException into a http 501 not implemented.
 */
@Provider
@Singleton
public class UnsupportedOperationExceptionMapper implements ExceptionMapper<UnsupportedOperationException> {

  @Override
  public Response toResponse(UnsupportedOperationException exception) {
    return Response.status(HttpStatus.SC_NOT_IMPLEMENTED).type(MediaType.TEXT_PLAIN).entity(exception.getMessage()).build();
  }
}
