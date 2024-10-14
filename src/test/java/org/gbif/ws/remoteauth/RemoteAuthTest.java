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
package org.gbif.ws.remoteauth;

import org.gbif.ws.remoteauth.TestApplication.TestController;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.gbif.ws.remoteauth.LoginServerExtension.ADMIN_USER;
import static org.gbif.ws.remoteauth.LoginServerExtension.INVALID_USER;
import static org.gbif.ws.remoteauth.LoginServerExtension.USER;
import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;

/**
 * Tests the remote auth classes
 *
 * @see org.gbif.ws.remoteauth
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = TestController.class)
@AutoConfigureMockMvc
@AutoConfigureWebClient
@ContextConfiguration(initializers = {RemoteAuthTest.ContextInitializer.class})
public class RemoteAuthTest {

  @RegisterExtension static LoginServerExtension loginServer = new LoginServerExtension();

  @Autowired MockMvc mockMvc;

  @Test
  public void noAuthSentTest() throws Exception {
    // non-secured endpoint
    mockMvc
        .perform(MockMvcRequestBuilders.get("/noAuth"))
        .andExpect(MockMvcResultMatchers.status().isOk());

    // secured endpoint
    mockMvc
        .perform(MockMvcRequestBuilders.get("/admin"))
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  public void securedBasicAuthTest() throws Exception {
    // user with no permissions
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/admin")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + HttpHeaders.encodeBasicAuth(
                            USER.getLoggedUser().getUserName(),
                            USER.getPassword(),
                            StandardCharsets.UTF_8)))
        .andExpect(MockMvcResultMatchers.status().isForbidden());

    // user with permissions
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/admin")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + HttpHeaders.encodeBasicAuth(
                            ADMIN_USER.getLoggedUser().getUserName(),
                            ADMIN_USER.getPassword(),
                            StandardCharsets.UTF_8)))
        .andExpect(MockMvcResultMatchers.status().isOk());

    // invalid user
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/admin")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + HttpHeaders.encodeBasicAuth(
                            INVALID_USER.getLoggedUser().getUserName(),
                            INVALID_USER.getPassword(),
                            StandardCharsets.UTF_8)))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  public void securedJwtTest() throws Exception {
    // user with no permissions
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER.getJwtToken()))
        .andExpect(MockMvcResultMatchers.status().isForbidden());

    // user with permissions
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_USER.getJwtToken()))
        .andExpect(MockMvcResultMatchers.status().isOk());

    // invalid user
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + INVALID_USER.getJwtToken()))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  public void securedGbifAppTest() throws Exception {
    // user with no permissions
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/admin")
                .header(HttpHeaders.AUTHORIZATION, USER.getGbifSchemeHeader())
                .header(HEADER_GBIF_USER, USER.getLoggedUser().getUserName())
                .header(HEADER_CONTENT_MD5, "foo"))
        .andExpect(MockMvcResultMatchers.status().isForbidden());

    // user with permissions
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/admin")
                .header(HttpHeaders.AUTHORIZATION, ADMIN_USER.getGbifSchemeHeader())
                .header(HEADER_GBIF_USER, ADMIN_USER.getLoggedUser().getUserName())
                .header(HEADER_CONTENT_MD5, "foo"))
        .andExpect(MockMvcResultMatchers.status().isOk());

    // invalid user
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/admin")
                .header(HttpHeaders.AUTHORIZATION, INVALID_USER.getGbifSchemeHeader())
                .header(HEADER_GBIF_USER, INVALID_USER.getLoggedUser().getUserName())
                .header(HEADER_CONTENT_MD5, "foo"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  static class ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of("login.url=" + loginServer.getWireMockServer().baseUrl())
          .applyTo(configurableApplicationContext.getEnvironment());
    }
  }
}
