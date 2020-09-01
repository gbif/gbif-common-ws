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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public abstract class BaseSigningService implements SigningService {

  private static final String ALGORITHM = "HmacSHA1";

  /**
   * Generates a Base64 encoded HMAC-SHA1 signature of the passed request data with the secret key.
   * See Message Authentication Code specs http://tools.ietf.org/html/rfc2104
   *
   * @param requestDataToSign the request data to be signed
   * @param secretKey         the secret key
   */
  @Override
  public String buildSignature(RequestDataToSign requestDataToSign, String secretKey) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      SecretKeySpec secret =
          new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
      mac.init(secret);
      byte[] digest = mac.doFinal(requestDataToSign.stringToSign().getBytes());

      return new String(Base64.getEncoder().encode(digest), StandardCharsets.US_ASCII);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Cant find " + ALGORITHM + " message digester", e);
    } catch (InvalidKeyException e) {
      throw new RuntimeException("Invalid secret key " + secretKey, e);
    }
  }
}
