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

import java.util.Collections;
import java.util.UUID;

import org.gbif.ws.security.UserRoles;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.Builder;
import lombok.Data;

import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME_PREFIX;
import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;
import static org.springframework.http.HttpHeaders.*;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

/**
 * This class mocks the auth services of the registry-ws that are used in the authentication providers of the remote
 * auth classes.
 */
public class LoginServerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static final TestUser ADMIN_USER = TestUser.builder().loggedUser(
      LoggedUser.builder()
          .userName("admin")
          .email("admin@gbif.org")
          .roles(Collections.singleton(UserRoles.ADMIN_ROLE))
          .build()).password("pass1").jwtToken("token1").appKey("app1").appSecret("secret1").build();

  public static final TestUser USER =
      TestUser.builder().loggedUser(LoggedUser.builder()
          .userName("user")
          .email("user@gbif.org")
          .roles(Collections.singleton(UserRoles.USER_ROLE))
          .build()).password("pass2").jwtToken("token2").appKey("app2").appSecret("secret2").build();

  public static final TestUser INVALID_USER =
      TestUser.builder().loggedUser(LoggedUser.builder()
          .userName("invalid")
          .email("invalid@gbif.org")
          .roles(Collections.singleton(UserRoles.USER_ROLE))
          .build()).password("pass3").jwtToken("token3").appKey("app3").appSecret("secret3").build();

  private final WireMockServer wireMockServer =
      new WireMockServer(WireMockConfiguration.DYNAMIC_PORT);

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    wireMockServer.stubFor(
        post("/user/auth/basic")
            .withBasicAuth(USER.getLoggedUser().getUserName(), USER.getPassword())
            .willReturn(
                aResponse().withBody(OBJECT_MAPPER.writeValueAsString(USER.getLoggedUser()))));
    wireMockServer.stubFor(
        post("/user/auth/basic")
            .withBasicAuth(ADMIN_USER.getLoggedUser().getUserName(), ADMIN_USER.getPassword())
            .willReturn(
                aResponse().withBody(OBJECT_MAPPER.writeValueAsString(ADMIN_USER.getLoggedUser()))));
    wireMockServer.stubFor(
        post("/user/auth/basic")
            .withBasicAuth(INVALID_USER.getLoggedUser().getUserName(), INVALID_USER.getPassword())
            .willReturn(
                aResponse().withStatus(HttpStatus.UNAUTHORIZED.value())));
    wireMockServer.stubFor(
        post("/user/auth/jwt")
            .withHeader(AUTHORIZATION, equalTo("Bearer " + USER.getJwtToken()))
            .willReturn(
                aResponse()
                    .withBody(OBJECT_MAPPER.writeValueAsString(USER.getLoggedUser()))
                    .withHeader("token", UUID.randomUUID().toString())));
    wireMockServer.stubFor(
        post("/user/auth/jwt")
            .withHeader(AUTHORIZATION, equalTo("Bearer " + ADMIN_USER.getJwtToken()))
            .willReturn(
                aResponse()
                    .withBody(OBJECT_MAPPER.writeValueAsString(ADMIN_USER.getLoggedUser()))
                    .withHeader("token", UUID.randomUUID().toString())));
    wireMockServer.stubFor(
        post("/user/auth/jwt")
            .withHeader(AUTHORIZATION, equalTo("Bearer " + INVALID_USER.getJwtToken()))
            .willReturn(
                aResponse().withStatus(HttpStatus.UNAUTHORIZED.value())));
    wireMockServer.stubFor(
        post("/user/auth/app")
            .withHeader(AUTHORIZATION, equalTo(USER.getGbifSchemeHeader()))
            .withHeader(HEADER_GBIF_USER, equalTo(USER.getLoggedUser().getUserName()))
            .withHeader(HEADER_CONTENT_MD5, matching(".*"))
            .withHeader(CONTENT_TYPE, matching(".*"))
            .willReturn(
                aResponse()
                    .withBody(OBJECT_MAPPER.writeValueAsString(USER.getLoggedUser()))));
    wireMockServer.stubFor(
        post("/user/auth/app")
            .withHeader(AUTHORIZATION, equalTo(ADMIN_USER.getGbifSchemeHeader()))
            .withHeader(HEADER_GBIF_USER, equalTo(ADMIN_USER.getLoggedUser().getUserName()))
            .withHeader(HEADER_CONTENT_MD5, matching(".*"))
            .withHeader(CONTENT_TYPE, matching(".*"))
            .willReturn(
                aResponse()
                    .withBody(OBJECT_MAPPER.writeValueAsString(ADMIN_USER.getLoggedUser()))));
    wireMockServer.stubFor(
        post("/user/auth/app")
            .withHeader(AUTHORIZATION, equalTo(INVALID_USER.getGbifSchemeHeader()))
            .withHeader(HEADER_GBIF_USER, equalTo(INVALID_USER.getLoggedUser().getUserName()))
            .withHeader(HEADER_CONTENT_MD5, matching(".*"))
            .withHeader(CONTENT_TYPE, matching(".*"))
            .willReturn(
                aResponse().withStatus(HttpStatus.UNAUTHORIZED.value())));

    wireMockServer.start();
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    wireMockServer.stop();
  }

  public WireMockServer getWireMockServer() {
    return wireMockServer;
  }

  @Builder
  @Data
  static class TestUser {

    LoggedUser loggedUser;
    String password;
    String jwtToken;
    String appKey;
    String appSecret;


    String getGbifSchemeHeader() {
      return GBIF_SCHEME_PREFIX + appKey + ":" + appSecret;
    }

  }

}
