package org.gbif.ws.server.mapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AccessDeniedExceptionMapper {

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Object> toResponse(AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.TEXT_PLAIN)
        .body(exception.getMessage());
  }
}
