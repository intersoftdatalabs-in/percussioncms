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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.apibridge.FileExplorerAdaptor.ConfiguredRoot;
import com.percussion.rest.fileexplorer.FileExplorerEntry;
import com.percussion.rest.fileexplorer.FileExplorerRoot;
import com.percussion.server.PSServer;
import com.percussion.util.PSProperties;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Path-safe File Explorer browse: allow-listed roots only; unsafe relative paths never walk the
 * filesystem and never echo raw paths.
 */
@Tag("UnitTest")
class FileExplorerAdaptorTest {

  @TempDir Path tempDir;

  private Path rootDir;
  private Path outsideDir;
  private FileExplorerAdaptor adaptor;
  private PSProperties previousProps;
  private Field propsField;

  @BeforeEach
  void setUp() throws Exception {
    rootDir = tempDir.resolve("allow");
    outsideDir = tempDir.resolve("outside");
    Files.createDirectories(rootDir);
    Files.createDirectories(outsideDir);
    Files.writeString(rootDir.resolve("readme.txt"), "hello", StandardCharsets.UTF_8);
    Files.createDirectories(rootDir.resolve("sub"));
    Files.writeString(rootDir.resolve("sub").resolve("nested.txt"), "n", StandardCharsets.UTF_8);
    Files.writeString(outsideDir.resolve("secret.txt"), "nope", StandardCharsets.UTF_8);

    Map<String, ConfiguredRoot> roots = new LinkedHashMap<>();
    roots.put("drop", new ConfiguredRoot("drop", "drop", rootDir));
    adaptor = new FileExplorerAdaptor(() -> roots, () -> true);

    propsField = PSServer.class.getDeclaredField("ms_serverProps");
    propsField.setAccessible(true);
    previousProps = (PSProperties) propsField.get(null);
    propsField.set(null, new PSProperties());
  }

  @AfterEach
  void restoreServerProps() throws Exception {
    if (propsField != null) {
      propsField.set(null, previousProps);
    }
  }

  @Test
  void listRoots_returnsIdWithoutFilesystemPath() {
    List<FileExplorerRoot> roots = adaptor.listRoots();
    assertEquals(1, roots.size());
    assertEquals("drop", roots.get(0).getId());
    assertEquals(Boolean.TRUE, roots.get(0).getExists());
    assertFalse(String.valueOf(roots.get(0).getId()).contains(rootDir.toString()));
  }

  @Test
  void listRoots_nonAdminIs403() {
    Map<String, ConfiguredRoot> roots = new LinkedHashMap<>();
    roots.put("drop", new ConfiguredRoot("drop", "drop", rootDir));
    adaptor = new FileExplorerAdaptor(() -> roots, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.listRoots());
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(FileExplorerAdaptor.ADMIN_REQUIRED, ex.getMessage());
  }

  @Test
  void listChildren_listsImmediateEntries() {
    List<FileExplorerEntry> kids = adaptor.listChildren("drop", "");
    assertNotNull(kids);
    assertEquals(2, kids.size());
    assertEquals("sub", kids.get(0).getName());
    assertEquals(Boolean.TRUE, kids.get(0).getDirectory());
    assertEquals("readme.txt", kids.get(1).getName());
    assertEquals("readme.txt", kids.get(1).getRelativePath());
    assertEquals(Boolean.FALSE, kids.get(1).getDirectory());
    assertEquals(5L, kids.get(1).getSize());
  }

  @Test
  void listChildren_nestedRelativePath() {
    List<FileExplorerEntry> kids = adaptor.listChildren("drop", "sub");
    assertNotNull(kids);
    assertEquals(1, kids.size());
    assertEquals("nested.txt", kids.get(0).getName());
    assertEquals("sub/nested.txt", kids.get(0).getRelativePath());
  }

  @Test
  void listChildren_unknownRootIsNull() {
    assertNull(adaptor.listChildren("not_configured", ""));
  }

  @Test
  void listChildren_missingDirIsNull() {
    assertNull(adaptor.listChildren("drop", "no-such-dir"));
  }

