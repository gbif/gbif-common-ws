package org.gbif.ws.util;

/**
 * Parameters used in the API webservices.
 */
public class WebserviceParameter {

  /**
   * Facet param name.
   */
  public static final String PARAM_FACET = "facet";

  /**
   * Facet multiselect parameter.
   */
  public static final String PARAM_FACET_MULTISELECT = "facetMultiselect";

  public static final String PARAM_FACET_LIMIT = "facetLimit";

  public static final String PARAM_FACET_OFFSET = "facetOffset";

  public static final String PARAM_HIGHLIGHT = "hl";

  /**
   * Parameter min count of facets, facets with less than this valued sholdn't be included in the response.
   */
  public static final String PARAM_FACET_MINCOUNT = "facetMincount";

  /**
   * spellCheck parameter.
   */
  public static final String PARAM_SPELLCHECK = "spellCheck";

  /**
   * spellCheckCount parameter.
   */
  public static final String PARAM_SPELLCHECK_COUNT = "spellCheckCount";

  /**
   * The query string for searches.
   * Repeated in SearchConstants, couldnt resolve dependencies.
   */
  public static final String PARAM_QUERY_STRING = "q";

  public static final String DEFAULT_SEARCH_PARAM_VALUE = "*";


  private WebserviceParameter() {
    throw new UnsupportedOperationException("Can't initialize utils class");
  }
}
