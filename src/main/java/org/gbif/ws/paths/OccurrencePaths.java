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
package org.gbif.ws.paths;

/**
 * Commons constants of occurrence service.
 */
public final class OccurrencePaths {

  public static final String OCCURRENCE_PATH = "occurrence";

  public static final String OCC_SEARCH_PATH = OCCURRENCE_PATH + "/search";

  public static final String RECORDED_BY_PATH = "recordedBy";

  public static final String IDENTIFIED_BY_PATH = "identifiedBy";

  public static final String RECORD_NUMBER_PATH = "recordNumber";

  public static final String CATALOG_NUMBER_PATH = "catalogNumber";

  public static final String INSTITUTION_CODE_PATH = "institutionCode";

  public static final String COLLECTION_CODE_PATH = "collectionCode";

  public static final String OCCURRENCE_ID_PATH = "occurrenceId";

  public static final String ORGANISM_ID_PATH = "organismId";

  public static final String LOCALITY_PATH = "locality";

  public static final String WATER_BODY_PATH = "waterBody";

  public static final String STATE_PROVINCE_PATH = "stateProvince";

  public static final String SAMPLING_PROTOCOL_PATH = "samplingProtocol";

  public static final String EVENT_ID_PATH = "eventId";

  public static final String PARENT_EVENT_ID_PATH = "parentEventId";

  public static final String VERBATIM_PATH = "verbatim";

  public static final String FRAGMENT_PATH = "fragment";

  /**
   * Private default constructor.
   */
  private OccurrencePaths() {
    // empty constructor
  }
}
