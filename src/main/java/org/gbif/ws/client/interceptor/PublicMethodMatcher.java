package org.gbif.ws.client.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.google.inject.matcher.AbstractMatcher;

/**
 * Matches public, non synthetic methods only which are no Object methods, i.e. equals, hashCode and toString.
 */
public class PublicMethodMatcher extends AbstractMatcher<Object> implements Serializable {

  private static final long serialVersionUID = 0;

  @Override
  public boolean matches(Object o) {
    if (o instanceof Method) {
      Method m = (Method) o;
      if (Modifier.isPublic(m.getModifiers()) && (m.isBridge() || !m.isSynthetic())) {
        for (Method om : Object.class.getMethods()) {
          if (om.getName().equals(m.getName()) && om.getParameterTypes().length == m.getParameterTypes().length) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }
}
