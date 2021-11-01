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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of a {@link RemoteAuthClient} by using {@link RestTemplate} as client.
 */
public class RestTemplateRemoteAuthClient implements RemoteAuthClient {

  private final RestTemplate restTemplate;

  public RestTemplateRemoteAuthClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public ResponseEntity<String> remoteAuth(String path, HttpHeaders headers) {
    try {
      return restTemplate.postForEntity(path, new HttpEntity<>(headers), String.class);
    } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden | HttpClientErrorException.BadRequest e) {
      throw new BadCredentialsException("Wrong credentials for user", e);
    } catch (Exception e) {
      throw new RestClientException("Could not authenticate user", e);
    }
  }
}
