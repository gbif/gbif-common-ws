package org.gbif.ws.client.guice.testclient;

import org.gbif.ws.client.guice.GbifWsClientModule;

import java.util.Properties;

public class InterceptorTestModule extends GbifWsClientModule {

  public InterceptorTestModule() {
    super(new Properties(), TestClient.class.getPackage());
  }

  @Override
  protected void configureClient() {
    // wire up test client
    bind(TestClient.class).asEagerSingleton();
    bind(TestInterface.class).to(TestClient.class);
    expose(TestInterface.class);
  }
}
