package org.gbif.ws.security;

import org.gbif.ws.server.GbifHttpServletRequestWrapper;

public interface GbifAuthService {

  boolean isValidRequest(GbifHttpServletRequestWrapper request);

  GbifHttpServletRequestWrapper signRequest(String username, GbifHttpServletRequestWrapper request);
}
