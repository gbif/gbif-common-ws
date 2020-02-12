package org.gbif.ws.server.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HttpServletRequestWrapperFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    final GbifHttpServletRequestWrapper requestWrapper =
        request instanceof GbifHttpServletRequestWrapper
            ? (GbifHttpServletRequestWrapper) request
            : new GbifHttpServletRequestWrapper(request);

    filterChain.doFilter(requestWrapper, response);
  }
}
