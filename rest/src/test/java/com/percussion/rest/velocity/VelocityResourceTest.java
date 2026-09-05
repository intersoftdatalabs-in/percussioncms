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

package com.percussion.rest.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.extensions.IExtensionAdaptor;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class VelocityResourceTest {

  private IExtensionAdaptor extensionAdaptor;
  private IVelocityAdaptor velocityAdaptor;
  private VelocityResource resource;

  @BeforeEach
  public void setUp() {
    extensionAdaptor = mock(IExtensionAdaptor.class);
    velocityAdaptor = mock(IVelocityAdaptor.class);
    resource = new VelocityResource(extensionAdaptor, velocityAdaptor);
  }

  @Test
  public void listSnippetsDelegates() {
    VelocitySnippet s =
        new VelocitySnippet("field.displayfield", "displayfield", "field", "#displayfield(\"rx:title\")");
    when(velocityAdaptor.listSnippets()).thenReturn(List.of(s));

    List<VelocitySnippet> out = resource.listSnippets();
    assertEquals(1, out.size());
    assertEquals("field.displayfield", out.get(0).getId());
    verify(velocityAdaptor).listSnippets();
  }

  @Test
  public void listSnippetsNullSafe() {
    when(velocityAdaptor.listSnippets()).thenReturn(null);
    assertTrue(resource.listSnippets().isEmpty());
  }

  @Test
  public void listSnippetsWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(velocityAdaptor.listSnippets()).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listSnippets());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void listSnippetsRequiresAdaptor() {
    VelocityResource bare = new VelocityResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listSnippets);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void getSnippetDelegates() {
    VelocitySnippet s =
        new VelocitySnippet("slot.slot_simple", "slot_simple", "slot", "#slot_simple(\"rffSidNav\")");
    when(velocityAdaptor.findSnippetById(eq("slot.slot_simple"))).thenReturn(s);

    assertEquals("slot.slot_simple", resource.getSnippet("slot.slot_simple").getId());
    verify(velocityAdaptor).findSnippetById("slot.slot_simple");
  }

  @Test
  public void getSnippetNotFoundIs404() {
    when(velocityAdaptor.findSnippetById(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSnippet("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Snippet not found", ex.getMessage());
  }

  @Test
  public void getSnippetWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("down");
    when(velocityAdaptor.findSnippetById(eq("field.field"))).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSnippet("field.field"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void getSnippetRethrowsWebApplicationException() {
    WebApplicationException mapped = new WebApplicationException("from adaptor", 404);
    when(velocityAdaptor.findSnippetById(eq("xx"))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSnippet("xx"));
    assertSame(mapped, ex);
  }
}
