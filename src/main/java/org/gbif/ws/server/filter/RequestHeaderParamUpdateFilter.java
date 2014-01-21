/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.ws.server.filter;

import org.gbif.ws.util.ExtraMediaTypes;

import javax.ws.rs.core.HttpHeaders;

import com.google.common.base.Strings;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 * A request filter that overwrites a few common http headers if their query parameter counterparts are given. In
 * particular the query parameters:
 * <dl>
 * <dt>callback</dt>
 * <dd>overwrites the Accept header with {@code application/javascript}</dd>
 * <dt>language</dt>
 * <dd>overwrites the Accept-Language header with the given language</dd>
 * </dl>
 */
public class RequestHeaderParamUpdateFilter implements ContainerRequestFilter {

  /**
   * A request filter that overwrites a few common http headers if their query parameter counterparts
   * are given.
   *
   * @param request the request.
   *
   * @return the modified or original request.
   *
   * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">HTTP 1.1 RFC 2616</a>
   */
  @Override
  public ContainerRequest filter(ContainerRequest request) {

    // update media type for JSONP request
    processJsonp(request);

    // update language headers
    processLanguage(request);

    return request;
  }

  private static void processLanguage(HttpRequestContext request) {
    String language = Strings.nullToEmpty(request.getQueryParameters().getFirst("language")).trim();
    if (!language.isEmpty()) {
      // overwrite http language
      request.getRequestHeaders().putSingle(HttpHeaders.ACCEPT_LANGUAGE, language.toLowerCase());
    }
  }

  private static void processJsonp(HttpRequestContext request) {
    String callback = Strings.nullToEmpty(request.getQueryParameters().getFirst("callback")).trim();
    if (!callback.isEmpty()) {
      // this is a jsonp request - force content type to javascript
      request.getRequestHeaders().putSingle(HttpHeaders.ACCEPT, ExtraMediaTypes.APPLICATION_JAVASCRIPT);
    }
  }
}
