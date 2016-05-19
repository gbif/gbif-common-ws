package org.gbif.ws.client;

import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;

import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.base.Strings;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * A simple builder class to generate query parameter maps.
 * It converts enum values to their name string, but uses ISO2 letter codes for the country and language enum.
 */
public class QueryParamBuilder {

  private final MultivaluedMap<String, String> params = new MultivaluedMapImpl();

  /**
   * Creates a new, empty query parameter builder.
   */
  public static QueryParamBuilder create() {
    return new QueryParamBuilder();
  }

  /**
   * Creates a query parameter builder from a list of key value pairs.
   *
   * @param kvp list of key value pairs with key first. For example K1,V1,K2,V2.
   *
   * @throws IllegalArgumentException if kvp list is not pairs with an even number
   */
  public static QueryParamBuilder create(Object... kvp) {
    if (kvp.length % 2 != 0) {
      throw new IllegalArgumentException("Equal number of kvp parameter required to build key value pairs.");
    }

    QueryParamBuilder builder = new QueryParamBuilder();
    for (int x = kvp.length - 1; x > 0; x -= 2) {
      Object k = kvp[x - 1];
      Object v = kvp[x];
      builder.queryParam(k, v);
    }
    return builder;
  }

  /**
   * Creates a query parameter builder with a list of key value pairs having the same key.
   *
   * @param key    parameter key
   * @param values list of values to be used to construct single key value pairs
   *
   * @throws IllegalArgumentException if kvp list is not pairs with an even number
   */
  public static QueryParamBuilder create(String key, Object[] values) {
    return new QueryParamBuilder().queryParam(key, values);
  }

  /**
   * Creates a query parameter builder from an existing parameter map.
   *
   * @param fromParams map of query parameters.
   */
  public static QueryParamBuilder create(Map<String, String> fromParams) {
    QueryParamBuilder builder = new QueryParamBuilder();
    for (Map.Entry<String, String> kvp : fromParams.entrySet()) {
      builder.queryParam(kvp.getKey(), kvp.getValue());
    }
    return builder;
  }

  /**
   * Adds an additional query parameter the parameter map.
   *
   * @param key   the query parameter name
   * @param value the query parameter value or a list if the parameter should be added several times
   *
   * @return the current builder to chain calls.
   */
  public QueryParamBuilder queryParam(Object key, Object... value) {
    if (key != null && value != null) {
      String k = key.toString();
      if (!Strings.isNullOrEmpty(k)) {
        for (Object val : value) {
          if (val != null) {
            // use iso codes for country and language enums, otherwise just toString
            String v;
            if (val instanceof Country){
              v = ((Country) val).getIso2LetterCode();
            } else if (val instanceof Language){
              v = ((Language) val).getIso2LetterCode();
            } else {
              v = val.toString();
            }
            if (!Strings.isNullOrEmpty(v)) {
              params.add(k, v);
            }
          }
        }
      }
    }
    return this;
  }

  public MultivaluedMap<String, String> build() {
    return params;
  }
}
