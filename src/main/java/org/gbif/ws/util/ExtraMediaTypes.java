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
