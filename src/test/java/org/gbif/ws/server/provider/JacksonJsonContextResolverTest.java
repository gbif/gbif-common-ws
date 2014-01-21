package org.gbif.ws.server.provider;

import org.gbif.ws.json.JacksonJsonContextResolver;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JacksonJsonContextResolverTest {

  private final JacksonJsonContextResolver provider = new JacksonJsonContextResolver();

  @Test
  public void testProvider() {
    ObjectMapper context = provider.getContext(String.class);
    assertEquals(JsonSerialize.Inclusion.NON_NULL, context.getSerializationConfig().getSerializationInclusion());
  }

}
