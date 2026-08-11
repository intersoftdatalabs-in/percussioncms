/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.rest.DesignGap;
import com.percussion.rest.Guid;
import com.percussion.rest.JacksonContextResolver;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

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
  public void listContentTypesReturnsNameLabelGuid() {
    ContentType ct = sampleContentType();
    when(adaptor.listContentTypes(any())).thenReturn(List.of(ct));

    List<ContentType> list = resource.listContentTypes();
    assertEquals(1, list.size());
    ContentType first = list.get(0);
    assertEquals("percPage", first.getName());
    assertEquals("Page", first.getLabel());
    assertNotNull(first.getGuid());
    assertFalse(first.isHideFromMenu());
  }

  @Test
  public void listContentTypesJsonIsNotHideFromMenuOnly() {
    ContentType ct = sampleContentType();
    when(adaptor.listContentTypes(any())).thenReturn(List.of(ct));

    List<ContentType> list = resource.listContentTypes();
    ObjectMapper mapper = new JacksonContextResolver().getContext(ContentType.class);
    String json = mapper.writeValueAsString(new ContentTypeList(list));

    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("percPage"), json);
    assertTrue(json.contains("\"label\""), json);
    assertTrue(json.contains("Page"), json);
    assertTrue(json.contains("\"guid\""), json);
    // Regression: live install returned only hideFromMenu per item (#1693)
    assertFalse(
        json.replaceAll("\\s", "").contains("{\"hideFromMenu\":false}")
            && !json.contains("\"name\""),
        json);
  }

  @Test
  public void getContentTypeReturnsDetail() {
    ContentTypeDetail d = new ContentTypeDetail();
    d.setName("percPage");
    when(adaptor.getContentType(any(), eq("percPage"))).thenReturn(d);
    assertEquals("percPage", resource.getContentType("percPage").getName());
  }

  @Test
  public void getContentTypeReturnsStructuredDesignGaps() {
    ContentTypeDetail d = new ContentTypeDetail();
    d.setName("percPage");
    d.setDesignGaps(List.of(DesignGap.of("CT_ITEM_EXITS", "Item-level pre/post exits not exposed")));
    when(adaptor.getContentType(any(), eq("percPage"))).thenReturn(d);

    ContentTypeDetail out = resource.getContentType("percPage");
    assertNotNull(out.getDesignGaps());
    assertEquals(1, out.getDesignGaps().size());
    assertEquals("CT_ITEM_EXITS", out.getDesignGaps().get(0).getCode());
    assertEquals("Item-level pre/post exits not exposed", out.getDesignGaps().get(0).getMessage());
  }

  private static ContentType sampleContentType() {
    ContentType ct = new ContentType();
    ct.setName("percPage");
    ct.setLabel("Page");
    ct.setDescription("Page content type");
    ct.setGuid(new Guid("0-2-311"));
    ct.setHideFromMenu(false);
    return ct;
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
