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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.server.PSApplicationSummary;
import com.percussion.error.PSNotFoundException;
import com.percussion.rest.applicationfiles.ApplicationFileSummary;
import com.percussion.security.PSSecurityToken;
import com.percussion.utils.io.PathUtils;
import jakarta.ws.rs.WebApplicationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * SY-05 path-safe application CMS/resource list/get/put. Catalog allow-list + relative-path
 * barriers never reach object-store write for unsafe input.
 */
@Tag("UnitTest")
class ApplicationFileAdaptorTest {

  private ApplicationFileAdaptor.ApplicationFileStore fileStore;
  private PSSecurityToken token;
  private ApplicationFileAdaptor adaptor;
  private final AtomicReference<String> savedContent = new AtomicReference<>();
  private final AtomicReference<byte[]> savedBytes = new AtomicReference<>();

  @TempDir Path rxTmp;

  @AfterEach
  void clearThreadRxDir() {
    PathUtils.unsetThreadOnlyRxDir(rxTmp.toFile());
  }

  @BeforeEach
  void setUp() throws Exception {
    fileStore = mock(ApplicationFileAdaptor.ApplicationFileStore.class);
    token = mock(PSSecurityToken.class);
    savedContent.set(null);
    savedBytes.set(null);

    PSApplicationSummary sum = mock(PSApplicationSummary.class);
    when(sum.getId()).thenReturn(42);
    when(sum.getName()).thenReturn("sys_resources");
    when(sum.getAppRoot()).thenReturn("sys_resources");

    adaptor =
        new ApplicationFileAdaptor(
            tok -> new PSApplicationSummary[] {sum}, fileStore, () -> true, () -> token);

    when(fileStore.listFiles(eq("sys_resources"), any()))
        .thenReturn(
            List.of(
                    new File("ApplicationFiles" + File.separator + "a.css"),
                    new File("ApplicationFiles" + File.separator + "b.js"))
                .iterator());

    when(fileStore.read(eq("sys_resources"), any(File.class), eq(token)))
        .thenAnswer(
            inv -> {
              String text = savedContent.get() != null ? savedContent.get() : "original";
              return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
            });

    org.mockito.Mockito.doAnswer(
            inv -> {
              InputStream in = inv.getArgument(2);
              byte[] raw = in.readAllBytes();
              savedBytes.set(raw);
              savedContent.set(new String(raw, StandardCharsets.UTF_8));
              return null;
            })
        .when(fileStore)
        .write(
            eq("sys_resources"),
            any(File.class),
            any(InputStream.class),
            anyBoolean(),
            eq(token),
            any());
  }

  @Test
  void list_mapsRelativePathsWithForwardSlash() {
    List<ApplicationFileSummary> out = adaptor.listFiles("sys_resources");
    assertNotNull(out);
    assertEquals(2, out.size());
    assertEquals("ApplicationFiles/a.css", out.get(0).getPath());
    assertEquals("ApplicationFiles/b.js", out.get(1).getPath());
    assertEquals("sys_resources", out.get(0).getApplicationName());
    assertNull(out.get(0).getDesignGaps());
  }

  @Test
  void list_unknownAppIsNull() {
    assertNull(adaptor.listFiles("no_such_app"));
    assertNull(adaptor.listFiles("../escape"));
  }

  @Test
  void list_directoryFlagsResolvedAgainstStoreNotCwd() throws Exception {
    // Store yields relative Files; the adaptor must ask the store (app-root-aware) whether each
    // entry is a directory, instead of File.isDirectory() which would resolve against the JVM CWD
    // and misreport real directories as files.
    when(fileStore.listFiles(eq("sys_resources"), any()))
        .thenReturn(
            List.of(
                    new File("ApplicationFiles" + File.separator + "subdir"),
                    new File("ApplicationFiles" + File.separator + "a.css"))
                .iterator());
    when(fileStore.isDirectory(eq("sys_resources"), eq("sys_resources"), any()))
        .thenAnswer(
            inv -> {
              File f = inv.getArgument(2);
              return f.getPath().equals("ApplicationFiles" + File.separator + "subdir");
            });
    List<ApplicationFileSummary> out = adaptor.listFiles("sys_resources");
    assertEquals(2, out.size());
    ApplicationFileSummary dir =
        out.stream().filter(s -> s.getPath().equals("ApplicationFiles/subdir")).findFirst().orElseThrow();
    ApplicationFileSummary file =
        out.stream().filter(s -> s.getPath().equals("ApplicationFiles/a.css")).findFirst().orElseThrow();
    assertTrue(Boolean.TRUE.equals(dir.getDirectory()));
    assertFalse(Boolean.TRUE.equals(file.getDirectory()));
  }

