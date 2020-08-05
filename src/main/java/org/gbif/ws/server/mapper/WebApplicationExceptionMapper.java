package org.gbif.ws.server.mapper;

import org.gbif.ws.NotFoundException;
import org.gbif.ws.WebApplicationException;
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
        .contentType(e.getContentType())
        .body(e.getMessage());
  }

  @SuppressWarnings("rawtypes")
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity handleNotException(NotFoundException e) {
    return ResponseEntity
        .status(e.getStatus())
        .contentType(e.getContentType())
        .body(e.getMessage());
  }
}
