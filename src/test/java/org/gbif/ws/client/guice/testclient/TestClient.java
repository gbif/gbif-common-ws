package org.gbif.ws.client.guice.testclient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.annotation.Nullable;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * test client class for GbifWsClientModuleTest.
 */
public class TestClient extends TestBaseClient<Integer> implements TestInterface {

  protected Integer hidden(Integer key) {
    if (key < 1) {
      InputStream stream = new ByteArrayInputStream("abcd".getBytes());
      ClientResponse r = new ClientResponse(404, null, stream, null);
      throw new UniformInterfaceException("no message", r, false);
    }
    return key;
  }

  @Nullable
  public Integer getNullNonTyped(Integer key) {
    return hidden(key);
  }

}