  @Test
  void get_roundTripsUtf8Content() {
    ApplicationFileSummary out = adaptor.getFile("sys_resources", "ApplicationFiles/a.css");
    assertNotNull(out);
    assertEquals("original", out.getContent());
    assertEquals("ApplicationFiles/a.css", out.getPath());
    assertEquals("text/css", out.getMimeType());
    assertNotNull(out.getDesignGaps());
    assertTrue(out.getDesignGaps().isEmpty());
  }

  @Test
  void get_rejectsUnsafePathsWithoutRead() throws Exception {
    assertNull(adaptor.getFile("sys_resources", "../escape.txt"));
    assertNull(adaptor.getFile("sys_resources", "/etc/passwd"));
    assertNull(adaptor.getFile("sys_resources", "C:\\Windows\\win.ini"));
    assertNull(adaptor.getFile("sys_resources", "a/../../b.txt"));
    assertNull(adaptor.getFile("sys_resources", ""));
    verify(fileStore, never()).read(any(), any(), any());
  }

  @Test
  void put_createsMissingRelativePath() throws Exception {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("/* created */");
    adaptor.lockFile("sys_resources", "ApplicationFiles/created.css");
    ApplicationFileSummary out =
        adaptor.putFile("sys_resources", "ApplicationFiles/created.css", body);
    assertNotNull(out);
    assertEquals("/* created */", out.getContent());
    assertEquals("ApplicationFiles/created.css", out.getPath());
    assertEquals("/* created */", savedContent.get());
    verify(fileStore)
        .write(
            eq("sys_resources"),
            any(File.class),
            any(InputStream.class),
            eq(true),
            eq(token),
            any());
  }

