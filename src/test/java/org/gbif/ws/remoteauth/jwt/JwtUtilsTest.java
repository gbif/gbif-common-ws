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
package org.gbif.ws.remoteauth.jwt;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JwtUtilsTest {

  @Test
  public void testFindTokenInRequest() {
    assertEquals(Optional.of("abc.def.ghi"), find("Bearer abc.def.ghi"));
    assertEquals(Optional.of("abc.def.ghi"), find("bearer   abc.def.ghi "));
    // a token containing the word is not truncated
    assertEquals(Optional.of("abcBearerdef"), find("Bearer abcBearerdef"));

    assertEquals(Optional.empty(), find(null));
    assertEquals(Optional.empty(), find("Bearer "));
    // Basic credentials whose Base64 contains "bearer" are not a JWT
    assertEquals(Optional.empty(), find("Basic YmVhcmVyOnBhc3N3b3Jk"));
    assertEquals(Optional.empty(), find("Basic xxbearerxx"));
    assertEquals(Optional.empty(), find("GBIF app:signature"));
  }

  private static Optional<String> find(String authorization) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    if (authorization != null) {
      request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
    }
    return JwtUtils.findTokenInRequest(request);
  }
}
