package org.gbif.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.ws.json.JacksonJsonContextResolver;

import java.io.IOException;
import java.util.Locale;
import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.base.Joiner;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.api.model.common.paging.PagingConstants.PARAM_LIMIT;
import static org.gbif.api.model.common.paging.PagingConstants.PARAM_OFFSET;

/**
 * The base webservice client for all GBIF clients not tight to a specific entity class but containing various
 * convenience methods to deal with a web resource, paging and parameters.
 */
public abstract class BaseWsClient {

  private static final Joiner PATH_JOINER = Joiner.on("/");
  // Jersey client resource used for the remote communication.
  protected final WebResource resource;
  protected final Logger log = LoggerFactory.getLogger(getClass());
  private final ObjectMapper mapper = new JacksonJsonContextResolver().getContext(null);

  public BaseWsClient(WebResource resource) {
    this.resource = resource;
    log.info("Creating new {} using webservices at {}", getClass().getSimpleName(), resource.toString());
  }

  protected WebResource getResource() {
    return resource;
  }

  /**
   * Gets the base web resource with added paging parameters.
   *
   * @param page Which may be null
   *
   * @return The resource with appropriate paging parameters set
   */
  protected WebResource getResource(@Nullable Pageable page) {
    return applyPage(resource, page);
  }

  protected WebResource applyPage(WebResource resource, @Nullable Pageable page) {
    if (page == null) {
      return resource;
    } else {
      return resource.queryParam(PARAM_LIMIT, String.valueOf(page.getLimit()))
        .queryParam(PARAM_OFFSET, String.valueOf(page.getOffset()));
    }
  }

  /**
   * Gets the base web resource with added paths.
   *
   * @param path optional paths to be appended to the base resource
   *
   * @return The resource with appropriate path and parameters set.
   */
  protected WebResource getResource(String... path) {
    return getResource(null, path);
  }

  /**
   * Gets the base web resource with added paths and paging parameters.
   *
   * @param page Which may be null
   * @param path optional paths to be appended to the base resource
   *
   * @return The resource with appropriate path and parameters set.
   */
  protected WebResource getResource(Pageable page, String... path) {
    return getResource(page).path(PATH_JOINER.join(path));
  }

  /**
   * Gets the base web resource with added paging parameters.
   *
   * @param page   optional paging parameters
   * @param params optional additional query parameters
   * @param path   optional paths to be appended to the base resource
   *
   * @return The resource with appropriate path and parameters set.
   */
  protected WebResource getResource(@Nullable Pageable page, @Nullable MultivaluedMap<String, String> params,
    String... path) {
    WebResource res = getResource(page, path);
    if (params != null) {
      return res.queryParams(params);
    }
    return res;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + resource + '}';
  }

  /**
   * Executes a json type http GET request with paths added to base resource.
   * The passed generic type for the requested resource is used to convert the response into an object.
   *
   * @param gt   the generic type of the returned resource.
   * @param path optional paths to be appended to the base resource.
   *
   * @return requested resource
   */
  protected <T> T get(GenericType<T> gt, String... path) {
    return get(gt, null, null, null, path);
  }

  /**
   * Executes a json type http GET request with paths added to base resource.
   * The passed generic type for the requested resource is used to convert the response into an object.
   *
   * @param gt   the generic type of the returned resource.
   * @param page paging parameters to be applied to request.
   * @param path optional paths to be appended to the base resource.
   *
   * @return requested resource
   */
  protected <T> T get(GenericType<T> gt, Pageable page, String... path) {
    return get(gt, null, null, page, path);
  }

  /**
   * Executes a json type http GET request with paths added to base resource.
   * The passed generic type for the requested resource is used to convert the response into an object.
   *
   * @param gt     the generic type of the returned resource.
   * @param params extra query parameters to be added to request
   * @param path   optional paths to be appended to the base resource.
   *
   * @return requested resource
   */
  protected <T> T get(GenericType<T> gt, MultivaluedMap<String, String> params, String... path) {
    return get(gt, null, params, null, path);
  }

