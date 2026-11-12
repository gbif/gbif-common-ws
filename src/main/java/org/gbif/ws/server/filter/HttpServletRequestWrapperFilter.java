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

import org.gbif.ws.WebApplicationException;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;
import org.gbif.ws.util.SecurityConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Wraps the request in a {@link GbifHttpServletRequestWrapper}.
 *
 * <p>The body is buffered only for requests signed with the GBIF scheme, the only ones whose body
 * is verified (Content-MD5) or signed again, and only when gbif.ws.security.wrapContent is true
 * (the default). Buffered bodies larger than gbif.ws.security.maxBufferedBodyBytes (default 10 MB)
 * are rejected with 413.
 */
@Component
public class HttpServletRequestWrapperFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(HttpServletRequestWrapperFilter.class);

  public static final long DEFAULT_MAX_BUFFERED_BODY_BYTES = 10L * 1024 * 1024;

  private final boolean wrapContent;
  private final long maxBufferedBodyBytes;

  public HttpServletRequestWrapperFilter(boolean wrapContent) {
    this(wrapContent, DEFAULT_MAX_BUFFERED_BODY_BYTES);
  }

  @Autowired
  public HttpServletRequestWrapperFilter(
      @Value("${gbif.ws.security.wrapContent:true}") boolean wrapContent,
      @Value("${gbif.ws.security.maxBufferedBodyBytes:" + DEFAULT_MAX_BUFFERED_BODY_BYTES + "}")
          long maxBufferedBodyBytes) {
    this.wrapContent = wrapContent;
    this.maxBufferedBodyBytes = maxBufferedBodyBytes;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    final GbifHttpServletRequestWrapper requestWrapper;
    if (request instanceof GbifHttpServletRequestWrapper) {
      requestWrapper = (GbifHttpServletRequestWrapper) request;
    } else {
      try {
        requestWrapper =
            new GbifHttpServletRequestWrapper(
                request, wrapContent && isGbifSigned(request), maxBufferedBodyBytes);
      } catch (WebApplicationException e) {
        LOG.info("Rejected request to {}: {}", request.getRequestURI(), e.getMessage());
        response.setStatus(e.getStatus());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().println(e.getMessage());
        return;
      }
    }

    filterChain.doFilter(requestWrapper, response);
  }

  private static boolean isGbifSigned(HttpServletRequest request) {
    return StringUtils.startsWith(
        request.getHeader(HttpHeaders.AUTHORIZATION), SecurityConstants.GBIF_SCHEME_PREFIX);
  }
}
