package org.gbif.ws.server.guice;

import org.gbif.ws.json.JacksonJsonContextResolver;
import org.gbif.ws.server.filter.AuthFilter;
import org.gbif.ws.server.filter.AuthResponseFilter;
import org.gbif.ws.server.filter.CreatedResponseFilter;
import org.gbif.ws.server.filter.CrossDomainResponseFilter;
import org.gbif.ws.server.filter.JsonpResponseFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.gbif.ws.server.interceptor.NullToNotFoundInterceptor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.matcher.Matchers;
import com.sun.jersey.api.container.filter.RolesAllowedResourceFilterFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilters;

/**
 * A basic jersey {@link JerseyServletModule} for JSON, JSONP etc.
 * All providers found in org.gbif.ws.server.provider will be automatically added.
 */
public class WsJerseyModule extends JerseyServletModule {

  private final WsJerseyModuleConfiguration config;
  private static final String ROOT_RESOURCES = "org.gbif.ws.server.provider,";

  private final Function<Class<?>, String> fnClassName = new Function<Class<?>, String>() {

    @Override
    public String apply(Class<?> input) {
      return input.getName();
    }
  };

  /**
   * Creates the module, optionally installing authentication filters and customising the
   * responses.
   * 
   * @param resourcePackages To scan for the Jersey resources
   * @param installAuth Controls whether the GBIF default AuthFilter should be configured
   * @param responseFilters Optionally controlling the response
   */
  public WsJerseyModule(String resourcePackages, boolean installAuth,
    @Nullable List<Class<? extends ContainerResponseFilter>> responseFilters) {
    this(resourcePackages, installAuth, responseFilters, null);
  }

  /**
   * Creates the module, optionally installing authentication filters and customising the
   * responses and/or the requests.
   * 
   * @param resourcePackages To scan for the Jersey resources
   * @param installAuth Controls whether the authentication filter should be configured
   * @param responseFilters To customise the response if necessary
   * @param requestFilters To apply in the chain of request filtering (see note above)
   */
  public WsJerseyModule(String resourcePackages, boolean installAuth,
    @Nullable List<Class<? extends ContainerResponseFilter>> responseFilters,
    List<Class<? extends ContainerRequestFilter>> requestFilters) {

    this.config = new WsJerseyModuleConfiguration()
            .resourcePackages(resourcePackages)
            .installAuthenticationFilter(installAuth)
            .responseFilters(responseFilters)
            .requestFilters(requestFilters);
  }

  /**
   * Creates the module using the provided configuration object.
   * Note that the request filters will be handled as follows if provided:
   *
   * <ol>
   * <li>The default filters will always be first</li>
   * <li>The authentication filter will be next if enabled (installAuth=true)</li>
   * <li>The provided filters will be last in the given order</li>
   * </ol>
   *
   * @param config
   */
  public WsJerseyModule(WsJerseyModuleConfiguration config){
    Preconditions.checkNotNull(config.isInstallAuthenticationFilter(), "installAuthenticationFilter must be set");
    this.config = config;
  }

  @Override
  protected void configureServlets() {
    Map<String, String> params = new HashMap<String, String>(6);

    bind(JacksonJsonContextResolver.class);
    params.put(JSONConfiguration.FEATURE_POJO_MAPPING, "true");

    // allow resources to use suffices to ask for media types
    Map<String, String> mediaTypes = Maps.newHashMap();
    mediaTypes.put("json", MediaType.APPLICATION_JSON);
    mediaTypes.put("xml", MediaType.APPLICATION_XML);
    mediaTypes.put("txt", MediaType.TEXT_PLAIN);
    mediaTypes.put("zip", MediaType.APPLICATION_OCTET_STREAM);
    params.put(ResourceConfig.PROPERTY_MEDIA_TYPE_MAPPINGS,
      Joiner.on(", ").withKeyValueSeparator(" : ").join(mediaTypes));

    // Let Jersey look for root resources automatically
    params.put(PackagesResourceConfig.PROPERTY_PACKAGES, ROOT_RESOURCES + config.getResourcePackages());

    // secure resources via JSR 250
    params.put(ResourceFilters.class.getName(), RolesAllowedResourceFilterFactory.class.getName());

    // request filters
    LinkedList<Class<? extends ContainerRequestFilter>> reqFilters = Lists.newLinkedList();
    if (config.isInstallAuthenticationFilter()) {
      reqFilters.addFirst(AuthFilter.class);
    }
    reqFilters.addFirst(RequestHeaderParamUpdateFilter.class);
    reqFilters.addAll(config.getRequestFilters());
    params.put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS,
      Joiner.on(";").join(Lists.transform(reqFilters, fnClassName)));

