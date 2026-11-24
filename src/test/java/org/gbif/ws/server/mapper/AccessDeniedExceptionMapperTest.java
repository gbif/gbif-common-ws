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
package org.gbif.ws.server.mapper;

import org.gbif.ws.security.GbifAuthenticationToken;
import org.gbif.ws.server.advice.AuthResponseFilter;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests that requests without credentials get a 401, not a 403 (gbif/security-issues#48). */
public class AccessDeniedExceptionMapperTest {

  @RestController
  static class DeniedController {
    @GetMapping("/denied")
    public String denied() {
      throw new AccessDeniedException("Access is denied");
    }
  }

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new DeniedController())
          .setControllerAdvice(new AccessDeniedExceptionMapper(), new AuthResponseFilter())
          .build();

  @BeforeEach
  @AfterEach
  public void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void testNoAuthenticationGets401WithChallenge() throws Exception {
    mvc.perform(get("/denied"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, AuthResponseFilter.CHALLENGE));
  }

  @Test
  public void testAnonymousGets401WithChallenge() throws Exception {
    authenticate(GbifAuthenticationToken.anonymous());
    mvc.perform(get("/denied"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, AuthResponseFilter.CHALLENGE));
  }

  @Test
  public void testAuthenticatedUserWithoutRoleGets403() throws Exception {
    authenticate(
        new UsernamePasswordAuthenticationToken(
            "user", null, Collections.singletonList(new SimpleGrantedAuthority("USER"))));
    mvc.perform(get("/denied"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
  }

  private static void authenticate(Authentication authentication) {
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
