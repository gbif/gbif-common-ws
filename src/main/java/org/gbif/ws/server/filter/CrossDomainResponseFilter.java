package org.gbif.ws.server.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * Filter that always adds a Access-Control-Allow-Origin: * header to the response
 * that will make all GBIF webservices available for simple cross domain calls without JSONP.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Cross-origin_resource_sharing">Wikipedia</a>
 * @see <a href="http://www.cypressnorth.com/blog/programming/cross-domain-ajax-request-with-xml-response-for-iefirefoxchrome-safari-jquery/">Cross domain blog</a>
 */
public class CrossDomainResponseFilter implements ContainerResponseFilter {

  @Override
  public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
    response.getHttpHeaders().putSingle("Access-Control-Allow-Origin", "*");
    return response;
  }
}
