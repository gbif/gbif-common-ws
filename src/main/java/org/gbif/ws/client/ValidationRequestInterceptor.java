package org.gbif.ws.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.lang3.SerializationUtils;

import javax.validation.ConstraintViolationException;

import static org.gbif.ws.client.ClientUtils.isPostOrPutRequest;
import static org.gbif.ws.client.ClientUtils.isRequestBodyNotEmpty;

public class ValidationRequestInterceptor implements RequestInterceptor {

  @Override
  public void apply(RequestTemplate template) {
    if (isPostOrPutRequest(template)
        && isRequestBodyNotEmpty(template)
        && template.headers().containsKey("hasConstraintViolations")) {
      throw SerializationUtils.<ConstraintViolationException>deserialize(template.requestBody().asBytes());
    }
  }
}
