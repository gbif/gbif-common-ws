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

import org.gbif.ws.server.filter.AppIdentityFilter;
import org.gbif.ws.server.filter.HttpServletRequestWrapperFilter;
import org.gbif.ws.server.filter.IdentityFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security Adapter that disables the authentication redirect and use GBIF identity filters for secure endpoints.
 * UserDetailsService and PasswordEncoder must be supplied by the SpringContext.
 * This class is not annotated to avoid automatic instantiation. To use it create a subclass of it:
 * <pre>
 *   @Configuration
 *   @EnableWebSecurity
 *   public static class ValidatorWebSecurity extends NoAuthWebSecurityConfigurer {
 *
 *     public ValidatorWebSecurity(
 *       UserDetailsService userDetailsService, ApplicationContext context, PasswordEncoder passwordEncoder
 *     ) {
 *       super(userDetailsService, context, passwordEncoder);
 *     }
 *  }
 * <pre/>
 */
public class NoAuthWebSecurityConfigurer {

  @Bean
  public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setPasswordEncoder(passwordEncoder);
    provider.setUserDetailsService(userDetailsService);
    return new ProviderManager(provider);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                 HttpServletRequestWrapperFilter httpServletRequestWrapperFilter,
                                                 RequestHeaderParamUpdateFilter requestHeaderParamUpdateFilter,
                                                 IdentityFilter identityFilter,
                                                 AppIdentityFilter appIdentityFilter) throws Exception {
    return SecurityUtils.gbifFilterChain(http, httpServletRequestWrapperFilter, requestHeaderParamUpdateFilter)
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .addFilterAfter(identityFilter,RequestHeaderParamUpdateFilter.class)
            .addFilterAfter(appIdentityFilter, IdentityFilter.class)
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
