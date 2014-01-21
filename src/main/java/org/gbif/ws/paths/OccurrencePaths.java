/*
 * Copyright 2012 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
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

  public final static String OCCURRENCE_PATH = "occurrence";

  public static final String OCC_SEARCH_PATH = OCCURRENCE_PATH + "/search";

  public static final String COLLECTOR_NAME_PATH = "collector_name";

  public static final String CATALOG_NUMBER_PATH = "catalog_number";

  public static final String INSTITUTION_CODE_PATH = "institution_code";

  public static final String COLLECTION_CODE_PATH = "collection_code";

  public static final String VERBATIM_PATH = "verbatim";

  public static final String FRAGMENT_PATH = "fragment";

  /**
   * Private default constructor.
   */
  private OccurrencePaths() {
    // empty constructor
  }

}
