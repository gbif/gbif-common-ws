package org.gbif.ws.response;

/**
 * Extended response status codes used by the GBIF Rest API.
 */
public enum GbifResponseStatus {
  /**
   * Returned when the client is being rate limited.
   */
  ENHANCE_YOUR_CALM(420),
  /**
   * The request was well-formed but was unable to be followed due to semantic errors.
   */
  UNPROCESSABLE_ENTITY(422);


  private int status;

  GbifResponseStatus(int status) {
    this.status = status;
  }

  /**
   * Response status code.
   */
  public int getStatus(){
    return status;
  }
}
