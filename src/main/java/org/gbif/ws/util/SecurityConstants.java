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
package org.gbif.ws.util;

public final class SecurityConstants {

  private SecurityConstants() {}

  public static final String GBIF_SCHEME = "GBIF";
  public static final String GBIF_SCHEME_PREFIX = GBIF_SCHEME + " ";
  public static final String BASIC_SCHEME_PREFIX = "Basic ";
  public static final String BEARER_SCHEME_PREFIX = "Bearer ";
  public static final String BASIC_AUTH = "BASIC";

  public static final String HEADER_TOKEN = "token";
  public static final String HEADER_GBIF_USER = "x-gbif-user";
  public static final String HEADER_CONTENT_MD5 = "Content-MD5";
  public static final String HEADER_ORIGINAL_REQUEST_URL = "x-url";
}
