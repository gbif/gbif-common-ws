package org.gbif.ws.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SigningServiceImpl implements SigningService {

  private static final Logger LOG = LoggerFactory.getLogger(SigningServiceImpl.class);
  private static final String ALGORITHM = "HmacSHA1";

  private final KeyStore keyStore;

  public SigningServiceImpl(KeyStore keyStore) {
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
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      SecretKeySpec secret = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8),
          ALGORITHM);
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

