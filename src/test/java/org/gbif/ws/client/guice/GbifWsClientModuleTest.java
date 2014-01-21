package org.gbif.ws.client.guice;

import org.gbif.ws.client.guice.testclient.InterceptorTestModule;
import org.gbif.ws.client.guice.testclient.TestInterface;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
}
