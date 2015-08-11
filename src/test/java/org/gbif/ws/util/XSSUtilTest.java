package org.gbif.ws.util;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class XSSUtilTest {

  private String request;
  private String updatedRequest;

  public XSSUtilTest(String request, String updatedRequest) {
    this.request = request;
    this.updatedRequest = updatedRequest;
  }

  /**
   * Additional requests to test can be added here.
   */
  @Parameterized.Parameters
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {
      {"http://localhost:8090/ipt/about.do?request_locale=\"'><script>alert(6227)</script>&email=\"'><script>alert(6227)</script>&password=\"'><script>alert(6227)</script>&login-submit=\"'><script>alert(6227)</script>", "http://localhost:8090/ipt/about.do?request_locale=\"'>&email=\"'>&password=\"'>&login-submit=\"'>"},
      {"http://www.gbif.org/dataset/search?q=\"><script>prompt('XSSPOSED')</script>", "http://www.gbif.org/dataset/search?q=\">"},
      {"http://www.gbif.org/index.php?name=<script>window.onload = function() {var link=document.getElementsByTagName(\"a\");link[0].href=\"http://not-real-xssattackexamples.com/\";}</script>", "http://www.gbif.org/index.php?name="}});
  }

  @Test
  public void testStripXSS() {
    assertEquals(updatedRequest, XSSUtil.stripXSS(request));
  }
}
