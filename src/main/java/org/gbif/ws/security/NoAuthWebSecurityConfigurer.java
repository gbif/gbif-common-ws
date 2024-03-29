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

import java.util.Arrays;
import java.util.Collections;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
public class NoAuthWebSecurityConfigurer extends WebSecurityConfigurerAdapter {

  private final UserDetailsService userDetailsService;

  private final PasswordEncoder passwordEncoder;

  public NoAuthWebSecurityConfigurer(
      UserDetailsService userDetailsService,
      ApplicationContext context,
      PasswordEncoder passwordEncoder) {
    this.userDetailsService = userDetailsService;
    setApplicationContext(context);
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) {
    auth.authenticationProvider(dbAuthenticationProvider());
  }

  private DaoAuthenticationProvider dbAuthenticationProvider() {
    final DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder);
    return authProvider;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.httpBasic()
        .disable()
        .addFilterAfter(
            getApplicationContext().getBean(HttpServletRequestWrapperFilter.class),
            CsrfFilter.class)
        .addFilterAfter(
            getApplicationContext().getBean(RequestHeaderParamUpdateFilter.class),
            HttpServletRequestWrapperFilter.class)
        .addFilterAfter(
            getApplicationContext().getBean(IdentityFilter.class),
            RequestHeaderParamUpdateFilter.class)
        .addFilterAfter(
            getApplicationContext().getBean(AppIdentityFilter.class), IdentityFilter.class)
        .csrf()
        .disable()
        .cors()
        .and()
        .authorizeRequests()
        .anyRequest()
        .authenticated();

    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
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
