package org.gbif.ws.client.guice.testclient;

import org.gbif.ws.client.guice.GbifWsClientModuleTest;

import com.google.inject.Inject;
import org.apache.http.client.HttpClient;

/**
 * This class is used to test that the HttpClient is built using custom connection parameters.
 * 
 * @see GbifWsClientModuleTest#testGetHttpClient()
 * @see GbifWsClientModuleTest#testGetHttpClientDefaultParams()
 */
public class TestHttpClient {

  private HttpClient httpClient;

  public HttpClient getHttpClient() {
    return httpClient;
  }

  @Inject
  public TestHttpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

}
