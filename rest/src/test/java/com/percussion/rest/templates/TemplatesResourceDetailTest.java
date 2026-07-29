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

package com.percussion.rest.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class TemplatesResourceDetailTest {

  private ITemplatesAdaptor adaptor;
  private TemplatesResource resource;

  @BeforeEach
  public void setUp() throws Exception {
    adaptor = mock(ITemplatesAdaptor.class);
    resource = new TemplatesResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    Field f = TemplatesResource.class.getDeclaredField("uriInfo");
    f.setAccessible(true);
    f.set(resource, uriInfo);
  }

  @Test
  public void getTemplateReturnsDetail() {
    TemplateDetail d = new TemplateDetail();
    d.setName("perc.page");
    when(adaptor.getTemplate(any(), eq("perc.page"))).thenReturn(d);

    TemplateDetail out = resource.getTemplate("perc.page");
    assertEquals("perc.page", out.getName());
  }

  @Test
  public void getTemplateNotFound() {
    when(adaptor.getTemplate(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getTemplate("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getTemplateWrapsFailures() {
    when(adaptor.getTemplate(any(), eq("boom")))
        .thenThrow(new IllegalStateException("fail"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getTemplate("boom"));
    assertEquals(500, ex.getResponse().getStatus());
  }
}
