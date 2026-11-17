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
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class GbifHttpServletRequestWrapper extends HttpServletRequestWrapper {

  /** No limit on the size of a buffered body. */
  public static final long UNLIMITED = -1;

  private static final int PAYLOAD_TOO_LARGE = 413;

  /** The buffered request body, null when the content is not wrapped. */
  private byte[] content;

  /** Query and form parameters of a buffered form body, parsed on first use. */
  private Map<String, String[]> formParameters;

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

  /** The request charset, UTF-8 if none is given or it is not supported. */
  private static Charset getCharset(HttpServletRequest request) {
    String encoding = request.getCharacterEncoding();
    if (encoding == null) {
      return StandardCharsets.UTF_8;
    }
    try {
      return Charset.forName(encoding);
    } catch (IllegalArgumentException e) {
      // IllegalCharsetNameException and UnsupportedCharsetException
      return StandardCharsets.UTF_8;
    }
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

  /**
   * The servlet container parses form parameters from the request input stream, which is empty once
   * the body is buffered. The parameters of a buffered application/x-www-form-urlencoded body are
   * therefore parsed from the buffer and added to the query parameters the container still reads.
   */
  @Override
  public String getParameter(String name) {
    if (!isBufferedForm()) {
      return super.getParameter(name);
    }
    String[] values = getFormParameters().get(name);
    return values != null && values.length > 0 ? values[0] : null;
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return isBufferedForm() ? getFormParameters() : super.getParameterMap();
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return isBufferedForm()
        ? Collections.enumeration(getFormParameters().keySet())
        : super.getParameterNames();
  }

  @Override
  public String[] getParameterValues(String name) {
    if (!isBufferedForm()) {
      return super.getParameterValues(name);
    }
    String[] values = getFormParameters().get(name);
    return values != null ? values.clone() : null;
  }

  private boolean isBufferedForm() {
    return content != null && isFormUrlEncoded(this);
  }

  /** @return true if the request has an application/x-www-form-urlencoded body */
  public static boolean isFormUrlEncoded(HttpServletRequest request) {
    return contentTypeIncludedIn(request, MediaType.APPLICATION_FORM_URLENCODED);
  }

  /** @return true if the request has a multipart body */
  public static boolean isMultipart(HttpServletRequest request) {
    return contentTypeIncludedIn(request, MediaType.parseMediaType("multipart/*"));
  }

  private static boolean contentTypeIncludedIn(HttpServletRequest request, MediaType mediaType) {
    String contentType = request.getContentType();
    if (contentType == null) {
      return false;
    }
    try {
      return mediaType.includes(MediaType.parseMediaType(contentType));
    } catch (InvalidMediaTypeException e) {
      return false;
    }
  }

  private Map<String, String[]> getFormParameters() {
    if (formParameters == null) {
      Map<String, List<String>> parameters = new LinkedHashMap<>();
      super.getParameterMap()
          .forEach(
              (name, values) ->
                  parameters
                      .computeIfAbsent(name, k -> new ArrayList<>())
                      .addAll(Arrays.asList(values)));

      Charset charset = getCharset(this);
      for (String pair : new String(content, charset).split("&")) {
        if (pair.isEmpty()) {
          continue;
        }
        int separator = pair.indexOf('=');
        try {
          String name =
              URLDecoder.decode(separator < 0 ? pair : pair.substring(0, separator), charset);
          String value =
              separator < 0 ? "" : URLDecoder.decode(pair.substring(separator + 1), charset);
          parameters.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        } catch (IllegalArgumentException e) {
          // malformed percent encoding, the parameter is ignored as servlet containers do
        }
      }

      Map<String, String[]> result = new LinkedHashMap<>();
      parameters.forEach((name, values) -> result.put(name, values.toArray(new String[0])));
      formParameters = Collections.unmodifiableMap(result);
    }
    return formParameters;
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
   * @return a copy of the buffered request body exactly as received, or null if the content was
   *     not wrapped
   */
  public byte[] getContentBytes() {
    return content != null ? content.clone() : null;
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
