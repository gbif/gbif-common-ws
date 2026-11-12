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

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests that request bodies are buffered when wrapContent is true, kept byte for byte, and only
 * within a size limit (gbif/informatics#54).
 */
public class HttpServletRequestWrapperFilterTest {

  private static final int MAX = 16;
  // not valid UTF-8, must be kept byte for byte
  private static final byte[] BODY = {(byte) 0xff, (byte) 0xfe, 'a', 'b', 0, 'c'};

  private final HttpServletRequestWrapperFilter filter =
      new HttpServletRequestWrapperFilter(true, MAX);

  @Test
  public void testGbifSignedRequestIsBuffered() throws Exception {
    MockHttpServletRequest request = post(BODY, "GBIF app:signature");

    GbifHttpServletRequestWrapper wrapped = doFilter(request, new MockHttpServletResponse());

    assertArrayEquals(BODY, wrapped.getContentBytes());
    // the buffered body can be read more than once
    assertArrayEquals(BODY, IOUtils.toByteArray(wrapped.getInputStream()));
    assertArrayEquals(BODY, IOUtils.toByteArray(wrapped.getInputStream()));
  }

  /** The registry's editor authorization filters read the body of Basic and JWT requests too. */
  @Test
  public void testOtherRequestsAreBuffered() throws Exception {
    for (String authorization : new String[] {null, "Basic dXNlcjpwYXNzd29yZA==", "Bearer a.b.c"}) {
      GbifHttpServletRequestWrapper wrapped =
          doFilter(post(BODY, authorization), new MockHttpServletResponse());

      assertArrayEquals(BODY, wrapped.getContentBytes(), authorization);
    }
  }

  @Test
  public void testNoLimit() throws Exception {
    MockHttpServletRequest request = post(new byte[MAX * 4], "Basic dXNlcjpwYXNzd29yZA==");
    MockFilterChain chain = new MockFilterChain();

    new HttpServletRequestWrapperFilter(true, GbifHttpServletRequestWrapper.UNLIMITED)
        .doFilter(request, new MockHttpServletResponse(), chain);

    assertEquals(
        MAX * 4, ((GbifHttpServletRequestWrapper) chain.getRequest()).getContentBytes().length);
  }

  @Test
  public void testWrapContentDisabled() throws Exception {
    MockHttpServletRequest request = post(BODY, "GBIF app:signature");

    MockFilterChain chain = new MockFilterChain();
    new HttpServletRequestWrapperFilter(false, MAX)
        .doFilter(request, new MockHttpServletResponse(), chain);

    assertNull(((GbifHttpServletRequestWrapper) chain.getRequest()).getContentBytes());
  }

  @Test
  public void testTooLargeBodyIsRejected() throws Exception {
    MockHttpServletRequest request = post(new byte[MAX + 1], "GBIF app:signature");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(413, response.getStatus());
    assertNull(chain.getRequest());
  }

  @Test
  public void testTooLargeBodyWithoutContentLengthIsRejected() throws Exception {
    // chunked request: no Content-Length, the limit applies while reading
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/dataset") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    request.setContent(new byte[MAX + 1]);
    request.addHeader(HttpHeaders.AUTHORIZATION, "GBIF app:signature");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(413, response.getStatus());
    assertNull(chain.getRequest());
  }

  @Test
  public void testContentWithoutCharsetIsUtf8() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/dataset");
    request.setContent("søren".getBytes(StandardCharsets.UTF_8));

    assertEquals("søren", new GbifHttpServletRequestWrapper(request, true).getContent());
  }

  @Test
  public void testNotWrappedInputStreamIsTheRequestStream() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/dataset");
    request.setContent(BODY);

    // used to throw a NullPointerException
    GbifHttpServletRequestWrapper wrapped = new GbifHttpServletRequestWrapper(request, null, false);

    assertArrayEquals(BODY, IOUtils.toByteArray(wrapped.getInputStream()));
  }

  private GbifHttpServletRequestWrapper doFilter(
      MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, response, chain);
    return assertInstanceOf(GbifHttpServletRequestWrapper.class, chain.getRequest());
  }

  private static MockHttpServletRequest post(byte[] body, String authorization) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/dataset");
    request.setContent(body);
    if (authorization != null) {
      request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
    }
    return request;
  }
}
