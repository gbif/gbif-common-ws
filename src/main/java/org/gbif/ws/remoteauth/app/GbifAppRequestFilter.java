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
package org.gbif.ws.remoteauth.app;

import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.ws.security.GbifAuthUtils;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.Md5EncodeServiceImpl;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;
import org.gbif.ws.util.SecurityConstants;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Intercepts all requests to look for a GBIF scheme authorization header.
 *
 * <p>The signature is verified remotely, so the original method and URL sent along are taken from
 * this request: a client supplied x-url is only forwarded if it refers to the real path, and the
 * Content-MD5 header is verified here against the actual body since the body is not forwarded.
 */
@Slf4j
public class GbifAppRequestFilter extends OncePerRequestFilter {

  private final AuthenticationManager authenticationManager;
  private final Md5EncodeService md5EncodeService;

  public GbifAppRequestFilter(AuthenticationManager authenticationManager) {
    this(
        authenticationManager,
        new Md5EncodeServiceImpl(JacksonJsonObjectMapperProvider.getObjectMapper()));
  }

  public GbifAppRequestFilter(
      AuthenticationManager authenticationManager, Md5EncodeService md5EncodeService) {
    this.authenticationManager = authenticationManager;
    this.md5EncodeService = md5EncodeService;
  }

  /**
   * Performs the authentication, only if JWT token is found.
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null
        && StringUtils.startsWith(authorization, SecurityConstants.GBIF_SCHEME_PREFIX)) {

      String gbifUser = request.getHeader(SecurityConstants.HEADER_GBIF_USER);
      String contentMd5 = request.getHeader(SecurityConstants.HEADER_CONTENT_MD5);
      String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
      String date = request.getHeader(SecurityConstants.HEADER_GBIF_DATE);
      String requestPath = GbifAuthUtils.getCanonicalizedPath(request.getRequestURI());
      String originalRequestUrl = request.getHeader(SecurityConstants.HEADER_ORIGINAL_REQUEST_URL);

      if (originalRequestUrl != null
          && !GbifAuthUtils.isOriginalUrlValid(originalRequestUrl, requestPath)) {
        log.info(
            "{} header {} does not match request path {}",
            SecurityConstants.HEADER_ORIGINAL_REQUEST_URL,
            originalRequestUrl,
            requestPath);
        reject(response);
        return;
      }
      // forward the original (raw) path with the real query string, both are signed
      String originalPath =
          URI.create(originalRequestUrl != null ? originalRequestUrl : request.getRequestURI())
              .normalize()
              .getRawPath();
      originalRequestUrl =
          StringUtils.isEmpty(request.getQueryString())
              ? originalPath
              : originalPath + '?' + request.getQueryString();

      if (!isContentValid(request, contentMd5)) {
        log.info(
            "{} header {} does not match the request body",
            SecurityConstants.HEADER_CONTENT_MD5,
            contentMd5);
        reject(response);
        return;
      }

      try {
        SecurityContextHolder.getContext()
            .setAuthentication(
                authenticationManager.authenticate(
                    new GbifAppAuthentication(
                        authorization,
                        gbifUser,
                        contentMd5,
                        contentType,
                        request.getMethod(),
                        originalRequestUrl,
                        date)));
      } catch (AuthenticationException exc) {
        reject(response);
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private boolean isContentValid(HttpServletRequest request, String contentMd5) {
    byte[] body =
        request instanceof GbifHttpServletRequestWrapper
            ? ((GbifHttpServletRequestWrapper) request).getContentBytes()
            : null;
    if (body == null) {
      log.debug(
          "Request content not wrapped, {} can't be verified", SecurityConstants.HEADER_CONTENT_MD5);
      return true;
    }
    if (body.length == 0 && contentMd5 == null) {
      return true;
    }
    return GbifAuthUtils.isContentMd5Valid(contentMd5, body, md5EncodeService);
  }

  private static void reject(HttpServletResponse response) {
    SecurityContextHolder.clearContext();
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }
}
