package org.gbif.ws.client;

import java.io.IOException;
import java.net.URI;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.Servlet;
import javax.ws.rs.core.UriBuilder;

import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainer;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;

/**
 * This is a Grizzly web container that will reuse the container across tests rather than creating
 * a new one each time. This is a patched version of the one that ships with Jersey, with the following
 * modifications:
 * a) Repeated calls to the same factory will return the SAME TestContainer
 * b) start() and stop() on the TestContainer do nothing
 * Because of these changes one should use this with caution. It is particularly useful when one wants
 * the same grizzly container to be used for a complete test run, and then teared down at the end of the
 * test with the JVM. Normally JerseyTest will create a new one per method which can be expensive and also
 * tearing down grizzly appears to be on a separate thread, thus meaning connection pools are not terminated
 * immediately (a suspicion that is not thoroughly verified).
 */
public class SharedGrizzlyWebContainerFactory implements TestContainerFactory {

  /**
   * This class has methods for instantiating, starting and stopping the Grizzly Web
   * Server.
   */
  private static class SharedGrizzlyWebTestContainer implements TestContainer {

    private static final Logger LOGGER = Logger.getLogger(SharedGrizzlyWebTestContainer.class.getName());

    final URI baseUri;

    final String contextPath;

    final String servletPath;

    final Class servletClass;

    List<WebAppDescriptor.FilterDescriptor> filters = null;

    final List<Class<? extends EventListener>> eventListeners;

    final Map<String, String> initParams;

    final Map<String, String> contextParams;

    private GrizzlyWebServer webServer;

    /**
     * Creates an instance of {@link GrizzlyWebTestContainer}
     *
     * @param baseUri URI of the application
     * @param ad      An instance of {@link WebAppDescriptor}
     */
    private SharedGrizzlyWebTestContainer(URI baseUri, WebAppDescriptor ad) {
      this.baseUri = UriBuilder.fromUri(baseUri).path(ad.getContextPath()).path(ad.getServletPath()).build();

      LOGGER.info("Creating Grizzly Web Container configured at the base URI " + this.baseUri);
      this.contextPath = ad.getContextPath();
      this.servletPath = ad.getServletPath();
      this.servletClass = ad.getServletClass();
      this.filters = ad.getFilters();
      this.initParams = ad.getInitParams();
      this.contextParams = ad.getContextParams();
      this.eventListeners = ad.getListeners();

      instantiateGrizzlyWebServer();

    }

    @Override
    public URI getBaseUri() {
      return baseUri;
    }

    @Override
    public Client getClient() {
      return null;
    }

    /**
     * Instantiates the Grizzly Web Server
     */
    private void instantiateGrizzlyWebServer() {
      webServer = new GrizzlyWebServer(baseUri.getPort());
      ServletAdapter sa = new ServletAdapter();
      Servlet servletInstance;
      if (servletClass != null) {
        try {
          servletInstance = (Servlet) servletClass.newInstance();
        } catch (InstantiationException ex) {
          throw new TestContainerException(ex);
        } catch (IllegalAccessException ex) {
          throw new TestContainerException(ex);
        }
        sa.setServletInstance(servletInstance);
      }

      for (Class<? extends EventListener> eventListener : eventListeners) {
        sa.addServletListener(eventListener.getName());
      }

      // Filter support
      if (filters != null) {
        try {
          for (WebAppDescriptor.FilterDescriptor d : this.filters) {
            sa.addFilter(d.getFilterClass().newInstance(), d.getFilterName(), d.getInitParams());
          }
        } catch (InstantiationException ex) {
          throw new TestContainerException(ex);
        } catch (IllegalAccessException ex) {
          throw new TestContainerException(ex);
        }
      }

      for (String contextParamName : contextParams.keySet()) {
        sa.addContextParameter(contextParamName, contextParams.get(contextParamName));
      }

      for (String initParamName : initParams.keySet()) {
        sa.addInitParameter(initParamName, initParams.get(initParamName));
      }

      if (contextPath != null && contextPath.length() > 0) {
        if (!contextPath.startsWith("/")) {
          sa.setContextPath("/" + contextPath);
        } else {
          sa.setContextPath(contextPath);
        }
      }

      if (servletPath != null && servletPath.length() > 0) {
        if (!servletPath.startsWith("/")) {
          sa.setServletPath("/" + servletPath);
        } else {
          sa.setServletPath(servletPath);
        }
      }

      String[] mapping = null;
      webServer.addGrizzlyAdapter(sa, mapping);
      try {
        LOGGER.info("Starting the Grizzly Web Container...");
        webServer.start();
      } catch (IOException e) {
        throw new TestContainerException(e);
      }
    }

    @Override
    public void start() {
      // Does nothing
    }

    @Override
    public void stop() {
      // Does nothing
    }

  }

  // The singleton object
  private static SharedGrizzlyWebTestContainer instance;

  private static synchronized SharedGrizzlyWebTestContainer getInstance(URI baseUri, WebAppDescriptor ad) {
    if (instance == null) {
      instance = new SharedGrizzlyWebTestContainer(baseUri, ad);
    }
    return instance;
  }

  @Override
  public TestContainer create(URI baseUri, AppDescriptor ad) {
    if (!(ad instanceof WebAppDescriptor)) {
      throw new IllegalArgumentException("The application descriptor must be an instance of WebAppDescriptor");
    }

    return getInstance(baseUri, (WebAppDescriptor) ad);
  }

  @Override
  public Class<WebAppDescriptor> supports() {
    return WebAppDescriptor.class;
  }
}
