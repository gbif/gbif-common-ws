package org.gbif.ws.server.mapper;

import org.gbif.ws.NotFoundException;
import org.gbif.ws.WebApplicationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class WebApplicationExceptionMapper {

  @ExceptionHandler(WebApplicationException.class)
  public ResponseEntity<Void> handleWebApplicationException(WebApplicationException e) {
    return ResponseEntity.status(e.getResponse().getStatusCode()).build();
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Void> handleWebApplicationException(NotFoundException e) {
    return ResponseEntity.notFound().build();
  }
}
