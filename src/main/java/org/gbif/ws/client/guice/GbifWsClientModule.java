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

/**
 * Base web service client module.
 * Allows to specify HTTP connection parameters: httpTimeout, maxHttpConnections and maxHttpConnectionsPerRoute.
 * The default values for those parameters are: httpTimeout = 10 seconds, maxHttpConnections = 100 and
 * maxHttpConnectionsPerRoute = 100.
 */
public abstract class GbifWsClientModule extends PrivateModule {


  /**
   * Default names/keys to create an HTTP client connection.
   * The values listed in this class are the expected key in a properties file.
   */
  public static class HttpClientConnParams {

    // Used for each of i) getting a connection from the pool, establishing a connection and the
    // socket timeout
    public static String HTTP_TIMEOUT = "httpTimeout";
    public static String MAX_HTTP_CONNECTIONS = "maxHttpConnections";
    public static String MAX_HTTP_CONNECTIONS_PER_ROUTE = "maxHttpConnectionsPerRoute";
  }

  private final Properties properties;
  private final Set<Package> clientPackages;

  // Default values to establish a client connection
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
    int httpTimeout = DEFAULT_HTTP_TIMEOUT_MSECS;
    int maxHttpConnections = DEFAULT_MAX_HTTP_CONNECTIONS;
    int maxHttpConnectionsPerRoute = DEFAULT_MAX_HTTP_CONNECTIONS_PER_ROUTE;

    if (properties.containsKey(HttpClientConnParams.HTTP_TIMEOUT)) {
      httpTimeout = Integer.parseInt(properties.getProperty(HttpClientConnParams.HTTP_TIMEOUT));
    }
    if (properties.containsKey(HttpClientConnParams.MAX_HTTP_CONNECTIONS)) {
      maxHttpConnections = Integer.parseInt(properties.getProperty(HttpClientConnParams.MAX_HTTP_CONNECTIONS));
    }
    if (properties.containsKey(HttpClientConnParams.MAX_HTTP_CONNECTIONS_PER_ROUTE)) {
      maxHttpConnectionsPerRoute =
        Integer.parseInt(properties.getProperty(HttpClientConnParams.MAX_HTTP_CONNECTIONS_PER_ROUTE));
    }
    return HttpUtil.newMultithreadedClient(httpTimeout, maxHttpConnections, maxHttpConnectionsPerRoute);
  }

  @Provides
  @Singleton
  @Inject
  public Client providesJerseyClient(HttpClient client) {
    return buildJerseyClient(client);
  }

  public static Client buildJerseyClient(HttpClient client) {
    ApacheHttpClient4Handler hch = new ApacheHttpClient4Handler(client, null, false);

    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getClasses().add(JacksonJsonContextResolver.class);
    // this line is critical! Note that this is the jersey version of this class name!
    clientConfig.getClasses().add(JacksonJsonProvider.class);
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    return new ApacheHttpClient4(hch, clientConfig);
  }

}
