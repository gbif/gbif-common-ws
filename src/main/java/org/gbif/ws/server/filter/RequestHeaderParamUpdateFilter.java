/*
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

import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A request filter that overwrites a few common http headers if their query parameter counterparts
 * are given. In particular the query parameters:
 *
 * <dl>
 *   <dt>language
 *   <dd>overwrites the Accept-Language header with the given language
 * </dl>
 */
@Component
public class RequestHeaderParamUpdateFilter extends OncePerRequestFilter {

  /**
   * A request filter that overwrites a few common http headers if their query parameter
   * counterparts are given.
   *
   * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">HTTP 1.1 RFC 2616</a>
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    GbifHttpServletRequestWrapper httpRequestWrapper =
        request instanceof GbifHttpServletRequestWrapper
            ? (GbifHttpServletRequestWrapper) request
            : new GbifHttpServletRequestWrapper(request);

    // update language headers
    processLanguage(httpRequestWrapper);

    filterChain.doFilter(httpRequestWrapper, response);
  }

  private static void processLanguage(GbifHttpServletRequestWrapper request) {
    String language = StringUtils.trimToEmpty(request.getParameter("language"));
    if (!language.isEmpty()) {
      // overwrite http language
      request.overwriteLanguageHeader(language.toLowerCase());
    }
  }
}
