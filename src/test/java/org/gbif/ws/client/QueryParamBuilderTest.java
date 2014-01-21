package org.gbif.ws.client;

import org.gbif.api.vocabulary.Rank;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class QueryParamBuilderTest {

  @Test
  public void testQueryParam() throws Exception {
    QueryParamBuilder builder = QueryParamBuilder.create();
    assertNotNull(builder.build());
    assertEquals(0, builder.build().size());

    builder.queryParam("a", "B").queryParam("b", "C").queryParam("2", "3").queryParam("", "ah").queryParam(null, "ah")
      .queryParam("ah", "");
    assertEquals(3, builder.build().size());
    assertEquals("C", builder.build().getFirst("b"));
    assertNull(builder.build().get("x"));

    builder = QueryParamBuilder.create("kie", new Object[] {1, 2, 3, 4});
    assertEquals(1, builder.build().size());
    assertEquals("1", builder.build().getFirst("kie"));
    assertEquals(4, builder.build().get("kie").size());
  }

  @Test
  public void testKvpBuilder() throws Exception {
    QueryParamBuilder builder = QueryParamBuilder.create("a", "B", "b", "C", "2", "3", "", "ah", null, "ah", "ah", "");
    assertEquals(3, builder.build().size());
    assertEquals("C", builder.build().getFirst("b"));
    assertEquals("B", builder.build().getFirst("a"));
    assertNull(builder.build().get("x"));

    builder = QueryParamBuilder.create("rank", Rank.SPECIES);
    assertEquals(1, builder.build().size());
    assertEquals("SPECIES", builder.build().getFirst("rank"));
    assertNull(builder.build().get("x"));

    builder = QueryParamBuilder.create("a", "B", "b", "C");
    assertEquals(2, builder.build().size());
    assertEquals("B", builder.build().getFirst("a"));
    assertEquals("C", builder.build().getFirst("b"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testKvpBuilderException() throws Exception {
    QueryParamBuilder.create("a", "B", "b", "C", "2");
  }

}
