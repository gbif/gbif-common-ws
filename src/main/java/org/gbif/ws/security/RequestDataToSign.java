package org.gbif.ws.security;

import javax.annotation.Nullable;

public class RequestDataToSign {

  // required
  private String method;

  // required
  private String url;

  // optional (POST or PUT only)
  @Nullable
  private String contentType;

  // optional (POST or PUT only)
  @Nullable
  private String contentTypeMd5;

  // required
  private String user;

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getContentTypeMd5() {
    return contentTypeMd5;
  }

  public void setContentTypeMd5(String contentTypeMd5) {
    this.contentTypeMd5 = contentTypeMd5;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  /**
   * Concatenates the information to be encrypted from a request into a single String. When the
   * server receives an authenticated request, it compares the computed request signature with the
   * signature provided in the request in StringToSign. For that reason this string may only contain
   * information also available in the exact same form to the server.
   *
   * @return unique string for a request
   * @see <a href="http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html">AWS
   * Docs</a>
   */
  public String stringToSign() {
    StringBuilder sb = new StringBuilder();

    sb.append(method);
    sb.append('\n');
    sb.append(url);
    if (contentType != null) {
      sb.append('\n');
      sb.append(contentType.toLowerCase());
    }
    if (contentTypeMd5 != null) {
      sb.append('\n');
      sb.append(contentTypeMd5);
    }
    sb.append('\n');
    sb.append(user);

    return sb.toString();
  }
}
