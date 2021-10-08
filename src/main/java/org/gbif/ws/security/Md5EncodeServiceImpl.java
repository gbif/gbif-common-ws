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
package org.gbif.ws.security;

import org.gbif.ws.CommonRuntimeException;

import java.io.IOException;
import java.util.Base64;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class Md5EncodeServiceImpl implements Md5EncodeService {

  private static final Logger LOG = LoggerFactory.getLogger(Md5EncodeServiceImpl.class);

  private ObjectMapper mapper;

  // See JacksonJsonObjectMapperProvider#getObjectMapper
  public Md5EncodeServiceImpl(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Generates the Base64 encoded 128 bit MD5 digest of the entire content string suitable for the
   * Content-MD5 header value.
   * See http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.15
   */
  @Override
  public String encode(Object entity) {
    try {
      byte[] content = mapper.writeValueAsBytes(entity);

      // TODO: 2019-07-31 char encoding should be ASCII
      return Base64.getEncoder().encodeToString(DigestUtils.md5(content));
    } catch (IOException e) {
      LOG.error("Failed to serialize http entity [{}]", entity);
      throw new CommonRuntimeException(e);
    }
  }
}
