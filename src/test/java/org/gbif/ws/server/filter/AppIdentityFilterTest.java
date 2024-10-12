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
package org.gbif.ws.server.filter;

import org.gbif.ws.security.AppkeysConfigurationProperties;
import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.server.DelegatingServletInputStream;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;
import org.gbif.ws.util.SecurityConstants;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests related to {@link AppIdentityFilter}.
 */
@ExtendWith(MockitoExtension.class)
public class AppIdentityFilterTest {

  // initialize in setUp()
  private AppIdentityFilter appIdentityFilter;

  @Mock private GbifAuthService authServiceMock;

  private SecurityContext context;

  private String content;

  @BeforeEach
  public void setUp() {
    content = "content";
    context = new SecurityContextImpl();
    SecurityContextHolder.setContext(context);
    AppkeysConfigurationProperties appkeysConfiguration = new AppkeysConfigurationProperties();
    appkeysConfiguration.setWhitelist(Collections.singletonList("appkey"));
    appIdentityFilter = new AppIdentityFilter(authServiceMock, appkeysConfiguration);
  }

  /**
   * AppIdentityFilter expect the appkey as username.
   * If the user (header 'x-gbif-user') does not match appkey then the APP role will not be provided.
   */
  @Test
  public void testRandomUsername() throws Exception {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("GBIF appkey:blabla");
    when(mockRequest.getHeader(SecurityConstants.HEADER_GBIF_USER))
        .thenReturn("myuser"); // user does not match appkey
    when(mockRequest.getInputStream())
        .thenReturn(
            new DelegatingServletInputStream(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
    when(authServiceMock.isValidRequest(any(GbifHttpServletRequestWrapper.class))).thenReturn(true);

    // WHEN
    appIdentityFilter.doFilter(mockRequest, mockResponse, chain);

    // THEN
    assertNull(context.getAuthentication());
    verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
    verify(mockRequest).getHeader(SecurityConstants.HEADER_GBIF_USER);
    verify(mockRequest, atLeastOnce()).getInputStream();
    verify(authServiceMock).isValidRequest(any(GbifHttpServletRequestWrapper.class));
  }

  /**
   * Try with the appkey which matches the user header but not present in the white list.
   * So the APP role will not be provided.
   */
  @Test
  public void testAppkeyNotInWhiteList() throws Exception {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("GBIF myuser:blabla");
    when(mockRequest.getHeader(SecurityConstants.HEADER_GBIF_USER))
        .thenReturn("myuser"); // user does not match appkey
    when(mockRequest.getInputStream())
        .thenReturn(
            new DelegatingServletInputStream(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
    when(authServiceMock.isValidRequest(any(GbifHttpServletRequestWrapper.class))).thenReturn(true);

    // WHEN
    appIdentityFilter.doFilter(mockRequest, mockResponse, chain);

    // THEN
    assertNull(context.getAuthentication());
    verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
    verify(mockRequest).getHeader(SecurityConstants.HEADER_GBIF_USER);
    verify(mockRequest, atLeastOnce()).getInputStream();
    verify(authServiceMock).isValidRequest(any(GbifHttpServletRequestWrapper.class));
  }

  /**
   * Try with the appkey which matches the user header and presents in the white list.
   * The user should be successfully authenticated with the APP role.
   */
  @Test
  public void testRightRequest() throws Exception {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("GBIF appkey:blabla");
    when(mockRequest.getHeader(SecurityConstants.HEADER_GBIF_USER)).thenReturn("appkey");
    when(mockRequest.getInputStream())
        .thenReturn(
            new DelegatingServletInputStream(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
    when(authServiceMock.isValidRequest(any(GbifHttpServletRequestWrapper.class))).thenReturn(true);

    // WHEN
    appIdentityFilter.doFilter(mockRequest, mockResponse, chain);

    // THEN
    assertNotNull(context.getAuthentication());
    assertEquals("appkey", context.getAuthentication().getName());
    assertNotNull(context.getAuthentication().getAuthorities());
    assertEquals(
        "APP",
        ((List<SimpleGrantedAuthority>) context.getAuthentication().getAuthorities())
            .get(0)
            .getAuthority());
    verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
    verify(mockRequest).getHeader(SecurityConstants.HEADER_GBIF_USER);
    verify(mockRequest, atLeastOnce()).getInputStream();
    verify(authServiceMock).isValidRequest(any(GbifHttpServletRequestWrapper.class));
  }
}
