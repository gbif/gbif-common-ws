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
package org.gbif.ws.security;

import org.gbif.ws.server.filter.HttpServletRequestWrapperFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecurityUtils {

  /**
   * Cors configuration, allows all methods and origins.
   */
  public static CorsConfigurationSource corsAllOriginsAndMethodsConfiguration() {
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

  /**
   * Configures the basic settings of the HttpSecurity.
   */
  public static HttpSecurity gbifFilterChain(
      HttpSecurity http,
      HttpServletRequestWrapperFilter httpServletRequestWrapperFilter,
      RequestHeaderParamUpdateFilter requestHeaderParamUpdateFilter)
      throws Exception {
    return http.httpBasic(AbstractHttpConfigurer::disable)
        .addFilterAfter(httpServletRequestWrapperFilter, CsrfFilter.class)
        .addFilterAfter(requestHeaderParamUpdateFilter, HttpServletRequestWrapperFilter.class)
        .cors(c -> c.configurationSource(corsAllOriginsAndMethodsConfiguration()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(smc -> smc.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
  }

  /**
   * Creates a filters with authentication disabled.
   */
  public static SecurityFilterChain noAuthFilter(HttpSecurity http) throws Exception {
    return http.httpBasic(AbstractHttpConfigurer::disable)
        .cors(c -> c.configurationSource(corsAllOriginsAndMethodsConfiguration()))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeRequests(auth -> auth.anyRequest().permitAll())
        .sessionManagement(smc -> smc.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .build();
  }
}
