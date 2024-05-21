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
import org.gbif.ws.server.filter.HttpServletRequestWrapperFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security Adapter that disables the authentication redirect and use GBIF remote services.
 * Supports Basic and JWT authentication through JwtRemoteBasicAuthenticationProvider and
 * JwtRemoteBasicAuthenticationProvider.
 */
public class RemoteAuthWebSecurityConfigurer {

  @Bean
  public AuthenticationManager authenticationManager(HttpSecurity http, RemoteAuthClient remoteAuthClient) throws Exception {
    AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
    builder.authenticationProvider(new BasicRemoteAuthenticationProvider(remoteAuthClient));
    builder.authenticationProvider(new JwtRemoteBasicAuthenticationProvider(remoteAuthClient));
    builder.authenticationProvider(new GbifAppRemoteAuthenticationProvider(remoteAuthClient));
    return builder.build();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager,
                                         HttpServletRequestWrapperFilter httpServletRequestWrapperFilter,
                                         RequestHeaderParamUpdateFilter requestHeaderParamUpdateFilter) throws Exception {
    return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
               .httpBasic(AbstractHttpConfigurer::disable)
               .addFilterAfter(httpServletRequestWrapperFilter, CsrfFilter.class)
               .addFilterAfter(requestHeaderParamUpdateFilter, HttpServletRequestWrapperFilter.class)
               .addFilterAfter(new BasicAuthRequestFilter(authenticationManager), RequestHeaderParamUpdateFilter.class)
               .addFilterAfter(new JwtRequestFilter(authenticationManager), BasicAuthRequestFilter.class)
               .addFilterAfter(new GbifAppRequestFilter(authenticationManager), JwtRequestFilter.class)
               .cors(c -> c.configurationSource(corsConfigurationSource()))
               .csrf(AbstractHttpConfigurer::disable)
               .sessionManagement(smc -> smc.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
               .build();
  }

  /**
   * Cors configuration, allows all methods and origins.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    // CorsFilter only applies this if the origin header is present in the request
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type"));
    configuration.setAllowedOrigins(Collections.singletonList("*"));
    configuration.setAllowedMethods(
        Arrays.asList("HEAD", "GET", "POST", "DELETE", "PUT", "OPTIONS"));
    configuration.setExposedHeaders(
        Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Methods",
            "Access-Control-Allow-Headers"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
