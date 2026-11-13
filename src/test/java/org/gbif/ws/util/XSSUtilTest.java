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
package org.gbif.ws.util;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XSSUtilTest {

  /**
   * Additional requests to test can be added here. stripXSS returns HTML, so its text is encoded.
   */
  public static Stream<Arguments> data() {
    return Stream.of(
        // contain xss
        Arguments.of(
            "http://localhost:8090/ipt/about.do?request_locale=\"'><script>alert(6227)</script>&email=\"'><script>alert(6227)</script>&password=\"'><script>alert(6227)</script>&login-submit=\"'><script>alert(6227)</script>",
            true,
            "http://localhost:8090/ipt/about.do?request_locale&#61;&#34;&#39;&gt;&amp;email&#61;&#34;&#39;&gt;&amp;password&#61;&#34;&#39;&gt;&amp;login-submit&#61;&#34;&#39;&gt;"),
        Arguments.of(
            "http://www.gbif.org/dataset/search?q=\"><script>prompt('XSSPOSED')</script>",
            true,
            "http://www.gbif.org/dataset/search?q&#61;&#34;&gt;"),
        Arguments.of(
            "http://www.gbif.org/index.php?name=<script>window.onload = function() {var link=document.getElementsByTagName(\"a\");link[0].href=\"http://not-real-xssattackexamples.com/\";}</script>",
            true,
            "http://www.gbif.org/index.php?name&#61;"),
        Arguments.of(
            "http://www.gbif.org/dataset/search?q=<iframe>frame it</iframe>",
            true,
            "http://www.gbif.org/dataset/search?q&#61;"),
        Arguments.of(
            "http://www.gbif.org/login.do?email=\"><img src=x onerror=prompt(/XSS/)>",
            true,
            "http://www.gbif.org/login.do?email&#61;&#34;&gt;"),
        Arguments.of(
            "http://gbif.org/login.do?email=\"><img src=x onmouseover=prompt(/XSS/)>",
            true,
            "http://gbif.org/login.do?email&#61;&#34;&gt;"),
        // do not contain xss
        Arguments.of(
            "http://gbif.org/login.do?email=sven@gbif.org&password=xyz",
            false,
            "http://gbif.org/login.do?email=sven@gbif.org&password=xyz"),
        Arguments.of("http://www.gbif.org/ipt", false, null),
        Arguments.of("http://api.gbif.org/v1/occurrence/search?year=1800", false, null),
        Arguments.of("http://api.gbif.org/v1/dataset?onlyPublished=true&online=1", false, null),
        Arguments.of("Abundance < 5 and condition = good", false, null));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testContainsXSS(String value, boolean containsXSS, String stripXSSValue) {
    assertEquals(containsXSS, XSSUtil.containsXSS(value));

    if (containsXSS) {
      assertEquals(stripXSSValue, XSSUtil.stripXSS(value));
    }
  }

  /** Bypasses of the earlier regex patterns (gbif/informatics#63). */
  public static Stream<String> bypasses() {
    return Stream.of(
        "<scr<script>ipt>alert(1)",
        "<scr<script>ipt>alert(1)</script>",
        "<<script>script>alert(1)<</script>/script>",
        "<script/src=//evil.example/x.js></script>",
        "<svg onload=alert(1)>",
        "<svg/onload=alert(1)>",
        "<div onanimationstart=alert(1) style=animation-name:x>",
        "<div title=\"<\" onfocus=alert(1) tabindex=1>",
        "<a href=x onpointerenter=alert(1)>x</a>",
        "<body onpageshow=alert(1)>",
        "<iframe src=//evil.example></iframe>",
        "<object data=\"data:text/html,<script>alert(1)</script>\"></object>",
        "<a href=\"javascript:alert(1)\">x</a>",
        "<a href=\"jav&#x09;ascript:alert(1)\">x</a>",
        "\" onmouseover=\"alert(1)");
  }

  @ParameterizedTest
  @MethodSource("bypasses")
  public void testBypassesAreCaught(String value) {
    assertTrue(XSSUtil.containsXSS(value), value);

    String stripped = XSSUtil.stripXSS(value);
    assertFalse(XSSUtil.containsXSS(stripped), stripped);
    assertFalse(stripped.toLowerCase().contains("<script"), stripped);
  }

  /** Repeated unclosed fragments made the old regex patterns take quadratic time. */
  @ParameterizedTest
  @ValueSource(strings = {"<a", "<img", "<script>", "onload", "eval(", "<scr"})
  public void testRepeatedFragmentsAreFast(String fragment) {
    String value = fragment.repeat(100_000);

    assertTimeoutPreemptively(
        Duration.ofSeconds(5),
        () -> {
          XSSUtil.containsXSS(value);
          XSSUtil.stripXSS(value);
        });
  }

  @Test
  public void testDeeplyNestedValueIsStripped() {
    String value = "<scr".repeat(1000) + "<script>" + "ipt>".repeat(1000) + "alert(1)</script>";

    String stripped = XSSUtil.stripXSS(value);

    assertFalse(XSSUtil.containsXSS(stripped), stripped);
  }

  @Test
  public void testAllowedMarkupIsKept() {
    String value = "<p><b>Bold</b> and <a href=\"https://www.gbif.org\">a link</a></p>";

    assertFalse(XSSUtil.containsXSS(value));
    assertEquals(
value, XSSUtil.stripXSS(value));
  }
}
