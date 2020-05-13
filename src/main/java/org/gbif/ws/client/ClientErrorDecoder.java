package org.gbif.ws.client;

import com.google.common.io.CharStreams;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.gbif.api.exception.ServiceUnavailableException;
import org.gbif.ws.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import javax.validation.ValidationException;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;

@SuppressWarnings("UnstableApiUsage")
public class ClientErrorDecoder implements ErrorDecoder {

  private static final Logger LOG = LoggerFactory.getLogger(ClientErrorDecoder.class);

  @Override
  public Exception decode(String methodKey, Response response) {
    String message = null;

    try (Reader reader = response.body().asReader()) {
      //Easy way to read the stream and get a String object
      message = CharStreams.toString(reader);
      LOG.error("Client exception: {}", message);
    } catch (IOException e) {
      LOG.error("Exception during reading client error response", e);
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
      case 422:
        return message != null
            ? new ValidationException(extractValidationErrorMessage(message))
            : new ValidationException();
      case 500:
        return new ServiceUnavailableException("An internal server error occurred, please try again later");
      case 501:
        return new UnsupportedOperationException(message != null ? message : "Method not implement yet");
      default:
        return new RuntimeException(message != null ? message : "Unexpected exception");
    }
  }

  private String extractValidationErrorMessage(String message) {
    return message.replace("<ul><li>", "")
        .replace("</li></ul>", "")
        .replace("<li></li>", ", ");
  }
}
