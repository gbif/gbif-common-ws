package org.gbif.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.lang.reflect.Type;

public class ClientDecoder implements Decoder {

  private JacksonDecoder jacksonDecoder;

  public ClientDecoder(ObjectMapper objectMapper) {
    this.jacksonDecoder = new JacksonDecoder(objectMapper);
  }

  @Override
  public Object decode(Response response, Type type)
      throws IOException, DecodeException, FeignException {
    HttpStatus responseStatus = HttpStatus.resolve(response.status());
    if (HttpStatus.NOT_FOUND  == responseStatus) {
      return null;
    } else if(responseStatus.isError()) {
      throw new DecodeException(response.status(), response.toString(), response.request());
    } else if (byte[].class.equals(type)) {
      return Util.toByteArray(response.body().asInputStream());
    } else {
      return jacksonDecoder.decode(response, type);
    }
  }
}
