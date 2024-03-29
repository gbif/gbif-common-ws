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
