package org.gbif.ws.server.filter;

import com.google.common.base.Strings;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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
            : new GbifHttpServletRequestWrapper((request));

    // update language headers
    processLanguage(httpRequestWrapper);

    filterChain.doFilter(httpRequestWrapper, response);
  }

  private static void processLanguage(GbifHttpServletRequestWrapper request) {
    String language = Strings.nullToEmpty(request.getParameter("language")).trim();
    if (!language.isEmpty()) {
      // overwrite http language
      request.overwriteLanguageHeader(language.toLowerCase());
    }
  }
}
