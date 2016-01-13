package org.gbif.ws.server.guice;

import org.gbif.ws.server.filter.CreatedResponseFilter;
import org.gbif.ws.server.filter.CrossDomainResponseFilter;
import org.gbif.ws.server.filter.JsonpResponseFilter;

import java.util.List;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * WsJerseyModule configuration class with fluent interface.
 * Default response filters represent filters that will be added in front of other response filters, including
 * authentication filter.
 */
public class WsJerseyModuleConfiguration {

  private String resourcePackages;
  private Boolean installAuthenticationFilter;

  private List<Class<? extends ContainerResponseFilter>> defaultResponseFilters = Lists.newArrayList();
  private List<Class<? extends ContainerResponseFilter>> responseFilters = Lists.newArrayList();
  private List<Class<? extends ContainerRequestFilter>> requestFilters = Lists.newLinkedList();

  /**
   * Build WsJerseyModuleConfiguration with default configuration values.
   */
  public WsJerseyModuleConfiguration() {
    defaultResponseFilters.add(JsonpResponseFilter.class);
    defaultResponseFilters.add(CreatedResponseFilter.class);
    defaultResponseFilters.add(CrossDomainResponseFilter.class);
  }

  /**
   * To scan for the Jersey resources.
   */
  public WsJerseyModuleConfiguration resourcePackages(String resourcePackages) {
    this.resourcePackages = resourcePackages;
    return this;
  }

  /**
   * Controls whether the GBIF default AuthFilter should be installed.
   */
  public WsJerseyModuleConfiguration installAuthenticationFilter(boolean installAuthenticationFilter) {
    this.installAuthenticationFilter = installAuthenticationFilter;
    return this;
  }

  public WsJerseyModuleConfiguration responseFilters(@Nullable List<Class<? extends ContainerResponseFilter>> responseFilters) {
    if (responseFilters != null) {
      this.responseFilters.addAll(responseFilters);
    }
    return this;
  }

  public WsJerseyModuleConfiguration requestFilters(@Nullable List<Class<? extends ContainerRequestFilter>> requestFilters) {
    if (requestFilters != null) {
      this.requestFilters.addAll(requestFilters);
    }
    return this;
  }

  /**
   * Replaces the defaultResponseFilters to be used by the WsJerseyModule.
   * defaultResponseFilters represent filters that will be added in front of other response filters, including
   * authentication filter.
   */
  public WsJerseyModuleConfiguration replaceDefaultResponseFilters(List<Class<? extends ContainerResponseFilter>> defaultResponseFilters) {
    this.defaultResponseFilters.clear();
    if (defaultResponseFilters != null) {
      this.defaultResponseFilters.addAll(defaultResponseFilters);
    }
    return this;
  }

  public String getResourcePackages() {
    return resourcePackages;
  }

  public Boolean isInstallAuthenticationFilter() {
    return installAuthenticationFilter;
  }

  public List<Class<? extends ContainerResponseFilter>> getDefaultResponseFilters() {
    return defaultResponseFilters;
  }

  public List<Class<? extends ContainerResponseFilter>> getResponseFilters() {
    return responseFilters;
  }

  public List<Class<? extends ContainerRequestFilter>> getRequestFilters() {
    return requestFilters;
  }

}
