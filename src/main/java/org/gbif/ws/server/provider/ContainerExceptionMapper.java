package org.gbif.ws.server.provider;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import com.google.inject.Singleton;
import com.sun.jersey.api.container.ContainerException;

/**
 * Mapper to handle ContainerException which wrap exceptions thrown from injectable providers.
 * This mapper looks for the root cause and delegates to the appropriate wrapper where there is
 * one registered for the cause of the exception, otherwise it returns 500.
 */
@Provider
@Singleton
public class ContainerExceptionMapper implements ExceptionMapper<ContainerException> {
  @Context
  private final Providers providers;

  public ContainerExceptionMapper(@Context Providers providers) {
    this.providers = providers;
  }

  @Override
  public Response toResponse(ContainerException exception) {
    Throwable cause = exception.getCause();
    if (exception.getCause() != null) {
      @SuppressWarnings("unchecked")
      ExceptionMapper<Throwable> em = (ExceptionMapper<Throwable>) providers.getExceptionMapper(cause.getClass());
      if (em != null) {
        return em.toResponse(exception.getCause());
      }
    }

    // no cause or not handled, then we are really hosed
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
           .type(MediaType.TEXT_PLAIN).entity(exception.getMessage()).build();
  }
}
