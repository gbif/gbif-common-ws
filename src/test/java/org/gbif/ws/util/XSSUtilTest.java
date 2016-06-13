package org.gbif.ws.util;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class XSSUtilTest {
  // decoded request, header, or parameter
  private String value;

  // value expected after stripXSS is called
  private String stripXSSValue;

  // true if request contains XSS, or false otherwise
  private boolean containsXSS;

  public XSSUtilTest(String value, boolean containsXSS, String stripXSSValue) {
    this.value = value;
    this.containsXSS = containsXSS;
    this.stripXSSValue = stripXSSValue;
  }

  /**
   * Additional requests to test can be added here.
   */
  @Parameterized.Parameters
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {
      // contain xss
      {"http://localhost:8090/ipt/about.do?request_locale=\"'><script>alert(6227)</script>&email=\"'><script>alert(6227)</script>&password=\"'><script>alert(6227)</script>&login-submit=\"'><script>alert(6227)</script>", true,
              "http://localhost:8090/ipt/about.do?request_locale=\"'>&email=\"'>&password=\"'>&login-submit=\"'>"},
      {"http://www.gbif.org/dataset/search?q=\"><script>prompt('XSSPOSED')</script>", true, "http://www.gbif.org/dataset/search?q=\">"},
      {"http://www.gbif.org/index.php?name=<script>window.onload = function() {var link=document.getElementsByTagName(\"a\");link[0].href=\"http://not-real-xssattackexamples.com/\";}</script>", true,
              "http://www.gbif.org/index.php?name="},
      {"http://www.gbif.org/dataset/search?q=<iframe>frame it</iframe>", true, "http://www.gbif.org/dataset/search?q="},

      // do not contain xss
      {"http://www.gbif.org/ipt", false, null},
      {"http://api.gbif.org/v1/occurrence/search?year=1800", false, null}});
  }

  @Test
  public void testContainsXSS() {
    assertEquals(containsXSS, XSSUtil.containsXSS(value));

    if(containsXSS){
      assertEquals(stripXSSValue, XSSUtil.stripXSS(value));
    }
  }

}
