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
package org.gbif.ws.client;

import org.gbif.api.exception.ServiceUnavailableException;
import org.gbif.ws.MethodNotAllowedException;
import org.gbif.ws.NotFoundException;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.validation.ValidationException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import feign.Response;
import feign.codec.ErrorDecoder;

public class ClientErrorDecoder implements ErrorDecoder {

  private static final Logger LOG = LoggerFactory.getLogger(ClientErrorDecoder.class);

  @Override
  public Exception decode(String methodKey, Response response) {
    String message = null;

    if (response.body() != null) {
      try (Reader reader = response.body().asReader(StandardCharsets.UTF_8)) {
        // Easy way to read the stream and get a String object
        message = IOUtils.toString(reader);
        LOG.error("Client exception: {}", message);
      } catch (IOException e) {
        LOG.error("Exception during reading client error response", e);
      }
    }

    switch (response.status()) {
      case 400:
        return new IllegalArgumentException("A bad request received");
      case 401:
        return new AccessDeniedException("Unauthorized request received");
      case 403:
        return new AccessDeniedException("Forbidden request received");
      case 404:
        return new NotFoundException("Resource not found", URI.create(response.request().url()));
      case 405:
        return new MethodNotAllowedException("Method not allowed to user");
      case 422:
        return message != null
            ? new ValidationException(extractValidationErrorMessage(message))
            : new ValidationException();
      case 500:
        return new ServiceUnavailableException(
            "An internal server error occurred, please try again later");
      case 501:
        return new UnsupportedOperationException(
            message != null ? message : "Method not implement yet");
      default:
        return new RuntimeException(message != null ? message : "Unexpected exception");
    }
  }

  private String extractValidationErrorMessage(String message) {
    return message.replace("<ul><li>", "").replace("</li></ul>", "").replace("<li></li>", ", ");
  }
}
