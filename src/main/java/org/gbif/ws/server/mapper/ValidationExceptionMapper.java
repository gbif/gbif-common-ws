/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.ws.server.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.validation.ConstraintViolationException;
import javax.validation.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Converts validation exceptions into a http 422 bad request and gives a meaningful messages on the
 * issues.
 */
@ControllerAdvice
public class ValidationExceptionMapper {

  private static final Logger LOG = LoggerFactory.getLogger(ValidationExceptionMapper.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> toResponse(MethodArgumentNotValidException exception) {
    LOG.error("Validation error: {}", exception.getMessage());
    List<String> errors = new ArrayList<>();

    exception.getBindingResult().getAllErrors().stream()
        .map(error -> ((FieldError) error))
        .sorted(Comparator.comparing(FieldError::getField, Comparator.naturalOrder()))
        .forEach(
            error -> {
              LOG.debug(
                  "Validation of [{}] failed: {}", error.getField(), error.getDefaultMessage());
              errors.add(
                  String.format(
                      "Validation of [%s] failed: %s",
                      error.getField(), error.getDefaultMessage()));
            });

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .contentType(MediaType.TEXT_PLAIN)
        .body("<ul><li>" + String.join("</li><li>", errors) + "</li></ul>");
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> toResponse(ConstraintViolationException exception) {
    LOG.error("Validation error: {}", exception.getMessage());
    List<String> errors = new ArrayList<>();

    exception.getConstraintViolations().stream()
        .sorted(
            Comparator.comparing(
                cv -> getPropertyFromPropertyPath(cv.getPropertyPath()), Comparator.naturalOrder()))
        .forEach(
            cv -> {
              LOG.debug("Validation of [{}] failed: {}", cv.getPropertyPath(), cv.getMessage());
              errors.add(
                  String.format(
                      "Validation of [%s] failed: %s",
                      getPropertyFromPropertyPath(cv.getPropertyPath()), cv.getMessage()));
            });

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .contentType(MediaType.TEXT_PLAIN)
        .body("<ul><li>" + String.join("</li><li>", errors) + "</li></ul>");
  }

  private String getPropertyFromPropertyPath(Path propertyPath) {
    String resultProperty = null;

    if (propertyPath != null) {
      resultProperty = propertyPath.toString();
      int lastDotIndex = resultProperty.lastIndexOf('.');
      if (lastDotIndex != -1) {
        resultProperty = resultProperty.substring(lastDotIndex + 1);
      }
    }

    return resultProperty;
  }
}
