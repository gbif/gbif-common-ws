package org.gbif.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.Md5EncodeServiceImpl;
import org.gbif.ws.security.SecretKeySigningService;
import org.gbif.ws.security.SigningService;

@SuppressWarnings("unused")
public class ClientFactory {

  private String url;
  private RequestInterceptor requestInterceptor;
  private Decoder decoder;
  private Encoder encoder;
  private ErrorDecoder errorDecoder;
  private Contract contract;
  private InvocationHandlerFactory invocationHandlerFactory;

  /**
   * Read-only clients factory.
   */
  public ClientFactory(String url) {
    this.url = url;
    ObjectMapper objectMapper = JacksonJsonObjectMapperProvider.getObjectMapper();
    this.requestInterceptor = null;
    this.encoder = new ClientEncoder(objectMapper);
    this.decoder = new ClientDecoder(objectMapper);
    this.errorDecoder = new ClientErrorDecoder();
    this.contract = new ClientContract();
    this.invocationHandlerFactory = new ClientInvocationHandlerFactory();
  }

  /**
   * Read-write clients factory using simple user basic authentication.
   */
  public ClientFactory(String username, String password, String url) {
    this.url = url;
    ObjectMapper objectMapper = JacksonJsonObjectMapperProvider.getObjectMapper();
    this.requestInterceptor = new SimpleUserAuthRequestInterceptor(username, password);
    this.encoder = new ClientEncoder(objectMapper);
    this.decoder = new ClientDecoder(objectMapper);
    this.errorDecoder = new ClientErrorDecoder();
    this.contract = new ClientContract();
    this.invocationHandlerFactory = new ClientInvocationHandlerFactory();
  }

  /**
   * Read-write client factory using GBIF authentication by application key.
   */
  public ClientFactory(String username, String url, String appKey, String secretKey) {
    this.url = url;
    ObjectMapper objectMapper = JacksonJsonObjectMapperProvider.getObjectMapper();
    this.requestInterceptor =
        new GbifAuthRequestInterceptor(username, appKey, secretKey, new SecretKeySigningService(),
            new Md5EncodeServiceImpl(objectMapper));
    this.encoder = new ClientEncoder(objectMapper);
    this.decoder = new ClientDecoder(objectMapper);
    this.errorDecoder = new ClientErrorDecoder();
    this.contract = new ClientContract();
    this.invocationHandlerFactory = new ClientInvocationHandlerFactory();
  }

  /**
   * Read-write client factory using GBIF authentication by application key. Uses custom services.
   */
  public ClientFactory(String username, String url, String appKey, String secretKey,
      SigningService signingService,
      Md5EncodeService md5EncodeService, ObjectMapper objectMapper) {
    this.url = url;
    this.requestInterceptor =
        new GbifAuthRequestInterceptor(username, appKey, secretKey, signingService, md5EncodeService);
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
    }

    return builder.target(clazz, url);
  }
}
