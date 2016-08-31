package org.gbif.ws.mixin;

import org.gbif.api.model.registry.Dataset;

import java.util.Map;

import com.google.common.base.Predicate;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class MixinsTest {

  @Test
  public void getPredefinedMixins(){

    // try a filer where we only want Mixin for the Dataset model.
    Map<Class<?>, Class<?>> result = Mixins.getPredefinedMixins(new Predicate<Class<?>>() {
      @Override
      public boolean apply(Class<?> input) {
        return Dataset.class.equals(input);
      }
    });

    assertEquals(1, result.size());
    assertEquals(DatasetMixin.class, result.get(Dataset.class));
  }
}
