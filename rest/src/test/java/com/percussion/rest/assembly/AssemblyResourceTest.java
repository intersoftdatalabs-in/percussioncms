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

package com.percussion.rest.assembly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class AssemblyResourceTest {

  private IAssemblyAdaptor adaptor;
  private AssemblyResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IAssemblyAdaptor.class);
    resource = new AssemblyResource(adaptor);
  }

  @Test
  public void previewLocationSuccess() {
    PreviewLocation loc = new PreviewLocation("/assembler/render?x=1", 10, 20, 1);
    when(adaptor.previewLocation(10, 20, null)).thenReturn(loc);
    PreviewLocation out = resource.previewLocation(10, 20, null);
    assertEquals("/assembler/render?x=1", out.getPreviewUrl());
    assertEquals(10, out.getContentId());
    assertEquals(20, out.getTemplateId());
    verify(adaptor).previewLocation(10, 20, null);
  }

  @Test
  public void missingIdsReturn400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.previewLocation(null, 1, null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void zeroIdsReturn400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.previewLocation(0, 1, null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void missingItemReturns404() {
    when(adaptor.previewLocation(9, 2, 1)).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.previewLocation(9, 2, 1));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void unexpectedFailureIs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.previewLocation(1, 2, null)).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.previewLocation(1, 2, null));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void missingAdaptorReturns503() {
    AssemblyResource bare = new AssemblyResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.previewLocation(1, 2, null));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void flushCacheDelegatesToAdaptor() {
    AssemblyOperationResult out = resource.flushCache();
    assertEquals(AssemblyOperationResult.ok("Assembler cache flushed"), out);
    verify(adaptor).flushAssemblerCache();
  }

  @Test
  public void navResetDelegatesToAdaptor() {
    AssemblyOperationResult out = resource.navReset();
    assertEquals(AssemblyOperationResult.ok("Managed navigation reset"), out);
    verify(adaptor).resetNavigation();
  }

  @Test
  public void flushCacheFailureIs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    doThrow(boom).when(adaptor).flushAssemblerCache();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.flushCache());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void flushCacheMissingAdaptorReturns503() {
    AssemblyResource bare = new AssemblyResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.flushCache());
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void navResetFailureIs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    doThrow(boom).when(adaptor).resetNavigation();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.navReset());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void navResetMissingAdaptorReturns503() {
    AssemblyResource bare = new AssemblyResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.navReset());
    assertEquals(503, ex.getResponse().getStatus());
  }
}
