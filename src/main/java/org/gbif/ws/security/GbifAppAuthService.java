package org.gbif.ws.security;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GBIF authentication scheme is modelled after the Amazon scheme on how to sign REST http requests
 * using a private key. It uses the standard Http Authorization Header to transport the following information:
 * Authorization: GBIF applicationKey:Signature
 * The header starts with the authentication scheme (GBIF), followed by the plain applicationKey and a unique signature
 * for the very request which is generated using a fixed set of request attributes which are then encrypted by
 * a standard HMAC-SHA1 algorithm.
 * A simple GET request with a GBIF header would look like this:
 * <pre>
 * GET /name_usage/536123 HTTP/1.1
 * Host: johnsmith.s3.amazonaws.com
 * Date: Mon, 26 Mar 2007 19:37:58 +0000
 * x-gbif-user: trobertson
 * x-gbif-date: 16 Mar 2007 19:37:58 GMT
 * Authorization: GBIF admin:frJIUN8DYpKDtOLCwo//yllqDzg=
 * </pre>
 * When signing an http request in addition to the Authentication header some additional custom headers are added
 * which are used to sign and digest the message.
 * <br/>
 * x-gbif-user is added to transport a proxied user in which the application is acting.
 * <br/>
 * x-gbif-date is added because there is no way for a jersey client to access the automatically generated date header
 * before the message is actually send.
 * <br/>
 * x-gbif-content-hash is added similar to the date above hashing the message body if it exists.
 * This will only be for POST/PUT requests instead of using the regular Content-MD5
 * which cannot be accessed in the jersey client.
 */
