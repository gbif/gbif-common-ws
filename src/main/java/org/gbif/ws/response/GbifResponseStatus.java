/*
 * Derived from Response.java in jersey-core.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.php
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

/*
 * GbifResponseStatus.java
 *
 * Created on 21 November 2018, 13:54.
 */
package org.gbif.ws.response;

import javax.ws.rs.core.Response;

/**
 * Extended response status codes used by the GBIF REST API.
 */
public enum GbifResponseStatus implements Response.StatusType {

  /**
   * Returned if you try to upload a file that is too large
   *
   * Use Response.Status.REQUEST_ENTITY_TOO_LARGE instead.
   */
  @Deprecated
  PAYLOAD_TOO_LARGE(413, "Request Entity Too Large"),

  /**
   * Returned when the client is being rate limited.
   */
  ENHANCE_YOUR_CALM(420, "Enhance Your Calm"),

  /**
   * The request was well-formed but was unable to be followed due to semantic errors.
   */
  UNPROCESSABLE_ENTITY(422, "Unprocessable Entity");

  /**
   * Response status code.
   */
  public int getStatus(){
    return code;
  }

  private final int code;
  private final String reason;
  private Response.Status.Family family;

  GbifResponseStatus(final int statusCode, final String reasonPhrase) {
    this.code = statusCode;
    this.reason = reasonPhrase;
    switch(code/100) {
      case 1: this.family = Response.Status.Family.INFORMATIONAL; break;
      case 2: this.family = Response.Status.Family.SUCCESSFUL; break;
      case 3: this.family = Response.Status.Family.REDIRECTION; break;
      case 4: this.family = Response.Status.Family.CLIENT_ERROR; break;
      case 5: this.family = Response.Status.Family.SERVER_ERROR; break;
      default: this.family = Response.Status.Family.OTHER; break;
    }
  }

  /**
   * Get the class of status code
   * @return the class of status code
   */
  public Response.Status.Family getFamily() {
    return family;
  }

  /**
   * Get the associated status code
   * @return the status code
   */
  public int getStatusCode() {
    return code;
  }

  /**
   * Get the reason phrase
   * @return the reason phrase
   */
  public String getReasonPhrase() {
    return toString();
  }

  /**
   * Get the reason phrase
   * @return the reason phrase
   */
  @Override
  public String toString() {
    return reason;
  }

  /**
   * Convert a numerical status code into the corresponding StatusType
   * @param statusCode the numerical status code
   * @return the matching StatusType or null is no matching StatusType is defined
   */
  public static Response.StatusType fromStatusCode(final int statusCode) {
    for (GbifResponseStatus s : GbifResponseStatus.values()) {
      if (s.code == statusCode) {
        return s;
      }
    }
    return Response.Status.fromStatusCode(statusCode);
  }
}
