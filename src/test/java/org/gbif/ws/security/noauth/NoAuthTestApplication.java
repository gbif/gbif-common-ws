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
package org.gbif.ws.security.noauth;

import org.gbif.ws.security.AppkeysConfigurationProperties;
import org.gbif.ws.security.GbifAuthenticationManagerImpl;
import org.gbif.ws.security.NoAuthWebSecurityConfigurer;
import org.gbif.ws.security.RoleMethodSecurityConfiguration;
import org.gbif.ws.security.UserRoles;
import org.gbif.ws.server.filter.AppIdentityFilter;
import org.gbif.ws.server.filter.HttpServletRequestWrapperFilter;
import org.gbif.ws.server.filter.IdentityFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulates a client project that secures its endpoints with {@link NoAuthWebSecurityConfigurer}
 * and method security. The {@code IdentityAccessService} and {@code GbifAuthService} are mocked in
 * {@link NoAuthWebSecurityTest}.
 */
@TestConfiguration
@SpringBootApplication(exclude = {FeignAutoConfiguration.class})
@Import({
  RoleMethodSecurityConfiguration.class,
  HttpServletRequestWrapperFilter.class,
  RequestHeaderParamUpdateFilter.class,
  IdentityFilter.class,
  AppIdentityFilter.class,
  GbifAuthenticationManagerImpl.class,
  AppkeysConfigurationProperties.class
})
public class NoAuthTestApplication {

  @Bean
  public UserDetailsService userDetailsService() {
    return username -> {
      throw new UnsupportedOperationException("unsupported");
    };
  }

  @Bean
  @SuppressWarnings("deprecation")
  public PasswordEncoder passwordEncoder() {
    return NoOpPasswordEncoder.getInstance();
  }

  @Configuration
  static class SecurityConfigurer extends NoAuthWebSecurityConfigurer {}

  @RestController
  static class TestController {

    @GetMapping("open")
    public void open() {}

    @PreAuthorize("isAuthenticated()")
    @GetMapping("authenticated")
    public void authenticated() {}

    @Secured(UserRoles.ADMIN_ROLE)
    @GetMapping("admin")
    public void admin() {}
  }
}
