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

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Tests that 401 responses do not make browsers prompt for Basic credentials (gbif/informatics#59). */
public class AuthResponseFilterTest {

  private final AuthResponseFilter filter = new AuthResponseFilter();

  @Test
  public void testUnauthorizedGetsGbifChallenge() throws Exception {
    MockHttpServletResponse servletResponse = respond(401);

    assertEquals(
        "GBIF realm=\"GBIF\"", servletResponse.getHeader(HttpHeaders.WWW_AUTHENTICATE));
  }

  @Test
  public void testOtherStatusesGetNoChallenge() throws Exception {
    assertNull(respond(200).getHeader(HttpHeaders.WWW_AUTHENTICATE));
    assertNull(respond(403).getHeader(HttpHeaders.WWW_AUTHENTICATE));
  }

  private MockHttpServletResponse respond(int status) throws Exception {
    MockHttpServletResponse servletResponse = new MockHttpServletResponse();
    servletResponse.setStatus(status);
    ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

    filter.beforeBodyWrite("body", null, null, null, null, response);
    response.flush();
    return servletResponse;
  }
}
