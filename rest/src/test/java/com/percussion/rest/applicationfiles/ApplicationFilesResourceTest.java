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

package com.percussion.rest.applicationfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.ObjectLockSummary;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class ApplicationFilesResourceTest {

  private IApplicationFileAdaptor adaptor;
  private ApplicationFilesResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IApplicationFileAdaptor.class);
    resource = new ApplicationFilesResource(adaptor);
  }

  @Test
  public void listFilesDelegates() {
    ApplicationFileSummary s = new ApplicationFileSummary();
    s.setPath("ApplicationFiles/style.css");
    when(adaptor.listFiles(eq("sys_resources"))).thenReturn(List.of(s));

    List<ApplicationFileSummary> out = resource.listFiles("sys_resources");
    assertEquals(1, out.size());
    assertEquals("ApplicationFiles/style.css", out.get(0).getPath());
    verify(adaptor).listFiles("sys_resources");
  }

  @Test
  public void listFilesUnknownAppIsGeneric404() {
    when(adaptor.listFiles(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listFiles("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Application not found", ex.getMessage());
  }

  @Test
  public void listFilesWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listFiles(eq("sys_resources"))).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listFiles("sys_resources"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void getFileDelegates() {
    ApplicationFileSummary s = new ApplicationFileSummary();
    s.setPath("ApplicationFiles/a.txt");
    s.setContent("hello");
    when(adaptor.getFile(eq("sys_resources"), eq("ApplicationFiles/a.txt"))).thenReturn(s);

    ApplicationFileSummary out = resource.getFile("sys_resources", "ApplicationFiles/a.txt");
    assertEquals("hello", out.getContent());
    verify(adaptor).getFile("sys_resources", "ApplicationFiles/a.txt");
  }

  @Test
  public void getFileNotFoundIsGeneric404() {
    when(adaptor.getFile(eq("sys_resources"), eq("../escape"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getFile("sys_resources", "../escape"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Application file not found", ex.getMessage());
  }

  @Test
  public void putFileDelegates() {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("updated");
    ApplicationFileSummary saved = new ApplicationFileSummary();
    saved.setPath("ApplicationFiles/a.txt");
    saved.setContent("updated");
    when(adaptor.putFile(eq("sys_resources"), eq("ApplicationFiles/a.txt"), eq(body)))
        .thenReturn(saved);

    ApplicationFileSummary out = resource.putFile("sys_resources", "ApplicationFiles/a.txt", body);
    assertEquals("updated", out.getContent());
    verify(adaptor).putFile("sys_resources", "ApplicationFiles/a.txt", body);
  }

  @Test
  public void putFileNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.putFile("sys_resources", "ApplicationFiles/a.txt", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).putFile(eq("sys_resources"), eq("ApplicationFiles/a.txt"), isNull());
  }

  @Test
  public void putFileNullPathIs400() {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.putFile("sys_resources", null, body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("path is required", ex.getMessage());
    verify(adaptor, never()).putFile(eq("sys_resources"), isNull(), eq(body));
  }

  @Test
  public void putFileBlankPathIs400() {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.putFile("sys_resources", "  ", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("path is required", ex.getMessage());
    verify(adaptor, never()).putFile(eq("sys_resources"), eq("  "), eq(body));
  }

  @Test
  public void putFileUnknownIsGeneric404() {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    when(adaptor.putFile(eq("sys_resources"), eq("nope.txt"), eq(body))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.putFile("sys_resources", "nope.txt", body));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Application file not found", ex.getMessage());
  }

  @Test
  public void putFileRethrowsAdaptor403() {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    WebApplicationException mapped = new WebApplicationException("Admin role required", 403);
    when(adaptor.putFile(eq("sys_resources"), eq("a.txt"), eq(body))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.putFile("sys_resources", "a.txt", body));
    assertSame(mapped, ex);
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnList() {
    ApplicationFilesResource bare = new ApplicationFilesResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.listFiles("any"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnGet() {
    ApplicationFilesResource bare = new ApplicationFilesResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.getFile("any", "a.txt"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void mapWriteFailurePreservesWebApplicationException() {
    WebApplicationException mapped = new WebApplicationException("x", 403);
    assertSame(mapped, ApplicationFilesResource.mapWriteFailure(mapped));
  }

  @Test
  public void mapWriteFailureMapsIllegalArgumentTo400() {
    WebApplicationException ex =
        ApplicationFilesResource.mapWriteFailure(new IllegalArgumentException("bad"));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("bad", ex.getMessage());
  }

  @Test
  public void createFolderDelegates() {
    ApplicationFileSummary created = new ApplicationFileSummary();
    created.setPath("ApplicationFiles/newdir");
    created.setDirectory(true);
    when(adaptor.createFolder(eq("sys_resources"), eq("ApplicationFiles/newdir")))
        .thenReturn(created);

    ApplicationFileSummary out = resource.createFolder("sys_resources", "ApplicationFiles/newdir");
    assertEquals("ApplicationFiles/newdir", out.getPath());
    verify(adaptor).createFolder("sys_resources", "ApplicationFiles/newdir");
  }

  @Test
  public void createFolderBlankPathIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createFolder("sys_resources", "  "));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(ApplicationFilesResource.PATH_REQUIRED, ex.getMessage());
    verify(adaptor, never()).createFolder(eq("sys_resources"), eq("  "));
  }

  @Test
  public void createFolderUnsafeFromAdaptorIs400() {
    when(adaptor.createFolder(eq("sys_resources"), eq("../escape")))
        .thenThrow(new IllegalArgumentException("Invalid path"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createFolder("sys_resources", "../escape"));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("Invalid path", ex.getMessage());
  }

  @Test
  public void createFolderUnknownAppIs404() {
    when(adaptor.createFolder(eq("missing"), eq("ApplicationFiles/x"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createFolder("missing", "ApplicationFiles/x"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals(ApplicationFilesResource.APP_NOT_FOUND, ex.getMessage());
  }

  @Test
  public void createFolderRethrowsAdaptor403() {
    WebApplicationException mapped = new WebApplicationException("Admin role required", 403);
    when(adaptor.createFolder(eq("sys_resources"), eq("ApplicationFiles/x"))).thenThrow(mapped);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createFolder("sys_resources", "ApplicationFiles/x"));
    assertSame(mapped, ex);
  }

  @Test
  public void deletePathDelegates204() {
    when(adaptor.deletePath(eq("sys_resources"), eq("ApplicationFiles/a.txt"))).thenReturn(true);
    Response out = resource.deletePath("sys_resources", "ApplicationFiles/a.txt");
    assertEquals(204, out.getStatus());
    verify(adaptor).deletePath("sys_resources", "ApplicationFiles/a.txt");
  }

  @Test
  public void deletePathUnknownIs404() {
    when(adaptor.deletePath(eq("sys_resources"), eq("nope.txt"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deletePath("sys_resources", "nope.txt"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals(ApplicationFilesResource.FILE_NOT_FOUND, ex.getMessage());
  }

  @Test
  public void deletePathBlankIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deletePath("sys_resources", ""));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).deletePath(eq("sys_resources"), eq(""));
  }

  @Test
  public void movePathDelegates() {
    ApplicationFileMove body = new ApplicationFileMove();
    body.setFromPath("ApplicationFiles/a.txt");
    body.setToPath("ApplicationFiles/b.txt");
    ApplicationFileSummary moved = new ApplicationFileSummary();
    moved.setPath("ApplicationFiles/b.txt");
    when(adaptor.movePath(
            eq("sys_resources"), eq("ApplicationFiles/a.txt"), eq("ApplicationFiles/b.txt")))
        .thenReturn(moved);

    ApplicationFileSummary out = resource.movePath("sys_resources", body);
    assertEquals("ApplicationFiles/b.txt", out.getPath());
  }

  @Test
  public void movePathNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.movePath("sys_resources", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).movePath(eq("sys_resources"), isNull(), isNull());
  }

  @Test
  public void movePathMissingToIs400() {
    ApplicationFileMove body = new ApplicationFileMove();
    body.setFromPath("ApplicationFiles/a.txt");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.movePath("sys_resources", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(ApplicationFilesResource.TO_PATH_REQUIRED, ex.getMessage());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnCreateFolder() {
    ApplicationFilesResource bare = new ApplicationFilesResource();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> bare.createFolder("any", "ApplicationFiles/x"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void lockFileDelegates() {
    ObjectLockSummary summary = new ObjectLockSummary();
    summary.setLocker("Admin");
    when(adaptor.lockFile(eq("sys_resources"), eq("ApplicationFiles/a.txt"))).thenReturn(summary);
    ObjectLockSummary out = resource.lockFile("sys_resources", "ApplicationFiles/a.txt");
    assertEquals("Admin", out.getLocker());
    verify(adaptor).lockFile("sys_resources", "ApplicationFiles/a.txt");
  }

  @Test
  public void lockFileBlankPathIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.lockFile("sys_resources", "  "));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).lockFile(eq("sys_resources"), eq("  "));
  }

  @Test
  public void lockFileUnknownIs404() {
    when(adaptor.lockFile(eq("sys_resources"), eq("missing.txt"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.lockFile("sys_resources", "missing.txt"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void unlockFileDelegates204() {
    when(adaptor.unlockFile(eq("sys_resources"), eq("ApplicationFiles/a.txt")))
        .thenReturn(Boolean.TRUE);
    Response out = resource.unlockFile("sys_resources", "ApplicationFiles/a.txt");
    assertEquals(204, out.getStatus());
  }

  @Test
  public void unlockFileUnknownIs404() {
    when(adaptor.unlockFile(eq("sys_resources"), eq("missing.txt"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.unlockFile("sys_resources", "missing.txt"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void putFileRethrowsAdaptor409() {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    WebApplicationException mapped = new WebApplicationException("Design lock required", 409);
    when(adaptor.putFile(eq("sys_resources"), eq("a.txt"), eq(body))).thenThrow(mapped);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.putFile("sys_resources", "a.txt", body));
    assertSame(mapped, ex);
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void getFileBytesDelegates200WithRawEntity() {
    byte[] raw = new byte[] {0x00, 0x01, 0x7f, 0x2a};
    when(adaptor.getFileBytes(eq("sys_resources"), eq("blobs/img.bin"))).thenReturn(raw);

    Response out = resource.getFileBytes("sys_resources", "blobs/img.bin");
    assertEquals(200, out.getStatus());
    assertSame(raw, out.getEntity());
    verify(adaptor).getFileBytes("sys_resources", "blobs/img.bin");
  }

  @Test
  public void getFileBytesBlankPathIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getFileBytes("sys_resources", "  "));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(ApplicationFilesResource.PATH_REQUIRED, ex.getMessage());
    verify(adaptor, never()).getFileBytes(eq("sys_resources"), eq("  "));
  }

  @Test
  public void getFileBytesUnsafeFromAdaptorIs400() {
    when(adaptor.getFileBytes(eq("sys_resources"), eq("../escape")))
        .thenThrow(new IllegalArgumentException("Invalid path"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getFileBytes("sys_resources", "../escape"));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("Invalid path", ex.getMessage());
  }

  @Test
  public void getFileBytesUnknownIs404() {
    when(adaptor.getFileBytes(eq("sys_resources"), eq("missing.bin"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.getFileBytes("sys_resources", "missing.bin"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Application file not found", ex.getMessage());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnGetBinary() {
    ApplicationFilesResource bare = new ApplicationFilesResource();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> bare.getFileBytes("any", "a.bin"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void putFileBytesDelegates() {
    byte[] raw = new byte[] {0x00, 0x01, 0x02};
    ApplicationFileSummary saved = new ApplicationFileSummary();
    saved.setPath("blobs/img.bin");
    saved.setBinary(true);
    saved.setContentLength(3L);
    when(adaptor.putFileBytes(eq("sys_resources"), eq("blobs/img.bin"), eq(raw)))
        .thenReturn(saved);

    ApplicationFileSummary out = resource.putFileBytes("sys_resources", "blobs/img.bin", raw);
    assertEquals(Boolean.TRUE, out.getBinary());
    verify(adaptor).putFileBytes("sys_resources", "blobs/img.bin", raw);
  }

  @Test
  public void putFileBytesNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.putFileBytes("sys_resources", "a.bin", null));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(ApplicationFilesResource.BODY_REQUIRED, ex.getMessage());
    verify(adaptor, never()).putFileBytes(eq("sys_resources"), eq("a.bin"), isNull());
  }

  @Test
  public void putFileBytesBlankPathIs400() {
    byte[] raw = new byte[] {0x01};
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.putFileBytes("sys_resources", " ", raw));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(ApplicationFilesResource.PATH_REQUIRED, ex.getMessage());
    verify(adaptor, never()).putFileBytes(eq("sys_resources"), eq(" "), eq(raw));
  }

  @Test
  public void putFileBytesUnsafeFromAdaptorIs400() {
    byte[] raw = new byte[] {0x01};
    when(adaptor.putFileBytes(eq("sys_resources"), eq("../escape"), eq(raw)))
        .thenThrow(new IllegalArgumentException("Invalid path"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.putFileBytes("sys_resources", "../escape", raw));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void putFileBytesUnknownIs404() {
    byte[] raw = new byte[] {0x01};
    when(adaptor.putFileBytes(eq("sys_resources"), eq("missing.bin"), eq(raw))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.putFileBytes("sys_resources", "missing.bin", raw));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void putFileBytesRethrowsAdaptor403() {
    byte[] raw = new byte[] {0x01};
    WebApplicationException mapped = new WebApplicationException("Admin role required", 403);
    when(adaptor.putFileBytes(eq("sys_resources"), eq("a.bin"), eq(raw))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.putFileBytes("sys_resources", "a.bin", raw));
    assertSame(mapped, ex);
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void putFileBytesRethrowsAdaptor409() {
    byte[] raw = new byte[] {0x01};
    WebApplicationException mapped = new WebApplicationException("Design lock required", 409);
    when(adaptor.putFileBytes(eq("sys_resources"), eq("a.bin"), eq(raw))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.putFileBytes("sys_resources", "a.bin", raw));
    assertSame(mapped, ex);
    assertEquals(409, ex.getResponse().getStatus());
  }
}
