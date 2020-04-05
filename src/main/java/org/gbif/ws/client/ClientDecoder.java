package org.gbif.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
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
    if (byte[].class.equals(type)) {
      return Util.toByteArray(response.body().asInputStream());
    } else {
      return jacksonDecoder.decode(response, type);
    }
  }
}
