package org.gbif.ws.server.filter;

import java.util.List;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * Filter that always adds a CORS related headers to the response
 * that will make all GBIF webservices available for simple cross domain calls without JSONP.
 *
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS">Mozilla documentation</a>
 * @see <a href="http://en.wikipedia.org/wiki/Cross-origin_resource_sharing">Wikipedia</a>
 * @see <a href="http://www.cypressnorth.com/blog/programming/cross-domain-ajax-request-with-xml-response-for-iefirefoxchrome-safari-jquery/">Cross domain blog</a>
 */
public class CrossDomainResponseFilter implements ContainerResponseFilter {

  @Override
  public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
    response.getHttpHeaders().putSingle("Access-Control-Allow-Origin", "*");

    response.getHttpHeaders().putSingle("Access-Control-Allow-Methods", "HEAD, GET, POST, DELETE, PUT, OPTIONS");

    //Used in response to a preflight request to indicate which HTTP headers can be used when making the actual request.
    //https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Preflighted_requests
    //we reflect the headers specified in the Access-Control-Request-Headers header of the request
    if(request.getRequestHeaders().containsKey("Access-Control-Request-Headers")){
      response.getHttpHeaders().put("Access-Control-Allow-Headers", (List)request.getRequestHeader("Access-Control-Request-Headers"));
    }

    return response;
  }
}
