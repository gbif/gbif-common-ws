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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

/**
 * A very specific logback appender that looks for http headers as logevents coming apache http client.
 * The headers are then aggregated into a map and can be used for further debugging.
 * Useful when debugging http header traffic for example when dealing with Authentication headers.
 * A simple logback file activating this appender would look like this:
 * <pre>
 * {@code
 *   <appender name="HEADER" class="org.gbif.ws.util.HttpClientHeaderAppender" />
 *   <logger name="org.apache.http.headers" level="DEBUG">
 *     <appender-ref ref="HEADER"/>
 *   </logger>
 * }
 * </pre>
 */
public class HttpClientHeaderAppender extends UnsynchronizedAppenderBase<LoggingEvent> {

  private static Map<String, String> headers = Maps.newHashMap();
  private static Pattern splitHeader = Pattern.compile(" (.+):(.+)");

  public static String getFirst(String key) {
    return headers.get(key);
  }

  public static boolean isEmpty() {
    return headers.isEmpty();
  }

  @Override
  protected void append(LoggingEvent event) {
    final String msg = event.getMessage();
    if (msg.startsWith(">>")) {
      Matcher m = splitHeader.matcher(msg);
      if (m.find()) {
        headers.put(m.group(1).trim(), m.group(2).trim());
      }
    }
  }
}
