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

import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;
import org.gbif.api.util.DnaUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

public class ParamUtils {

  private static final String DNA_SEQUENCE_PARAM = "dnaSequence";
  private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_");

  public static void convertDnaSequenceParam(
      Map<String, String[]> parametersMap, OccurrenceSearchRequest searchRequest) {
    parametersMap.forEach(
        (k, v) -> {
          if (UNDERSCORE_PATTERN.matcher(k).replaceAll("").equalsIgnoreCase(DNA_SEQUENCE_PARAM)) {
            Arrays.stream(v)
                .map(DnaUtils::convertDnaSequenceToID)
                .forEach(searchRequest::addDnaSequenceIDFilter);
          }
        });
  }
}
