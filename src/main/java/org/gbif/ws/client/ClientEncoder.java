package org.gbif.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.RequestTemplate;
import feign.Util;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.jackson.JacksonEncoder;
import org.apache.commons.lang3.SerializationUtils;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class ClientEncoder implements Encoder {

  private JacksonEncoder jacksonEncoder;

  public ClientEncoder(ObjectMapper objectMapper) {
    this.jacksonEncoder = new JacksonEncoder(objectMapper);
  }

  @Override
  public void encode(Object object, Type bodyType, RequestTemplate template)
      throws EncodeException {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    Set<ConstraintViolation<Object>> violations = new HashSet<>();

    if ("POST".equals(template.method())) {
      violations = validator.validate(object, javax.validation.groups.Default.class, PrePersist.class);
    } else if ("PUT".equals(template.method())) {
      violations = validator.validate(object, javax.validation.groups.Default.class, PostPersist.class);
    }

    if (bodyType == String.class) {
      template.body(object.toString());
    } else if (bodyType == byte[].class) {
      template.body((byte[]) object, null);
    } else {
      if (!violations.isEmpty()) {
        ConstraintViolationException exception = new ConstraintViolationException(violations);
        byte[] data = SerializationUtils.serialize(exception);
        template.header("hasConstraintViolations", String.valueOf(violations.size()));
        template.body(Request.Body.encoded(data, Util.UTF_8));
      } else {
        jacksonEncoder.encode(object, bodyType, template);
      }
    }
  }
}
