package org.gbif.ws.server.mapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Converts a UnsupportedOperationException into a http 501 not implemented.
 */
@ControllerAdvice
public class UnsupportedOperationExceptionMapper {

  @ExceptionHandler(UnsupportedOperationException.class)
  public ResponseEntity<Object> toResponse(UnsupportedOperationException exception) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
        .contentType(MediaType.TEXT_PLAIN)
        .body(exception.getMessage());
  }
}
