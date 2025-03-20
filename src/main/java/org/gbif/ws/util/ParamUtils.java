package org.gbif.ws.util;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;
import org.gbif.api.util.DnaUtils;

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
