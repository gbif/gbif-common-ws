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
package org.gbif.ws.security.noauth;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.security.GbifAuthenticationToken;
import org.gbif.ws.security.noauth.NoAuthTestApplication.TestController;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests that anonymous requests are not treated as authenticated (gbif/informatics#53).
 */
@WebMvcTest(controllers = TestController.class)
@AutoConfigureMockMvc
public class NoAuthWebSecurityTest {

  private static final String PASSWORD = "password";

  @Autowired MockMvc mockMvc;

  @MockitoBean IdentityAccessService identityAccessService;

  @MockitoBean GbifAuthService gbifAuthService;

  @BeforeEach
  public void setUp() {
    when(identityAccessService.authenticate("user", PASSWORD)).thenReturn(user("user", UserRole.USER));
    when(identityAccessService.authenticate("admin", PASSWORD))
        .thenReturn(user("admin", UserRole.REGISTRY_ADMIN));
  }

  @Test
  public void anonymousTokenIsNotAuthenticatedTest() {
    Authentication anonymous = GbifAuthenticationToken.anonymous();
    AuthenticationTrustResolverImpl trustResolver = new AuthenticationTrustResolverImpl();

    assertTrue(trustResolver.isAnonymous(anonymous));
    assertFalse(trustResolver.isAuthenticated(anonymous));
    assertFalse(trustResolver.isFullyAuthenticated(anonymous));
  }

  @Test
  public void anonymousTest() throws Exception {
    mockMvc.perform(get("/open")).andExpect(status().isOk());
    mockMvc.perform(get("/authenticated")).andExpect(status().isForbidden());
    mockMvc.perform(get("/admin")).andExpect(status().isForbidden());
  }

  @Test
  public void userTest() throws Exception {
    mockMvc.perform(basic(get("/open"), "user")).andExpect(status().isOk());
    mockMvc.perform(basic(get("/authenticated"), "user")).andExpect(status().isOk());
    mockMvc.perform(basic(get("/admin"), "user")).andExpect(status().isForbidden());
  }

  @Test
  public void adminTest() throws Exception {
    mockMvc.perform(basic(get("/authenticated"), "admin")).andExpect(status().isOk());
    mockMvc.perform(basic(get("/admin"), "admin")).andExpect(status().isOk());
  }

  private static MockHttpServletRequestBuilder get(String path) {
    return MockMvcRequestBuilders.get(path);
  }

  private static MockHttpServletRequestBuilder basic(
      MockHttpServletRequestBuilder request, String userName) {
    return request.header(
        HttpHeaders.AUTHORIZATION, "Basic " + HttpHeaders.encodeBasicAuth(userName, PASSWORD, null));
  }

  private static GbifUser user(String userName, UserRole role) {
    GbifUser user = new GbifUser();
    user.setUserName(userName);
    user.setRoles(Collections.singleton(role));
    return user;
  }
}
