package org.gbif.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.Md5EncodeServiceImpl;
import org.gbif.ws.security.SecretKeySigningService;
import org.gbif.ws.security.SigningService;

import java.util.Arrays;

@SuppressWarnings("unused")
public class ClientFactory {

  private String url;
  private GbifAuthRequestInterceptor requestInterceptor;
  private ValidationRequestInterceptor validationRequestInterceptor;
  private Decoder decoder;
  private Encoder encoder;
  private ErrorDecoder errorDecoder;
  private Contract contract;
  private InvocationHandlerFactory invocationHandlerFactory;

  public ClientFactory(String url) {
    this.url = url;
    ObjectMapper objectMapper = JacksonJsonObjectMapperProvider.getObjectMapper();
    this.requestInterceptor = null;
    this.validationRequestInterceptor = new ValidationRequestInterceptor();
    this.encoder = new ClientEncoder(objectMapper);
    this.decoder = new ClientDecoder(objectMapper);
    this.errorDecoder = new ClientErrorDecoder();
    this.contract = new ClientContract();
    this.invocationHandlerFactory = new ClientInvocationHandlerFactory();
  }

  public ClientFactory(String username, String url, String appKey, String secretKey) {
    this.url = url;
    ObjectMapper objectMapper = JacksonJsonObjectMapperProvider.getObjectMapper();
    this.requestInterceptor =
        new GbifAuthRequestInterceptor(username, appKey, secretKey, new SecretKeySigningService(),
            new Md5EncodeServiceImpl(objectMapper));
    this.validationRequestInterceptor = new ValidationRequestInterceptor();
    this.encoder = new ClientEncoder(objectMapper);
    this.decoder = new ClientDecoder(objectMapper);
    this.errorDecoder = new ClientErrorDecoder();
    this.contract = new ClientContract();
    this.invocationHandlerFactory = new ClientInvocationHandlerFactory();
  }

  public ClientFactory(String username, String url, String appKey, String secretKey,
      SigningService signingService,
      Md5EncodeService md5EncodeService, ObjectMapper objectMapper) {
    this.url = url;
    this.requestInterceptor =
        new GbifAuthRequestInterceptor(username, appKey, secretKey, signingService, md5EncodeService);
    this.validationRequestInterceptor = new ValidationRequestInterceptor();
    this.encoder = new ClientEncoder(objectMapper);
    this.decoder = new ClientDecoder(objectMapper);
    this.errorDecoder = new ClientErrorDecoder();
    this.contract = new ClientContract();
    this.invocationHandlerFactory = new ClientInvocationHandlerFactory();
  }

  public <T> T newInstance(Class<T> clazz) {
    Feign.Builder builder = Feign.builder()
        .encoder(encoder)
        .decoder(decoder)
        .errorDecoder(errorDecoder)
        .contract(contract)
        .invocationHandlerFactory(invocationHandlerFactory);

    if (requestInterceptor != null) {
      builder.requestInterceptor(requestInterceptor);
      builder.requestInterceptors(Arrays.asList(validationRequestInterceptor, requestInterceptor));
    } else {
      builder.requestInterceptor(validationRequestInterceptor);
    }

    return builder.target(clazz, url);
  }
}