  @Test
  void put_directoryTargetIs409AndDoesNotWrite() throws Exception {
    when(fileStore.exists(eq("sys_resources"), any(File.class))).thenReturn(true);
    when(fileStore.isDirectory(eq("sys_resources"), eq("sys_resources"), any(File.class)))
        .thenReturn(true);
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    adaptor.lockFile("sys_resources", "ApplicationFiles");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.putFile("sys_resources", "ApplicationFiles", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  void put_savesAllowListedPathAndRoundTrips() throws Exception {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("body{color:red}");
    // Body path must not drive persistence
    body.setPath("../../evil.css");

    adaptor.lockFile("sys_resources", "ApplicationFiles/a.css");
    ApplicationFileSummary out =
        adaptor.putFile("sys_resources", "ApplicationFiles/a.css", body);

    assertNotNull(out);
    assertEquals("body{color:red}", out.getContent());
    assertEquals("body{color:red}", savedContent.get());
    assertEquals("ApplicationFiles/a.css", out.getPath());
    assertNotNull(out.getLock());
    assertEquals("Admin", out.getLock().getLocker());
    verify(fileStore)
        .write(
            eq("sys_resources"),
            any(File.class),
            any(InputStream.class),
            eq(true),
            eq(token),
            any());
  }

  @Test
  void put_withoutLockIs409() throws Exception {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.putFile("sys_resources", "ApplicationFiles/a.css", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  void lock_unlock_roundTripsOwnerOnGet() {
    assertNull(adaptor.getFile("sys_resources", "ApplicationFiles/a.css").getLock());
    var summary = adaptor.lockFile("sys_resources", "ApplicationFiles/a.css");
    assertEquals("Admin", summary.getLocker());
    assertEquals("test-session", summary.getSession());
    assertEquals("Admin", adaptor.getFile("sys_resources", "ApplicationFiles/a.css").getLock().getLocker());
    assertTrue(adaptor.unlockFile("sys_resources", "ApplicationFiles/a.css"));
    assertNull(adaptor.getFile("sys_resources", "ApplicationFiles/a.css").getLock());
  }

  @Test
  void lock_unknownAppIsNull() {
    assertNull(adaptor.lockFile("no_such_app", "ApplicationFiles/a.css"));
    assertNull(adaptor.lockFile("sys_resources", "../escape.css"));
  }

  @Test
  void lock_otherUserIs409AndPutIs409() {
    ApplicationFileAdaptor.ApplicationDesignLockStore shared =
        new ApplicationFileAdaptor.InMemoryApplicationDesignLockStore();
    ApplicationFileAdaptor other =
        new ApplicationFileAdaptor(
            tok -> {
              PSApplicationSummary sum = mock(PSApplicationSummary.class);
              when(sum.getName()).thenReturn("sys_resources");
              when(sum.getId()).thenReturn(42);
              when(sum.getAppRoot()).thenReturn("sys_resources");
              return new PSApplicationSummary[] {sum};
            },
            fileStore,
            () -> true,
            () -> token,
            shared,
            () -> "other-session",
            () -> "editor");
    adaptor =
        new ApplicationFileAdaptor(
            tok -> {
              PSApplicationSummary sum = mock(PSApplicationSummary.class);
              when(sum.getName()).thenReturn("sys_resources");
              when(sum.getId()).thenReturn(42);
              when(sum.getAppRoot()).thenReturn("sys_resources");
              return new PSApplicationSummary[] {sum};
            },
            fileStore,
            () -> true,
            () -> token,
            shared,
            () -> "test-session",
            () -> "Admin");
    adaptor.lockFile("sys_resources", "ApplicationFiles/a.css");
    WebApplicationException lockEx =
        assertThrows(
            WebApplicationException.class,
            () -> other.lockFile("sys_resources", "ApplicationFiles/a.css"));
    assertEquals(409, lockEx.getResponse().getStatus());
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("stolen");
    WebApplicationException putEx =
        assertThrows(
            WebApplicationException.class,
            () -> other.putFile("sys_resources", "ApplicationFiles/a.css", body));
    assertEquals(409, putEx.getResponse().getStatus());
  }

  @Test
  void lock_nonAdminIs403() {
    adaptor =
        new ApplicationFileAdaptor(
            tok -> {
              PSApplicationSummary sum = mock(PSApplicationSummary.class);
              when(sum.getName()).thenReturn("sys_resources");
              when(sum.getId()).thenReturn(42);
              return new PSApplicationSummary[] {sum};
            },
            fileStore,
            () -> false,
            () -> token);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.lockFile("sys_resources", "ApplicationFiles/a.css"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void put_pathTraversalNeverWrites() throws Exception {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    assertNull(adaptor.putFile("sys_resources", "../escape.txt", body));
    assertNull(adaptor.putFile("sys_resources", "a\\..\\b.txt", body));
    assertNull(adaptor.putFile("nope", "ApplicationFiles/a.css", body));
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  void put_nullContentIs400() throws Exception {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.putFile(
                    "sys_resources", "ApplicationFiles/a.css", new ApplicationFileSummary()));
    assertTrue(ex.getMessage().contains("content is required"));
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  void put_nonAdminIs403AndDoesNotWrite() throws Exception {
    adaptor =
        new ApplicationFileAdaptor(
            tok -> {
              PSApplicationSummary sum = mock(PSApplicationSummary.class);
              when(sum.getName()).thenReturn("sys_resources");
              when(sum.getId()).thenReturn(42);
              return new PSApplicationSummary[] {sum};
            },
            fileStore,
            () -> false,
            () -> token);
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.putFile("sys_resources", "ApplicationFiles/a.css", body));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(ApplicationFileAdaptor.ADMIN_REQUIRED, ex.getMessage());
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  void put_nullUserServiceFailsClosedAs403() throws Exception {
    // adminChecker null → isCurrentUserAdmin; userService field stays null → false.
    adaptor =
        new ApplicationFileAdaptor(
            tok -> {
              PSApplicationSummary sum = mock(PSApplicationSummary.class);
              when(sum.getName()).thenReturn("sys_resources");
              when(sum.getId()).thenReturn(42);
              return new PSApplicationSummary[] {sum};
            },
            fileStore,
            null,
            () -> token);
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.putFile("sys_resources", "ApplicationFiles/a.css", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  void toListSummary_setsDirectoryFlag() {
    ApplicationFileSummary file =
        ApplicationFileAdaptor.toListSummary("sys_resources", "ApplicationFiles/a.css", false);
    assertFalse(Boolean.TRUE.equals(file.getDirectory()));
    ApplicationFileSummary dir =
        ApplicationFileAdaptor.toListSummary("sys_resources", "ApplicationFiles", true);
    assertTrue(Boolean.TRUE.equals(dir.getDirectory()));
    assertEquals("ApplicationFiles", dir.getName());
  }

  @Test
  void normalizeSafeRelativePath_acceptsNestedRelative() {
    assertEquals(
        "ApplicationFiles/css/site.css",
        ApplicationFileAdaptor.normalizeSafeRelativePath("ApplicationFiles/css/site.css"));
    assertEquals(
        "ApplicationFiles/css/site.css",
        ApplicationFileAdaptor.normalizeSafeRelativePath("ApplicationFiles\\css\\site.css"));
  }

  @Test
  void normalizeSafeRelativePath_rejectsTraversalAndAbsolute() {
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath("../x"));
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath("a/../b"));
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath("/abs"));
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath("C:/Windows/x"));
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath(null));
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath(""));
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath("a/\0/b"));
  }

  @Test
  void isSafeApplicationName_rejectsSeparators() {
    assertTrue(ApplicationFileAdaptor.isSafeApplicationName("sys_resources"));
    assertFalse(ApplicationFileAdaptor.isSafeApplicationName("../x"));
    assertFalse(ApplicationFileAdaptor.isSafeApplicationName("a/b"));
    assertFalse(ApplicationFileAdaptor.isSafeApplicationName("a\\b"));
  }

  @Test
  void resolveApplicationName_usesCatalogNotRawInput() {
    PSApplicationSummary sum = mock(PSApplicationSummary.class);
    when(sum.getId()).thenReturn(7);
    when(sum.getName()).thenReturn("sys_resources");
    PSApplicationSummary[] sums = {sum};

    assertEquals(
        "sys_resources", ApplicationFileAdaptor.resolveApplicationName("SYS_RESOURCES", sums));
    assertEquals("sys_resources", ApplicationFileAdaptor.resolveApplicationName("7", sums));
    assertNull(ApplicationFileAdaptor.resolveApplicationName("other", sums));
    assertNull(ApplicationFileAdaptor.resolveApplicationName("../x", sums));
  }

  @Test
  void list_emptyIteratorIsEmptyListNotNull() throws Exception {
    when(fileStore.listFiles(eq("sys_resources"), any()))
        .thenReturn(
            new Iterator<>() {
              @Override
              public boolean hasNext() {
                return false;
              }

              @Override
              public File next() {
                return null;
              }
            });
    List<ApplicationFileSummary> out = adaptor.listFiles("sys_resources");
    assertNotNull(out);
    assertTrue(out.isEmpty());
  }

  @Test
  void createFolder_writesAllowListedPath() throws Exception {
    when(fileStore.exists(any(), any(File.class))).thenReturn(false);
    ApplicationFileSummary out = adaptor.createFolder("sys_resources", "ApplicationFiles/qa-dir");
    assertNotNull(out);
    assertEquals("ApplicationFiles/qa-dir", out.getPath());
    assertTrue(Boolean.TRUE.equals(out.getDirectory()));
    verify(fileStore).mkdir(eq("sys_resources"), any(File.class), eq(token));
  }

  @Test
  void createFolder_pathTraversalIs400AndDoesNotWrite() throws Exception {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.createFolder("sys_resources", "../escape"));
    assertEquals(ApplicationFileAdaptor.INVALID_PATH, ex.getMessage());
    verify(fileStore, never()).mkdir(any(), any(), any());
  }

  @Test
  void createFolder_unknownAppIsNull() throws Exception {
    assertNull(adaptor.createFolder("no_such_app", "ApplicationFiles/qa-dir"));
    verify(fileStore, never()).mkdir(any(), any(), any());
  }

  @Test
  void createFolder_nonAdminIs403() throws Exception {
    adaptor =
        new ApplicationFileAdaptor(
            tok -> {
              PSApplicationSummary sum = mock(PSApplicationSummary.class);
              when(sum.getName()).thenReturn("sys_resources");
              when(sum.getId()).thenReturn(42);
              when(sum.getAppRoot()).thenReturn("sys_resources");
              return new PSApplicationSummary[] {sum};
            },
            fileStore,
            () -> false,
            () -> token);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createFolder("sys_resources", "ApplicationFiles/qa-dir"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(fileStore, never()).mkdir(any(), any(), any());
  }

  @Test
  void createFolder_existingFileIs409() throws Exception {
    when(fileStore.exists(any(), any(File.class))).thenReturn(true);
    when(fileStore.isDirectory(any(), any(), any(File.class))).thenReturn(false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createFolder("sys_resources", "ApplicationFiles/a.css"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(fileStore, never()).mkdir(any(), any(), any());
  }

  @Test
  void deletePath_deletesAllowListedPath() throws Exception {
    when(fileStore.delete(eq("sys_resources"), any(), any(File.class), eq(token))).thenReturn(true);
    assertEquals(Boolean.TRUE, adaptor.deletePath("sys_resources", "ApplicationFiles/a.css"));
    verify(fileStore).delete(eq("sys_resources"), any(), any(File.class), eq(token));
  }

  @Test
  void deletePath_unknownIsNull() throws Exception {
    when(fileStore.delete(eq("sys_resources"), any(), any(File.class), eq(token))).thenReturn(false);
    assertNull(adaptor.deletePath("sys_resources", "ApplicationFiles/missing.txt"));
  }

  @Test
  void deletePath_unsafeIs400() throws Exception {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.deletePath("sys_resources", "/etc/passwd"));
    assertEquals(ApplicationFileAdaptor.INVALID_PATH, ex.getMessage());
    verify(fileStore, never()).delete(any(), any(), any(), any());
  }

  @Test
  void movePath_renamesAllowListedPath() throws Exception {
    when(fileStore.rename(eq("sys_resources"), any(), any(File.class), any(File.class), eq(token)))
        .thenReturn(true);
    when(fileStore.isDirectory(any(), any(), any(File.class))).thenReturn(false);
    ApplicationFileSummary out =
        adaptor.movePath(
            "sys_resources", "ApplicationFiles/a.css", "ApplicationFiles/renamed.css");
    assertNotNull(out);
    assertEquals("ApplicationFiles/renamed.css", out.getPath());
  }

  @Test
  void movePath_samePathIs400() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.movePath(
                    "sys_resources", "ApplicationFiles/a.css", "ApplicationFiles/a.css"));
    assertEquals(ApplicationFileAdaptor.SOURCE_IS_DESTINATION, ex.getMessage());
  }

  @Test
  void movePath_nestedFolderIs400() throws Exception {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.movePath(
                    "sys_resources", "ApplicationFiles/dir", "ApplicationFiles/dir/child"));
    assertEquals(ApplicationFileAdaptor.NESTED_MOVE, ex.getMessage());
    verify(fileStore, never()).rename(any(), any(), any(), any(), any());
  }

