package org.gbif.ws.server.provider;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a ConstraintViolationException into a http 422 bad request and gives a meaningful messages on the issues.
 */
@Provider
@Singleton
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

  private static final int HTTP_CODE = 422;
  private static final Logger LOG = LoggerFactory.getLogger(ValidationExceptionMapper.class);

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    LOG.error(exception.getMessage(), exception);
    ImmutableList.Builder<String> b = ImmutableList.builder();
    for (ConstraintViolation<?> cv : exception.getConstraintViolations()) {
      LOG.debug("Validation of [{}] failed: {}", cv.getPropertyPath(), cv.getMessage());
      b.add(String.format("Validation of [%s] failed: %s", cv.getPropertyPath(), cv.getMessage()));
    }
    return Response.status(HTTP_CODE).type(MediaType.TEXT_PLAIN)
      .entity("<ul><li>" + Joiner.on("</li><li>").join(b.build()) + "</li></ul>").build();
  }
}
