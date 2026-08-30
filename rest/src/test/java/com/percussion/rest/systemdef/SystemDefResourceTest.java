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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class SystemDefResourceTest {

  private ISystemDefAdaptor adaptor;
  private SystemDefResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(ISystemDefAdaptor.class);
    resource = new SystemDefResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    resource.setUriInfo(uriInfo);
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

  @Test
  public void getSystemDefForbiddenWhenNotAdmin() {
    when(adaptor.getSystemDef(any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSystemDef());
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateSystemDefDelegatesToAdaptor() {
    SystemDefDetail body = new SystemDefDetail();
    SystemDefDetail updated = new SystemDefDetail();
    updated.setFieldCount(2);
    when(adaptor.updateSystemDef(any(), any())).thenReturn(updated);

    assertEquals(2, resource.updateSystemDef(body).getFieldCount());
    verify(adaptor).updateSystemDef(any(), eq(body));
  }

  @Test
  public void updateSystemDefInvalidFieldIs400() {
    when(adaptor.updateSystemDef(any(), any()))
        .thenThrow(new IllegalArgumentException("Unknown field: missing"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateSystemDef(new SystemDefDetail()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void updateSystemDefLockConflictIs409() {
    when(adaptor.updateSystemDef(any(), any()))
        .thenThrow(
            new SystemDefDesignLockException("Could not save system definition; locked by other"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateSystemDef(new SystemDefDetail()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void updateSystemDefForbiddenWhenNotAdmin() {
    when(adaptor.updateSystemDef(any(), any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateSystemDef(new SystemDefDetail()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateSystemDefWrapsUnexpectedFailures() {
    IllegalStateException boom = new IllegalStateException("design ws down");
    when(adaptor.updateSystemDef(any(), any())).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateSystemDef(new SystemDefDetail()));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void mapWriteFailureLockMessageOnIllegalStateIs409() {
    WebApplicationException ex =
        SystemDefResource.mapWriteFailure(new IllegalStateException("object is not locked"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void addFieldDelegatesToAdaptor() {
    SystemDefFieldSummary body = new SystemDefFieldSummary();
    body.setName("sys_custom");
    SystemDefDetail updated = new SystemDefDetail();
    updated.setFieldCount(1);
    when(adaptor.addField(any(), any())).thenReturn(updated);

    assertEquals(1, resource.addField(body).getFieldCount());
    verify(adaptor).addField(any(), eq(body));
  }

  @Test
  public void addFieldInvalidNameIs400() {
    when(adaptor.addField(any(), any()))
        .thenThrow(new IllegalArgumentException("name is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.addField(new SystemDefFieldSummary()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void addFieldDuplicateIs409() {
    when(adaptor.addField(any(), any()))
        .thenThrow(new WebApplicationException("System field already exists: sys_custom", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.addField(new SystemDefFieldSummary()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void addFieldLockConflictIs409() {
    when(adaptor.addField(any(), any()))
        .thenThrow(
            new SystemDefDesignLockException("Could not save system definition; locked by other"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.addField(new SystemDefFieldSummary()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void addFieldForbiddenWhenNotAdmin() {
    when(adaptor.addField(any(), any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.addField(new SystemDefFieldSummary()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void addFieldWrapsUnexpectedFailures() {
    IllegalStateException boom = new IllegalStateException("design ws down");
    when(adaptor.addField(any(), any())).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.addField(new SystemDefFieldSummary()));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void deleteFieldReturns204() {
    Response out = resource.deleteField("sys_custom");
    assertEquals(204, out.getStatus());
    verify(adaptor).deleteField(any(), eq("sys_custom"));
  }

  @Test
  public void deleteFieldBlankNameIs400() {
    doThrow(new IllegalArgumentException("name is required"))
        .when(adaptor)
        .deleteField(any(), eq(" "));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteField(" "));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void deleteFieldUnknownIs400() {
    doThrow(new IllegalArgumentException("Unknown field: missing"))
        .when(adaptor)
        .deleteField(any(), eq("missing"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteField("missing"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void deleteFieldLockConflictIs409() {
    doThrow(new SystemDefDesignLockException("Could not save system definition; locked by other"))
        .when(adaptor)
        .deleteField(any(), eq("sys_custom"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteField("sys_custom"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteFieldForbiddenWhenNotAdmin() {
    doThrow(new WebApplicationException("Admin role required", 403))
        .when(adaptor)
        .deleteField(any(), eq("sys_custom"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteField("sys_custom"));
    assertEquals(403, ex.getResponse().getStatus());
  }
}
