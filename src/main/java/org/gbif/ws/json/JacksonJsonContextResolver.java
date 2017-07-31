package org.gbif.ws.json;

import java.util.List;
import java.util.Map;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.google.inject.Singleton;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Provider that initializes the {@link ObjectMapper} to ignore {@code null} fields.
 * NOTE! This class has the same name as a class provided by jersey, BUT THEY DO DIFFERENT THINGS! This
 * class depends on the presence of the jersey version in order to do anything useful. If you get errors that
 * suggest that you don't have an appropriate MessageBodyReader for the MIME type application/json, then chances
 * are good that you have not included the jersey-provided JacksonJsonProvider in your configuration.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class JacksonJsonContextResolver implements ContextResolver<ObjectMapper> {

  protected static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    // determines whether encountering of unknown properties (ones that do not map to a property, and there is no
    // "any setter" or handler that can handle it) should result in a failure (throwing a JsonMappingException) or not.
    MAPPER.disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
    
    // Enforce use of ISO-8601 format dates (http://wiki.fasterxml.com/JacksonFAQDateHandling)
    MAPPER.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  /**
   * Specific polymorphic serialization is supported by passing a map of mixIns into the class.
   * 
   * @param mixIns class map
   */
  public static void addMixIns(Map<Class<?>, Class<?>> mixIns) {
    // handle polymorphic JSON
    for (Map.Entry<Class<?>, Class<?>> classClassEntry : mixIns.entrySet()) {
      MAPPER.getSerializationConfig().addMixInAnnotations(classClassEntry.getKey(), classClassEntry.getValue());
      MAPPER.getDeserializationConfig().addMixInAnnotations(classClassEntry.getKey(), classClassEntry.getValue());
    }
  }

  /**
   * Disable Jackson's SerializationConfig.Feature.
   * @param features
   */
  public static void disableFeature(List<SerializationConfig.Feature> features) {
    if (features == null) {
      return;
    }
    features.stream()
            .forEach(f -> MAPPER.configure(f, false));
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return MAPPER;
  }
}
