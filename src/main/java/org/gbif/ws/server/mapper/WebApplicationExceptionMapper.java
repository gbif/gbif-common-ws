package org.gbif.ws.server.mapper;

import org.gbif.ws.WebApplicationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class WebApplicationExceptionMapper {

  @SuppressWarnings("rawtypes")
  @ExceptionHandler(WebApplicationException.class)
  public ResponseEntity handleWebApplicationException(WebApplicationException e) {
    return ResponseEntity
        .status(e.getStatus())
        .contentType(MediaType.TEXT_PLAIN)
        .body(e.getMessage());
  }
}
