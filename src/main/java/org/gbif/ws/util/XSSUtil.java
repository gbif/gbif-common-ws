package org.gbif.ws.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class with utility methods for XSS filtering.
 */
public class XSSUtil {
  private static final Logger LOG = LoggerFactory.getLogger(XSSUtil.class);

  private XSSUtil() {
    // empty private constructor
  }

  private static final String NULL_CHAR = "\0";
  private static final Pattern[] PATTERNS = new Pattern[] {
    // Avoid anything in a <script> type of expression
    Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
    // Avoid anything in a src='...' type of expression
    Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
    Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
    // Remove any lonesome </script> tag
    Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
    // Remove any lonesome <script ...> tag
    Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
    // Avoid eval(...) expressions
    Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
    // Avoid expression(...) expressions
    Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
    // Avoid javascript:... expressions
    Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
    // Avoid vbscript:... expressions
    Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
    // Avoid onload= expressions
    Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
  };


  public static boolean containsXSS(String value) {
      if (value != null) {

          // Avoid null characters
          value = value.replaceAll(NULL_CHAR, "");

          // Remove all sections that match a pattern
          for (Pattern scriptPattern : PATTERNS) {
              Matcher matcher = scriptPattern.matcher(value);
              if (matcher.find()) {
                  return true;
              }
          }
      }
      return false;
  }

  public static String stripXSS(String value) {
    if (value != null) {

      // Avoid null characters
      value = value.replaceAll(NULL_CHAR, "");

      // Remove all sections that match a pattern
      boolean found = false;
      for (Pattern scriptPattern : PATTERNS) {
          Matcher matcher = scriptPattern.matcher(value);
          if (matcher.find()) {
            found = true;
            value = matcher.replaceAll("");
          }
      }
      if (found) {
        LOG.warn("Malicious XSS script found: {}", value);
      }
    }
    return value;
  }
}
