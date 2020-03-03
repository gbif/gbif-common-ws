package org.gbif.ws.security;

public interface SigningService {

  String buildSignature(RequestDataToSign requestDataToSign, String appKey);
}
