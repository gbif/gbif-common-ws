package org.gbif.ws.server.interceptor;

import static org.junit.Assert.assertNull;

import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Dataset;
import org.junit.Test;

public class StringTrimInterceptorTest {

  private static final StringTrimInterceptor TRIMMER = new StringTrimInterceptor();

  @SuppressWarnings("ConstantConditions")
  @Test
  public void test() {
    Dataset dataset = new Dataset();
    dataset.setTitle("   ");
    TRIMMER.trimStringsOf(dataset);
    assertNull("Dataset title should be null", dataset.getTitle());

    Citation citation = new Citation();
    citation.setText("");
    dataset.setCitation(citation);
    TRIMMER.trimStringsOf(dataset);
    assertNull("Citation text should be null", dataset.getCitation().getText());
  }
}
