package org.gbif.ws.server.provider;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

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
  private final FacetedSearchRequestProvider<OccurrenceSearchRequest, OccurrenceSearchParameter>
      provider =
          new FacetedSearchRequestProvider<>(
              OccurrenceSearchRequest.class, OccurrenceSearchParameter.class);

  private OccurrenceSearchRequest parse(String... nameValues) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    for (int i = 0; i < nameValues.length; i += 2) {
      request.addParameter(nameValues[i], nameValues[i + 1]);
    }
    return provider.getValue(new ServletWebRequest(request));
  }

  @Test
  void facetParametersWithinBounds() {
    OccurrenceSearchRequest request =
        parse(
            "facet", "country",
            "facetLimit", "50",
            "facetOffset", "20",
            "facetMincount", "2",
            "country.facetLimit", "30",
            "country.facetOffset", "10",
            "spellCheckCount", "5");

    assertEquals(50, request.getFacetLimit());
    assertEquals(20, request.getFacetOffset());
    assertEquals(2, request.getFacetMinCount());
    Pageable countryPage = request.getFacetPage(OccurrenceSearchParameter.COUNTRY);
    assertEquals(30, countryPage.getLimit());
    assertEquals(10, countryPage.getOffset());
    assertEquals(5, request.getSpellCheckCount());
  }

  @Test
  void facetLimitsAreCapped() {
    OccurrenceSearchRequest request =
        parse(
            "facet", "country",
            "facetLimit", "2147483647",
            "country.facetLimit", "100000",
            "spellCheckCount", "100000");

    assertEquals(FacetedSearchRequestProvider.DEFAULT_MAX_FACET_LIMIT, request.getFacetLimit());
    assertEquals(
        FacetedSearchRequestProvider.DEFAULT_MAX_FACET_LIMIT,
        request.getFacetPage(OccurrenceSearchParameter.COUNTRY).getLimit());
    assertEquals(SearchRequestProvider.MAX_SPELL_CHECK_COUNT, request.getSpellCheckCount());
  }

  @Test
  void invalidFacetParametersAreRejected() {
    // IllegalArgumentException is mapped to 400 by IllegalArgumentExceptionMapper
    assertThrows(IllegalArgumentException.class, () -> parse("facetLimit", "-1"));
    assertThrows(IllegalArgumentException.class, () -> parse("facetLimit", "ten"));
    assertThrows(IllegalArgumentException.class, () -> parse("facetOffset", "-5"));
    assertThrows(IllegalArgumentException.class, () -> parse("facetOffset", "10001"));
    assertThrows(IllegalArgumentException.class, () -> parse("facetMincount", "-1"));
    assertThrows(IllegalArgumentException.class, () -> parse("spellCheckCount", "-2"));
    assertThrows(
        IllegalArgumentException.class,
        () -> parse("facet", "country", "country.facetOffset", "2147483647"));
    assertThrows(
        IllegalArgumentException.class,
        () -> parse("facet", "country", "country.facetLimit", "abc"));
  }
}
