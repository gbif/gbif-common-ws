/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.ws.security;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME_PREFIX;

public final class GbifAuthUtils {

  /**
   * Path prefixes a proxy can strip before the request reaches the service, and that clients can
   * keep in the x-url header.
   */
  public static final String DEFAULT_PROXY_PATH_PREFIX = "/v1";

  public static final List<String> DEFAULT_PROXY_PATH_PREFIXES = List.of(DEFAULT_PROXY_PATH_PREFIX);

  private static final Pattern COLON_PATTERN = Pattern.compile(":");

  private GbifAuthUtils() {}

  /**
   * Tries to get the appkey from the request header.
   * @param authorizationHeader 'Authorization' header.
   * @return the appkey found or null
   */
  public static String getAppKeyFromRequest(final String authorizationHeader) {
    if (StringUtils.startsWith(authorizationHeader, GBIF_SCHEME_PREFIX)) {
      String[] values = COLON_PATTERN.split(authorizationHeader.substring(5), 2);
      if (values.length == 2) {
        return values[0];
      }
    }
    return null;
  }

  /**
   * @return the current time formatted as an RFC 1123 date, the format of the x-gbif-date header
   */
  public static String formatDate(Clock clock) {
    return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(clock.withZone(ZoneOffset.UTC)));
  }

  /**
   * Checks the RFC 1123 date of a signed request is within the allowed clock skew of now.
   */
  public static boolean isDateWithinWindow(String date, Clock clock, Duration maxClockSkew) {
    if (StringUtils.isEmpty(date)) {
      return false;
    }
    try {
      Instant requestInstant = ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
      return Duration.between(requestInstant, clock.instant()).abs().compareTo(maxClockSkew) <= 0;
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  /**
   * @return an absolute uri of the resource path alone, excluding host, scheme and query parameters
   */
  public static String getCanonicalizedPath(final String strUri) {
    return URI.create(strUri).normalize().getPath();
  }

  /**
   * Builds the URL used in the string to sign: the path followed by the canonical query string, if
   * any.
   *
   * @param path the path, used as is
   * @param rawQuery the raw (encoded) query string, may be null
   * @throws IllegalArgumentException if the query string is not properly encoded
   */
  public static String getCanonicalizedUrl(String path, String rawQuery) {
    String query = getCanonicalizedQuery(rawQuery);
    return query.isEmpty() ? path : path + '?' + query;
  }

  /**
   * Canonical form of a query string, so clients and servers sign the same value regardless of
   * parameter order (the proxy sorts them) and encoding differences: parameters are decoded, sorted
   * by name and value, and encoded again as RFC 3986. A parameter without value is equivalent to
   * one with an empty value.
   *
   * @param rawQuery the raw (encoded) query string, may be null
   * @throws IllegalArgumentException if the query string is not properly encoded
   */
  public static String getCanonicalizedQuery(String rawQuery) {
    if (StringUtils.isEmpty(rawQuery)) {
      return "";
    }
    return Arrays.stream(rawQuery.split("&"))
        .filter(param -> !param.isEmpty())
        .map(
            param -> {
              int idx = param.indexOf('=');
              String name = idx < 0 ? param : param.substring(0, idx);
              String value = idx < 0 ? "" : param.substring(idx + 1);
              return new SimpleEntry<>(decode(name), decode(value));
            })
        .sorted(Map.Entry.<String, String>comparingByKey().thenComparing(Map.Entry.comparingByValue()))
        .map(e -> encode(e.getKey()) + '=' + encode(e.getValue()))
        .collect(Collectors.joining("&"));
  }

  private static String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8)
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~");
  }

  /**
   * Checks the client supplied original URL (x-url header) refers to the path actually requested,
   * with the default proxy path prefixes.
   *
   * @see #isOriginalUrlValid(String, String, Collection)
   */
  public static boolean isOriginalUrlValid(String originalUrl, String requestPath) {
    return isOriginalUrlValid(originalUrl, requestPath, DEFAULT_PROXY_PATH_PREFIXES);
  }

  /**
   * Checks the client supplied original URL (x-url header) refers to the path actually requested.
   * The proxy can strip a leading path prefix (e.g. /v1), so the original path must equal the real
   * path, or one of the given prefixes followed by the real path. Any other prefix is refused, else
   * a signature for /v1/foo/admin/user/x would also be valid for /admin/user/x. Scheme, host and
   * query parameters are ignored, a relative original path (e.g. admin/user/x) is taken as rooted.
   */
  public static boolean isOriginalUrlValid(
      String originalUrl, String requestPath, Collection<String> proxyPathPrefixes) {
    final String originalPath;
    try {
      originalPath = getCanonicalizedPath(originalUrl);
    } catch (IllegalArgumentException e) {
      return false;
    }
    if (originalPath == null || requestPath == null) {
      return false;
    }
    String path = StringUtils.removeEnd(requestPath, "/");
    String original = StringUtils.removeEnd(StringUtils.prependIfMissing(originalPath, "/"), "/");
    return original.equals(path)
        || (!path.isEmpty() && path.startsWith("/") && hasPrefix(original, path, proxyPathPrefixes));
  }

  /**
   * Checks the path is one of the given paths, possibly behind one of the proxy path prefixes.
   */
  public static boolean isPathOneOf(
      String path, Collection<String> paths, Collection<String> proxyPathPrefixes) {
    if (path == null) {
      return false;
    }
    String p = StringUtils.removeEnd(path, "/");
    return paths.stream()
        .map(s -> StringUtils.removeEnd(StringUtils.prependIfMissing(s, "/"), "/"))
        .anyMatch(s -> p.equals(s) || hasPrefix(p, s, proxyPathPrefixes));
  }

  /** Checks full is exactly one of the prefixes followed by path. */
  private static boolean hasPrefix(String full, String path, Collection<String> prefixes) {
    return prefixes.stream()
        .map(prefix -> StringUtils.removeEnd(StringUtils.prependIfMissing(prefix, "/"), "/"))
        .filter(prefix -> !prefix.isEmpty())
        .anyMatch(prefix -> full.equals(prefix + path));
  }

  /**
   * Checks the Content-MD5 header against the actual request body. Two encodings are accepted:
   * the MD5 of the raw body, and the MD5 of the body serialized as a JSON byte array, which is
   * what {@link org.gbif.ws.client.GbifAuthRequestInterceptor} produces.
   */
  public static boolean isContentMd5Valid(
      String contentMd5, byte[] body, Md5EncodeService md5EncodeService) {
    if (contentMd5 == null || body == null) {
      return false;
    }
    byte[] expected = contentMd5.getBytes(StandardCharsets.UTF_8);
    String rawMd5 = Base64.getEncoder().encodeToString(DigestUtils.md5(body));
    String clientMd5 = md5EncodeService.encode(body);
    // evaluate both to avoid leaking which encoding matched through timing
    boolean rawMatch = MessageDigest.isEqual(expected, rawMd5.getBytes(StandardCharsets.UTF_8));
    boolean clientMatch = MessageDigest.isEqual(expected, clientMd5.getBytes(StandardCharsets.UTF_8));
    return rawMatch || clientMatch;
  }
}
