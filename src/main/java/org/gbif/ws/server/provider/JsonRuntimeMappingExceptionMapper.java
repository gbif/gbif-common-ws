package org.gbif.ws.server.provider;

import com.google.inject.Singleton;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.RuntimeJsonMappingException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Converts a RuntimeJsonMappingException into a http 400 bad request.
 */
@Provider
@Singleton
public class JsonRuntimeMappingExceptionMapper implements ExceptionMapper<RuntimeJsonMappingException> {

  @Override
  public Response toResponse(RuntimeJsonMappingException exception) {
    return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(exception.getMessage()).build();
  }
}
