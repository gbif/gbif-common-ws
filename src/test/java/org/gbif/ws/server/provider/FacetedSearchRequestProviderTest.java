package org.gbif.ws.server.provider;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FacetedSearchRequestProviderTest {

  @Test
  void getFirstIgnoringCase() {
    Map<String, String[]> params = new LinkedHashMap<>();
    params.put("facetMultiSelect", new String[] {"true"});
    params.put("facetMinCount", new String[] {"100","101"});
    params.put("faCET", new String[] {"rank"});
    params.put("facet", new String[] {"status"});

    String val = FacetedSearchRequestProvider.getFirstIgnoringCase("facet", params);
    assertEquals("status", val);

    val = FacetedSearchRequestProvider.getFirstIgnoringCase("Facet", params);
    assertEquals("status", val);

    val = FacetedSearchRequestProvider.getFirstIgnoringCase("FACET", params);
    assertEquals("status", val);

    val = FacetedSearchRequestProvider.getFirstIgnoringCase("faCET", params);
    assertEquals("rank", val);

    val = FacetedSearchRequestProvider.getFirstIgnoringCase("facetMinCount", params);
    assertEquals("100", val);

    val = FacetedSearchRequestProvider.getFirstIgnoringCase("facetMincount", params);
    assertEquals("100", val);

    val = FacetedSearchRequestProvider.getFirstIgnoringCase("facetmincount", params);
    assertEquals("100", val);

    val = FacetedSearchRequestProvider.getFirstIgnoringCase("FACETMINCOUNT", params);
    assertEquals("100", val);
  }
}