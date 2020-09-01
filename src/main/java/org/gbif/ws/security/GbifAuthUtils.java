/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.ws.security;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME_PREFIX;

public final class GbifAuthUtils {

  private static final Pattern COLON_PATTERN = Pattern.compile(":");

  private GbifAuthUtils() {}

  /**
   * Tries to get the appkey from the request header.
   * @param authorizationHeader 'Authorization' header.
   * @return the appkey found or null
   */
  public static String getAppKeyFromRequest(final String authorizationHeader) {
    if (StringUtils.startsWith(authorizationHeader, GBIF_SCHEME_PREFIX)) {
      String[] values = COLON_PATTERN.split(authorizationHeader.substring(5), 2);
      if (values.length == 2) {
        return values[0];
      }
    }
    return null;
  }
}
