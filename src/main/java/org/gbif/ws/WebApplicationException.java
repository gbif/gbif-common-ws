/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
