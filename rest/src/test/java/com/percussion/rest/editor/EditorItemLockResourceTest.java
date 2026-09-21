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

package com.percussion.rest.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class EditorItemLockResourceTest {

  @Mock private IEditorItemLockAdaptor adaptor;
  @Mock private UriInfo uriInfo;

  private EditorItemLockResource resource;

  @BeforeEach
  void init() {
    resource = new EditorItemLockResource(adaptor);
    resource.setUriInfo(uriInfo);
    org.mockito.Mockito.lenient()
        .when(uriInfo.getBaseUri())
        .thenReturn(UriBuilder.fromUri("http://localhost/rest").build());
  }

  @Test
  void checkoutDelegates() {
    EditorItemLockInfo info = new EditorItemLockInfo("Home", "admin", "admin", "Assignee");
    when(adaptor.checkout(any(), eq("42"))).thenReturn(info);
    assertEquals("admin", resource.checkout("42").getCheckOutUser());
  }

  @Test
  void checkoutPropagates409() {
    when(adaptor.checkout(any(), eq("42")))
        .thenThrow(new WebApplicationException("held", 409));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.checkout("42"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void checkoutPropagates403() {
    when(adaptor.checkout(any(), eq("42")))
        .thenThrow(new WebApplicationException("forbidden", 403));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.checkout("42"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void checkinDelegates() {
    EditorItemLockInfo info = new EditorItemLockInfo("Home", "", "admin", "Assignee");
    when(adaptor.checkin(any(), eq("42"))).thenReturn(info);
    assertEquals("", resource.checkin("42").getCheckOutUser());
  }

  @Test
  void checkinPropagates403() {
    when(adaptor.checkin(any(), eq("42")))
        .thenThrow(new WebApplicationException("forbidden", 403));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.checkin("42"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void missingAdaptorIs503() {
    EditorItemLockResource bare = new EditorItemLockResource();
    bare.setUriInfo(uriInfo);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.checkout("42"));
    assertEquals(503, ex.getResponse().getStatus());
  }
}
