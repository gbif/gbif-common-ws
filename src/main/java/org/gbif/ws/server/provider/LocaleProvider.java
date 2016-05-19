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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import com.google.common.base.Strings;
import com.google.inject.Singleton;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/**
 * Jersey provider class that extracts the requested locale based on http header or language query parameter.
 * This allows resources to access a locale context very easily while keeping all logic in this class.
 * The accept any language value * will be converted into a null locale.
 * Example resource use:
 * <pre>
 * {@code
 * public String getVernacularName(@Context Locale locale) {
 *   return "this is the " + locale + " vernacular name: xyz");
 * }
 * }
 * </pre>
 */
@Provider
@Singleton
public class LocaleProvider extends AbstractHttpContextInjectable<Locale> implements InjectableProvider<Context, Type> {

  private static final String LANGUAGE_PARAM = "language";

  private static final String ANY_LANGUAGE = "*";

  @Override
  public Injectable<Locale> getInjectable(ComponentContext ic, Context a, Type c) {
    if (c.equals(Locale.class)) {
      return this;
    }

    return null;
  }

  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  public Locale getValue(HttpContext c) {
    return getLocale(c);
  }

  public static Locale getLocale(HttpContext c) {
    // try language parameter first
    if (c.getRequest().getQueryParameters() != null && c.getRequest().getQueryParameters()
      .containsKey(LANGUAGE_PARAM)) {
      String lang = c.getRequest().getQueryParameters().getFirst(LANGUAGE_PARAM).trim().toLowerCase();
      // iso language has to be 2 lower case letters!
      if (!Strings.isNullOrEmpty(lang) && lang.length() == 2) {
        return new Locale(lang);
      }
    }

    // try headers next
    final List<Locale> locales = c.getRequest().getAcceptableLanguages();
    for (Locale loc : locales) {
      // ignore accept any language value: * and non iso 2 letter codes
      if (!Strings.isNullOrEmpty(loc.getLanguage()) && !ANY_LANGUAGE.equalsIgnoreCase(loc.getLanguage())
          && loc.getLanguage().length() == 2) {
        return loc;
      }
    }
    return null;
  }
}
