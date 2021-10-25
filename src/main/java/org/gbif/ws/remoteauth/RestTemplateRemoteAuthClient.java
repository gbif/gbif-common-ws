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
      return
          restTemplate.postForEntity(
              path,
              new HttpEntity<>(headers),
              String.class);
    } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
      throw new BadCredentialsException("Wrong credentials for user", e);
    } catch (Exception e) {
      throw new RestClientException("Could not authenticate user", e);
    }
  }
}
