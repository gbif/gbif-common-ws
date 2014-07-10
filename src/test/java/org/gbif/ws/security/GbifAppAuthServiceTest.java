package org.gbif.ws.security;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.core.header.OutBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.gbif.ws.security.GbifAppAuthService.HEADER_AUTHORIZATION;
import static org.gbif.ws.security.GbifAppAuthService.HEADER_CONTENT_TYPE;
import static org.gbif.ws.security.GbifAppAuthService.HEADER_GBIF_CONTENT_HASH;
import static org.gbif.ws.security.GbifAppAuthService.HEADER_GBIF_DATE;
import static org.gbif.ws.security.GbifAppAuthService.HEADER_GBIF_USER;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GbifAppAuthServiceTest {

  public static final String APPKEY = "appKey";
  private static final String APPSECRET = "fghj56g66b676DFG";

  public static Map<String, String> buildAppKeyMap() {
    Map<String, String> map = Maps.newHashMap();
    map.put(APPKEY, APPSECRET);
    return map;
  }

  GbifAppAuthService service = new GbifAppAuthService(buildAppKeyMap());

  @Mock
  ContainerRequest containerRequest;
  @Mock
  ClientRequest mockRequest;
  MultivaluedMap<String, Object> headers;

  private static class HeaderWrapper implements MultivaluedMap<String, String> {

    private final MultivaluedMap<String, Object> map;

    public HeaderWrapper(MultivaluedMap<String, Object> map) {
      this.map = map;
    }

    @Override
    public void putSingle(String key, String value) {
      map.putSingle(key, value);
    }

    @Override
    public void add(String key, String value) {
      map.add(key, value);
    }

    @Override
    public String getFirst(String key) {
      return (String) map.getFirst(key);
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
      return map.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
      return map.containsValue(o);
    }

    @Override
    public List<String> get(Object o) {
      final List<Object> src = map.get(o);
      if (src == null) {
        return null;
      }
      List<String> l = Lists.newArrayList();
      for (Object lo : src) {
        l.add(lo.toString());
      }
      return l;
    }

    @Override
    public List<String> put(String s, List<String> strings) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<String> remove(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> map) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      map.clear();
    }

    @Override
    public Set<String> keySet() {
      return map.keySet();
    }

    @Override
    public Collection<List<String>> values() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
      throw new UnsupportedOperationException();
    }
  }

  @Before
  public void initMocks() throws Exception {
    URI uri = new URI("http://www.gbif.org/dataset");
    when(mockRequest.getURI()).thenReturn(uri);
    when(containerRequest.getRequestUri()).thenReturn(uri);
    when(containerRequest.getAbsolutePath()).thenReturn(uri);

    when(mockRequest.getMethod()).thenReturn("POST");
    when(containerRequest.getMethod()).thenReturn("POST");

    Object entity = "Simsalabim";
    when(mockRequest.getEntity()).thenReturn(entity);
    // we instantiate a real request here, because there is no implementation of MultivaluedMap<String, Object>
    headers = new OutBoundHeaders();
    when(mockRequest.getHeaders()).thenReturn(headers);
    when(containerRequest.getRequestHeaders()).thenReturn(new HeaderWrapper(headers));
    when(containerRequest.getHeaderValue(eq(HEADER_AUTHORIZATION))).thenCallRealMethod();
    when(containerRequest.getHeaderValue(eq(HEADER_CONTENT_TYPE))).thenCallRealMethod();

  }

  @Test
  public void testSignRequest() throws Exception {

    service.signRequest("appKey", "heinz", mockRequest);

    assertNotNull(headers.getFirst(HEADER_GBIF_CONTENT_HASH));
    assertNotNull(headers.getFirst(HEADER_GBIF_DATE));
    assertEquals("heinz", headers.getFirst(HEADER_GBIF_USER));
    assertTrue(headers.getFirst(HEADER_AUTHORIZATION).toString().startsWith("GBIF appKey:"));
  }

  @Test
  public void testIsValid() throws Exception {

    service.signRequest("appKey", "heinz", mockRequest);

    assertTrue(service.isValidRequest(containerRequest));

    headers.putSingle(HEADER_GBIF_CONTENT_HASH, 73);
    assertFalse(service.isValidRequest(containerRequest));
  }

}