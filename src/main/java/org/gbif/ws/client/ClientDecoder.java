package org.gbif.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;

public class ClientDecoder implements Decoder {

  private JacksonDecoder jacksonDecoder;

  public ClientDecoder(ObjectMapper objectMapper) {
    this.jacksonDecoder = new JacksonDecoder(objectMapper);
  }

  @Override
  public Object decode(Response response, Type type)
      throws IOException, DecodeException, FeignException {
    HttpStatus responseStatus = HttpStatus.resolve(response.status());

    if (HttpStatus.NOT_FOUND  == responseStatus) {
      return null;
    } else if (responseStatus.isError()) {
      throw new DecodeException(response.status(), response.toString(), response.request());
    }

    MediaType contentType = getContentType(response);
    if (MediaType.APPLICATION_JSON.equalsTypeAndSubtype(contentType)) {
      return jacksonDecoder.decode(response, type);
    } else if (MediaType.TEXT_PLAIN.equalsTypeAndSubtype(contentType)){
      return  Util.toString(response.body().asReader());
    } else if (MediaType.APPLICATION_OCTET_STREAM.equalsTypeAndSubtype(contentType)) {
      return StreamUtils.copyToByteArray(response.body().asInputStream());
    } else if (byte[].class.equals(type)) {
      return Util.toByteArray(response.body().asInputStream());
    } else {
      throw  new DecodeException(response.status(), "Unsupported response type", response.request());
    }
  }

  /**
   * Gets the first MediaType listed in the Content-Type header.
   */
  private static MediaType getContentType(Response response) {
    return response.headers().get(HttpHeaders.CONTENT_TYPE).stream()
                              .findFirst()
                              .filter(StringUtils::hasLength)
                              .map(MediaType::parseMediaType).orElse(null);
  }
}
