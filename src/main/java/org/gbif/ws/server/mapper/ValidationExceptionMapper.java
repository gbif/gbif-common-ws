package org.gbif.ws.server.mapper;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import javax.validation.ConstraintViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.validation.ConstraintViolationException;
import java.util.Comparator;

/**
 * Converts validation exceptions into a http 422 bad request and gives a meaningful messages on the issues.
 */
@ControllerAdvice
public class ValidationExceptionMapper {

  private static final Logger LOG = LoggerFactory.getLogger(ValidationExceptionMapper.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> toResponse(MethodArgumentNotValidException exception) {
    LOG.error(exception.getMessage(), exception);

    ImmutableList.Builder<String> builder = ImmutableList.builder();

    exception.getBindingResult()
        .getAllErrors()
        .stream()
        .map(error -> ((FieldError) error))
        .sorted(Comparator.comparing(FieldError::getField, Comparator.naturalOrder()))
        .forEach(error -> {
          LOG.debug("Validation of [{}] failed: {}", error.getField(), error.getDefaultMessage());
          builder.add(String.format("Validation of [%s] failed: %s", error.getField(), error.getDefaultMessage()));
        });

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .contentType(MediaType.TEXT_PLAIN)
        .body("<ul><li>" + Joiner.on("</li><li>").join(builder.build()) + "</li></ul>");
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> toResponse(ConstraintViolationException exception) {
    LOG.error(exception.getMessage(), exception);

    ImmutableList.Builder<String> builder = ImmutableList.builder();

    for (ConstraintViolation<?> cv : exception.getConstraintViolations()) {
      LOG.debug("Validation of [{}] failed: {}", cv.getPropertyPath(), cv.getMessage());
      builder.add(String.format("Validation of [%s] failed: %s", cv.getPropertyPath(), cv.getMessage()));
    }

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .contentType(MediaType.TEXT_PLAIN)
        .body("<ul><li>" + Joiner.on("</li><li>").join(builder.build()) + "</li></ul>");
  }
}
