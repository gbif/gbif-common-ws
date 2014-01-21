package org.gbif.ws.server.filter;

import org.gbif.api.model.common.User;
import org.gbif.api.service.common.UserService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.ws.security.GbifAppAuthService;
import org.gbif.ws.security.GbifAppAuthServiceTest;

import java.io.UnsupportedEncodingException;
import java.util.UUID;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.sun.jersey.core.util.Base64;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.ContainerRequest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthFilterTest {

  @Mock
  UserService userService;
  @Mock
  ContainerRequest mockRequest;
  ArgumentCaptor<SecurityContext> secCtxCaptor = ArgumentCaptor.forClass(SecurityContext.class);

  AuthFilter filter;
  static String adminAuthGbif;
  static String heinzAuthBasic;
  static String legacyBasic;

  static User admin = new User();
  static User heinz = new User();

  static {
    admin.setUserName("admin");
    admin.setEmail("admin@mailinator.com");
    admin.getRoles().add(UserRole.ADMIN);
    admin.getRoles().add(UserRole.USER);

    heinz.setUserName("heinz");
    heinz.setEmail("heinz@mailinator.com");
    heinz.getRoles().add(UserRole.USER);

    try {
      adminAuthGbif = "GBIF " + new String(Base64.encode("1234567890:admin"), "UTF8");
      heinzAuthBasic = "Basic " + new String(Base64.encode("heinz:HEINZ"), "UTF8");
      legacyBasic = "Basic " + new String(Base64.encode(UUID.randomUUID().toString() + ":password"), "UTF8");
    } catch (UnsupportedEncodingException e) {
      // TODO: Handle exception
    }
  }

  @Before
  public void initMocks() {
    when(userService.get(Matchers.<String>eq("admin"))).thenReturn(admin);
    when(userService.authenticate(Matchers.<String>eq("admin"), Matchers.<String>any())).thenReturn(admin);
    when(userService.get(Matchers.<String>eq("heinz"))).thenReturn(heinz);
    when(userService.authenticate(Matchers.<String>eq("heinz"), Matchers.<String>any())).thenReturn(heinz);
    // setup basic POST request
    when(mockRequest.getQueryParameters()).thenReturn(new MultivaluedMapImpl());
    when(mockRequest.getMethod()).thenReturn("POST");
    setMockAuth(null);

    GbifAppAuthService authService = new GbifAppAuthService(GbifAppAuthServiceTest.buildAppKeyMap());
    filter = new AuthFilter(userService, authService);
  }

  @Test
  public void testFilterWithAnonymous() throws Exception {
    assertPrincipalName(null);

    setMockAuth("");
    assertPrincipalName(null);

    setMockAuth("Digest ");
    assertPrincipalName(null);
  }

  @Test
  public void testFilterWithLegacyAccount() throws Exception {
    setMockAuth(legacyBasic);
    assertPrincipalName(null);
  }

  @Test
  public void testFilterWithBasicHeinz() throws Exception {
    setMockAuth(heinzAuthBasic);
    assertPrincipalName("heinz");
  }

  @Test
  @Ignore("Test needs to be updated to have real headers")
  public void testFilterWithGbifAdmin() throws Exception {
    setMockAuth(adminAuthGbif);
    assertPrincipalName("admin");
  }


  @Test
  public void testFilterWithGET() throws Exception {
    when(mockRequest.getMethod()).thenReturn("GET");
    assertPrincipalName(null);
  }

  private void assertPrincipalName(String username) {
    ContainerRequest req = filter.filter(mockRequest);
    if (username == null) {
      verify(req, atLeastOnce()).setSecurityContext(secCtxCaptor.capture());
      assertNull(secCtxCaptor.getValue().getUserPrincipal());
      assertFalse(secCtxCaptor.getValue().isUserInRole("ADMIN"));
    } else {
      verify(req, atLeastOnce()).setSecurityContext(secCtxCaptor.capture());
      assertEquals(username, secCtxCaptor.getValue().getUserPrincipal().getName());
      assertTrue(secCtxCaptor.getValue().isUserInRole("ADMIN") || secCtxCaptor.getValue().isUserInRole("USER"));
    }
  }

  /**
   * @param expectedType if null expect to be authorized
   */
  private void assertResponseStatus(Response.Status expectedType) {
    boolean webexcept = false;
    try {
      ContainerRequest req = filter.filter(mockRequest);
    } catch (WebApplicationException e) {
      webexcept = true;
      assertWebApplicationExceptionOfType(e, expectedType);
    }
    if (!webexcept && expectedType != null) {
      fail("Expected WebApplicationException of type " + expectedType);
    }
  }

  private void assertWebApplicationExceptionOfType(WebApplicationException e, Response.Status expectedType) {
    assertTrue(expectedType.getStatusCode() == e.getResponse().getStatus());
  }

  private void setMockAuth(String authentication) {
    when(mockRequest.getHeaderValue(Matchers.<String>eq(ContainerRequest.AUTHORIZATION))).thenReturn(authentication);
  }

}
