package org.gbif.ws.server.filter;

import org.gbif.ws.util.ExtraMediaTypes;

import javax.ws.rs.core.MediaType;

import com.google.common.base.Strings;
import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * Filter that optionally wraps a JSON request in a JSONP response.
 * <p/>
 * For this wrapping to happen two things need to be true:
 * <ul>
 * <li>The Media type of the response must be set to JSON or application/javascript</li>
 * <li>The request must have a query parameter called {@code callback}</li>
 * </ul>
 * <p/>
 *
 * @see <a href="http://weblogs.java.net/blog/felipegaucho/archive/2010/02/25/jersey-feat-jquery-jsonp">JSONP with
 *      Jersey and jQuery</a>
 */
public class JsonpResponseFilter implements ContainerResponseFilter {

  @Override
  public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
    if (response.getMediaType() == null || !response.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE) && !response
      .getMediaType().equals(ExtraMediaTypes.APPLICATION_JAVASCRIPT_TYPE)) {
      return response;
    }

    String callback = Strings.nullToEmpty(request.getQueryParameters().getFirst("callback"));
    if (callback.isEmpty()) {
      return response;
    }

    if (response.getEntity() != null) {
      JSONWithPadding jsonp = new JSONWithPadding(response.getEntity(), callback);
      response.setEntity(jsonp);
      return response;
    }

    return response;
  }
}
