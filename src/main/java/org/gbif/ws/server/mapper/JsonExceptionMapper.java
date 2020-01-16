package org.gbif.ws.server.mapper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Converts json related exceptions into a http 400 bad request.
 */
@ControllerAdvice
public class JsonExceptionMapper {

  @ExceptionHandler({
      JsonMappingException.class,
      JsonParseException.class,
      JsonProcessingException.class,
      RuntimeJsonMappingException.class})
  public ResponseEntity<Object> toResponse(Exception exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.TEXT_PLAIN)
        .body(exception.getMessage());
  }
}
