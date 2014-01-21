package org.gbif.ws.client.filter;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.UserPrincipal;

import java.security.Principal;

import com.google.common.base.Strings;
import com.google.inject.Provider;

/**
 * A Principal provider providing the same principal every time.
 * The principal to be provided can be changed at any time.
 *
 * Useful for testing ws-clients.
 */
public class SimplePrincipalProvider implements Provider<Principal> {

  private UserPrincipal current;

  public void setPrincipal(String username) {
    if (Strings.isNullOrEmpty(username)) {
      current = null;
    } else {
      User user = new User();
      user.setUserName(username);
      current = new UserPrincipal(user);
    }
  }

  @Override
  public Principal get() {
    return current;
  }
}
