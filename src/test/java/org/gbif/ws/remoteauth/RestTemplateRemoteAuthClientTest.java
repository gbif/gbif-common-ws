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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests which failures of the login service are retried. */
public class RestTemplateRemoteAuthClientTest {

  private static final String PATH = "/user/auth/basic";

  private WireMockServer wireMockServer;
  private RestTemplateRemoteAuthClient client;

  @BeforeEach
  public void setUp() {
    wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    wireMockServer.start();
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(wireMockServer.baseUrl()));
    client = new RestTemplateRemoteAuthClient(restTemplate);
  }

  @AfterEach
  public void tearDown() {
    wireMockServer.stop();
  }

  @Test
  public void retriesWhileServiceUnavailableTest() {
    wireMockServer.stubFor(
        post(PATH)
            .inScenario("unavailable")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(HttpStatus.SERVICE_UNAVAILABLE.value()))
            .willSetStateTo("available"));
    wireMockServer.stubFor(
        post(PATH)
            .inScenario("unavailable")
            .whenScenarioStateIs("available")
            .willReturn(aResponse().withBody("{}")));

    ResponseEntity<String> response = client.remoteAuth(PATH, new HttpHeaders());

    assertEquals(HttpStatus.OK, response.getStatusCode());
    wireMockServer.verify(2, postRequestedFor(urlEqualTo(PATH)));
  }

  @Test
  public void stopsRetryingAfterMaxAttemptsTest() {
    wireMockServer.stubFor(
        post(PATH).willReturn(aResponse().withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())));

    assertThrows(RestClientException.class, () -> client.remoteAuth(PATH, new HttpHeaders()));
    wireMockServer.verify(3, postRequestedFor(urlEqualTo(PATH)));
  }

  @Test
  public void badCredentialsNotRetriedTest() {
    wireMockServer.stubFor(
        post(PATH).willReturn(aResponse().withStatus(HttpStatus.UNAUTHORIZED.value())));

    assertThrows(BadCredentialsException.class, () -> client.remoteAuth(PATH, new HttpHeaders()));
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(PATH)));
  }
}
