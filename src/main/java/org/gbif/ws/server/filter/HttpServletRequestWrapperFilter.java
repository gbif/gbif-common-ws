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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class HttpServletRequestWrapperFilter extends OncePerRequestFilter {

  private boolean wrapContent;

  public HttpServletRequestWrapperFilter(
      @Value("${gbif.ws.security.wrapContent:true}") boolean wrapContent) {
    this.wrapContent = wrapContent;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    final GbifHttpServletRequestWrapper requestWrapper =
        request instanceof GbifHttpServletRequestWrapper
            ? (GbifHttpServletRequestWrapper) request
            : new GbifHttpServletRequestWrapper(request, wrapContent);

    filterChain.doFilter(requestWrapper, response);
  }
}
