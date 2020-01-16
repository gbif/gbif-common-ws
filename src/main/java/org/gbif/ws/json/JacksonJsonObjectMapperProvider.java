package org.gbif.ws.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.gbif.api.ws.mixin.Mixins;

public final class JacksonJsonObjectMapperProvider {

  private JacksonJsonObjectMapperProvider() {
  }

  public static ObjectMapper getObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // determines whether encountering of unknown properties (ones that do not map to a property,
    // and there is no
    // "any setter" or handler that can handle it) should result in a failure (throwing a
    // JsonMappingException) or not.
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Enforce use of ISO-8601 format dates (http://wiki.fasterxml.com/JacksonFAQDateHandling)
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    Mixins.getPredefinedMixins().forEach(objectMapper::addMixIn);

    return objectMapper;
  }
}
