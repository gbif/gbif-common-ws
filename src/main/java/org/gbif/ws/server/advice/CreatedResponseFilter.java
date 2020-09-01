/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.ws.server.advice;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Filter that updates http headers when a new resource is successfully created via a POST request unless
 * the response returns 204 No Content.
 * <p>
 * The following headers are added or replaced if they existed:
 * <ul>
 *   <li>Http response code 201</li>
 *   <li>Location header is set accordingly based on returned key</li>
 * </ul>
 */
@SuppressWarnings("NullableProblems")
@ControllerAdvice
public class CreatedResponseFilter implements ResponseBodyAdvice<Object> {

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
    final int intStatus = ((ServletServerHttpResponse) response).getServletResponse().getStatus();
    final HttpStatus httpStatus = HttpStatus.resolve(intStatus);

    if (request.getMethod() != null
        && httpStatus != null
        && request.getMethod() == HttpMethod.POST
        && httpStatus != HttpStatus.NO_CONTENT
        && httpStatus.is2xxSuccessful()) {
      response.setStatusCode(HttpStatus.CREATED);

      // TODO: 15/01/2020 if response contains the key, also set Location
    }

    return body;
  }
}
