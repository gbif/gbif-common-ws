package org.gbif.ws.client.guice;

import org.gbif.utils.HttpUtil;
import org.gbif.ws.client.BaseWsGetClient;
import org.gbif.ws.client.interceptor.HttpErrorResponseInterceptor;
import org.gbif.ws.client.interceptor.PublicMethodMatcher;
import org.gbif.ws.json.JacksonJsonContextResolver;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;
import org.apache.http.client.HttpClient;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;


public abstract class GbifWsClientModule extends PrivateModule {

  private final Properties properties;
  private final Set<Package> clientPackages;
  // Used for each of i) getting a connection from the pool, establishing a connection and the
  // socket timeout
  protected static final int DEFAULT_HTTP_TIMEOUT_MSECS = 10000;
  protected static final int DEFAULT_MAX_HTTP_CONNECTIONS = 100;
  protected static final int DEFAULT_MAX_HTTP_CONNECTIONS_PER_ROUTE = 100;

  protected GbifWsClientModule(Properties properties, Package... clientPackages) {
    this.properties = properties;
    this.clientPackages = Sets.newHashSet(clientPackages);
    this.clientPackages.add(BaseWsGetClient.class.getPackage());
  }

  @Override
  protected final void configure() {
    Names.bindProperties(binder(), properties);

    // configure jsonMixIns
    JacksonJsonContextResolver.addMixIns(getPolymorphicClassMap());

    configureClient();

    // bind common interceptors
    for (Package clientPackage : clientPackages) {
      // order is important!
      bindInterceptor(Matchers.inPackage(clientPackage), new PublicMethodMatcher(), new HttpErrorResponseInterceptor());
    }
  }

  /**
   * Implement this method to configure the clients guice module.
   * You can install modules, bind classes.
   * Dont forget to expose any classes that should be visible to the outside world!
   */
  protected abstract void configureClient();

  /**
   * The Jackson JSON configuration needs to know about how to (de)serialize polymorphic classes.
   * Override this method to pass a map of mixIn classes into the Jackson context resolver.
   * 
   * @return the mixIn class map. Defaults to an empty map.
   */
  protected Map<Class<?>, Class<?>> getPolymorphicClassMap() {
    return Maps.newHashMap();
  }

  protected Properties getProperties() {
    return properties;
  }

  @Provides
  @Singleton
  public HttpClient provideHttpClient() {
    return HttpUtil.newMultithreadedClient(DEFAULT_HTTP_TIMEOUT_MSECS, DEFAULT_MAX_HTTP_CONNECTIONS,
      DEFAULT_MAX_HTTP_CONNECTIONS_PER_ROUTE);
  }

  @Provides
  @Singleton
  @Inject
  public Client providesRegistryJerseyClient(HttpClient client) {
    ApacheHttpClient4Handler hch = new ApacheHttpClient4Handler(client, null, false);

    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getClasses().add(JacksonJsonContextResolver.class);
    // this line is critical! Note that this is the jersey version of this class name!
    clientConfig.getClasses().add(JacksonJsonProvider.class);
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    return new ApacheHttpClient4(hch, clientConfig);
  }

}
