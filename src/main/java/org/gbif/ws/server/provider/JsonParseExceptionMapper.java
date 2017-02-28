package org.gbif.ws.server.provider;

import com.google.inject.Singleton;
import org.codehaus.jackson.JsonParseException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Implementation of {@link ExceptionMapper} to send down a "400 Bad Request"
 * in the event unparsable JSON is received.
 */
@Provider
@Singleton
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

  @Override
  public Response toResponse(JsonParseException exception) {
    return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).type("text/plain").build();
  }
}
