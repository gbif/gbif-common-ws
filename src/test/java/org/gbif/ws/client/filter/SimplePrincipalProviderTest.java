package org.gbif.ws.client.filter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SimplePrincipalProviderTest {

  @Test
  public void testGet() throws Exception {
    SimplePrincipalProvider pp = new SimplePrincipalProvider();
    assertNull(pp.get());
    pp.setPrincipal("");
    assertNull(pp.get());
    pp.setPrincipal("heinz");
    assertEquals("heinz", pp.get().getName());
  }
}
