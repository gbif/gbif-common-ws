package org.gbif.ws.client;


import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.common.SearchService;

import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_LIMIT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MINCOUNT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MULTISELECT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_OFFSET;

/**
 * Base web service search client supporting a {@link FacetedSearchRequest}.
 *
 * @param <T> type of the response content
 * @param <P> search parameter type
 * @param <R> search request type
 */
public abstract class BaseWsFacetedSearchClient<T, P extends Enum<?> & SearchParameter, R extends FacetedSearchRequest<P>>
  extends BaseWsSearchClient<T, P, R> implements SearchService<T, P, R> {

  protected BaseWsFacetedSearchClient(WebResource resource, GenericType<SearchResponse<T, P>> gType) {
    super(resource, gType);
  }

  /**
   * Adds support for faceted search request parameters.
   */
  @Override
  protected MultivaluedMap<String, String> getParameterFromRequest(@Nullable R searchRequest) {
    // The searchRequest is transformed in a parameter map
    MultivaluedMap<String, String> parameters = super.getParameterFromRequest(searchRequest);

    if (searchRequest != null) {
      parameters.putSingle(PARAM_FACET_MULTISELECT, Boolean.toString(searchRequest.isMultiSelectFacets()));
      if (searchRequest.getFacetMinCount() != null) {
        parameters.putSingle(PARAM_FACET_MINCOUNT, Integer.toString(searchRequest.getFacetMinCount()));
      }
      if (searchRequest.getFacetLimit() != null) {
        parameters.putSingle(PARAM_FACET_LIMIT, Integer.toString(searchRequest.getFacetLimit()));
      }
      if (searchRequest.getFacetOffset() != null) {
        parameters.putSingle(PARAM_FACET_OFFSET, Integer.toString(searchRequest.getFacetOffset()));
      }
      if (searchRequest.getFacets() != null) {
        for (P facet : searchRequest.getFacets()) {
          parameters.add(PARAM_FACET, facet.name());
          Pageable facetPage = searchRequest.getFacetPage(facet);
          if (facetPage != null) {
            parameters.add(facet.name() + '.' + PARAM_FACET_OFFSET, Long.toString(facetPage.getOffset()));
            parameters.add(facet.name() + '.' + PARAM_FACET_LIMIT, Long.toString(facetPage.getLimit()));
          }
        }
      }
    }

    return parameters;
  }
}
