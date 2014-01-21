package org.gbif.ws.client.interceptor;

import org.gbif.api.exception.ServiceUnavailableException;

import java.security.AccessControlException;
import javax.ws.rs.core.Response;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpErrorResponseInterceptorTest {

  MethodInvocation invocation = mock(MethodInvocation.class);
  HttpErrorResponseInterceptor interceptor = new HttpErrorResponseInterceptor();

  @Test
  public void testGetNoContent() throws Throwable {
    assertNull(testHttpCode(Response.Status.NO_CONTENT));
  }

  @Test(expected = ServiceUnavailableException.class)
  public void testGetInternalServerError() throws Throwable {
    testHttpCode(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetUnsupportedOperationException() throws Throwable {
    testHttpCode(501);
  }

  @Test
  public void testNotFoundException() throws Throwable {
    assertNull(testHttpCode(Response.Status.NOT_FOUND));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIllegalArgumentException() throws Throwable {
    testHttpCode(Response.Status.BAD_REQUEST);
  }

  @Test(expected = AccessControlException.class)
  public void testAccessControlException() throws Throwable {
    testHttpCode(Response.Status.UNAUTHORIZED);
  }

  @Test(expected = AccessControlException.class)
  public void testAccessControlException2() throws Throwable {
    testHttpCode(Response.Status.FORBIDDEN);
  }

  private Object testHttpCode(Response.Status respCode) throws Throwable {
    return testHttpCode(respCode.getStatusCode());
  }

  private Object testHttpCode(int respCode) throws Throwable {
    ClientResponse mockClientResponse = mock(ClientResponse.class);
    when(mockClientResponse.getStatus()).thenReturn(respCode);
    UniformInterfaceException nfe = new UniformInterfaceException(mockClientResponse);
    when(invocation.proceed()).thenThrow(nfe);

    return interceptor.invoke(invocation);
  }

}
