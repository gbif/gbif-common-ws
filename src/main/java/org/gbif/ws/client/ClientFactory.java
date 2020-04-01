package org.gbif.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.Md5EncodeServiceImpl;
import org.gbif.ws.security.SecretKeySigningService;
import org.gbif.ws.security.SigningService;

public class ClientFactory {

  private String url;
  private GbifAuthRequestInterceptor requestInterceptor;
  private Decoder decoder;
  private Encoder encoder;
  private ErrorDecoder errorDecoder;
  private Contract contract;

  public ClientFactory(String username, String url, String appKey, String secretKey) {
    this.url = url;
    ObjectMapper objectMapper = JacksonJsonObjectMapperProvider.getObjectMapper();
    this.requestInterceptor =
        new GbifAuthRequestInterceptor(username, appKey, secretKey, new SecretKeySigningService(),
            new Md5EncodeServiceImpl(objectMapper));
    this.encoder = new JacksonEncoder(objectMapper);
    this.decoder = new JacksonDecoder(objectMapper);
    this.errorDecoder = new ClientErrorDecoder();
    this.contract = new ClientContract();
  }

  public ClientFactory(String username, String url, String appKey, String secretKey,
      SigningService signingService,
      Md5EncodeService md5EncodeService, ObjectMapper objectMapper) {
    this.url = url;
    this.requestInterceptor =
        new GbifAuthRequestInterceptor(username, appKey, secretKey, signingService,
            md5EncodeService);
    this.encoder = new JacksonEncoder(objectMapper);
    this.decoder = new JacksonDecoder(objectMapper);
    this.errorDecoder = new ClientErrorDecoder();
    this.contract = new ClientContract();
  }

  public <T> T newInstance(Class<T> clazz) {
    return Feign.builder()
        .encoder(encoder)
        .decoder(decoder)
        .errorDecoder(errorDecoder)
        .contract(contract)
        .requestInterceptor(requestInterceptor)
        .target(clazz, url);
  }
}
