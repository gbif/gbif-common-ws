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
package org.gbif.ws.server.filter.jwt;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Intercepts all requests to look for a JWT token.
 */
public class JwtFilter extends OncePerRequestFilter {

  private final RestTemplate restTemplate;

  private final String gbifApiLoginUrl;

  private static final String LOGIN_PATH = "user/login/jwt";

  public JwtFilter(RestTemplate restTemplate, String gbifApiUrl) {
    this.restTemplate = restTemplate;
    this.gbifApiLoginUrl = gbifApiUrl + LOGIN_PATH;
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {

    JwtUtils.findTokenInRequest(request)
      .ifPresent(
        token -> {
          try {
            ResponseEntity<String> authResponse = restTemplate.postForEntity(gbifApiLoginUrl, new HttpEntity<>(jwtHeaders(token)), String.class);
            if (authResponse.getStatusCode().isError()) {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
          } catch (Exception exc) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          }
        });
    filterChain.doFilter(request, response);
  }

  public HttpHeaders jwtHeaders(String jwtToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);
    return headers;
  }
}
