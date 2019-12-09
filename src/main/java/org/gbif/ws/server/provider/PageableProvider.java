/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Type;

import static org.gbif.api.model.common.paging.PagingConstants.*;

/**
 * Jersey provider class that extracts the page size and offset from the query parameters, or provides the default
 * implementation if necessary.
 * <p/>
 * Example resource use:
 * <pre>
 * {@code
 * public List<Checklist> list(@QueryParam("page") Pageable pageable) {
 *   // do stuff
 * }
 * }
 * </pre>
 * <p/>
 * Note, this implementation is based on the documentation provided on:
 * http://stackoverflow.com/questions/5722506/how-do-you-map-multiple-query-parameters-to-the-fields-of-a-bean-on-jersey-get-re
 */
@Provider
@Singleton
public class PageableProvider extends AbstractHttpContextInjectable<Pageable>
  implements InjectableProvider<Context, Type> {

  private static final Logger LOG = LoggerFactory.getLogger(PageableProvider.class);
  @VisibleForTesting
  static final int LIMIT_CAP = 1000;

  @Override
  public Injectable<Pageable> getInjectable(ComponentContext ic, Context a, Type c) {
    if (c.equals(Pageable.class)) {
      return this;
    }
    return null;
  }

  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  public Pageable getValue(HttpContext c) {
    return getPagingRequest(c);
  }

  public static PagingRequest getPagingRequest(HttpContext c) {
    MultivaluedMap<String, String> params = c.getRequest().getQueryParameters();

    int limit = DEFAULT_PARAM_LIMIT;
    if (params.getFirst(PARAM_LIMIT) != null) {
      try {
        limit = Integer.parseInt(params.getFirst(PARAM_LIMIT));
        if (limit < 0) {
          LOG.info("Limit parameter was no positive integer [{}]. Using default {}",
                   params.getFirst(PARAM_LIMIT), DEFAULT_PARAM_LIMIT);
          limit = DEFAULT_PARAM_LIMIT;
        } else if (limit > LIMIT_CAP) {
          LOG.debug("Limit parameter too high. Use maximum {}", LIMIT_CAP);
          limit = LIMIT_CAP;
        }
      } catch (NumberFormatException e) {
        LOG.warn("Unparsable value supplied for limit [{}]. Using default {}", params.getFirst(PARAM_LIMIT),
          DEFAULT_PARAM_LIMIT);
      }
    }

    long offset = DEFAULT_PARAM_OFFSET;
    if (params.getFirst(PARAM_OFFSET) != null) {
      try {
        offset = Long.parseLong(params.getFirst(PARAM_OFFSET));
        if (offset < 0) {
          LOG.warn("Offset parameter is a negative integer [{}]. Using default {}", params.getFirst(PARAM_OFFSET),
            DEFAULT_PARAM_OFFSET);
          offset = DEFAULT_PARAM_OFFSET;
        }
      } catch (NumberFormatException e) {
        LOG.warn("Unparsable value supplied for offset [{}]. Using default {}", params.getFirst(PARAM_OFFSET),
          DEFAULT_PARAM_OFFSET);
      }
    }
    return new PagingRequest(offset, limit);
  }
}
