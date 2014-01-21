package org.gbif.ws.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.google.common.base.Strings;
import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common utilities for dealing with Properties.
 *
 * @deprecated use org.gbif.utils.file.properties.PropertiesUtil instead
 */
@Deprecated
public class PropertiesUtil {

  private static final Logger LOG = LoggerFactory.getLogger(PropertiesUtil.class);

  /**
   * Reads a property file from the classpath.
   */
  public static Properties readFromClasspath(String filename) {
    InputStream inputStream = PropertiesUtil.class.getClassLoader().getResourceAsStream(filename);
    Properties properties = new Properties();
    try {
      properties.load(inputStream);
    } catch (IOException e) {
      LOG.error("Cannot read property file {}", filename, e);
    } catch (NullPointerException e) {
      LOG.error("Cannot read property file {}", filename, e);
    } finally {
      Closeables.closeQuietly(inputStream);
    }
    return properties;
  }

  /**
   * Reads a property file from an absolute filepath.
   */
  public static Properties readFromFile(String filepath) throws IOException {
    if (Strings.isNullOrEmpty(filepath)) {
      throw new IOException("No properties file given");
    }
    File pf = new File(filepath);
    if (!pf.exists()) {
      throw new IOException("Cannot find properties file " + filepath);
    }
    FileReader reader = new FileReader(pf);
    Properties properties = new Properties();
    try {
      properties.load(reader);
    } finally {
      Closeables.closeQuietly(reader);
    }
    return properties;
  }

  private PropertiesUtil() {
  }
}
