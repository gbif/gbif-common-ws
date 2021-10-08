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
package org.gbif.ws.server.interceptor;

import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StringTrimInterceptorTest {

  private static final StringTrimInterceptor TRIMMER = new StringTrimInterceptor();

  @SuppressWarnings("ConstantConditions")
  @Test
  public void test() {
    Dataset dataset = new Dataset();
    dataset.setTitle("   ");
    TRIMMER.trimStringsOf(dataset);
    assertNull(dataset.getTitle(), "Dataset title shall be null");

    Citation citation = new Citation();
    citation.setText("");
    dataset.setCitation(citation);
    TRIMMER.trimStringsOf(dataset);
    assertNull(dataset.getCitation().getText(), "Citation text shall be null");

    System.out.println("a\u0001b");
    Organization organization = new Organization();
    organization.setTitle(" hello \u0001world \u001a");
    TRIMMER.trimStringsOf(organization);
    assertEquals("hello world", organization.getTitle(), "Titles shall match");
  }
}