  /**
   * Executes a json type http GET request with paths added to base resource.
   * The passed generic type for the requested resource is used to convert the response into an object.
   *
   * @param gt     the generic type of the returned resource.
   * @param locale identifier for a region to set http language header.
   * @param page   paging parameters to be applied to request.
   * @param params extra query parameters to be added to request
   * @param path   optional paths to be appended to the base resource.
   *
   * @return requested resource
   */
  protected <T> T get(GenericType<T> gt, @Nullable Locale locale, @Nullable MultivaluedMap<String, String> params,
    @Nullable Pageable page, String... path) {

    WebResource res = getResource(page, params, path);

    if (locale == null) {
      return res.type(MediaType.APPLICATION_JSON).get(gt);
    }
    return res.type(MediaType.APPLICATION_JSON).acceptLanguage(locale).get(gt);
  }

  /**
   * Executes a json type http GET request to any given resource.
   * The passed generic type for the requested resource is used to convert the response into an object.
   * Note that the given web resource is used to send the http request and not the internal base resource.
   *
   * @param gt       the generic type of the returned resource.
   * @param page     paging parameters to be applied to request.
   * @param resource web resource to send request to.
   *
   * @return requested resource
   */
  protected <T> T get(GenericType<T> gt, Pageable page, WebResource resource) {
    return applyPage(resource, page).type(MediaType.APPLICATION_JSON).get(gt);
  }

  /**
   * Convert the http entity into a byte array to avoid jackson creating a chunked encoding request!
   */
  protected byte[] toBytes(Object entity) {
    try {
      return mapper.writeValueAsBytes(entity);
    } catch (IOException e) {
      log.error("Failed to serialize http entity [{}]", entity);
      throw new IllegalStateException(e);
    }
  }

  /**
   * Executes an http POST passing on a json encoded entity.
   *
   * @param entity to POST
   * @param path   to POST to
   */
  protected void post(Object entity, String... path) {
    getResource(path).type(MediaType.APPLICATION_JSON).post(toBytes(entity));
  }

  /**
   * Executes an http POST passing on a json encoded entity.
   *
   * @param gt     the generic type of the returned resource.
   * @param entity to POST
   * @param path   to POST to
   */
  protected <T> T post(GenericType<T> gt, Object entity, String... path) {
    return getResource(path).type(MediaType.APPLICATION_JSON).post(gt, toBytes(entity));
  }

  /**
   * Executes an http POST passing on a json encoded entity.
   *
   * @param cl     the class of the returned resource.
   * @param entity to POST
   * @param path   to POST to
   */
  protected <T> T post(Class<T> cl, Object entity, String... path) {
    return getResource(path).type(MediaType.APPLICATION_JSON).post(cl, toBytes(entity));
  }

  /**
   * Executes an http PUT passing on a json encoded entity.
   *
   * @param entity to PUT
   * @param path   to PUT to
   */
  protected void put(Object entity, String... path) {
    getResource(path).type(MediaType.APPLICATION_JSON).put(toBytes(entity));
  }

  /**
   * Executes an http PUT passing on a json encoded entity.
   *
   * @param gt     the generic type of the returned resource.
   * @param entity to PUT
   * @param path   to PUT to
   */
  protected <T> T put(GenericType<T> gt, Object entity, String... path) {
    return getResource(path).type(MediaType.APPLICATION_JSON).put(gt, toBytes(entity));
  }

  /**
   * Executes an http PUT passing on a json encoded entity.
   *
   * @param cl     the class of the returned resource.
   * @param entity to PUT
   * @param path   to PUT to
   */
  protected <T> T put(Class<T> cl, Object entity, String... path) {
    return getResource(path).type(MediaType.APPLICATION_JSON).put(cl, toBytes(entity));
  }

  /**
   * Executes an http DELETE.
   *
   * @param path to DELETE to
   */
  protected void delete(String... path) {
    getResource(path).type(MediaType.APPLICATION_JSON).delete();
  }
}
