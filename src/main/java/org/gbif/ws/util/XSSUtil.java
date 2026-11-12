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

import java.util.List;
import java.util.regex.Pattern;

import org.owasp.html.HtmlChangeListener;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class with utility methods for XSS filtering, backed by the OWASP Java HTML Sanitizer.
 *
 * <p>Markup is checked against an allowlist: basic formatting, lists and http(s) links. Anything
 * else, such as script, event handler attributes or javascript: links, is XSS.
 */
public class XSSUtil {
  private static final Logger LOG = LoggerFactory.getLogger(XSSUtil.class);

  private XSSUtil() {
    // empty private constructor
  }

  private static final PolicyFactory POLICY =
      new HtmlPolicyBuilder()
          .allowElements("b", "strong", "i", "em", "u", "p", "br", "ul", "ol", "li", "a")
          .allowUrlProtocols("http", "https")
          .allowAttributes("href")
          .onElements("a")
          .toFactory();

  /**
   * Payloads that are dangerous outside markup too, e.g. pasted into an attribute or a URL. These
   * patterns have no unbounded wildcards, so they run in linear time.
   */
  private static final List<Pattern> TEXT_PATTERNS =
      List.of(
          Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
          Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
          Pattern.compile("data:\\s*text/html", Pattern.CASE_INSENSITIVE),
          Pattern.compile(
              "on(load|error|mouseover|submit|reset|focus|click)\\s*=", Pattern.CASE_INSENSITIVE));

  /**
   * Method tests whether a string contains malicious XSS script or not.
   *
   * @param value decoded string to test
   *
   * @return true if the string contains markup not on the allowlist or a known script payload,
   *     false otherwise
   */
  public static boolean containsXSS(String value) {
    if (value == null) {
      return false;
    }

    DiscardListener listener = new DiscardListener();
    POLICY.sanitize(value, listener, null);
    if (listener.discarded
        || TEXT_PATTERNS.stream().anyMatch(p -> p.matcher(value).find())) {
      LOG.warn("Potentially malicious XSS script found: {}", value);
      return true;
    }
    return false;
  }

  /**
   * Removes all markup not on the allowlist.
   *
   * @return sanitized HTML: markup not on the allowlist is removed and text is HTML encoded, e.g.
   *     {@code &} becomes {@code &amp;}
   */
  public static String stripXSS(String value) {
    if (value == null) {
      return null;
    }
    return POLICY.sanitize(value);
  }

  private static final class DiscardListener implements HtmlChangeListener<Object> {
    private boolean discarded;

    @Override
    public void discardedTag(Object context, String elementName) {
      discarded = true;
    }

    @Override
    public void discardedAttributes(Object context, String tagName, String... attributeNames) {
      discarded = true;
    }
  }
}
