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

package com.percussion.rest.communities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Issue #5177 — CommunityResource rename status mapping. */
@Tag("UnitTest")
public class CommunityResourceRenameTest {

  private ICommunityAdaptor adaptor;
  private CommunityResource resource;

  @BeforeEach
  public void setUp() throws Exception {
    adaptor = mock(ICommunityAdaptor.class);
    resource = new CommunityResource();
    Field f = CommunityResource.class.getDeclaredField("adaptor");
    f.setAccessible(true);
    f.set(resource, adaptor);
  }

  @Test
  public void renameSuccess() {
    Community renamed = new Community();
    renamed.setName("Enterprise");
    when(adaptor.renameCommunity(eq("Default"), eq("Enterprise"))).thenReturn(renamed);
    Community out = resource.renameCommunity("Default", body("Enterprise"));
    assertEquals("Enterprise", out.getName());
    verify(adaptor).renameCommunity("Default", "Enterprise");
  }

  @Test
  public void renameRequiresBody() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.renameCommunity("Default", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).renameCommunity(any(), any());
  }

  @Test
  public void blankNameIs400() {
    when(adaptor.renameCommunity(any(), any()))
        .thenThrow(new IllegalArgumentException("name cannot be null or empty"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.renameCommunity("Default", body(" ")));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void overlongNameIs400() {
    when(adaptor.renameCommunity(any(), any()))
        .thenThrow(new IllegalArgumentException("Community name cannot have more than 50 characters"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameCommunity("Default", body("E".repeat(51))));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void duplicateIs409() {
    when(adaptor.renameCommunity(any(), any()))
        .thenThrow(new WebApplicationException("Community already exists: Enterprise", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameCommunity("Default", body("Enterprise")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void nonAdminIs403() {
    when(adaptor.renameCommunity(any(), any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameCommunity("Default", body("Enterprise")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingIs404() {
    when(adaptor.renameCommunity(eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.renameCommunity("missing", body("Enterprise")));
    assertEquals(404, ex.getResponse().getStatus());
  }

  private static CommunityRename body(String name) {
    CommunityRename body = new CommunityRename();
    body.setName(name);
    return body;
  }
}
