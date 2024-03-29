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
package org.gbif.ws.util.spring;

import org.apache.commons.lang3.StringUtils;

/**
 * Assertion utility class that assists in validating arguments.
 * Useful for identifying programmer errors early and clearly at runtime.
 * <p>For example, if the contract of a public method states it does not
 * allow <code>null</code> arguments, Assert can be used to validate that
 * contract. Doing this clearly indicates a contract violation when it
 * occurs and protects the class's invariants.
 * <p>Typically used to validate method arguments rather than configuration
 * properties, to check for cases that are usually programmer errors rather than
 * configuration errors. In contrast to config initialization code, there is
 * usally no point in falling back to defaults in such methods.
 * <p>This class is similar to JUnit's assertion library. If an argument value is
 * deemed invalid, an {@link IllegalArgumentException} is thrown (typically).
 * For example:
 * <pre class="code">
 * Assert.notNull(clazz, "The class must not be null");
 * Assert.isTrue(i > 0, "The value must be greater than zero");</pre>
 * Mainly for internal use within the framework; consider Jakarta's Commons Lang
 * >= 2.0 for a more comprehensive suite of assertion utilities.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Rob Harrop
 * @since 1.1.2
 */
public abstract class Assert {

  /**
   * Assert a boolean expression, throwing <code>IllegalArgumentException</code>
   * if the test result is <code>false</code>.
   * <pre class="code">Assert.isTrue(i &gt; 0, "The value must be greater than zero");</pre>
   *
   * @param expression a boolean expression
   * @param message    the exception message to use if the assertion fails
   *
   * @throws IllegalArgumentException if expression is <code>false</code>
   */
  public static void isTrue(boolean expression, String message) {
    if (!expression) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Assert a boolean expression, throwing <code>IllegalArgumentException</code>
   * if the test result is <code>false</code>.
   * <pre class="code">Assert.isTrue(i &gt; 0);</pre>
   *
   * @param expression a boolean expression
   *
   * @throws IllegalArgumentException if expression is <code>false</code>
   */
  public static void isTrue(boolean expression) {
    isTrue(expression, "[Assertion failed] - this expression must be true");
  }

  /**
   * Assert that an object is <code>null</code> .
   * <pre class="code">Assert.isNull(value, "The value must be null");</pre>
   *
   * @param object  the object to check
   * @param message the exception message to use if the assertion fails
   *
   * @throws IllegalArgumentException if the object is not <code>null</code>
   */
  public static void isNull(Object object, String message) {
    if (object != null) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Assert that an object is <code>null</code> .
   * <pre class="code">Assert.isNull(value);</pre>
   *
   * @param object the object to check
   *
   * @throws IllegalArgumentException if the object is not <code>null</code>
   */
  public static void isNull(Object object) {
    isNull(object, "[Assertion failed] - the object argument must be null");
  }

  /**
   * Assert that an object is not <code>null</code> .
   * <pre class="code">Assert.notNull(clazz, "The class must not be null");</pre>
   *
   * @param object  the object to check
   * @param message the exception message to use if the assertion fails
   *
   * @throws IllegalArgumentException if the object is <code>null</code>
   */
  public static void notNull(Object object, String message) {
    if (object == null) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Assert that an object is not <code>null</code> .
   * <pre class="code">Assert.notNull(clazz);</pre>
   *
   * @param object the object to check
   *
   * @throws IllegalArgumentException if the object is <code>null</code>
   */
  public static void notNull(Object object) {
    notNull(object, "[Assertion failed] - this argument is required; it must not be null");
  }

  /**
   * Assert that the given String has valid text content; that is, it must not
   * be <code>null</code> and must contain at least one non-whitespace character.
   * <pre class="code">Assert.hasText(name, "'name' must not be empty");</pre>
   *
   * @param text    the String to check
   * @param message the exception message to use if the assertion fails
   */
  public static void hasText(String text, String message) {
    if (StringUtils.isEmpty(text)) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Assert that the given String has valid text content; that is, it must not
   * be <code>null</code> and must contain at least one non-whitespace character.
   * <pre class="code">Assert.hasText(name, "'name' must not be empty");</pre>
   *
   * @param text the String to check
   */
  public static void hasText(String text) {
    hasText(
        text,
        "[Assertion failed] - this String argument must have text; it must not be <code>null</code>, empty, or blank");
  }
}
