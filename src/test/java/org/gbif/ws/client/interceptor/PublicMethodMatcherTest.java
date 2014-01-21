package org.gbif.ws.client.interceptor;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PublicMethodMatcherTest {

  @Test
  public void testMatches() throws Exception {
    PublicMethodMatcher m = new PublicMethodMatcher();
    // only match methods
    assertFalse(m.matches(MatchTest.class.getDeclaredField("key")));
    // match only public methods
    assertFalse(m.matches(MatchTest.class.getDeclaredMethod("getInternal")));
    assertTrue(m.matches(MatchTest.class.getDeclaredMethod("get")));
    assertTrue(m.matches(MatchTest.class.getDeclaredMethod("set", Integer.class)));
    assertTrue(m.matches(MatchTest2.class.getDeclaredMethod("get2")));
    assertTrue(m.matches(MatchTest.class.getDeclaredMethod("getTyped", Object.class)));
    // exclude Object methods
    assertFalse(m.matches(MatchTest.class.getDeclaredMethod("hashCode")));
    assertFalse(m.matches(MatchTest.class.getDeclaredMethod("equals", Object.class)));
    assertFalse(m.matches(MatchTest2.class.getDeclaredMethod("toString")));

  }

  public static class MatchTest<T> {

    private Integer key = 1;

    public Integer get() {
      return getInternal();
    }

    public Integer getTyped(T in) {
      return getInternal();
    }

    public void set(Integer key) {
      this.key = key;
    }

    private Integer getInternal() {
      return key;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MatchTest)) return false;

      MatchTest matchTest = (MatchTest) o;

      if (key != null ? !key.equals(matchTest.key) : matchTest.key != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return key != null ? key.hashCode() : 0;
    }
  }

  public static class MatchTest2 extends MatchTest {

    public Integer get2() {
      return 2 * get();
    }

    @Override
    public String toString() {
      return "22";
    }
  }

}
