package org.gbif.ws.server.guice;

import org.gbif.ws.json.JacksonJsonContextResolver;
import org.gbif.ws.util.PropertiesUtil;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * A basic servlet listener for all our webservices.
 * Note that the request filters will be ordered as follows if provided:
 * <ol>
 * <li>The default filters will always be first</li>
 * <li>The authentication filter will be next if enabled (installAuth=true)</li>
 * <li>The provided filters will be last in the given order</li>
 * </ol>
 */
public abstract class GbifServletListener extends GuiceServletContextListener {

  private final Properties properties;
  private final String resourcePackages;
  private final boolean installAuth;
  private final List<Class<? extends ContainerResponseFilter>> responseFilters = Lists.newArrayList();
  private final List<Class<? extends ContainerRequestFilter>> requestFilters = Lists.newArrayList();

  private Injector injector;

  /**
   * @param properties The properties to load
   * @param resourcePackages packages to scan for jersey resources,
   *        for example "org.gbif.registry.ws,org.gbif.registry.common.ws"
   * @param installAuthenticationFilters if true installs support for authentication. Requires an implementation of
   *        UserService to be installed by one of the additional modules.
   */
  protected GbifServletListener(Properties properties, String resourcePackages, boolean installAuthenticationFilters) {
    this(properties, resourcePackages, installAuthenticationFilters, null);
  }

  /**
   * @param properties The properties to load
   * @param resourcePackages packages to scan for jersey resources,
   *        for example "org.gbif.registry.ws,org.gbif.registry.common.ws"
   * @param installAuthenticationFilters if true installs support for authentication. Requires an implementation of
   *        UserService to be installed by one of the additional modules.
   * @param responseFilters A list of response filters that are applied to the requests.
   */
  protected GbifServletListener(Properties properties, String resourcePackages, boolean installAuthenticationFilters,
    @Nullable List<Class<? extends ContainerResponseFilter>> responseFilters) {
    this(properties, resourcePackages, installAuthenticationFilters, responseFilters, null);
  }

  /**
   * @param properties The properties to load
   * @param resourcePackages packages to scan for jersey resources,
   *        for example "org.gbif.registry.ws,org.gbif.registry.common.ws"
   * @param installAuthenticationFilters if true installs support for authentication. Requires an implementation of
   *        UserService to be installed by one of the additional modules.
   * @param responseFilters A list of response filters that are applied to the requests.
   * @param requestFilters A list of request filters that are applied to the requests. Note: read constructor
   *        documentation on {@link WsJerseyModule}
   */
  protected GbifServletListener(Properties properties, String resourcePackages, boolean installAuthenticationFilters,
    @Nullable List<Class<? extends ContainerResponseFilter>> responseFilters,
    @Nullable List<Class<? extends ContainerRequestFilter>> requestFilters) {
    this.properties = properties;
    this.resourcePackages = resourcePackages;
    this.installAuth = installAuthenticationFilters;
    if (responseFilters != null) {
      this.responseFilters.addAll(responseFilters);
    }
    if (requestFilters != null) {
      this.requestFilters.addAll(requestFilters);
    }
  }

  /**
   * @param propertyFileName Fully qualified filename that must be on the classpath
   * @param resourcePackages packages to scan for jersey resources,
   *        for example "org.gbif.registry.ws,org.gbif.registry.common.ws"
   * @param installAuthenticationFilters if true installs support for authentication. Requires an implementation of
   *        UserService to be installed by one of the additional modules.
   * @param responseFilters A list of response filters that are applied to the requests.
   */
  protected GbifServletListener(String propertyFileName, String resourcePackages, boolean installAuthenticationFilters,
    @Nullable List<Class<? extends ContainerResponseFilter>> responseFilters) {
    this(PropertiesUtil.readFromClasspath(propertyFileName), resourcePackages, installAuthenticationFilters,
      responseFilters);
  }

  /**
   * @param propertyFileName Fully qualified filename that must be on the classpath
   * @param resourcePackages packages to scan for jersey resources,
   *        for example "org.gbif.registry.ws,org.gbif.registry.common.ws"
   * @param installAuthenticationFilters if true installs support for authentication. Requires an implementation of
   *        UserService to be installed by one of the additional modules.
   */
  protected GbifServletListener(String propertyFileName, String resourcePackages,
    boolean installAuthenticationFilters) {
    this(propertyFileName, resourcePackages, installAuthenticationFilters, null);
  }


  /**
   * Implement this to return the list of all additional modules needed to be installed.
   * 
   * @return a list of modules, never null.
   */
  protected abstract List<Module> getModules(Properties properties);

  /**
   * The Jackson JSON configuration needs to know about how to (de)serialize polymorphic classes.
   * Override this method to pass a map of mixIn classes into the Jackson context resolver.
   * 
   * @return the mixIn class map. Defaults to an empty map.
   */
  protected Map<Class<?>, Class<?>> getPolymorphicClassMap() {
    return Maps.newHashMap();
  }

  @Override
  protected synchronized Injector getInjector() {
    if (injector == null) {

      // configure jsonMixIns
      JacksonJsonContextResolver.addMixIns(getPolymorphicClassMap());

      List<Module> modules = getModules(properties);
      modules.add(new WsJerseyModule(resourcePackages, installAuth, responseFilters, requestFilters));

      injector = Guice.createInjector(modules);
    }
    return injector;
  }

}
