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

import java.net.URI;

import org.springframework.http.HttpStatus;

/**
 * A HTTP 404 (Not Found) exception.
 */
public class NotFoundException extends WebApplicationException {

  /**
   * The URI that cannot be found. Required.
   */
  private final URI notFoundUri;

  /**
   * Create a HTTP 404 (Not Found) exception.
   *
   * @param message     the String that is the entity of the 404 response.
   * @param notFoundUri the URI that cannot be found.
   */
  public NotFoundException(String message, URI notFoundUri) {
    super(message + " for uri: " + notFoundUri, HttpStatus.NOT_FOUND);
    this.notFoundUri = notFoundUri;
  }

  /**
   * Get the URI that is not found.
   */
  public URI getNotFoundUri() {
    return notFoundUri;
  }
}
