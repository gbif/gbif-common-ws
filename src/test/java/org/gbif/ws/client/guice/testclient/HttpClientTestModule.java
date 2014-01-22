package org.gbif.ws.client.guice.testclient;

import org.gbif.ws.client.guice.GbifWsClientModule;

import java.util.Properties;


public class HttpClientTestModule extends GbifWsClientModule {

  public HttpClientTestModule(Properties properties) {
    super(properties, TestHttpClient.class.getPackage());
  }

  @Override
  protected void configureClient() {
    bind(TestHttpClient.class).asEagerSingleton();
    expose(TestHttpClient.class);
  }

}
