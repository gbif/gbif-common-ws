package org.gbif.ws.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.gbif.api.exception.ServiceUnavailableException;
import org.gbif.ws.NotFoundException;
import org.springframework.security.access.AccessDeniedException;

import javax.validation.ValidationException;
import java.net.URI;

public class ClientErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    switch (response.status()) {
      case 400:
        return new IllegalArgumentException("A bad request received");
      case 401:
        return new AccessDeniedException("Unauthorized request received");
      case 403:
        return new AccessDeniedException("Forbidden request received");
      case 404:
        return new NotFoundException("Resource not found", URI.create(response.request().url()));
      case 422:
        return new ValidationException();
      case 500:
        return new ServiceUnavailableException("An internal server error occurred, please try again later");
      case 501:
        return new UnsupportedOperationException("Method not implement yet");
      default:
        return new RuntimeException("Unexpected exception");
    }
  }
}
