package org.gbif.ws.client;

import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.Md5EncodeServiceImpl;
import org.gbif.ws.security.SecretKeySigningService;
import org.gbif.ws.security.SigningService;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.httpclient.ApacheHttpClient;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.Builder;
import lombok.Data;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ClientBuilder used to create Feign Clients.
 * This builders support retry using exponential backoff and multithreaded http client.
 */
public class ClientBuilder {

  private static final String HTTP_PROTOCOL = "http";
  private static final String HTTPS_PROTOCOL = "https";

  private static final RetryRegistry RETRY_REGISTRY = RetryRegistry.ofDefaults();
  private static final Logger LOG = LoggerFactory.getLogger(ClientBuilder.class);

  private String url;
  private RequestInterceptor requestInterceptor;
  private Decoder decoder;
  private Encoder encoder;
  private ConnectionPoolConfig connectionPoolConfig;
  private RetryConfig retryConfig;
  private ObjectMapper objectMapper;

  private final ErrorDecoder errorDecoder = new ClientErrorDecoder();
  private final Contract contract = new ClientContract();
  private final InvocationHandlerFactory invocationHandlerFactory = new ClientInvocationHandlerFactory();

  /**
   * Creates a builder instance, by default uses the GBIF Jackson ObjectMapper.
   */
  public ClientBuilder() {
    withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapper());
  }

  /**
   * Exponential backoff retry configuration.
   */
  public ClientBuilder withExponentialBackoffRetry(Duration initialInterval, double multiplier, int maxAttempts) {
    retryConfig = RetryConfig.custom()
                              .maxAttempts(maxAttempts)
                              .intervalFunction(IntervalFunction.ofExponentialBackoff(initialInterval, multiplier))
                              .build();
    return this;
  }

  /**
   * Target base url.
   */
  public ClientBuilder withUrl(String url) {
    this.url = url;
    return this;
  }

  /**
   * Simple base credentials.
   */
  public ClientBuilder withCredentials(String username, String password) {
    this.requestInterceptor = new SimpleUserAuthRequestInterceptor(username, password);
    return this;
  }

  /**
   * Custom AppKey credentials.
   */
  public ClientBuilder withAppKeyCredentials(String username, String appKey, String secretKey) {
    this.requestInterceptor =
      new GbifAuthRequestInterceptor(username, appKey, secretKey, new SecretKeySigningService(),
                                     new Md5EncodeServiceImpl(objectMapper));
    return this;
  }

  /**
   * Connection pool configuration to create a multithreaded client.
   */
  public ClientBuilder withConnectionPoolConfig(ConnectionPoolConfig connectionPoolConfig) {
    this.connectionPoolConfig = connectionPoolConfig;
    return this;
  }

  /**
   * Jakcson ObjectMapper used to serialize JSON data.
   */
  public ClientBuilder withObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.encoder = new ClientEncoder(objectMapper);
    this.decoder = new ClientDecoder(objectMapper);
    return this;
  }

  /**
   * Custom GBIF authentication.
   */
  public ClientBuilder withCustomGbifAuth(String username,
                                          String appKey,
                                          String secretKey,
                                          SigningService signingService,
                                          Md5EncodeService md5EncodeService) {
    this.requestInterceptor =
      new GbifAuthRequestInterceptor(username, appKey, secretKey, signingService, md5EncodeService);
    return this;
  }

  /**
   * Creates a new client instance.
   */
  public <T> T build(Class<T> clazz) {

    Feign.Builder builder = Feign.builder();

    if (Objects.nonNull(retryConfig)) {
      Retry retry = RETRY_REGISTRY.retry(clazz.getName(), retryConfig);
      //logging
      retry.getEventPublisher().onError(event -> LOG.error(event.toString()));

      FeignDecorators decorators = FeignDecorators
                                    .builder()
                                    .withRetry(retry)
                                    .build();

      builder = Resilience4jFeign.builder(decorators);
    }

    builder
      .encoder(encoder)
      .decoder(decoder)
      .errorDecoder(errorDecoder)
      .contract(contract)
      .decode404()
      .invocationHandlerFactory(invocationHandlerFactory);

    if (requestInterceptor != null) {
      builder.requestInterceptor(requestInterceptor);
    }
    if (connectionPoolConfig != null) {
      builder.client(new ApacheHttpClient(newMultithreadedClient(connectionPoolConfig)));
    }
    return builder.target(clazz, url);
  }

  /**
   * Creates a Http multithreaded client.
   */
  private static HttpClient newMultithreadedClient(ConnectionPoolConfig connectionPoolConfig) {

    return HttpClients.custom()
            .setMaxConnTotal(connectionPoolConfig.getMaxConnections())
            .setMaxConnPerRoute(connectionPoolConfig.getMaxPerRoute())
            .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(connectionPoolConfig.getTimeout()).build())
            .setDefaultConnectionConfig(ConnectionConfig.custom().setCharset(Charset.forName(StandardCharsets.UTF_8.name())).build())
            .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(connectionPoolConfig.getTimeout())
                                       .setConnectionRequestTimeout(connectionPoolConfig.getTimeout()).build())
            .build();
  }


  @Data
  @Builder
  public static class ConnectionPoolConfig {
    private final Integer timeout;
    private final Integer maxConnections;
    private final Integer maxPerRoute;
  }

}
