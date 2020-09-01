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
package org.gbif.ws.client;

import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.ObjectMapper;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.jackson.JacksonEncoder;

public class ClientEncoder implements Encoder {

  private JacksonEncoder jacksonEncoder;

  public ClientEncoder(ObjectMapper objectMapper) {
    this.jacksonEncoder = new JacksonEncoder(objectMapper);
  }

  @Override
  public void encode(Object object, Type bodyType, RequestTemplate template)
      throws EncodeException {
    if (bodyType == String.class) {
      template.body(object.toString());
    } else if (bodyType == byte[].class) {
      template.body((byte[]) object, null);
    } else {
      jacksonEncoder.encode(object, bodyType, template);
    }
  }
}
