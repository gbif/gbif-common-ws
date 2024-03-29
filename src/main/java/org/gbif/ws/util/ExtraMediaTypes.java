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

/**
 * Extra media types used in Http responses.
 */
public class ExtraMediaTypes {

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
