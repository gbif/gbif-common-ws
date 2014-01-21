package org.gbif.ws.client.interceptor;

import org.gbif.api.exception.ServiceUnavailableException;

import java.security.AccessControlException;

import javax.validation.ValidationException;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * A client method interceptor that translates http response errors into actual java exceptions.
 *
 * @see <a href="http://dev.gbif.org/wiki/display/POR/Service+Exceptions">GBIF Dev wiki</a> for exception mappings.
 *      400 => IllegalArgumentException,
 *      401 => AccessControlException,
 *      403 => AccessControlException,
 *      404 => NULL,
 *      422 => ValidationException,
 *      500 => ServiceUnavailableException,
 *      501 => UnsupportedOperationException,
 *      All other types of unexpected responses the exception passes through the interceptor unchanged.
 */
public class HttpErrorResponseInterceptor implements MethodInterceptor {

  /**
   * @return a short description of the http status followed by the entire http response body
   */
  private static String readBody(UniformInterfaceException e) {
    String status = "HTTP " + e.getResponse().getStatus() + ": ";
    try {
      return status + e.getResponse().getEntity(String.class);
    } catch (Exception e1) {
      return status + "Failed to read http body";
    }
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Object result = null;
    // exceptions can surface as the result or be thrown - catch both in this var:
    UniformInterfaceException e = null;
    try {
      result = invocation.proceed();
    } catch (UniformInterfaceException e2) {
      e = e2;
    }

    if (result instanceof UniformInterfaceException) {
      e = (UniformInterfaceException) result;
    }

    // now check the UniformInterfaceException if it exists
    if (e != null) {
      ClientResponse response = e.getResponse();
      UniformInterfaceException eX = new UniformInterfaceException(readBody(e), e.getResponse());

      switch (response.getStatus()) {
        case 204:
          // no content, return null;
          return null;
        case 400:
          throw new IllegalArgumentException("A bad request received.", eX);
        case 401:
          throw new AccessControlException("Unauthorized request received.");
        case 403:
          throw new AccessControlException("Forbidden request received.");
        case 404:
          return null;
        case 422:
          throw new ValidationException(eX); // TODO: consider passing fields in headers
        case 500:
          throw new ServiceUnavailableException("An internal server error occurred, please try again later.", eX);
        case 501:
          throw new UnsupportedOperationException("Method not implement yet.", eX);
        default:
          throw eX;
      }
    }

    return result;
  }
}
