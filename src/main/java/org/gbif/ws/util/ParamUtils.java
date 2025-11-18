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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.gbif.api.model.event.search.EventSearchParameter;
import org.gbif.api.model.event.search.EventSearchRequest;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;
import org.gbif.api.util.DnaUtils;
import org.gbif.api.util.Range;
import org.gbif.api.util.SearchTypeValidator;
import org.gbif.api.vocabulary.DurationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParamUtils {

  private static final String DNA_SEQUENCE_PARAM = "dnaSequence";
  private static final String HUMBOLDT_EVENT_DURATION_VALUE_PARAM = "humboldtEventDurationValue";
  private static final String HUMBOLDT_EVENT_DURATION_UNIT_PARAM = "humboldtEventDurationUnit";
  private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_");
  private static final Logger log = LoggerFactory.getLogger(ParamUtils.class);

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

  public static void convertHumboldtUnitsParam(
      Map<String, String[]> parametersMap, EventSearchRequest searchRequest) {
    String[] eventDurationValueParams = null;
    String[] eventDurationUnitParams = null;
    for (Map.Entry<String, String[]> entry : parametersMap.entrySet()) {
      String k = entry.getKey();
      String[] v = entry.getValue();
      if (UNDERSCORE_PATTERN
          .matcher(k)
          .replaceAll("")
          .equalsIgnoreCase(HUMBOLDT_EVENT_DURATION_VALUE_PARAM)) {
        eventDurationValueParams = v;
      } else if (UNDERSCORE_PATTERN
          .matcher(k)
          .replaceAll("")
          .equalsIgnoreCase(HUMBOLDT_EVENT_DURATION_UNIT_PARAM)) {
        eventDurationUnitParams = v;
      }
    }

    // we only take the first param since there is no way to link multiple params with their units
    if (eventDurationValueParams != null && eventDurationUnitParams != null) {
      String eventDurationValue = eventDurationValueParams[0];
      DurationUnit eventDurationUnit =
          DurationUnit.parseDurationUnit(eventDurationUnitParams[0]).orElse(null);
      if (eventDurationUnit != null) {
        try {
          // convert values to minutes
          if (SearchTypeValidator.isNumericRange(eventDurationValue)) {
            Range<Double> values = SearchTypeValidator.parseDecimalRange(eventDurationValue);
            Range<Double> minutesRange =
                Range.closed(
                    values.lowerEndpoint() * eventDurationUnit.getDurationInMinutes(),
                    values.upperEndpoint() * eventDurationUnit.getDurationInMinutes());

            searchRequest
                .getParameters()
                .put(
                    EventSearchParameter.HUMBOLDT_EVENT_DURATION_VALUE_IN_MINUTES,
                    Set.of(minutesRange.toString()));
          } else {
            double durationInMinutes =
                Double.parseDouble(eventDurationValue) * eventDurationUnit.getDurationInMinutes();

            searchRequest
                .getParameters()
                .put(
                    EventSearchParameter.HUMBOLDT_EVENT_DURATION_VALUE_IN_MINUTES,
                    Set.of(String.valueOf(durationInMinutes)));
          }

          searchRequest.getParameters().remove(EventSearchParameter.HUMBOLDT_EVENT_DURATION_VALUE);
          searchRequest.getParameters().remove(EventSearchParameter.HUMBOLDT_EVENT_DURATION_UNIT);
        } catch (Exception ex) {
          log.info("Couldn't convert humboldt event duration value", ex);
        }
      }
    }
  }
}
