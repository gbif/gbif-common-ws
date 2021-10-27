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

import java.nio.charset.StandardCharsets;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Client to perform remote authentication using Basic and JWT Authentication.
 */
public interface IdentityServiceClient extends IdentityAccessService {

  @GetMapping(
      value = "admin/user/{userName}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  GbifUser get(@PathVariable("userName") String userName);

  @Override
  default GbifUser authenticate(String userName, String password) {
    return login("Basic " + HttpHeaders.encodeBasicAuth(userName, password, StandardCharsets.UTF_8)).toGbifUser();
  }

  @PostMapping(
      value = "user/login",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  LoggedUser login(@RequestHeader(HttpHeaders.AUTHORIZATION) String credentials);

  /**
   * Creates an instance suitable to be used by a registered application.
   */
  static IdentityServiceClient getInstance(String apiUrl, String userName, String appKey, String secretKey) {
    return new ClientBuilder()
        .withUrl(apiUrl)
        .withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport())
        .withAppKeyCredentials(userName, appKey, secretKey)
        .build(IdentityServiceClient.class);
  }

  /**
   * Creates an instance suitable to be used by an admin user.
   */
  static IdentityServiceClient getInstance(String apiUrl, String userName, String password) {
    return new ClientBuilder()
        .withUrl(apiUrl)
        .withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport())
        .withCredentials(userName, password)
        .build(IdentityServiceClient.class);
  }
}
