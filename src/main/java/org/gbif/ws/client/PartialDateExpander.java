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
package org.gbif.ws.client;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import feign.Param;

/**
 * Convert date string from the default {@link Date} format to custom 'yyyy-MM'.
 */
public class PartialDateExpander implements Param.Expander {

  private static final Logger LOG = LoggerFactory.getLogger(PartialDateExpander.class);

  private static final String UTIL_DATE_FORMAT_PATTERN = "EEE MMM dd HH:mm:ss zzz yyyy";
  private static final String CUSTOM_DATE_FORMAT_PATTERN = "yyyy-MM";

  @Override
  public String expand(Object value) {
    return convertDate(value);
  }

  private String convertDate(Object rawDate) {
    Date utilDate;

    try {
      utilDate = new SimpleDateFormat(UTIL_DATE_FORMAT_PATTERN).parse(rawDate.toString());
    } catch (ParseException e) {
      LOG.error("Wrong date format {}. Expected format: {}", rawDate, UTIL_DATE_FORMAT_PATTERN);
      return null;
    }

    return new SimpleDateFormat(CUSTOM_DATE_FORMAT_PATTERN).format(utilDate);
  }
}
