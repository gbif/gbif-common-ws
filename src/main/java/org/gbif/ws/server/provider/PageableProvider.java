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
package org.gbif.ws.server.provider;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.WebRequest;

import com.google.common.annotations.VisibleForTesting;

import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_LIMIT;
import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_OFFSET;
import static org.gbif.api.model.common.paging.PagingConstants.PARAM_LIMIT;
import static org.gbif.api.model.common.paging.PagingConstants.PARAM_OFFSET;
import static org.gbif.ws.util.CommonWsUtils.getFirst;

/**
 * Provider class that extracts the page size and offset from the query parameters, or provides the default
 * implementation if necessary.
 * <p/>
 * Example resource use:
 * <pre>
 * {@code
 * public List<Checklist> list(Pageable pageable) {
 *   // do stuff
 * }
 * }
 * </pre>
 * <p/>
 */
public class PageableProvider implements ContextProvider<Pageable> {

  private static final Logger LOG = LoggerFactory.getLogger(PageableProvider.class);

  private final Integer maxPageSize;

  @VisibleForTesting static final int LIMIT_CAP = 1000;

  public PageableProvider() {
    this.maxPageSize = LIMIT_CAP;
  }

  public PageableProvider(Integer maxPageSize) {
    this.maxPageSize = maxPageSize;
  }

  @Override
  public Pageable getValue(WebRequest webRequest) {
    return getPagingRequest(webRequest, maxPageSize);
  }

  public static PagingRequest getPagingRequest(WebRequest webRequest, int maxPageSize) {
    Map<String, String[]> params = webRequest.getParameterMap();

    int limit = DEFAULT_PARAM_LIMIT;
    String limitParam = getFirst(params, PARAM_LIMIT);
    if (limitParam != null) {
      try {
        limit = Integer.parseInt(limitParam);
        if (limit < 0) {
          LOG.info(
              "Limit parameter was no positive integer [{}]. Using default {}",
              limitParam,
              DEFAULT_PARAM_LIMIT);
          limit = DEFAULT_PARAM_LIMIT;
        } else if (limit > maxPageSize) {
          LOG.debug("Limit parameter too high. Use maximum {}", maxPageSize);
          limit = maxPageSize;
        }
      } catch (NumberFormatException e) {
        LOG.warn(
            "Unparsable value supplied for limit [{}]. Using default {}",
            limitParam,
            DEFAULT_PARAM_LIMIT);
      }
    }

    long offset = DEFAULT_PARAM_OFFSET;
    String offsetParam = getFirst(params, PARAM_OFFSET);
    if (offsetParam != null) {
      try {
        offset = Long.parseLong(offsetParam);
        if (offset < 0) {
          LOG.warn(
              "Offset parameter is a negative integer [{}]. Using default {}",
              offsetParam,
              DEFAULT_PARAM_OFFSET);
          offset = DEFAULT_PARAM_OFFSET;
        }
      } catch (NumberFormatException e) {
        LOG.warn(
            "Unparsable value supplied for offset [{}]. Using default {}",
            offsetParam,
            DEFAULT_PARAM_OFFSET);
      }
    }
    return new PagingRequest(offset, limit);
  }
}
