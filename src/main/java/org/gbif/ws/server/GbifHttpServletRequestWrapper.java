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
package org.gbif.ws.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;

import com.google.common.base.Strings;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class GbifHttpServletRequestWrapper extends HttpServletRequestWrapper {

  private String content;

  private HttpHeaders httpHeaders;

  public GbifHttpServletRequestWrapper(HttpServletRequest request) {
    this(request, null);
  }

  public GbifHttpServletRequestWrapper(HttpServletRequest request, String contentAsString) {
    super(request);

    try {
      if (!Strings.isNullOrEmpty(contentAsString)) {
        content = contentAsString;
      } else if (request.getInputStream() != null) {
        content = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
      } else {
        content = null;
      }
    } catch (IOException e) {
      throw new RuntimeException("Stream can't be read", e);
    }

    httpHeaders = getHttpHeaders(request);
  }

  @Override
  public ServletInputStream getInputStream() {
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content.getBytes());
    return new DelegatingServletInputStream(byteArrayInputStream);
  }

  @Override
  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(this.getInputStream()));
  }

  private HttpHeaders getHttpHeaders(HttpServletRequest request) {
    final HttpHeaders requestHeaders = new HttpHeaders();
    Enumeration<String> headerNames = request.getHeaderNames();

    if (headerNames != null) {
      while (headerNames.hasMoreElements()) {
        String currentHeaderName = headerNames.nextElement();
        requestHeaders.set(currentHeaderName, request.getHeader(currentHeaderName));
      }
    }

    return requestHeaders;
  }

  public String getContent() {
    return content;
  }

  public HttpHeaders getHttpHeaders() {
    return new HttpHeaders(httpHeaders);
  }

  public void overwriteLanguageHeader(String newValue) {
    httpHeaders.set(HttpHeaders.ACCEPT_LANGUAGE, newValue);
  }

  @Override
  public String getHeader(String name) {
    if (getHttpHeaders().containsKey(name)) {
      return getHttpHeaders().getFirst(name);
    }
    return super.getHeader(name);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return Collections.enumeration(httpHeaders.keySet());
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    List<String> values = new ArrayList<>();
    if (httpHeaders.containsKey(name)) {
      Optional.ofNullable(httpHeaders.get(name)).ifPresent(values::addAll);
    }
    return Collections.enumeration(values);
  }
}
