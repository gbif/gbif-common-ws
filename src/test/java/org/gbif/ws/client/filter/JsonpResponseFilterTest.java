package org.gbif.ws.client.filter;

import org.gbif.ws.server.filter.JsonpResponseFilter;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.Collection;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JsonpResponseFilterTest {

  private JsonpResponseFilter filter = new JsonpResponseFilter();
  private MultivaluedMap<String, String> params;

  @Before
  public void setUp() {
    params = new MultivaluedMapImpl();
    when(mockRequest.getQueryParameters()).thenReturn(params);
  }

  @Mock
  ContainerResponse mockResponse;

  @Mock
  ContainerRequest mockRequest;

  @Test
  public void testFilterOnlyJsonAndJavascript() {
    Collection<MediaType> validMediaTypes =
      ImmutableList.of(MediaType.APPLICATION_JSON_TYPE, ExtraMediaTypes.APPLICATION_JAVASCRIPT_TYPE);

    when(mockRequest.getQueryParameters()).thenReturn(params);
    for (MediaType mediaType : validMediaTypes) {
      when(mockResponse.getMediaType()).thenReturn(mediaType);
      when(mockResponse.getEntity()).thenReturn("{id:1}");
      params.putSingle("callback", "foo");

      assertEquals(mockResponse, filter.filter(mockRequest, mockResponse));

      verify(mockResponse).setEntity(any(JSONWithPadding.class));
      reset(mockResponse);
    }
  }

  @Test
  public void testDontFilterOtherMediatypes() {
    when(mockResponse.getMediaType()).thenReturn(MediaType.APPLICATION_ATOM_XML_TYPE);
    filter.filter(mockRequest, mockResponse);

    verify(mockResponse, never()).setEntity(any());
  }

  @Test
  public void testNullMediaType() {
    when(mockResponse.getMediaType()).thenReturn(null);
    filter.filter(mockRequest, mockResponse);

    verify(mockResponse, never()).setEntity(any());
  }

  @Test
  public void testEmptyAndMissingCallback() {
    when(mockResponse.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);

    assertEquals(mockResponse, filter.filter(mockRequest, mockResponse));
    verify(mockResponse, never()).setEntity(any());

    params.putSingle("callback", "");
    assertEquals(mockResponse, filter.filter(mockRequest, mockResponse));
    verify(mockResponse, never()).setEntity(any());
  }

  @Test
  public void testEmptyEntity() {
    when(mockResponse.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);

    params.putSingle("callback", "foo");
    when(mockRequest.getQueryParameters()).thenReturn(params);

    when(mockResponse.getEntity()).thenReturn(null);
    assertEquals(mockResponse, filter.filter(mockRequest, mockResponse));
    verify(mockResponse, never()).setEntity(any());
  }
}