  @Test
  void movePath_unsafeToIs400() throws Exception {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.movePath("sys_resources", "ApplicationFiles/a.css", "../escape.css"));
    assertEquals(ApplicationFileAdaptor.INVALID_PATH, ex.getMessage());
    verify(fileStore, never()).rename(any(), any(), any(), any(), any());
  }

  @Test
  void nioRelativePath_resolvesSegmentsPortably() {
    Path p = ApplicationFileAdaptor.nioRelativePath("ApplicationFiles/css/site.css");
    assertEquals("site.css", p.getFileName().toString());
    assertEquals(3, p.getNameCount());
  }

  @Test
  void isNestedDestination_detectsDescendant() {
    assertTrue(
        ApplicationFileAdaptor.isNestedDestination("ApplicationFiles/dir", "ApplicationFiles/dir/x"));
    assertFalse(
        ApplicationFileAdaptor.isNestedDestination("ApplicationFiles/dir", "ApplicationFiles/dir2"));
  }

  @Test
  void requireSafeRelativePath_rejectsTraversal() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> ApplicationFileAdaptor.requireSafeRelativePath("a/../b"));
    assertEquals(ApplicationFileAdaptor.INVALID_PATH, ex.getMessage());
  }

  @Test
  void resolveUnderAppRoot_rejectsTraversalFileBeforeRxDirIo() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ApplicationFileAdaptor.resolveUnderAppRoot(
                    "sys_resources", new File(".." + File.separator + "escape")));
    assertEquals(ApplicationFileAdaptor.INVALID_PATH, ex.getMessage());
  }

  @Test
  void resolveUnderAppRoot_rejectsAbsoluteFile() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ApplicationFileAdaptor.resolveUnderAppRoot(
                    "sys_resources", new File(File.separator + "etc" + File.separator + "passwd")));
    assertEquals(ApplicationFileAdaptor.INVALID_PATH, ex.getMessage());
  }

  @Test
  void deleteRecursively_removesTreeUnderTempDir() throws Exception {
    Path root = Files.createTempDirectory("appfile-qa");
    Path child = root.resolve("sub").resolve("f.txt");
    Files.createDirectories(child.getParent());
    Files.writeString(child, "x");
    assertTrue(Files.exists(child));
    ApplicationFileAdaptor.deleteRecursively(root);
    assertFalse(Files.exists(root));
  }

  @Test
  void objectStore_existsIsDirectoryDeleteRenameStayUnderRxDir() throws Exception {
    PathUtils.setThreadOnlyRxDir(rxTmp.toFile());
    Path appFiles = rxTmp.resolve("sys_resources").resolve("ApplicationFiles");
    Files.createDirectories(appFiles);
    Path css = appFiles.resolve("a.css");
    Files.writeString(css, "body{}");

    ApplicationFileAdaptor.ObjectStoreApplicationFileStore store =
        new ApplicationFileAdaptor.ObjectStoreApplicationFileStore();
    File rel = Path.of("ApplicationFiles", "a.css").toFile();

    assertTrue(store.exists("sys_resources", rel));
    assertFalse(store.isDirectory("sys_resources", "sys_resources", rel));

    File destRel = Path.of("ApplicationFiles", "renamed.css").toFile();
    assertTrue(store.rename("sys_resources", "sys_resources", rel, destRel, token));
    assertTrue(Files.exists(appFiles.resolve("renamed.css")));
    assertTrue(Files.notExists(css));

    assertTrue(store.delete("sys_resources", "sys_resources", destRel, token));
    assertTrue(Files.notExists(appFiles.resolve("renamed.css")));
  }

  @Test
  void objectStore_existsRejectsTraversalBeforeIo() {
    PathUtils.setThreadOnlyRxDir(rxTmp.toFile());
    ApplicationFileAdaptor.ObjectStoreApplicationFileStore store =
        new ApplicationFileAdaptor.ObjectStoreApplicationFileStore();
    File escape = Path.of("..", "escape").toFile();
    Path outside = rxTmp.getParent().resolve("escape");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> store.exists("sys_resources", escape));
    assertEquals(ApplicationFileAdaptor.INVALID_PATH, ex.getMessage());
    assertTrue(Files.notExists(outside));
  }

  @Test
  void objectStore_renameRejectsEscapeDestination() throws Exception {
    PathUtils.setThreadOnlyRxDir(rxTmp.toFile());
    Path appFiles = rxTmp.resolve("sys_resources").resolve("ApplicationFiles");
    Files.createDirectories(appFiles);
    Files.writeString(appFiles.resolve("a.css"), "body{}");

    ApplicationFileAdaptor.ObjectStoreApplicationFileStore store =
        new ApplicationFileAdaptor.ObjectStoreApplicationFileStore();
    File from = Path.of("ApplicationFiles", "a.css").toFile();
    File to = Path.of("..", "escape.css").toFile();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> store.rename("sys_resources", "sys_resources", from, to, token));
    assertEquals(ApplicationFileAdaptor.INVALID_PATH, ex.getMessage());
    assertTrue(Files.exists(appFiles.resolve("a.css")));
  }

  @Test
  void get_marksBinaryWithoutManglingContent() throws Exception {
    byte[] raw = {(byte) 0x0a, (byte) 0xff, 0x00, 0x01, 0x2a};
    when(fileStore.read(eq("sys_resources"), any(File.class), eq(token)))
        .thenReturn(new ByteArrayInputStream(raw));
    ApplicationFileSummary out = adaptor.getFile("sys_resources", "blob.bin");
    assertNotNull(out);
    assertEquals(Boolean.TRUE, out.getBinary());
    assertNull(out.getContent());
    assertNull(out.getCharacterEncoding());
    assertEquals((long) raw.length, out.getContentLength());
    assertNotNull(out.getDesignGaps());
    assertTrue(out.getDesignGaps().stream().noneMatch(g -> g.contains("Binary")));
  }

  @Test
  void get_textDetailKeepsBinaryNullForMultibyteUtf8() throws Exception {
    when(fileStore.read(eq("sys_resources"), any(File.class), eq(token)))
        .thenReturn(new ByteArrayInputStream("héllo".getBytes(StandardCharsets.UTF_8)));
    ApplicationFileSummary out = adaptor.getFile("sys_resources", "site.css");
    assertNotNull(out);
    assertNull(out.getBinary());
    assertEquals("héllo", out.getContent());
    assertEquals(6L, out.getContentLength()); // bytes, not chars
  }

  @Test
  void getFileBytes_returnsExactBytes() throws Exception {
    byte[] raw = {0x00, (byte) 0xff, 0x2a, 0x09};
    when(fileStore.read(eq("sys_resources"), any(File.class), eq(token)))
        .thenReturn(new ByteArrayInputStream(raw));
    assertArrayEquals(raw, adaptor.getFileBytes("sys_resources", "blobs/raw.bin"));
  }

  @Test
  void getFileBytes_unsafePathThrows400BeforeAnyRead() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.getFileBytes("sys_resources", "../escape.bin"));
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.getFileBytes("sys_resources", "a/../b.bin"));
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.getFileBytes("sys_resources", "/etc/passwd"));
    verify(fileStore, never()).read(any(), any(), any());
  }

  @Test
  void getFileBytes_unknownAppIsNull() throws Exception {
    assertNull(adaptor.getFileBytes("no_such_app", "ApplicationFiles/a.css"));
    verify(fileStore, never()).read(any(), any(), any());
  }

  @Test
  void getFileBytes_missingFileIsNull() throws Exception {
    when(fileStore.read(eq("sys_resources"), any(File.class), eq(token)))
        .thenThrow(new PSNotFoundException("missing"));
    assertNull(adaptor.getFileBytes("sys_resources", "nope.bin"));
  }

  @Test
  void putFileBytes_savesExactBytesAndReturnsBinaryDetail() throws Exception {
    byte[] raw = {0x00, 0x01, (byte) 0xff, 0x2a};
    adaptor.lockFile("sys_resources", "blobs/raw.bin");
    ApplicationFileSummary out =
        adaptor.putFileBytes("sys_resources", "blobs/raw.bin", raw);
    assertNotNull(out);
    assertEquals(Boolean.TRUE, out.getBinary());
    assertNull(out.getContent());
    assertEquals((long) raw.length, out.getContentLength());
    assertNotNull(out.getLock());
    assertEquals("Admin", out.getLock().getLocker());
    assertArrayEquals(raw, savedBytes.get());
    verify(fileStore)
        .write(
            eq("sys_resources"),
            any(File.class),
            any(InputStream.class),
            eq(true),
            eq(token),
            any());
  }

  @Test
  void putFileBytes_textBodyReturnsTextDetailAndStoresBytes() throws Exception {
    byte[] raw = "héllo".getBytes(StandardCharsets.UTF_8);
    adaptor.lockFile("sys_resources", "ApplicationFiles/plain.txt");
    ApplicationFileSummary out =
        adaptor.putFileBytes("sys_resources", "ApplicationFiles/plain.txt", raw);
    assertNotNull(out);
    assertNull(out.getBinary());
    assertEquals("héllo", out.getContent());
    assertEquals("UTF-8", out.getCharacterEncoding());
    assertArrayEquals(raw, savedBytes.get());
  }

  @Test
  void putFileBytes_withoutLockIs409() throws Exception {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.putFileBytes("sys_resources", "ApplicationFiles/a.css", new byte[] {1}));
    assertEquals(409, ex.getResponse().getStatus());
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  void putFileBytes_unsafePathIs400() throws Exception {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.putFileBytes("sys_resources", "../escape.bin", new byte[] {1}));
    assertEquals(ApplicationFileAdaptor.INVALID_PATH, ex.getMessage());
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  void putFileBytes_nullBodyIs400() throws Exception {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.putFileBytes("sys_resources", "ApplicationFiles/a.bin", null));
    assertTrue(ex.getMessage().contains("body is required"));
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  void putFileBytes_nonAdminIs403AndDoesNotWrite() throws Exception {
    adaptor =
        new ApplicationFileAdaptor(
            tok -> {
              PSApplicationSummary sum = mock(PSApplicationSummary.class);
              when(sum.getName()).thenReturn("sys_resources");
              when(sum.getId()).thenReturn(42);
              return new PSApplicationSummary[] {sum};
            },
            fileStore,
            () -> false,
            () -> token);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.putFileBytes("sys_resources", "ApplicationFiles/a.bin", new byte[] {1}));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(ApplicationFileAdaptor.ADMIN_REQUIRED, ex.getMessage());
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  void putFileBytes_unknownAppIsNull() throws Exception {
    assertNull(adaptor.putFileBytes("no_such_app", "ApplicationFiles/a.bin", new byte[] {1}));
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  void isUtf8Text_distinguishesTextFromBinary() {
    assertTrue(ApplicationFileAdaptor.isUtf8Text("héllo".getBytes(StandardCharsets.UTF_8)));
    assertTrue(ApplicationFileAdaptor.isUtf8Text(new byte[0]));
    assertFalse(ApplicationFileAdaptor.isUtf8Text(null));
    assertFalse(ApplicationFileAdaptor.isUtf8Text(new byte[] {(byte) 0xc3, 0x28})); // malformed
    assertFalse(ApplicationFileAdaptor.isUtf8Text(new byte[] {0x61, 0x00, 0x62})); // NUL inside
    assertFalse(ApplicationFileAdaptor.isUtf8Text(new byte[] {(byte) 0xff})); // not utf-8
  }

  @Test
  void designGaps_noLongerClaimBinaryRoundTripGap() throws Exception {
    ApplicationFileSummary text = adaptor.getFile("sys_resources", "ApplicationFiles/a.css");
    assertNotNull(text.getDesignGaps());
    assertTrue(text.getDesignGaps().isEmpty());
    assertTrue(text.getDesignGaps().stream().noneMatch(g -> g.contains("Binary")));
    assertTrue(text.getDesignGaps().stream().noneMatch(g -> g.contains("create a new file")));
    when(fileStore.read(eq("sys_resources"), any(File.class), eq(token)))
        .thenReturn(new ByteArrayInputStream(new byte[] {(byte) 0xff, 0x00}));
    ApplicationFileSummary binary = adaptor.getFile("sys_resources", "raw.bin");
    assertNotNull(binary.getDesignGaps());
    assertTrue(binary.getDesignGaps().stream().noneMatch(g -> g.contains("Binary")));
  }
}
