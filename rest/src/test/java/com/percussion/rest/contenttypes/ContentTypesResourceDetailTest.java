/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.rest.contenttypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class ContentTypesResourceDetailTest {

  private IContentTypesAdaptor adaptor;
  private ContentTypesResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IContentTypesAdaptor.class);
    resource = new ContentTypesResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    resource.setUriInfo(uriInfo);
  }

  @Test
  public void getContentTypeReturnsDetail() {
    ContentTypeDetail d = new ContentTypeDetail();
    d.setName("percPage");
    when(adaptor.getContentType(any(), eq("percPage"))).thenReturn(d);
    assertEquals("percPage", resource.getContentType("percPage").getName());
  }

  @Test
  public void getContentTypeNotFound() {
    when(adaptor.getContentType(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getContentType("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateContentTypeSuccess() {
    ContentTypeDetail body = new ContentTypeDetail();
    body.setLabel("Page");
    ContentTypeDetail updated = new ContentTypeDetail();
    updated.setName("percPage");
    updated.setLabel("Page");
    when(adaptor.updateContentType(any(), eq("percPage"), any())).thenReturn(updated);
    assertEquals("Page", resource.updateContentType("percPage", body).getLabel());
  }

  @Test
  public void updateContentTypeNotFound() {
    when(adaptor.updateContentType(any(), eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateContentType("missing", new ContentTypeDetail()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateContentTypeBadRequest() {
    when(adaptor.updateContentType(any(), eq("percPage"), any()))
        .thenThrow(new IllegalArgumentException("body is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateContentType("percPage", null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void updateContentTypeLockConflict() {
    when(adaptor.updateContentType(any(), eq("percPage"), any()))
        .thenThrow(new IllegalStateException("Could not acquire design lock for content type"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateContentType("percPage", new ContentTypeDetail()));
    assertEquals(409, ex.getResponse().getStatus());
  }
}
