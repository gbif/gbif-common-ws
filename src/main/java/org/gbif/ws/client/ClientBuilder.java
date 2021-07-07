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

import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.Md5EncodeServiceImpl;
import org.gbif.ws.security.SecretKeySigningService;
import org.gbif.ws.security.SigningService;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import feign.form.FormEncoder;
import feign.form.spring.SpringFormEncoder;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Contract;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.httpclient.ApacheHttpClient;
import lombok.Builder;
import lombok.Data;

/**
 * ClientBuilder used to create Feign Clients.
 * This builders support retry using exponential backoff and multithreaded http client.
 */
@SuppressWarnings("unused")
public class ClientBuilder {

  private static final String HTTP_PROTOCOL = "http";
  private static final String HTTPS_PROTOCOL = "https";

  private String url;
  private int connectTimeoutMillis = 10_000;
  private int readTimeoutMillis = 60_000;
  private RequestInterceptor requestInterceptor;
  private Decoder decoder;
  private Encoder encoder;
  private ConnectionPoolConfig connectionPoolConfig;
  private ObjectMapper objectMapper;
  private Retryer retryer;
  private boolean formEncoder;

  private final ErrorDecoder errorDecoder = new ClientErrorDecoder();
  private final Contract contract = new ClientContract();
  private final InvocationHandlerFactory invocationHandlerFactory =
      new ClientInvocationHandlerFactory();

  /**
   * Creates a builder instance, by default uses the GBIF Jackson ObjectMapper.
   */
  public ClientBuilder() {
    withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapper());
  }

  /**
   * Exponential backoff retryer.
   */
  public ClientBuilder withExponentialBackoffRetry(
      Duration initialInterval, double multiplier, int maxAttempts) {
    retryer = new ClientRetryer(initialInterval.toMillis(), maxAttempts, multiplier);
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
        new GbifAuthRequestInterceptor(
            username,
            appKey,
            secretKey,
            new SecretKeySigningService(),
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
   * Client connection timeout in milliseconds.
   */
  public ClientBuilder withConnectTimeout(int connectTimeoutMillis) {
    this.connectTimeoutMillis = connectTimeoutMillis;
    return this;
  }

  /**
   * Client read timeout in milliseconds.
   */
  public ClientBuilder withReadTimeout(int readTimeoutMillis) {
    this.readTimeoutMillis = readTimeoutMillis;
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
  public ClientBuilder withCustomGbifAuth(
      String username,
      String appKey,
      String secretKey,
      SigningService signingService,
      Md5EncodeService md5EncodeService) {
    this.requestInterceptor =
        new GbifAuthRequestInterceptor(
            username, appKey, secretKey, signingService, md5EncodeService);
    return this;
  }

  public ClientBuilder withFormEncoder() {
    this.formEncoder = true;
    return this;
  }

  /**
   * Creates a new client instance.
   */
  public <T> T build(Class<T> clazz) {

    Feign.Builder builder =
        Feign.builder()
            .encoder(formEncoder? new SpringFormEncoder(encoder) : encoder)
            .decoder(decoder)
            .errorDecoder(errorDecoder)
            .contract(contract)
            .options(new Request.Options(connectTimeoutMillis, readTimeoutMillis))
            .decode404()
            .invocationHandlerFactory(invocationHandlerFactory);

    if (retryer != null) {
      builder.retryer(retryer);
    }

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
        .setDefaultSocketConfig(
            SocketConfig.custom().setSoTimeout(connectionPoolConfig.getTimeout()).build())
        .setDefaultConnectionConfig(
            ConnectionConfig.custom()
                .setCharset(Charset.forName(StandardCharsets.UTF_8.name()))
                .build())
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setConnectTimeout(connectionPoolConfig.getTimeout())
                .setConnectionRequestTimeout(connectionPoolConfig.getTimeout())
                .build())
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
