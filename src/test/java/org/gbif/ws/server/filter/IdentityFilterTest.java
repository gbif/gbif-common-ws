package org.gbif.ws.server.filter;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.security.GbifAuthServiceTest;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.SecurityContext;

import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.gbif.ws.security.GbifAuthServiceTest.APPKEY;
import static org.gbif.ws.security.GbifAuthServiceTest.APPSECRET;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests related to {@link IdentityFilter}.
 */
@RunWith(MockitoJUnitRunner.class)
public class IdentityFilterTest {
  @Mock
  private IdentityAccessService identityAccessService;
  @Mock
  private ContainerRequest mockRequest;

  //initialize in setUp()
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
    heinz.setPasswordHash("HEINZ"); //this is not a hash but it doesn't matter for the test
    heinz.setEmail("heinz@mailinator.com");
    heinz.getRoles().add(UserRole.USER);
    TEST_USERS.put(heinz.getUserName(), heinz);

    søren.setUserName("søren");
    søren.setPasswordHash("SØREN"); //this is not a hash but it doesn't matter for the test
    søren.setEmail("søren@mailinator.com");
    søren.getRoles().add(UserRole.USER);
    TEST_USERS.put(søren.getUserName(), søren);

    heinzAuthBasic = "Basic " + toAuthorizationString(heinz);
    sørenAuthBasic = "Basic " + toAuthorizationString(søren);
    legacyBasic = "Basic " + toAuthorizationString(UUID.randomUUID().toString(),":password");
  }

  @Before
  public void setUp() {
    authService = GbifAuthService.multiKeyAuthService(GbifAuthServiceTest.buildAppKeyMap());
    identityFilter = new IdentityFilter(new IdentityAccessService() {

      @Nullable
      @Override
      public GbifUser get(String username) {
        return TEST_USERS.get(username);
      }

      @Nullable
      @Override
      public GbifUser authenticate(String username, String password) {
        return Optional.ofNullable(get(username))
                .filter( u -> u.getPasswordHash().equals(password))
                .orElse(null);
      }
    }, authService);
  }

  private static String toAuthorizationString(GbifUser user) {
    return toAuthorizationString(user.getUserName(), user.getPasswordHash());
  }

  private static String toAuthorizationString(String user, String password) {
    return new String(Base64.encode((user + ":" + password).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
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
  public void testFilterWithBasicAuthWrongPassword()  {
    assertPrincipalName(null,
            getMockRequestAuthorization("Basic " + toAuthorizationString(heinz.getUserName(), "wrong")));
  }

  /**
   * Test a user authenticated by appkey.
   * @throws Exception
   */
  @Test
  public void testUserByAppKey() throws Exception {
    ContainerRequest req = GbifAuthServiceTest.createMockContainerRequestFor(heinz.getUserName(),
            GbifAuthService.singleKeyAuthService(APPKEY, APPSECRET));
    ContainerRequest filteredReq = identityFilter.filter(req);
    assertNotNull(filteredReq.getUserPrincipal());
    assertEquals(heinz.getUserName(), filteredReq.getUserPrincipal().getName());
    assertFalse(filteredReq.isUserInRole(UserRole.REGISTRY_ADMIN.name()));
  }

  private void assertPrincipalName(String expectedUsername, ContainerRequest request) {
    ContainerRequest req = identityFilter.filter(request);
    SecurityContext securityContext = req.getSecurityContext();

    if (expectedUsername == null) {
      assertNull(securityContext.getUserPrincipal());
      assertFalse(securityContext.isUserInRole("ADMIN"));
    } else {
      assertEquals(expectedUsername, securityContext.getUserPrincipal().getName());
      assertTrue(securityContext.isUserInRole("ADMIN") || securityContext.isUserInRole("USER"));
    }
  }

  /**
   * Creates a mock {@link ContainerRequest} that will return the provided authorization string in the
   * "Authorization" header.
   *
   * @param authorization
   * @return
   */
  private ContainerRequest getMockRequestAuthorization(String authorization) {
    ContainerRequest cr = mock(ContainerRequest.class);
    when(cr.getHeaderValue(ArgumentMatchers.eq(ContainerRequest.AUTHORIZATION))).thenReturn(authorization);
    when(cr.getSecurityContext()).thenCallRealMethod();
    Mockito.doCallRealMethod().when(cr).setSecurityContext(any(SecurityContext.class));
    return cr;
  }

}
