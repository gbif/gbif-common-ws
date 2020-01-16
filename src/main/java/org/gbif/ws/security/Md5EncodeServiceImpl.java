package org.gbif.ws.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.gbif.ws.CommonRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;

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