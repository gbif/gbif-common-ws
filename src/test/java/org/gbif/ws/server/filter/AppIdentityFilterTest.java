package org.gbif.ws.server.filter;

import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.security.GbifAuthServiceTest;

import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.SecurityContext;

import com.sun.jersey.spi.container.ContainerRequest;
import org.junit.Before;
import org.junit.Test;

import static org.gbif.ws.security.GbifAuthServiceTest.APPKEY;
import static org.gbif.ws.security.GbifAuthServiceTest.APPSECRET;
import static org.gbif.ws.security.GbifAuthServiceTest.createMockContainerRequestFor;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests related to {@link AppIdentityFilter}.
 */
public class AppIdentityFilterTest {

  //initialize in setUp()
  private AppIdentityFilter appIdentityFilter;

  private GbifAuthService clientAuthService;
  private GbifAuthService authService;

  @Before
  public void setUp() {
    authService = GbifAuthService.multiKeyAuthService(GbifAuthServiceTest.buildAppKeyMap());
    clientAuthService = GbifAuthService.singleKeyAuthService(APPKEY, APPSECRET);

    List<String> appKeyWhiteList = Collections.singletonList(APPKEY);
    appIdentityFilter = new AppIdentityFilter(authService, appKeyWhiteList);
  }

  /**
   * AppIdentityFilter expect the appkey as username.
   * @throws URISyntaxException
   */
  @Test
  public void testRandomUsername() throws URISyntaxException {
    ContainerRequest cr = createMockContainerRequestFor("myuser", clientAuthService);
    cr = appIdentityFilter.filter(cr);
    assertNull(cr.getSecurityContext());
  }

  @Test
  public void testRightRequest() throws URISyntaxException {
    ContainerRequest cr = createMockContainerRequestFor(APPKEY, clientAuthService);
    cr = appIdentityFilter.filter(cr);
    assertNotNull(cr.getSecurityContext());
    assertEquals(APPKEY, cr.getUserPrincipal().getName());
  }

  /**
   * Ensure that if a SecurityContext is already set we do  ot overwrite it.
   * @throws URISyntaxException
   */
  @Test
  public void testRightRequestWithSecurityContext() throws URISyntaxException {
    ContainerRequest cr = createMockContainerRequestFor(APPKEY, clientAuthService);
    SecurityContext sc = mock(SecurityContext.class);
    when(sc.getAuthenticationScheme()).thenReturn("Mock:");
    when(sc.getUserPrincipal()).thenReturn(mock(Principal.class));

    cr.setSecurityContext(sc);

    cr = appIdentityFilter.filter(cr);
    assertNotNull(cr.getSecurityContext());
    assertEquals("Mock:", cr.getSecurityContext().getAuthenticationScheme());
  }

  @Test
  public void testRightRequestNotOnWhiteList() throws URISyntaxException {
    ContainerRequest cr = createMockContainerRequestFor(APPKEY, clientAuthService);
    AppIdentityFilter appIdentityFilterNoWhiteList = new AppIdentityFilter(authService, Collections.emptyList());
    cr = appIdentityFilterNoWhiteList.filter(cr);
    assertNull(cr.getSecurityContext());
  }

}
