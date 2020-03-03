package org.gbif.ws;

import org.springframework.http.HttpStatus;

/**
 * analogue of JAX-RS' one
 */
public class WebApplicationException extends RuntimeException {

  private static final long serialVersionUID = 11660101L;

  /**
   * HTTP status code of the response. Required.
   */
  private Integer status;

  /**
   * Construct a new instance with a message, and an HTTP status code
   */
  public WebApplicationException(String message, Integer status) {
    super(message);
    this.status = status;
  }

  /**
   * Construct a new instance with a message, and an HTTP status code
   */
  public WebApplicationException(String message, HttpStatus status) {
    this(message, status.value());
  }

  public Integer getStatus() {
    return status;
  }
}
