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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

/**
 * Class with util methods for WS.
 */
public final class CommonWsUtils {

  private CommonWsUtils() {}

  /**
   * Retrieve the first occurrence of the param in params.
   * Can be applied for HttpHeaders.
   */
  public static String getFirst(Map<String, String[]> params, String param) {
    final String[] values = params.get(param);
    String resultValue = null;

    if (values != null && values[0] != null) {
      resultValue = values[0];
    }

    return resultValue;
  }

  /**
   * MediaType by string extension.
   */
  @Nullable
  public static String getResponseTypeByExtension(
      @Nullable String extension, @NotNull String defaultMediaType) {
    if (StringUtils.isEmpty(extension)) {
      return defaultMediaType;
    } else if (".xml".equals(extension)) {
      return MediaType.APPLICATION_XML_VALUE;
    } else if (".json".equals(extension)) {
      return MediaType.APPLICATION_JSON_VALUE;
    } else {
      return null;
    }
  }
}
