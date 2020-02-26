package org.gbif.ws.security;

public interface KeyStore {

  String getPrivateKey(String applicationKey);
}
