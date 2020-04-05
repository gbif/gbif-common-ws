package org.gbif.ws.client;

import feign.Param;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert date string from the default {@link Date} format to custom 'yyyy-MM'.
 */
public class PartialDateExpander implements Param.Expander {

  private static final Logger LOG = LoggerFactory.getLogger(PartialDateExpander.class);

  private static final String UTIL_DATE_FORMAT_PATTERN = "EEE MMM dd HH:mm:ss zzz yyyy";
  private static final SimpleDateFormat UTIL_DATE_FORMAT =
      new SimpleDateFormat(UTIL_DATE_FORMAT_PATTERN);
  private static final String CUSTOM_DATE_FORMAT_PATTERN = "yyyy-MM";
  private static final SimpleDateFormat CUSTOM_DATE_FORMAT =
      new SimpleDateFormat(CUSTOM_DATE_FORMAT_PATTERN);

  @Override
  public String expand(Object value) {
    return convertDate(value);
  }

  private String convertDate(Object rawDate) {
    Date utilDate;

    try {
      utilDate = UTIL_DATE_FORMAT.parse(rawDate.toString());
    } catch (ParseException e) {
      LOG.error("Wrong date format {}. Expected format: {}", rawDate, UTIL_DATE_FORMAT_PATTERN);
      return null;
    }

    return CUSTOM_DATE_FORMAT.format(utilDate);
  }
}
