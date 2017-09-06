/*
 * Copyright 2011 - 2015 Global Biodiversity Information Facility (GBIF)
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

import javax.ws.rs.core.MediaType;

/**
 * Extra media types used in Http responses.
 */
public class ExtraMediaTypes {

  public static final String APPLICATION_JAVASCRIPT = "application/javascript";
  public static final MediaType APPLICATION_JAVASCRIPT_TYPE = new MediaType("application", "javascript");

  public static final String TEXT_CSV = "text/csv";
  public static final MediaType TEXT_CSV_TYPE = new MediaType("text", "csv");

  public static final String TEXT_TSV = "text/tab-separated-values";
  public static final MediaType TEXT_TSV_TYPE = new MediaType("text", "tab-separated-values");

  //.xls
  public static final String APPLICATION_EXCEL = "application/vnd.ms-excel";
  public static final MediaType APPLICATION_EXCEL_TYPE = new MediaType("application", "vnd.ms-excel");

  //.xlsx
  public static final String APPLICATION_OFFICE_SPREADSHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  public static final MediaType APPLICATION_OFFICE_SPREADSHEET_TYPE = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  //.ods
  public static final String APPLICATION_OPEN_DOC_SPREADSHEET = "application/vnd.oasis.opendocument.spreadsheet";
  public static final MediaType APPLICATION_OPEN_DOC_SPREADSHEET_TYPE = new MediaType("application", "vnd.oasis.opendocument.spreadsheet");

  //the common one is defined by com.sun.jersey.multipart.file.CommonMediaTypes.ZIP , this is another used by some sites
  public static final String APPLICATION_XZIP_COMPRESSED = "application/x-zip-compressed";
  public static final MediaType APPLICATION_XZIP_COMPRESSED_TYPE = new MediaType("application", "x-zip-compressed");

  /**
   * Darwin Core archive media type with underlying zip structure.
   * Use carefully, it's an unregistered media type, in most of the cases it is more appropriate to return a simple
   * application/zip
   * http://www.iana.org/assignments/media-types/media-types.xhtml
   * Currently used for experimenting in OAI-PMH DublinCore resources.
   */
  public static final String APPLICATION_DWCA = "application/dwca+zip";

  private ExtraMediaTypes() {
    throw new UnsupportedOperationException("Can't initialize class");
  }

}
