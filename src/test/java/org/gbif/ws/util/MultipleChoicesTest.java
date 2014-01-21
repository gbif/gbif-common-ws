package org.gbif.ws.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MultipleChoicesTest {

  @Test
  public void testGetChoices() {
    MultipleChoices choices = new MultipleChoices();
    assertNotNull(choices.getChoices());
    assertTrue(choices.getChoices().isEmpty());
  }

  @Test
  public void testAddChoice() {
    MultipleChoices choices = new MultipleChoices();
    choices.addChoice(321, "http://www.gbif.org", "GBIF", null);
    choices.addChoice(new Choice(null, "http://www.eol.org", "EOL", "Encyclopedia of Life"));
    assertEquals(2, choices.getChoices().size());
    assertEquals("GBIF", choices.getChoices().get(0).getTitle());
    assertNull(choices.getChoices().get(1).getKey());
  }

}
