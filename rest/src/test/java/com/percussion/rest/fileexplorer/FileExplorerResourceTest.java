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

package com.percussion.rest.fileexplorer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class FileExplorerResourceTest {

  private IFileExplorerAdaptor adaptor;
  private FileExplorerResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IFileExplorerAdaptor.class);
    resource = new FileExplorerResource(adaptor);
  }

  @Test
  public void listRootsDelegates() {
    FileExplorerRoot root = new FileExplorerRoot();
    root.setId("rx_resources");
    when(adaptor.listRoots()).thenReturn(List.of(root));
    List<FileExplorerRoot> out = resource.listRoots();
    assertEquals(1, out.size());
    assertEquals("rx_resources", out.get(0).getId());
    verify(adaptor).listRoots();
  }

  @Test
  public void listRootsNullSafe() {
    when(adaptor.listRoots()).thenReturn(null);
    assertTrue(resource.listRoots().isEmpty());
  }

  @Test
  public void listRootsWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listRoots()).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listRoots());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void listRootsNonAdminIs403() {
    when(adaptor.listRoots())
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listRoots());
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnListRoots() {
    FileExplorerResource bare = new FileExplorerResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listRoots);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void listChildrenDelegates() {
    FileExplorerEntry entry = new FileExplorerEntry();
    entry.setName("readme.txt");
    entry.setRelativePath("readme.txt");
    when(adaptor.listChildren(eq("drop"), eq("sub"))).thenReturn(List.of(entry));
    List<FileExplorerEntry> out = resource.listChildren("drop", "sub");
    assertEquals(1, out.size());
    assertEquals("readme.txt", out.get(0).getName());
    verify(adaptor).listChildren("drop", "sub");
  }

  @Test
  public void listChildrenUnknownIsGeneric404WithoutRawPath() {
    when(adaptor.listChildren(eq("missing"), isNull())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.listChildren("missing", null));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals(FileExplorerResource.PATH_NOT_FOUND, ex.getMessage());
    assertFalse(ex.getMessage().contains("missing"));
  }

  @Test
  public void listChildrenUnsafePathIs400WithoutEcho() {
    when(adaptor.listChildren(eq("drop"), eq("../etc/passwd")))
        .thenThrow(new IllegalArgumentException("Invalid path"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.listChildren("drop", "../etc/passwd"));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("Invalid path", ex.getMessage());
    assertFalse(ex.getMessage().contains("etc"));
    assertFalse(ex.getMessage().contains("passwd"));
  }

  @Test
  public void listChildrenIllegalArgumentWithRawPathIsSanitized() {
    when(adaptor.listChildren(any(), any()))
        .thenThrow(new IllegalArgumentException("escaped C:\\Windows\\system32"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.listChildren("drop", "x"));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(FileExplorerResource.INVALID_PATH, ex.getMessage());
    assertFalse(ex.getMessage().contains("Windows"));
  }

  @Test
  public void listChildrenWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("down");
    when(adaptor.listChildren(eq("drop"), isNull())).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listChildren("drop", null));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void looksLikeRawPathIsPathShapedNotBareSlashInProse() {
    assertTrue(FileExplorerResource.looksLikeRawPath("/etc/passwd"));
    assertTrue(FileExplorerResource.looksLikeRawPath("C:\\Windows\\secret"));
    assertTrue(FileExplorerResource.looksLikeRawPath("a/../b"));
    assertFalse(FileExplorerResource.looksLikeRawPath("Invalid path"));
    assertFalse(FileExplorerResource.looksLikeRawPath("See /admin for details"));
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnListChildren() {
    FileExplorerResource bare = new FileExplorerResource();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> bare.listChildren("drop", null));
    assertEquals(503, ex.getResponse().getStatus());
    verify(adaptor, never()).listChildren(any(), any());
  }
}
