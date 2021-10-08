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
package org.gbif.ws.server.provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.WebRequest;

import static org.gbif.ws.util.CommonWsUtils.getFirst;

/**
 * Provider class that extracts the requested locale based on http header or language query parameter.
 * This allows resources to access a locale context very easily while keeping all logic in this class.
 * The accept any language value * will be converted into a null locale.
 * Example resource use:
 * <pre>
 * {@code
 * public String getVernacularName(Locale locale) {
 *   return "this is the " + locale + " vernacular name: xyz");
 * }
 * }
 * </pre>
 */
public class LocaleProvider implements ContextProvider<Locale> {

  private static final String LANGUAGE_PARAM = "language";
  private static final String ANY_LANGUAGE = "*";

  @Override
  public Locale getValue(WebRequest webRequest) {
    return getLocale(webRequest);
  }

  public static Locale getLocale(WebRequest webRequest) {
    Map<String, String[]> params = webRequest.getParameterMap();

    // try language parameter first
    String languageParam = getFirst(params, LANGUAGE_PARAM);
    if (languageParam != null) {
      String lang = languageParam.trim().toLowerCase();
      // iso language has to be 2 lower case letters!
      if (StringUtils.isNotEmpty(lang) && lang.length() == 2) {
        return new Locale(lang);
      }
    }

    // try headers next
    String[] acceptLanguageHeaderValues = webRequest.getHeaderValues(HttpHeaders.ACCEPT_LANGUAGE);

    List<String> languages =
        acceptLanguageHeaderValues != null
            ? Arrays.asList(acceptLanguageHeaderValues)
            : Collections.emptyList();

    for (String lang : languages) {
      // ignore accept any language value: * and non iso 2-letter codes
      if (StringUtils.isNotEmpty(lang)
          && !ANY_LANGUAGE.equalsIgnoreCase(lang)
          && lang.length() == 2) {
        return Locale.forLanguageTag(lang);
      }
    }

    return null;
  }
}
