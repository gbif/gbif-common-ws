/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.ws.server.provider;

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;

import java.lang.reflect.Type;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import com.google.inject.Singleton;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/**
 * Jersey provider class that extracts the requested country enum out of request parameters.
 * <p/>
 * Example resource use:
 * <pre>
 * {@code
 * public List<Node> list(@QueryParam("country") Country country) {
 *   // do stuff
 * }
 * }
 * </pre>
 * <p/>
 */
@Provider
@Singleton
public class CountryProvider extends AbstractHttpContextInjectable<Country> implements InjectableProvider<Context, Type> {

  private static final String PARAM = "country";

  @Override
  public Injectable<Country> getInjectable(ComponentContext ic, Context a, Type c) {
    if (c.equals(Country.class)) {
      return this;
    }

    return null;
  }

  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  public Country getValue(HttpContext c) {
    return getCountry(c);
  }

  public static Country getCountry(HttpContext ctx) {
    if (ctx.getRequest().getQueryParameters() != null && ctx.getRequest().getQueryParameters().containsKey(PARAM)) {
      String country = ctx.getRequest().getQueryParameters().getFirst(PARAM).trim();
      // first try iso codes
      Country c = Country.fromIsoCode(country);
      if (c == null) {
        // if nothing found also try by enum name
        c = (Country) VocabularyUtils.lookupEnum(country, Country.class);
      }
      return c;
    }

    return null;
  }
}
