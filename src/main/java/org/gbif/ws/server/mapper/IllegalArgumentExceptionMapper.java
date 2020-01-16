package org.gbif.ws.server.mapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Converts a IllegalArgumentException into a http 400 bad request.
 */
@ControllerAdvice
public class IllegalArgumentExceptionMapper {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> toResponse(IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.TEXT_PLAIN)
        .body(exception.getMessage());
  }
}
