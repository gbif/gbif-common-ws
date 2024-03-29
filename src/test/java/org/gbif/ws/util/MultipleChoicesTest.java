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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
