package org.gbif.ws.client;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly.web.GrizzlyWebTestContainerFactory;

public class BaseResourceTest extends JerseyTest {
  // The system properties that can be set to override the test port
  private final static String[] SYSTEM_PROPERTY_TEST_PORT = {"jersey.test.port","JERSEY_HTTP_PORT"};

  public static WebAppDescriptor buildWebAppDescriptor(String baseResourcePackage, String contextPath,
    Class<? extends GuiceServletContextListener> contextListenerClass) throws TestContainerException {

    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, true);

    return new WebAppDescriptor.Builder().contextListenerClass(contextListenerClass).filterClass(GuiceFilter.class)
      .contextPath(contextPath).servletPath("/").clientConfig(clientConfig)
      .initParam(PackagesResourceConfig.PROPERTY_PACKAGES, baseResourcePackage).build();
  }

  protected final String contextPath;

  /**
   * @throws TestContainerException
   */
  public BaseResourceTest(String baseResourcePackage, String contextPath,
    Class<? extends GuiceServletContextListener> contextListenerClass)
    throws TestContainerException {
    super(BaseResourceTest.buildWebAppDescriptor(baseResourcePackage, contextPath, contextListenerClass));
    this.contextPath = contextPath;
  }

  public WebResource createExternalWebResource(String baseUrl, String resourceURL) {
    ClientConfig cc = new DefaultClientConfig();
    // Custom readers and writers must be added to the client
    Client client;
    client = Client.create(cc);
    WebResource resource = client.resource(baseUrl + resourceURL);
    return resource;
  }

  public WebResource createExternalWebResource(String baseUrl, String resourceURL, String userName, String password) {
    ClientConfig cc = new DefaultClientConfig();
    // Custom readers and writers must be added to the client
    Client client;
    client = Client.create(cc);
    this.setCredentials(userName, password);
    WebResource resource = client.resource(baseUrl + resourceURL);
    return resource;
  }

  @Override
  protected TestContainerFactory getTestContainerFactory() {
    return new GrizzlyWebTestContainerFactory();
  }

  protected void setCredentials(String userName, String password) {
    client().addFilter(new HTTPBasicAuthFilter(userName, password));
  }

  /**
   * Returns the base URI of the application.
   * 
   * @return The base URI of the application
   */
  protected static URI getTestBaseURI() {
    return UriBuilder.fromUri("http://localhost/").port(getTestPort(9998)).build();
  }

  /**
   * Reads the system property defined in {@link SYSTEM_PROPERTY_TEST_PORT} to override
   * the port for Grizzly.  
   * @param defaultPort default port to use should no properties be provided
   * @return The port, or the default
   * @throws TestContainerException Should a property be provided but be an illegal argument
   */
  protected static int getTestPort(int defaultPort) throws TestContainerException {
    for (String property : SYSTEM_PROPERTY_TEST_PORT) {
      String port = System.getProperty(property);
      if (port != null) {
        try {
          return Integer.parseInt(port);
        } catch (NumberFormatException e) {
          throw new TestContainerException(
            "illegal value[" + port +"] supplied for property[" + property + "] which should defined a port number to use.", e);
        }
      }
    }
    return defaultPort;
  }
}
