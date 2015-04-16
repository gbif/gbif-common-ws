package org.gbif.ws.security;

import org.gbif.ws.json.JacksonJsonContextResolver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GBIF authentication scheme is modelled after the Amazon scheme on how to sign REST http requests
 * using a private key. It uses the standard Http Authorization Header to transport the following information:
 * Authorization: GBIF applicationKey:Signature
 * The header starts with the authentication scheme (GBIF), followed by the plain applicationKey and a unique signature
 * for the very request which is generated using a fixed set of request attributes which are then encrypted by
 * a standard HMAC-SHA1 algorithm.
 * A POST request with a GBIF header would look like this:
 * <pre>
 * POST /dataset HTTP/1.1
 * Host: johnsmith.s3.amazonaws.com
 * Date: Mon, 26 Mar 2007 19:37:58 +0000
 * x-gbif-user: trobertson
 * Content-MD5: 12NnTPXtoDX8k2g6QQ/2hw==
 * Authorization: GBIF admin:frJIUN8DYpKDtOLCwo//yllqDzg=
 * </pre>
 *
 * When signing an http request in addition to the Authentication header some additional custom headers are added
 * which are used to sign and digest the message.
 * <br/>
 * x-gbif-user is added to transport a proxied user in which the application is acting.
 * <br/>
 * Content-MD5 is added if a body entity exists.
 */
@Singleton
public class GbifAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(GbifAuthService.class);

  private static final String ALGORITHM = "HmacSHA1";
  private static final String CHAR_ENCODING = "UTF8";
  public static final String HEADER_AUTHORIZATION = "Authorization";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_CONTENT_MD5 = "Content-MD5";
  public static final String GBIF_SCHEME = "GBIF";
  public static final String HEADER_GBIF_USER = "x-gbif-user";
  public static final String HEADER_ORIGINAL_REQUEST_URL = "x-url";
  private static final Pattern COLON_PATTERN = Pattern.compile(":");

  private final ImmutableMap<String, String> keyStore;
  private final String appKey;
  private final ObjectMapper mapper = new JacksonJsonContextResolver().getContext(null);

  private GbifAuthService(Map<String, String> appKeys, String appKey) {
    keyStore = ImmutableMap.copyOf(appKeys);
    this.appKey = appKey;
    LOG.info("Initialised appkey store with {} keys", keyStore.size());
  }

  /**
   * Creates a new GBIF authentication service for applications that need to validate requests
   * for various application keys. Used by the GBIF webservice apps that require authentication.
   */
  public static GbifAuthService multiKeyAuthService(Map<String, String> appKeys) {
    return new GbifAuthService(appKeys, null);
  }

  /**
   * Creates a new GBIF authentication service for clients that want to sign requests always using a single
   * application key. Used by the GBIF portal and other trusted applications that need to proxy a user.
   */
  public static GbifAuthService singleKeyAuthService(String appKey, String secret) {
    LOG.info("Initialising auth service with key {}", appKey);
    return new GbifAuthService(ImmutableMap.of(appKey, secret), appKey);
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
    // custom header set by varnish overrides real URI
    // see http://dev.gbif.org/issues/browse/GBIFCOM-137
    if (request.getRequestHeaders().containsKey(HEADER_ORIGINAL_REQUEST_URL)) {
      sb.append(request.getRequestHeaders().getFirst(HEADER_ORIGINAL_REQUEST_URL));
    } else {
      sb.append(getCanonicalizedPath(request.getRequestUri()));
    }

    sb.append("/n");
    sb.append(request.getHeaderValue(HEADER_CONTENT_TYPE));

    // include md5 hash if existing (only for PUT/POST with content)
    if (request.getRequestHeaders().containsKey(HEADER_CONTENT_MD5)) {
      sb.append("/n");
      sb.append(request.getHeaderValue(HEADER_CONTENT_MD5));
    }

    sb.append("/n");
    sb.append(request.getHeaderValue(HEADER_GBIF_USER));

    return sb.toString();
  }

  /**
   * Build the string to be signed for a client request by extracting header information from the request.
   * For PUT/POST requests that contain a body content it is required that the Content-MD5 header
   * is already present on the request instance!
   */
  private String buildStringToSign(ClientRequest request) {
    StringBuilder sb = new StringBuilder();
    sb.append(request.getMethod());

    sb.append("/n");
    sb.append(getCanonicalizedPath(request.getURI()));

    sb.append("/n");
    sb.append(request.getHeaders().getFirst(HEADER_CONTENT_TYPE));

    // include md5 hash if existing (only for PUT/POST with content)
    if (request.getHeaders().containsKey(HEADER_CONTENT_MD5)) {
      sb.append("/n");
      sb.append(request.getHeaders().getFirst(HEADER_CONTENT_MD5));
    }

    sb.append("/n");
    sb.append(request.getHeaders().getFirst(HEADER_GBIF_USER));

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

  /**
   * Signs a request by adding a Content-MD5 and Authorization header.
   * For PUT/POST requests that contain a body entity the Content-MD5 header is created using the same
   * JSON mapper for serialization as the clients use.
   *
   * Other format than JSON are not supported currently !!!
   */
  public void signRequest(String username, ClientRequest request) {
    Preconditions.checkNotNull(appKey, "To sign request a single application key is required");
    // first add custom GBIF headers so we can use them to build the string to sign

    // the proxied username
    request.getHeaders().putSingle(HEADER_GBIF_USER, username);
    // the canonical path header
    request.getHeaders().putSingle(HEADER_ORIGINAL_REQUEST_URL, getCanonicalizedPath(request.getURI()));
    // adds content md5
    if (request.getEntity() != null) {
      request.getHeaders().putSingle(HEADER_CONTENT_MD5, buildContentMD5(request.getEntity()));
    }

    // build the unique string to sign
    final String stringToSign = buildStringToSign(request);
    // find private key for this app
    final String secretKey = getPrivateKey(appKey);
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

  private String buildContentMD5(Object entity) {
    try {
      MessageDigest instance = MessageDigest.getInstance("MD5");
      return new String(Base64.encode(instance.digest(mapper.writeValueAsBytes(entity))), "ASCII");

    } catch (IOException e) {
      LOG.error("Failed to serialize http entity [{}]", entity);
      throw new RuntimeException(e);

    } catch (NoSuchAlgorithmException e) {
      LOG.error("MD5 algorithm not found");
      throw new RuntimeException(e);
    }
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
