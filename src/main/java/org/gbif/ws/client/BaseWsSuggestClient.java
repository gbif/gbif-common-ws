package org.gbif.ws.client;

import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.common.SuggestService;

import java.util.List;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;


/**
 * Base web service suggest & faceted search client.
 */
public abstract class BaseWsSuggestClient<T, P extends Enum<?> & SearchParameter, R extends FacetedSearchRequest<P>, ST, RSUG extends SearchRequest<P>>
  extends BaseWsFacetedSearchClient<T, P, R> implements SuggestService<ST, P, RSUG> {

  private final GenericType<List<ST>> suggestType;

  /**
   * @param resource the base web resource being the parent of /search and /suggest
   * @param searchType the generic type for the search response.
   * @param suggestType the generic type for the suggest response.
   */
  protected BaseWsSuggestClient(WebResource resource, GenericType<SearchResponse<T, P>> searchType,
    GenericType<List<ST>> suggestType) {
    super(resource, searchType);
    this.suggestType = suggestType;
  }

  /**
   * Generic suggest operation retrieving search entities T.
   * 
   * @param suggestRequest to issue the search/suggest operation.
   * @return a List of NameUsageSearchSuggestResult objects
   */
  public List<ST> suggest(RSUG suggestRequest) {
    if (suggestRequest == null) {
      throw new IllegalArgumentException("searchSuggestRequest cannot be null");
    }
    return get(suggestType, null, getParameterFromSearchRequest(suggestRequest), suggestRequest, "suggest");
  }

}
