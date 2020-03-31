package org.gbif.ws.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.gbif.ws.WebApplicationException;

public class ClientErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    return new WebApplicationException(response.reason(), response.status());
  }
}