    // response filters
    List<Class<? extends ContainerResponseFilter>> respFilters = Lists.newArrayList();
    // The default filters are always added to start of the chain followed by authentication
    respFilters.addAll(config.getDefaultResponseFilters());

    if (config.isInstallAuthenticationFilter()) {
      respFilters.add(AuthResponseFilter.class);
    }
    respFilters.addAll(config.getResponseFilters());
    params.put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS,
      Joiner.on(";").join(Lists.transform(respFilters, fnClassName)));

    serve("/*").with(GuiceContainer.class, params);

    bindInterceptor(Matchers.any(), Matchers.annotatedWith(NullToNotFound.class), new NullToNotFoundInterceptor());
  }

  /**
   * WsJerseyModule configuration class with fluent interface.
   * Default response filters represent filters that will be added in front of other response filters, including
   * authentication filter.
   */
  public static class WsJerseyModuleConfiguration {

    private String resourcePackages;
    private Boolean installAuthenticationFilter;

    private List<Class<? extends ContainerResponseFilter>> defaultResponseFilters = Lists.newArrayList();
    private List<Class<? extends ContainerResponseFilter>> responseFilters = Lists.newArrayList();
    private List<Class<? extends ContainerRequestFilter>> requestFilters = Lists.newLinkedList();

    /**
     * Build WsJerseyModuleConfiguration with default configuration values.
     *
     */
    public WsJerseyModuleConfiguration(){
      defaultResponseFilters.add(JsonpResponseFilter.class);
      defaultResponseFilters.add(CreatedResponseFilter.class);
      defaultResponseFilters.add(CrossDomainResponseFilter.class);
    }

    /**
     * To scan for the Jersey resources.
     *
     * @param resourcePackages
     * @return
     */
    public WsJerseyModuleConfiguration resourcePackages(String resourcePackages){
      this.resourcePackages = resourcePackages;
      return this;
    }

    /**
     * Controls whether the GBIF default AuthFilter should be installed.
     *
     * @param installAuthenticationFilter
     * @return
     */
    public WsJerseyModuleConfiguration installAuthenticationFilter(boolean installAuthenticationFilter){
      this.installAuthenticationFilter = installAuthenticationFilter;
      return this;
    }

    public WsJerseyModuleConfiguration responseFilters(@Nullable List<Class<? extends ContainerResponseFilter>> responseFilters){
      if(responseFilters != null) {
        this.responseFilters.addAll(responseFilters);
      }
      return this;
    }

    public WsJerseyModuleConfiguration requestFilters(@Nullable List<Class<? extends ContainerRequestFilter>> requestFilters){
      if(requestFilters != null) {
        this.requestFilters.addAll(requestFilters);
      }
      return this;
    }

    /**
     * Replaces the defaultResponseFilters to be used by the WsJerseyModule.
     * defaultResponseFilters represent filters that will be added in front of other response filters, including
     * authentication filter.
     *
     * @param defaultResponseFilters
     * @return
     */
    public WsJerseyModuleConfiguration replaceDefaultResponseFilters(List<Class<? extends ContainerResponseFilter>> defaultResponseFilters){
      this.defaultResponseFilters.clear();
      if(defaultResponseFilters != null) {
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

}
