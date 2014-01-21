package org.gbif.ws.server.interceptor;

import com.sun.jersey.api.NotFoundException;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NullToNotFoundInterceptorTest {
  private final NullToNotFoundInterceptor interceptor = new NullToNotFoundInterceptor();

  private MethodInvocation invocation;

  @Before
  public void setUp() {
    invocation = mock(MethodInvocation.class);
  }

  @Test
  public void testInterceptNonNull() throws Throwable {
    when(invocation.proceed()).thenReturn("I'm not null");
    assertEquals("I'm not null", interceptor.invoke(invocation));
  }

  @Test(expected = NotFoundException.class)
  public void testInterceptNull() throws Throwable {
    when(invocation.proceed()).thenReturn(null);
    interceptor.invoke(invocation);
  }
}
