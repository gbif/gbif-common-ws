package org.gbif.ws.client;

import java.util.Locale;
import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * Base client providing a get by key method for a specific entity type T.
 * The client applies a json media type and provides various convenience methods to execute CRUD requests
 * and can be configured to work with authentication.
 * The client can return other types than the main T class via generic types passed in.
 *
 * @param <T> the main entity class
 */
public abstract class BaseWsGetClient<T, K> extends BaseWsClient {

  protected final Class<T> resourceClass;

  /**
   * @param resourceClass the main entity class
   * @param resource      the base url to the underlying webservice
   * @param authFilter    optional authentication filter, can be null
   */
  protected BaseWsGetClient(Class<T> resourceClass, WebResource resource, @Nullable ClientFilter authFilter) {
    super(resource);
    this.resourceClass = resourceClass;
    if (authFilter != null) {
      this.resource.addFilter(authFilter);
    }
  }

  /**
   * Gets a resource by its key by executing a json type http GET request.
   * The clients resourceClass is used to convert the response into an object.
   *
   * @return requested resource or {@code null} if it couldn't be found
   */
  @Nullable
  public T get(K key) {
    if (key == null) {
      throw new IllegalArgumentException("Key cannot be null");
    }
    return resource.path(key.toString()).type(MediaType.APPLICATION_JSON).get(resourceClass);
  }

  /**
   * Executes a json type http GET request with paths added to base resource.
   * The clients resourceClass is used to convert the response into an object.
   *
   * @param path optional paths to be appended to the base resource
   *
   * @return requested resource or null
   */
  @Nullable
  protected T get(String... path) {
    return getResource(path).type(MediaType.APPLICATION_JSON).get(resourceClass);
  }

  /**
   * Gets a resource by its key by executing a json type http GET request.
   * Also applies a language http header.
   * The clients resourceClass is used to convert the response into an object.
   *
   * @param key    primary key for base resource class
   * @param locale identifier for a region
   *
   * @return requested resource or null
   */
  @Nullable
  protected T get(K key, Locale locale) {
    if (key == null) {
      throw new IllegalArgumentException("Key cannot be null");
    }
    return get(locale, key.toString());
  }

  /**
   * Gets a resource by a path by executing a json type http GET request.
   * Also applies a language http header.
   * The clients resourceClass is used to convert the response into an object.
   *
   * @param locale identifier for a region.
   * @param path   optional paths to be appended to the base resource.
   *
   * @return requested resource or null
   */
  @Nullable
  protected T get(Locale locale, String... path) {
    WebResource.Builder res = getResource(path).type(MediaType.APPLICATION_JSON);
    if (locale != null) {
      res = res.acceptLanguage(locale);
    }
    return res.get(resourceClass);
  }

  /**
   * Executes an http POST passing on a json encoded entity.
   *
   * @param entity to POST
   * @param path   to POST to
   */
  protected void post(Object entity, String... path) {
    getResource(path).type(MediaType.APPLICATION_JSON).post(entity);
  }

  /**
   * Executes an http POST passing on a json encoded entity and returning the given class, e.g. an Integer id.
   *
   * @param returnClass the class of the type returned by the webservice.
   * @param entity      to POST
   * @param path        to POST to
   * @param <T>         returned class type
   */
  protected <T> T post(Class<T> returnClass, Object entity, String... path) {
    return getResource(path).type(MediaType.APPLICATION_JSON).post(returnClass, entity);
  }

  /**
   * Executes an http PUT passing on a json encoded entity.
   *
   * @param entity to PUT
   * @param path   to PUT to
   */
  protected <T> void put(Object entity, String... path) {
    getResource(path).type(MediaType.APPLICATION_JSON).put(entity);
  }

  /**
   * Executes an http DELETE.
   *
   * @param path to DELETE to
   */
  protected <T> void delete(String... path) {
    getResource(path).type(MediaType.APPLICATION_JSON).delete();
  }

}
