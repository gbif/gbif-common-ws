package org.gbif.ws.remoteauth;

import java.time.Duration;

import org.gbif.ws.security.RoleMethodSecurityConfiguration;
import org.gbif.ws.security.UserRoles;
import org.gbif.ws.server.filter.HttpServletRequestWrapperFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * This Spring application simulates a client project that uses gbif-common-ws for the remote auth.
 * <p>
 * It contains a simple controller {@link TestController} that has a non-secured and a secured endpoint to make
 * requests
 * agains them with the different kind of authentication methods that we support.
 * <p>
 * The registry-ws endpoints that we use in the remote auth to authenticate users are mocked in the {@link
 * LoginServerExtension}.
 */
@TestConfiguration
@SpringBootApplication(exclude = {
    FeignAutoConfiguration.class
})
@Import({
    RoleMethodSecurityConfiguration.class,
    HttpServletRequestWrapperFilter.class,
    RequestHeaderParamUpdateFilter.class
})
@ComponentScan(
    basePackages = {
        "org.gbif.ws.remoteauth"
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                IdentityServiceClient.class
            })
    })
public class TestApplication {

  public static void main(String[] args) {
    SpringApplication.run(TestApplication.class, args);
  }

  @Configuration
  public class WebMvcConfig implements WebMvcConfigurer {
  }

  @Bean
  public RestTemplate restTemplate(
      RestTemplateBuilder builder, @Value("${login.url}") String gbifApiUrl) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(30))
        .setReadTimeout(Duration.ofSeconds(60))
        .rootUri(gbifApiUrl)
        .additionalInterceptors(
            (request, body, execution) -> {
              request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
              return execution.execute(request, body);
            })
        .build();
  }

  @Bean
  public RemoteAuthClient remoteAuthClient(RestTemplate restTemplate) {
    return new RestTemplateRemoteAuthClient(restTemplate);
  }

  @Configuration
  static class SecurityConfigurer extends RemoteAuthWebSecurityConfigurer {

    public SecurityConfigurer(ApplicationContext context,
        RemoteAuthClient remoteAuthClient) {
      super(context, remoteAuthClient);
    }
  }

  @RestController
  static class TestController {

    @GetMapping("noAuth")
    public void noAuth() {

    }

    @Secured(UserRoles.ADMIN_ROLE)
    @GetMapping("admin")
    public void admin() {

    }
  }

}
