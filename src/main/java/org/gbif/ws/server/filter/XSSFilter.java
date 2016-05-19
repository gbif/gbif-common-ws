package org.gbif.ws.server.filter;

import org.gbif.api.model.common.messaging.Response;
import org.gbif.ws.util.XSSUtil;

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;

/**
 * Request filter that detects XSS in paramater, header or querystring values and responds with a 400 bad request in such cases.
 */
@Singleton
public class XSSFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (request instanceof HttpServletRequest) {
      HttpServletRequest httpReq = (HttpServletRequest) request;
      // test for xss in headers
      Enumeration<String> headers =  httpReq.getHeaderNames();
      while(headers.hasMoreElements()) {
        if (XSSUtil.containsXSS(httpReq.getHeader(headers.nextElement()))) {
          respondBadRequest(response);
        }
      }
      // test for xss in querystring
      // note: servlet container does not decode querystring when creating HttpServletRequest object
      if (XSSUtil.containsXSS(httpReq.getQueryString())) {
        respondBadRequest(response);
      }
      // test for xss in parameters
      // note: servlet container decodes parameter strings when creating HttpServletRequest object
      Enumeration<String> params =  httpReq.getParameterNames();
      while(params.hasMoreElements()) {
        if (XSSUtil.containsXSS(httpReq.getParameter(params.nextElement()))) {
          respondBadRequest(response);
        }
      }
    }
    chain.doFilter(request, response);
  }

  /**
   * Sends a 400 error response, provided the response hasn't already been committed.
   *
   * @param response response
   *
   * @throws IOException if there is a problem committing response
   */
  private static void respondBadRequest(ServletResponse response) throws IOException {
    HttpServletResponse resp = (HttpServletResponse) response;
    if (!resp.isCommitted()) {
      resp.sendError(Response.StatusCode.BAD_REQUEST.getCode());
    }
  }

}
