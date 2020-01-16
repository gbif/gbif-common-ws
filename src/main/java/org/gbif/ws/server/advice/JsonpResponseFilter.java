package org.gbif.ws.server.advice;

/**
 * Filter that optionally wraps a JSON request in a JSONP response.
 * <p/>
 * For this wrapping to happen two things need to be true:
 * <ul>
 * <li>The Media type of the response must be set to JSON or application/javascript</li>
 * <li>The request must have a query parameter called {@code callback}</li>
 * </ul>
 * <p/>
 *
 * @see <a href="http://weblogs.java.net/blog/felipegaucho/archive/2010/02/25/jersey-feat-jquery-jsonp">JSONP with
 * Jersey and jQuery</a>
 */
public class JsonpResponseFilter {
  // TODO: 15/01/2020 not implemented
}
