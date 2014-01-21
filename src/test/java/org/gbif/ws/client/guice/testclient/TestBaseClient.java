package org.gbif.ws.client.guice.testclient;

import javax.annotation.Nullable;

/**
 * test client class for GbifWsClientModuleTest.
 */
public abstract class TestBaseClient<T> {

  @Nullable
  public Integer getNull(T key) {
    return hidden(key);
  }

  protected abstract Integer hidden(T keyT);

}
