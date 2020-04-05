package org.gbif.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.jackson.JacksonEncoder;
import java.lang.reflect.Type;

public class ClientEncoder implements Encoder {

  private JacksonEncoder jacksonEncoder;

  public ClientEncoder(ObjectMapper objectMapper) {
    this.jacksonEncoder = new JacksonEncoder(objectMapper);
  }

  @Override
  public void encode(Object object, Type bodyType, RequestTemplate template)
      throws EncodeException {
    if (bodyType == String.class) {
      template.body(object.toString());
    } else if (bodyType == byte[].class) {
      template.body((byte[]) object, null);
    } else {
      jacksonEncoder.encode(object, bodyType, template);
    }
  }
}
