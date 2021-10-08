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

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;

public class ClientDecoder implements Decoder {

  private final JacksonDecoder jacksonDecoder;

  public ClientDecoder(ObjectMapper objectMapper) {
    this.jacksonDecoder = new JacksonDecoder(objectMapper);
  }

  @Override
  public Object decode(Response response, Type type) throws IOException, FeignException {
    HttpStatus responseStatus = HttpStatus.resolve(response.status());

    if (responseStatus == HttpStatus.NOT_FOUND || responseStatus == HttpStatus.NO_CONTENT) {
      return null;
    } else if (responseStatus != null && responseStatus.isError()) {
      throw new DecodeException(response.status(), response.toString(), response.request());
    }

    MediaType contentType = getContentType(response);
    if (MediaType.APPLICATION_JSON.equalsTypeAndSubtype(contentType)) {
      return jacksonDecoder.decode(response, type);
    } else if (MediaType.TEXT_PLAIN.equalsTypeAndSubtype(contentType)) {
      return Util.toString(response.body().asReader());
    } else if (MediaType.APPLICATION_OCTET_STREAM.equalsTypeAndSubtype(contentType)) {
      return Util.toByteArray(response.body().asInputStream());
    } else if (byte[].class.equals(type)) {
      return Util.toByteArray(response.body().asInputStream());
    } else {
      throw new DecodeException(response.status(), "Unsupported response type", response.request());
    }
  }

  /**
   * Gets the first MediaType listed in the Content-Type header.
   */
  private static MediaType getContentType(Response response) {
    return response.headers().get(HttpHeaders.CONTENT_TYPE).stream()
        .findFirst()
        .filter(StringUtils::hasLength)
        .map(MediaType::parseMediaType)
        .orElse(null);
  }
}