  @Test
  void listChildren_parentTraversalIs400WithoutEcho() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.listChildren("drop", "../outside"));
    assertEquals(FileExplorerAdaptor.INVALID_PATH, ex.getMessage());
    assertFalse(ex.getMessage().contains("outside"));
    assertFalse(ex.getMessage().contains(".."));
  }

  @Test
  void listChildren_absoluteUnixIs400() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.listChildren("drop", "/etc/passwd"));
    assertEquals(FileExplorerAdaptor.INVALID_PATH, ex.getMessage());
    assertFalse(ex.getMessage().contains("passwd"));
  }

  @Test
  void listChildren_driveLetterIs400() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.listChildren("drop", "C:\\Windows\\system32"));
    assertEquals(FileExplorerAdaptor.INVALID_PATH, ex.getMessage());
    assertFalse(ex.getMessage().contains("Windows"));
  }

  @Test
  void listChildren_uncIs400() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.listChildren("drop", "\\\\server\\share"));
    assertEquals(FileExplorerAdaptor.INVALID_PATH, ex.getMessage());
  }

  @Test
  void listChildren_unsafeRootIdIs400() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.listChildren("../drop", ""));
    assertEquals(FileExplorerAdaptor.INVALID_ROOT, ex.getMessage());
    assertFalse(ex.getMessage().contains(".."));
  }

  @Test
  void listChildren_nonAdminIs403AndDoesNotDependOnPath() {
    Map<String, ConfiguredRoot> roots = new LinkedHashMap<>();
    roots.put("drop", new ConfiguredRoot("drop", "drop", rootDir));
    adaptor = new FileExplorerAdaptor(() -> roots, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.listChildren("drop", "readme.txt"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void normalizeSafeRelativePath_rejectsTraversalAndAbsolute() {
    assertEquals("", FileExplorerAdaptor.normalizeSafeRelativePath(null));
    assertEquals("", FileExplorerAdaptor.normalizeSafeRelativePath("  "));
    assertEquals("a/b.txt", FileExplorerAdaptor.normalizeSafeRelativePath("a/b.txt"));
    assertNull(FileExplorerAdaptor.normalizeSafeRelativePath(".."));
    assertNull(FileExplorerAdaptor.normalizeSafeRelativePath("a/../b"));
    assertNull(FileExplorerAdaptor.normalizeSafeRelativePath("/abs"));
    assertNull(FileExplorerAdaptor.normalizeSafeRelativePath("C:/Windows"));
    assertNull(FileExplorerAdaptor.normalizeSafeRelativePath("//server/share"));
    assertNull(FileExplorerAdaptor.normalizeSafeRelativePath("a\\..\\b"));
  }

  @Test
  void parseAllowListedRoots_relativeResolvedAgainstRxDir() {
    Path rx = tempDir.resolve("install");
    Map<String, ConfiguredRoot> parsed =
        FileExplorerAdaptor.parseAllowListedRoots("rx_resources=rx_resources", rx);
    assertEquals(1, parsed.size());
    assertEquals(rx.resolve("rx_resources").toAbsolutePath().normalize(), parsed.get("rx_resources").directory());
  }

  @Test
  void parseAllowListedRoots_skipsTraversalAndBadIds() {
    Path rx = tempDir.resolve("install");
    Map<String, ConfiguredRoot> parsed =
        FileExplorerAdaptor.parseAllowListedRoots(
            "../x=foo;bad/id=bar;ok=rx_resources;empty=", rx);
    assertEquals(1, parsed.size());
    assertTrue(parsed.containsKey("ok"));
  }

  @Test
  void parseAllowListedRoots_emptySpec() {
    assertTrue(FileExplorerAdaptor.parseAllowListedRoots(null, tempDir).isEmpty());
    assertTrue(FileExplorerAdaptor.parseAllowListedRoots("  ", tempDir).isEmpty());
  }

  @Test
  void loadRootsFromServerProperties_readsPsProperties() throws Exception {
    Path rx = tempDir.resolve("cms-home");
    Files.createDirectories(rx.resolve("rx_resources"));
    PSProperties props = (PSProperties) propsField.get(null);
    props.setProperty(
        FileExplorerAdaptor.PROP_ALLOW_LISTED_ROOTS, "rx_resources=rx_resources");
    Map<String, ConfiguredRoot> parsed =
        FileExplorerAdaptor.parseAllowListedRoots(
            PSServer.getProperty(FileExplorerAdaptor.PROP_ALLOW_LISTED_ROOTS), rx);
    assertEquals(1, parsed.size());
    assertEquals("rx_resources", parsed.get("rx_resources").id());
  }

  @Test
  void resolveUnderRoot_staysInside() {
    Path resolved = FileExplorerAdaptor.resolveUnderRoot(rootDir, "sub");
    assertNotNull(resolved);
    assertTrue(resolved.startsWith(rootDir.toAbsolutePath().normalize()));
  }

  @Test
  void isSafeRootId() {
    assertTrue(FileExplorerAdaptor.isSafeRootId("drop"));
    assertTrue(FileExplorerAdaptor.isSafeRootId("rx_resources"));
    assertFalse(FileExplorerAdaptor.isSafeRootId(""));
    assertFalse(FileExplorerAdaptor.isSafeRootId("../x"));
    assertFalse(FileExplorerAdaptor.isSafeRootId("a/b"));
    assertFalse(FileExplorerAdaptor.isSafeRootId("1bad"));
  }
}
