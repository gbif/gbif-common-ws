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
package org.gbif.ws.server;

import org.gbif.ws.WebApplicationException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class GbifHttpServletRequestWrapper extends HttpServletRequestWrapper {

  /** No limit on the size of a buffered body. */
  public static final long UNLIMITED = -1;

  private static final int PAYLOAD_TOO_LARGE = 413;

  /** The buffered request body, null when the content is not wrapped. */
  private byte[] content;

  private HttpHeaders httpHeaders;

  public GbifHttpServletRequestWrapper(HttpServletRequest request) {
    this(request, false);
  }

  /**
   * @param wrapContent buffer the request body, so it can be read more than once
   */
  public GbifHttpServletRequestWrapper(HttpServletRequest request, boolean wrapContent) {
    this(request, wrapContent, UNLIMITED);
  }

  /**
   * @param wrapContent     buffer the request body, so it can be read more than once
   * @param maxContentBytes maximum size of the buffered body, or {@link #UNLIMITED}
   * @throws WebApplicationException with status 413 if the body is larger than maxContentBytes
   */
  public GbifHttpServletRequestWrapper(
      HttpServletRequest request, boolean wrapContent, long maxContentBytes) {
    super(request);
    if (wrapContent) {
      content = readContent(request, maxContentBytes);
    }
    httpHeaders = getHttpHeaders(request);
  }

  public GbifHttpServletRequestWrapper(
      HttpServletRequest request, String contentAsString, boolean wrapContent) {
    super(request);
    if (StringUtils.isNotEmpty(contentAsString)) {
      content = contentAsString.getBytes(getCharset(request));
    } else if (wrapContent) {
      content = readContent(request, UNLIMITED);
    }
    httpHeaders = getHttpHeaders(request);
  }

  private static byte[] readContent(HttpServletRequest request, long maxContentBytes) {
    if (maxContentBytes >= 0 && request.getContentLengthLong() > maxContentBytes) {
      throw tooLarge(maxContentBytes);
    }
    try {
      InputStream in = request.getInputStream();
      if (in == null) {
        return null;
      }
      if (maxContentBytes < 0) {
        return IOUtils.toByteArray(in);
      }
      // Content-Length can be missing (chunked encoding): read at most one byte over the limit
      byte[] bytes =
          IOUtils.toByteArray(
              BoundedInputStream.builder()
                  .setInputStream(in)
                  .setMaxCount(maxContentBytes + 1)
                  .get());
      if (bytes.length > maxContentBytes) {
        throw tooLarge(maxContentBytes);
      }
      return bytes;
    } catch (IOException e) {
      throw new RuntimeException("Stream can't be read", e);
    }
  }

  private static WebApplicationException tooLarge(long maxContentBytes) {
    return new WebApplicationException(
        "Request body larger than " + maxContentBytes + " bytes", PAYLOAD_TOO_LARGE);
  }

  /** The request charset, UTF-8 if none is given. */
  private static Charset getCharset(HttpServletRequest request) {
    String encoding = request.getCharacterEncoding();
    return encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    if (content == null) {
      return super.getInputStream();
    }
    return new DelegatingServletInputStream(new ByteArrayInputStream(content));
  }

  @Override
  public BufferedReader getReader() throws IOException {
    if (content == null) {
      return super.getReader();
    }
    return new BufferedReader(new InputStreamReader(getInputStream(), getCharset(this)));
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

  /**
   * @return the buffered request body decoded with the request charset (UTF-8 if none is given),
   * or null if the content was not wrapped
   */
  public String getContent() {
    return content != null ? new String(content, getCharset(this)) : null;
  }

  /**
   * @return the buffered request body exactly as received, or null if the content was not wrapped
   */
  public byte[] getContentBytes() {
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
