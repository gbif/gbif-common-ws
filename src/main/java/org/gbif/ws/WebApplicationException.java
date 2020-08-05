package org.gbif.ws;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Analogue of JAX-RS' WebApplicationException.
 */
public class WebApplicationException extends RuntimeException {

  private static final long serialVersionUID = 11660101L;

  /**
   * HTTP status code of the response. Required.
   */
  private final Integer status;

  /**
   * Content media type of the response. Optional.
   */
  private final MediaType contentType;

  /**
   * Construct a new instance with a message, and an HTTP status code.
   */
  public WebApplicationException(String message, Integer status) {
    super(message);
    this.status = status;
    this.contentType = MediaType.TEXT_PLAIN;
  }

  /**
   * Construct a new instance with a message, and an HTTP status code.
   */
  public WebApplicationException(String message, HttpStatus status) {
    super(message);
    this.status = status.value();
    this.contentType = MediaType.TEXT_PLAIN;
  }

  /**
   * Construct a new instance with a message, an HTTP status code, and content media type.
   */
  public WebApplicationException(String message, HttpStatus status, MediaType contentType) {
    super(message);
    this.status = status.value();
    this.contentType = contentType;
  }

  public Integer getStatus() {
    return status;
  }

  public MediaType getContentType() {
    return contentType;
  }
}
