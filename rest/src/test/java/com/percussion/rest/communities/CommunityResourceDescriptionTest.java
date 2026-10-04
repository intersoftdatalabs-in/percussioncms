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
import static org.junit.jupiter.api.Assertions.assertNull;
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

/** Issue #5178 — CommunityResource description status mapping. */
@Tag("UnitTest")
public class CommunityResourceDescriptionTest {

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
  public void descriptionSuccess() {
    Community updated = new Community();
    updated.setName("Default");
    updated.setDescription("Enterprise notes");
    when(adaptor.updateCommunityDescription(eq("Default"), eq("Enterprise notes")))
        .thenReturn(updated);
    Community out = resource.updateCommunityDescription("Default", body("Enterprise notes"));
    assertEquals("Default", out.getName());
    assertEquals("Enterprise notes", out.getDescription());
    verify(adaptor).updateCommunityDescription("Default", "Enterprise notes");
  }

  @Test
  public void clearSendsEmptyDescription() {
    Community cleared = new Community();
    cleared.setName("Default");
    when(adaptor.updateCommunityDescription(eq("Default"), eq(""))).thenReturn(cleared);
    Community out = resource.updateCommunityDescription("Default", body(""));
    assertEquals("Default", out.getName());
    assertNull(out.getDescription());
    verify(adaptor).updateCommunityDescription("Default", "");
  }

  @Test
  public void descriptionRequiresBody() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateCommunityDescription("Default", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).updateCommunityDescription(any(), any());
  }

  @Test
  public void overlongDescriptionIs400() {
    when(adaptor.updateCommunityDescription(any(), any()))
        .thenThrow(
            new IllegalArgumentException(
                "Community description cannot have more than 255 characters"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateCommunityDescription("Default", body("D".repeat(256))));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void lockIs409() {
    when(adaptor.updateCommunityDescription(any(), any()))
        .thenThrow(new WebApplicationException("Community could not be locked", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateCommunityDescription("Default", body("notes")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void nonAdminIs403() {
    when(adaptor.updateCommunityDescription(any(), any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateCommunityDescription("Default", body("notes")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingIs404() {
    when(adaptor.updateCommunityDescription(eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateCommunityDescription("missing", body("notes")));
    assertEquals(404, ex.getResponse().getStatus());
  }

  private static CommunityDescription body(String description) {
    CommunityDescription body = new CommunityDescription();
    body.setDescription(description);
    return body;
  }
}
