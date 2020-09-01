/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.ws.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class AppKeySigningService extends BaseSigningService {

  private static final Logger LOG = LoggerFactory.getLogger(AppKeySigningService.class);

  private final KeyStore keyStore;

  public AppKeySigningService(KeyStore keyStore) {
    this.keyStore = keyStore;
  }

  /**
   * Generates a Base64 encoded HMAC-SHA1 signature of the passed request data with the secret key
   * associated with the given application key. See Message Authentication Code specs
   * http://tools.ietf.org/html/rfc2104
   *
   * @param requestDataToSign the request data to be signed
   * @param appKey            the application key
   */
  @Override
  public String buildSignature(RequestDataToSign requestDataToSign, String appKey) {
    // find private key for this app
    final String secretKey = keyStore.getPrivateKey(appKey);
    if (secretKey == null) {
      LOG.error("Unknown application key: {}", appKey);
      throw new PrivateKeyNotFoundException();
    }

    // sign
    return super.buildSignature(requestDataToSign, secretKey);
  }
}
