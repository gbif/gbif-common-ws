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

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;
import org.gbif.ws.util.SecurityConstants;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Tests which admin users an app may act as (gbif/informatics#55). */
public class GbifAuthenticationManagerImplTest {

  private static final String APP = "gbif.occurrence.ws";

  private final IdentityAccessService identityAccessService = mock(IdentityAccessService.class);
  private final GbifAuthService authService = mock(GbifAuthService.class);
  private final AppkeysConfigurationProperties appkeys = new AppkeysConfigurationProperties();

  @BeforeEach
  public void setUp() {
    when(authService.isValidRequest(any(GbifHttpServletRequestWrapper.class))).thenReturn(true);
    when(identityAccessService.get("download.gbif.org"))
        .thenReturn(user("download.gbif.org", UserRole.USER, UserRole.REGISTRY_ADMIN));
    when(identityAccessService.get("other.admin"))
        .thenReturn(user("other.admin", UserRole.USER, UserRole.GRSCICOLL_ADMIN));
    when(identityAccessService.get("jane")).thenReturn(user("jane", UserRole.USER));
    appkeys.setAdminImpersonation(Map.of(APP, List.of("download.gbif.org")));
  }

  @Test
  public void testAllowedAdmin() {
    appkeys.setEnforceAdminImpersonation(true);

    assertEquals("download.gbif.org", actAs(APP, "download.gbif.org").getName());
  }

  @Test
  public void testNormalUser() {
    appkeys.setEnforceAdminImpersonation(true);

    assertEquals("jane", actAs("gbif.portal", "jane").getName());
  }

  @Test
  public void testOtherAdminIsOnlyLoggedWhenNotEnforced() {
    assertEquals("other.admin", actAs(APP, "other.admin").getName());
    assertEquals("download.gbif.org", actAs("gbif.portal", "download.gbif.org").getName());
  }

  @Test
  public void testOtherAdminIsRejectedWhenEnforced() {
    appkeys.setEnforceAdminImpersonation(true);

    WebApplicationException e =
        assertThrows(WebApplicationException.class, () -> actAs(APP, "other.admin"));
    assertEquals(403, e.getStatus());
    assertThrows(WebApplicationException.class, () -> actAs("gbif.portal", "download.gbif.org"));
  }

  private GbifAuthentication actAs(String appKey, String username) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dataset");
    request.addHeader(HttpHeaders.AUTHORIZATION, SecurityConstants.GBIF_SCHEME_PREFIX + appKey + ":sig");
    request.addHeader(SecurityConstants.HEADER_GBIF_USER, username);
    return new GbifAuthenticationManagerImpl(identityAccessService, authService, appkeys)
        .authenticate(request);
  }

  private static GbifUser user(String username, UserRole... roles) {
    GbifUser user = new GbifUser();
    user.setUserName(username);
    user.getRoles().addAll(List.of(roles));
    return user;
  }
}
