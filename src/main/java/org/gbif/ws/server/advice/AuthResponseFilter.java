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
package org.gbif.ws.server.advice;

import org.gbif.ws.util.SecurityConstants;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Response filter that adds a WWW-Authenticate header if response status is Unauthenticated.
 *
 * <p>The challenge uses the GBIF scheme, not Basic: browsers prompt for Basic credentials and then
 * send them with every request to the API, including requests forged by other sites (CSRF).
 * Clients must send their credentials without waiting for a challenge.
 */
@SuppressWarnings("NullableProblems")
@ControllerAdvice
public class AuthResponseFilter implements ResponseBodyAdvice<Object> {

  public static final String CHALLENGE = SecurityConstants.GBIF_SCHEME + " realm=\"GBIF\"";

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (response instanceof ServletServerHttpResponse) {
      int responseStatusCode =
          ((ServletServerHttpResponse) response).getServletResponse().getStatus();

      if (responseStatusCode == HttpStatus.UNAUTHORIZED.value()) {
        response.getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, CHALLENGE);
      }
    }

    return body;
  }
}
