/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.security.GbifAuthServiceTest;
import org.gbif.ws.security.GbifAuthenticationManagerImpl;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Unit tests related to {@link IdentityFilter}.
 */
@RunWith(MockitoJUnitRunner.class)
public class IdentityFilterTest {

  @Mock private HttpServletResponse mockResponse;
  @Mock private FilterChain mockFilterChain;

  // initialize in setUp()
  private IdentityFilter identityFilter;
  private GbifAuthService authService;

  private static String heinzAuthBasic;
  private static String sørenAuthBasic;
  private static String legacyBasic;

  private static GbifUser admin = new GbifUser();
  private static GbifUser heinz = new GbifUser();
  private static GbifUser søren = new GbifUser();

  private static final Map<String, GbifUser> TEST_USERS = new HashMap<>();

  static {
    admin.setUserName("admin");
    admin.setPasswordHash("1234567890");
    admin.setEmail("admin@mailinator.com");
    admin.getRoles().add(UserRole.REGISTRY_ADMIN);
    admin.getRoles().add(UserRole.USER);
    TEST_USERS.put(admin.getUserName(), admin);

    heinz.setUserName("heinz");
    heinz.setPasswordHash("HEINZ"); // this is not a hash but it doesn't matter for the test
    heinz.setEmail("heinz@mailinator.com");
    heinz.getRoles().add(UserRole.USER);
    TEST_USERS.put(heinz.getUserName(), heinz);

    søren.setUserName("søren");
    søren.setPasswordHash("SØREN"); // this is not a hash but it doesn't matter for the test
    søren.setEmail("søren@mailinator.com");
    søren.getRoles().add(UserRole.USER);
    TEST_USERS.put(søren.getUserName(), søren);

    heinzAuthBasic = "Basic " + toAuthorizationString(heinz);
    sørenAuthBasic = "Basic " + toAuthorizationString(søren);
    legacyBasic = "Basic " + toAuthorizationString(UUID.randomUUID().toString(), ":password");
  }

  @Before
  public void setUp() throws Exception {
    doThrow(WebApplicationException.class).when(mockResponse).setStatus(anyInt());
    authService = GbifAuthServiceTest.prepareGbifAuthService();
    identityFilter =
        new IdentityFilter(
            new GbifAuthenticationManagerImpl(
                new IdentityAccessService() {

                  @Nullable
                  @Override
                  public GbifUser get(String username) {
                    return TEST_USERS.get(username);
                  }

                  @Nullable
                  @Override
                  public GbifUser authenticate(String username, String password) {
                    return Optional.ofNullable(get(username))
                        .filter(u -> u.getPasswordHash().equals(password))
                        .orElse(null);
                  }
                },
                authService));
  }

  private static String toAuthorizationString(GbifUser user) {
    return toAuthorizationString(user.getUserName(), user.getPasswordHash());
  }

  private static String toAuthorizationString(String user, String password) {
    return new String(
        Base64.getEncoder().encode((user + ":" + password).getBytes(StandardCharsets.UTF_8)),
        StandardCharsets.UTF_8);
  }

  @Test
  public void testFilterWithAnonymous() throws Exception {
    assertPrincipalName(null, getMockRequestAuthorization(null));
    assertPrincipalName(null, getMockRequestAuthorization(""));
    assertPrincipalName(null, getMockRequestAuthorization("Digest "));
  }

  @Test
  public void testFilterWithLegacyAccount() throws Exception {
    assertPrincipalName(null, getMockRequestAuthorization(legacyBasic));
  }

  @Test
  public void testFilterWithBasicAuth() throws Exception {
    assertPrincipalName("heinz", getMockRequestAuthorization(heinzAuthBasic));
  }

  @Test
  public void testFilterWithBasicAuthUtf8() throws Exception {
    assertPrincipalName("søren", getMockRequestAuthorization(sørenAuthBasic));
  }

  @Test(expected = WebApplicationException.class)
  public void testFilterWithBasicAuthWrongPassword() throws Exception {
    assertPrincipalName(
        null,
        getMockRequestAuthorization(
            "Basic " + toAuthorizationString(heinz.getUserName(), "wrong")));
  }

  private void assertPrincipalName(String expectedUsername, GbifHttpServletRequestWrapper request)
      throws Exception {
    identityFilter.doFilter(request, mockResponse, mockFilterChain);
    SecurityContext securityContext = SecurityContextHolder.getContext();

    if (expectedUsername == null) {
      assertNull(securityContext.getAuthentication().getName());
      assertFalse(
          securityContext
              .getAuthentication()
              .getAuthorities()
              .contains(new SimpleGrantedAuthority("ADMIN")));
    } else {
      assertEquals(expectedUsername, securityContext.getAuthentication().getName());
      assertTrue(
          securityContext
                  .getAuthentication()
                  .getAuthorities()
                  .contains(new SimpleGrantedAuthority("ADMIN"))
              || securityContext
                  .getAuthentication()
                  .getAuthorities()
                  .contains(new SimpleGrantedAuthority("USER")));
    }
  }

  private GbifHttpServletRequestWrapper getMockRequestAuthorization(String authorization) {
    GbifHttpServletRequestWrapper requestObjectMock = mock(GbifHttpServletRequestWrapper.class);
    when(requestObjectMock.getHeader(AUTHORIZATION)).thenReturn(authorization);

    return requestObjectMock;
  }
}
