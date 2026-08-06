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

package com.percussion.rest.systemdef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class SystemDefResourceTest {

  private ISystemDefAdaptor adaptor;
  private SystemDefResource resource;

  @BeforeEach
  public void setUp() throws Exception {
    adaptor = mock(ISystemDefAdaptor.class);
    resource = new SystemDefResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    Field f = SystemDefResource.class.getDeclaredField("uriInfo");
    f.setAccessible(true);
    f.set(resource, uriInfo);
  }

  @Test
  public void getSystemDefDelegatesToAdaptor() {
    SystemDefDetail d = new SystemDefDetail();
    d.setFieldCount(3);
    when(adaptor.getSystemDef(any())).thenReturn(d);

    assertEquals(3, resource.getSystemDef().getFieldCount());
    verify(adaptor).getSystemDef(any());
  }

  @Test
  public void getSystemDefWrapsFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("object store down");
    when(adaptor.getSystemDef(any())).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSystemDef());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void getSystemDefWithoutInjectionFailsWithDiagnostic() {
    SystemDefResource bare = new SystemDefResource();
    WebApplicationException ex = assertThrows(WebApplicationException.class, bare::getSystemDef);
    assertEquals(500, ex.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, ex.getCause());
  }
}
