package org.gbif.ws.client.guice;

import org.gbif.ws.client.guice.testclient.HttpClientTestModule;
import org.gbif.ws.client.guice.testclient.InterceptorTestModule;
import org.gbif.ws.client.guice.testclient.TestHttpClient;
import org.gbif.ws.client.guice.testclient.TestInterface;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests the interceptor bindings order of the module.
 */
public class GbifWsClientModuleTest {

  private static TestInterface tc;

  @BeforeClass
  public static void initInjector() {
    Injector injector = Guice.createInjector(new InterceptorTestModule());
    tc = injector.getInstance(TestInterface.class);
  }

  @Test
  public void testNull() {
    assertEquals((Integer) 10, tc.getNull(10));
    assertNull(tc.getNull(-10));
  }

  @Test
  public void testNullNonTyped() {
    assertEquals((Integer) 10, tc.getNullNonTyped(10));
    assertNull(tc.getNullNonTyped(-10));
  }

  /**
   * Tests that custom HTTP connection parameters are used.
   */
  @Test
  public void testGetHttpClient() {
    Properties testProperties = new Properties();
    testProperties.put("httpTimeout", "123456");
    testProperties.put("maxHttpConnections", "7");
    testProperties.put("maxHttpConnectionsPerRoute", "8");
    Injector injector = Guice.createInjector(new HttpClientTestModule(testProperties));
    TestHttpClient testHttpClient = injector.getInstance(TestHttpClient.class);
    DefaultHttpClient defaultHttpClient = (DefaultHttpClient) testHttpClient.getHttpClient();
    assertNotNull(defaultHttpClient);
    assertEquals(defaultHttpClient.getParams().getParameter(ClientPNames.CONN_MANAGER_TIMEOUT), 123456L);
    PoolingClientConnectionManager connectionManager =
      (PoolingClientConnectionManager) defaultHttpClient.getConnectionManager();
    assertEquals(connectionManager.getMaxTotal(), 7);
    assertEquals(connectionManager.getDefaultMaxPerRoute(), 8);
  }


  /**
   * Tests that the default HTTP connection parameters are used.
   */
  @Test
  public void testGetHttpClientDefaultParams() {
    Injector injector = Guice.createInjector(new HttpClientTestModule(new Properties()));
    TestHttpClient testHttpClient = injector.getInstance(TestHttpClient.class);
    DefaultHttpClient defaultHttpClient = (DefaultHttpClient) testHttpClient.getHttpClient();
    assertNotNull(defaultHttpClient);
    assertEquals(defaultHttpClient.getParams().getParameter(ClientPNames.CONN_MANAGER_TIMEOUT), new Long(
      GbifWsClientModule.DEFAULT_HTTP_TIMEOUT_MSECS));
    PoolingClientConnectionManager connectionManager =
      (PoolingClientConnectionManager) defaultHttpClient.getConnectionManager();
    assertEquals(connectionManager.getMaxTotal(), GbifWsClientModule.DEFAULT_MAX_HTTP_CONNECTIONS);
    assertEquals(connectionManager.getDefaultMaxPerRoute(), GbifWsClientModule.DEFAULT_MAX_HTTP_CONNECTIONS_PER_ROUTE);
  }
}