@Singleton
public class GbifAppAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(GbifAppAuthService.class);

  public static final String ALGORITHM = "HmacSHA1";
  public static final String CHAR_ENCODING = "UTF8";
  public static final String HEADER_AUTHORIZATION = "Authorization";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String GBIF_SCHEME = "GBIF";
  public static final String HEADER_GBIF_USER = "x-gbif-user";
  public static final String HEADER_GBIF_DATE = "x-gbif-date";
  public static final String HEADER_GBIF_CONTENT_HASH = "x-gbif-content-hash";
  public static final String HEADER_ORIGINAL_REQUEST_URL = "x-url";
  private static final Pattern COLON_PATTERN = Pattern.compile(":");

  private final ImmutableMap<String, String> keyStore;

  public GbifAppAuthService(Map<String, String> appKeys) {
    keyStore = ImmutableMap.copyOf(appKeys);
    LOG.info("Initialised appkey store with {} keys", keyStore.size());
  }

  public GbifAppAuthService(String appKey, String secret) {
    keyStore = ImmutableMap.of(appKey, secret);
    LOG.info("Initialised appkey store with key {}", appKey);
  }

  /**
   * Extracts the information to be encrypted from a request and concatenates them into a single String.
   * When the server receives an authenticated request, it compares the computed request signature
   * with the signature provided in the request in StringToSign.
   * For that reason this string may only contain information also available in the exact same form to the server.
   *
   * @return unique string for a request
   *
   * @see <a href="http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html">AWS Docs</a>
   */
  private String buildStringToSign(ContainerRequest request) {
    StringBuilder sb = new StringBuilder();
    sb.append(request.getMethod());
    sb.append("/n");
    sb.append(request.getHeaderValue(HEADER_CONTENT_TYPE));
    sb.append("/n");
    sb.append(getCanonicalizedGbifHeaders(request.getRequestHeaders()));
    sb.append("/n");
    // custom header set by varnish overrides real URI
    // see http://dev.gbif.org/issues/browse/GBIFCOM-137
    if (request.getRequestHeaders().containsKey(HEADER_ORIGINAL_REQUEST_URL)) {
      sb.append(request.getRequestHeaders().getFirst(HEADER_ORIGINAL_REQUEST_URL));
    } else {
      sb.append(getCanonicalizedPath(request.getRequestUri()));
    }

    return sb.toString();
  }

  /**
   * Build the string to be signed for a client request
   */
  private String buildStringToSign(ClientRequest request) {
    StringBuilder sb = new StringBuilder();
    sb.append(request.getMethod());
    sb.append("/n");
    sb.append(request.getHeaders().getFirst(HEADER_CONTENT_TYPE));
    sb.append("/n");
    sb.append(getCanonicalizedGbifHeaders(request.getHeaders()));
    sb.append("/n");
    sb.append(getCanonicalizedPath(request.getURI()));
    return sb.toString();
  }

  /**
   * Grabs all gbif specific headers starting with x-gbif.
   */
  private String getCanonicalizedGbifHeaders(MultivaluedMap<String, ?> headers) {
    StringBuilder sb = new StringBuilder();
    List<String> gbifHeaders = Lists.newArrayList();
    for (String hKey : headers.keySet()) {
      if (hKey.startsWith("x-gbif")) {
        for (Object val : headers.get(hKey)) {
          gbifHeaders.add(hKey + ":" + val.toString());
        }
      }
    }
    Collections.sort(gbifHeaders);
    for (String h : gbifHeaders) {
      sb.append(h);
      sb.append("/n");
    }
    return sb.toString();
  }

  /**
   * @return an absolute uri of the resource path alone, excluding host, scheme and query parameters
   */
  private String getCanonicalizedPath(URI uri) {
    return uri.normalize().getPath();
  }

  private String buildAuthHeader(String applicationKey, String signature) {
    return GBIF_SCHEME + " " + applicationKey + ":" + signature;
  }

  private String buildSignature(String stringToSign, String secretKey) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      SecretKeySpec secret = new SecretKeySpec(secretKey.getBytes(Charset.forName("UTF8")), ALGORITHM);
      mac.init(secret);
      byte[] digest = mac.doFinal(stringToSign.getBytes());

      return new String(Base64.encode(digest), "ASCII");

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Cant find " + ALGORITHM + " message digester", e);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unsupported character encoding " + CHAR_ENCODING, e);
    } catch (InvalidKeyException e) {
      throw new RuntimeException("Invalid secret key " + secretKey, e);
    }
  }

  private void addGbifHeaders(String username, ClientRequest request) {
    // adds the datestamp
    request.getHeaders().putSingle(HEADER_GBIF_DATE, new Date().toGMTString());
    // adds the proxied user name to the headers
    request.getHeaders().putSingle(HEADER_GBIF_USER, username);
    // adds the entities java hash code
    if (request.getEntity() != null) {
      request.getHeaders().putSingle(HEADER_GBIF_CONTENT_HASH, request.getEntity().hashCode());
    }
    // adds the canonical path header used for signing
    request.getHeaders().putSingle(HEADER_ORIGINAL_REQUEST_URL, getCanonicalizedPath(request.getURI()));
  }

  public void signRequest(String appKey, String username, ClientRequest request) {
    // first add custom GBIF headers so we can use them to build the string to sign
    addGbifHeaders(username, request);
    // build the unique string to sign
    String stringToSign = buildStringToSign(request);
    // find private key for this app
    String secretKey = getPrivateKey(appKey);
    if (secretKey == null) {
      LOG.warn("Skip signing request with unknown application key: {}", appKey);
      return;
    }
    // sign
    String signature = buildSignature(stringToSign, secretKey);
    // build authorization header string
    String header = buildAuthHeader(appKey, signature);
    // add authorization header
    LOG.debug("Adding authentication header to request {} for proxied user {} : {}", request.getURI(), username, header);
    request.getHeaders().putSingle(HEADER_AUTHORIZATION, header);
  }

  public boolean isValidRequest(ContainerRequest request) {
    // parse auth header
    final String authHeader = request.getHeaderValue(HEADER_AUTHORIZATION);
    if (Strings.isNullOrEmpty(authHeader) || !authHeader.startsWith(GBIF_SCHEME + " ")) {
      LOG.info(HEADER_AUTHORIZATION + " header is no GBIF scheme");
      return false;
    }

    String[] values = COLON_PATTERN.split(authHeader.substring(5), 2);
    if (values.length < 2) {
      LOG.warn("Invalid syntax for application key and signature: {}", authHeader);
      return false;
    }

    final String appKey = values[0];
    final String signatureFound = values[1];
    if (appKey == null || signatureFound == null) {
      LOG.warn("Authentication header missing applicationKey or signature: {}", authHeader);
      return false;
    }

    String secretKey = getPrivateKey(appKey);
    if (secretKey == null) {
      LOG.warn("Unknown application key: {}", appKey);
      return false;
    }
    //
    String stringToSign = buildStringToSign(request);
    // sign
    String signature = buildSignature(stringToSign, secretKey);
    // compare signatures
    if (signatureFound.equals(signature)) {
      LOG.debug("Trusted application with matching signatures");
      return true;

    }
    LOG.info("Invalid signature: {}", authHeader);
    return false;
  }

  private String getPrivateKey(String appKey) {
    return keyStore.get(appKey);
  }

}
