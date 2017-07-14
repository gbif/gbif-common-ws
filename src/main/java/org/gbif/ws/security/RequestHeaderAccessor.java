package org.gbif.ws.security;

/**
 * Simple function interface to access request headers from different classes.
 */
@FunctionalInterface
public interface RequestHeaderAccessor {
  String getHeader(String header);
}
