package org.gbif.ws.client;


import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.model.common.search.SearchResponse;

import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import static org.gbif.ws.util.WebserviceParameter.DEFAULT_SEARCH_PARAM_VALUE;
import static org.gbif.ws.util.WebserviceParameter.PARAM_HIGHLIGHT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_QUERY_STRING;
import static org.gbif.ws.util.WebserviceParameter.PARAM_SPELLCHECK;
import static org.gbif.ws.util.WebserviceParameter.PARAM_SPELLCHECK_COUNT;

/**
 * Base web service search client supporting the basic {@link SearchRequest}.
 *
 * @param <T> type of the response content
 * @param <P> search parameter type
 * @param <R> search request type
 */
public abstract class BaseWsSearchClient<T, P extends Enum<?> & SearchParameter, R extends SearchRequest<P>>
  extends BaseWsClient {

  private final GenericType<SearchResponse<T, P>> gType;

  /**
   * @param resource the base web resource. /search will be added
   * @param gType the generic type for the search response.
   */
  protected BaseWsSearchClient(WebResource resource, GenericType<SearchResponse<T, P>> gType) {
    super(resource);
    this.gType = gType;
  }

  /**
   * Generic search operation.
   *
   * @param searchRequest to issue the search operation. If null matches everything
   * @return a SearchResponse object with the operation result.
   */
  public SearchResponse<T, P> search(@Nullable R searchRequest) {
    return get(gType, null, getParameterFromRequest(searchRequest), searchRequest, "search");
  }

  /**
   * Transforms the attributes of the {@link SearchRequest} parameter into a map of parameters.
   * The output of this method is intended to be used with the operation WebResource.queryParams(parameters).
   *
   * @return a {@link MultivaluedMap} containing the HTTP parameters taken from the {@link SearchRequest} object.
   */
  protected MultivaluedMap<String, String> getParameterFromRequest(@Nullable R searchRequest) {
    return getParameterFromSearchRequest(searchRequest);
  }

  protected MultivaluedMap<String, String> getParameterFromSearchRequest(@Nullable SearchRequest<P> searchRequest) {

    // The searchRequest is transformed in a parameter map
    MultivaluedMap<String, String> parameters = new MultivaluedMapImpl();

    if (searchRequest == null) {
      parameters.putSingle(PARAM_QUERY_STRING, DEFAULT_SEARCH_PARAM_VALUE);

    } else {
      String searchParamValue = searchRequest.getQ();
      if (Strings.isNullOrEmpty(searchParamValue)) {
        searchParamValue = DEFAULT_SEARCH_PARAM_VALUE;
      }
      parameters.putSingle(PARAM_QUERY_STRING, searchParamValue);
      parameters.putSingle(PARAM_HIGHLIGHT, Boolean.toString(searchRequest.isHighlight()));
      parameters.putSingle(PARAM_SPELLCHECK, Boolean.toString(searchRequest.isSpellCheck()));
      parameters.putSingle(PARAM_SPELLCHECK_COUNT,Integer.toString(searchRequest.getSpellCheckCount()));

      Multimap<P, String> requestParameters = searchRequest.getParameters();
      if (requestParameters != null) {
        for (P param : requestParameters.keySet()) {
          parameters.put(param.name(), Lists.newArrayList(requestParameters.get(param)));
        }
      }
    }
    return parameters;
  }

}
