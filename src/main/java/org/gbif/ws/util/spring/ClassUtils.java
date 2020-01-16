/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gbif.ws.util.spring;

import java.util.ArrayList;
import java.util.List;

/**
 * Miscellaneous class utility methods. Mainly for internal use within the
 * framework; consider Jakarta's Commons Lang for a more comprehensive suite
 * of class utilities.
 *
 * @author Keith Donald
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see ReflectionUtils
 * @since 1.1
 */
public abstract class ClassUtils {

  /**
   * Return all interfaces that the given class implements as array,
   * including ones implemented by superclasses.
   * <p>If the class itself is an interface, it gets returned as sole interface.
   *
   * @param clazz the class to analyse for interfaces
   *
   * @return all interfaces that the given object implements as array
   */
  public static Class[] getAllInterfacesForClass(Class clazz) {
    Assert.notNull(clazz, "Class must not be null");
    if (clazz.isInterface()) {
      return new Class[] {clazz};
    }
    List interfaces = new ArrayList();
    while (clazz != null) {
      for (int i = 0; i < clazz.getInterfaces().length; i++) {
        Class ifc = clazz.getInterfaces()[i];
        if (!interfaces.contains(ifc)) {
          interfaces.add(ifc);
        }
      }
      clazz = clazz.getSuperclass();
    }
    return (Class[]) interfaces.toArray(new Class[interfaces.size()]);
  }
}
