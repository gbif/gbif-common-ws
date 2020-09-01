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

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Contract;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.httpclient.ApacheHttpClient;
import lombok.Builder;
import lombok.Data;

@SuppressWarnings("unused")
/**
 * @deprecated see {@link ClientBuilder}.
 */
@Deprecated
public class ClientFactory {

  private static final String HTTP_PROTOCOL = "http";
  private static final String HTTPS_PROTOCOL = "https";

  private String url;
  private RequestInterceptor requestInterceptor;
  private Decoder decoder;
  private Encoder encoder;
  private ErrorDecoder errorDecoder;
  private Contract contract;
  private InvocationHandlerFactory invocationHandlerFactory;
  private ConnectionPoolConfig connectionPoolConfig;

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
        new GbifAuthRequestInterceptor(
            username,
            appKey,
            secretKey,
            new SecretKeySigningService(),
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
  public ClientFactory(
      String username,
      String url,
      String appKey,
      String secretKey,
      SigningService signingService,
      Md5EncodeService md5EncodeService,
      ObjectMapper objectMapper) {
    this.url = url;
    this.requestInterceptor =
        new GbifAuthRequestInterceptor(
            username, appKey, secretKey, signingService, md5EncodeService);
    this.encoder = new ClientEncoder(objectMapper);
    this.decoder = new ClientDecoder(objectMapper);
    this.errorDecoder = new ClientErrorDecoder();
    this.contract = new ClientContract();
    this.invocationHandlerFactory = new ClientInvocationHandlerFactory();
  }

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

  public <T> T newInstance(Class<T> clazz) {
    return newInstance(clazz, null);
  }

  public <T> T newInstance(Class<T> clazz, ConnectionPoolConfig connectionPoolConfig) {
    Feign.Builder builder =
        Feign.builder()
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

  @Data
  @Builder
  public static class ConnectionPoolConfig {
    private final Integer timeout;
    private final Integer maxConnections;
    private final Integer maxPerRoute;
  }
}
