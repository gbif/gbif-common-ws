package org.gbif.ws.client.filter;

import org.gbif.ws.server.filter.CreatedResponseFilter;

import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import com.google.common.collect.Maps;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.core.util.StringKeyIgnoreCaseMultivaluedMap;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreatedResponseFilterTest {

  private CreatedResponseFilter filter = new CreatedResponseFilter();
  private MultivaluedMap<String, String> params;
  private MultivaluedMap<String, Object> headers;
  private Object content;
  
  @Mock
  ContainerResponse mockResponse;

  @Mock
  ContainerRequest mockRequest;

  @Before
  public void setUp() {
    params = new MultivaluedMapImpl();
    headers = new StringKeyIgnoreCaseMultivaluedMap<Object>();
    content = UUID.randomUUID();
  }

  @Test
  public void testEmptyEntity() {
    when(mockResponse.getHttpHeaders()).thenReturn(headers);
    when(mockResponse.getStatusType()).thenReturn(ClientResponse.Status.OK);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockResponse.getEntity()).thenReturn(content);
    setContent(null);
    assertNoLocationHeader(filter.filter(mockRequest, mockResponse));
  }

  @Test
  public void testWrongMethod() {
    when(mockResponse.getHttpHeaders()).thenReturn(headers);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockResponse.getEntity()).thenReturn(content);
    assertNoLocationHeader(filter.filter(mockRequest, mockResponse));
  }

  @Test
  public void testWrongReturnType() {
    when(mockResponse.getHttpHeaders()).thenReturn(headers);
    when(mockResponse.getStatusType()).thenReturn(ClientResponse.Status.OK);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockResponse.getEntity()).thenReturn(content);
    Map<String, Integer > data = Maps.newHashMap();
    data.put("feel", 17);
    data.put("act", 21);
    data.put("look", 26);
    data.put("own", 38);
    setContent(data);

    assertNoLocationHeader(filter.filter(mockRequest, mockResponse));
  }

  private void setContent(Object content) {
    when(mockResponse.getEntity()).thenReturn(content);
    this.content = content;
  }

  @Test
  public void testSuccessfulPost() {
    when(mockResponse.getHttpHeaders()).thenReturn(headers);
    when(mockResponse.getStatusType()).thenReturn(ClientResponse.Status.OK);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockResponse.getEntity()).thenReturn(content);
    when(mockRequest.getRequestUriBuilder()).thenReturn(UriBuilder.fromUri("http://api.gbif.org/node"));
    content = UUID.randomUUID();
    when(mockResponse.getEntity()).thenReturn(content);

    assertLocationHeader(filter.filter(mockRequest, mockResponse));
  }

  private void assertLocationHeader(ContainerResponse resp) {
    assertEquals(content, resp.getEntity());
    assertTrue(resp.getHttpHeaders().getFirst("Location").toString().endsWith(content.toString()));
  }

  private void assertNoLocationHeader(ContainerResponse resp) {
    assertEquals(content, resp.getEntity());
    assertNull(resp.getHttpHeaders().getFirst("Location"));
    verify(mockResponse, never()).setEntity(any());
  }

}
