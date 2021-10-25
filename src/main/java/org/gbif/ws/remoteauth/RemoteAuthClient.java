package org.gbif.ws.remoteauth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * HTTP client to auth applications remotely against the registry.
 * <p>
 * This abstraction was created mainly to ease the creation of mocks for testing.
 */
public interface RemoteAuthClient {

  ResponseEntity<String> remoteAuth(String path, HttpHeaders headers);

}
