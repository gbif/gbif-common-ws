package org.gbif.ws.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sun.jersey.core.util.MultivaluedMapImpl;


/**
 * Utility class for common operations on {@link Map}.
 * In particular {@link MultivaluedMap} and {@link Multimap} are commonly converted between them.
 */
public class MapConvertUtils {


  /**
   * Private constructor.
   */
  private MapConvertUtils() {
    // Empty block
  }

  /**
   * Converts a {@link MultivaluedMap} into a {@link Multimap}.
   * If the parameter is null an empty multimap is returned.
   */
  public static <K, V> Multimap<K, V> toMultiMap(MultivaluedMap<K, V> multivaluedMap) {
    Multimap<K, V> multimap = HashMultimap.create();
    if (multivaluedMap != null) {
      for (Entry<K, List<V>> entry : multivaluedMap.entrySet()) {
        multimap.putAll(entry.getKey(), entry.getValue());
      }
    }
    return multimap;
  }

  /**
   * Converts a {@link Multimap} into a {@link MultivaluedMap}.
   * If the parameter is null an empty multivaluedmap is returned.
   */
  public static MultivaluedMap<String, String> toMultivaluedMap(Multimap<String, String> multimap) {
    MultivaluedMap<String, String> multivaluedMap = new MultivaluedMapImpl();
    if (multimap != null) {
      for (Entry<String, String> entry : multimap.entries()) {
        multivaluedMap.add(entry.getKey(), entry.getValue());
      }
    }
    return multivaluedMap;
  }

}
