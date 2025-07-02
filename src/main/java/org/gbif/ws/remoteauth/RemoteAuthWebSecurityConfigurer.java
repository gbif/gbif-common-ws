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
package org.gbif.ws.remoteauth;

import org.gbif.ws.remoteauth.app.GbifAppRemoteAuthenticationProvider;
import org.gbif.ws.remoteauth.app.GbifAppRequestFilter;
import org.gbif.ws.remoteauth.basic.BasicAuthRequestFilter;
import org.gbif.ws.remoteauth.basic.BasicRemoteAuthenticationProvider;
import org.gbif.ws.remoteauth.jwt.JwtRemoteBasicAuthenticationProvider;
import org.gbif.ws.remoteauth.jwt.JwtRequestFilter;
import org.gbif.ws.security.SecurityUtils;
import org.gbif.ws.server.filter.HttpServletRequestWrapperFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security Adapter that disables the authentication redirect and use GBIF remote services.
 * Supports Basic and JWT authentication through JwtRemoteBasicAuthenticationProvider and
 * JwtRemoteBasicAuthenticationProvider.
 */
public class RemoteAuthWebSecurityConfigurer {

  @Bean
  public AuthenticationManager authenticationManager(RemoteAuthClient remoteAuthClient)
      throws Exception {
    return new ProviderManager(
        new BasicRemoteAuthenticationProvider(remoteAuthClient),
        new JwtRemoteBasicAuthenticationProvider(remoteAuthClient),
        new GbifAppRemoteAuthenticationProvider(remoteAuthClient));
  }

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      AuthenticationManager authenticationManager,
      HttpServletRequestWrapperFilter httpServletRequestWrapperFilter,
      RequestHeaderParamUpdateFilter requestHeaderParamUpdateFilter)
      throws Exception {
    return SecurityUtils.gbifFilterChain(
            http, httpServletRequestWrapperFilter, requestHeaderParamUpdateFilter)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .addFilterAfter(
            new BasicAuthRequestFilter(authenticationManager), RequestHeaderParamUpdateFilter.class)
        .addFilterAfter(new JwtRequestFilter(authenticationManager), BasicAuthRequestFilter.class)
        .addFilterAfter(new GbifAppRequestFilter(authenticationManager), JwtRequestFilter.class)
        .build();
  }

  /**
   * Cors configuration, allows all methods and origins.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    return SecurityUtils.corsAllOriginsAndMethodsConfiguration();
  }
}
