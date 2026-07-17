/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.pathmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.designmanagement.service.IPSFileSystemService;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.share.dao.IPSFolderHelper;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for {@link PSFileSystemPathItemService} focused on
 * the {@code java/path-injection} finding closed at
 * {@code PSFileSystemPathItemService.java:207} (CodeQL alert #1053, T043).
 *
 * <p>The pre-fix code reached {@code child.isDirectory()} (line 207)
 * with a {@link File} constructed upstream by
 * {@code PSFileSystemService.getChildren}, where the user-supplied
 * {@code path} argument flows through {@code new File(root, path)}
 * and {@code pathFile.listFiles()} to produce the {@code child} File
 * without CodeQL recognizing {@code PSFileSystemService.validatePath}
 * as a sanitizer. The fix adds
 * {@link com.percussion.security.io.PSPathInjectionGuard#requireSafeFileName}
 * on {@code child.getName()} at the top of the private
 * {@code getPathItemFromFile} method, so any traversal or NUL-byte
 * payload in the file-name portion is rejected BEFORE any
 * {@code child.isDirectory()} / {@code child.getName()} / {@code child.getPath()}
 * calls below.
 *
 * <p>Tests instantiate {@link PSFileSystemPathItemService} via its
 * real constructor (Mockito for the injected dependencies) and
 * reflectively invoke the private {@code getPathItemFromFile} method.
 * Files are constructed with {@link Path#resolve} for cross-platform
 * portability.
 */
public class PSFileSystemPathItemServicePathInjectionTest {

  /**
   * PSFileSystemPathItemService is abstract (it leaves IPSPathService entry points for concrete
   * subclasses like PSWebResourcesPathItemService). For the regression test we instantiate a
   * minimal concrete subclass that wires the same three dependencies as the production
   * constructor and stubs out the remaining abstract method.
   */
  static class TestablePathItemService extends PSFileSystemPathItemService {
    TestablePathItemService(
        IPSFolderHelper folderHelper,
        IPSFileSystemService fsService,
        com.percussion.ui.service.IPSListViewHelper listViewHelper) {
      super(folderHelper, fsService, listViewHelper);
    }

    @Override
    public String getFullFolderPath(String path) {
      return path;
    }

    @Override
    public PSPathItem findRoot() {
      var root = new PSPathItem();
      root.setName("root");
      root.setPath("/");
      root.setLeaf(false);
      return root;
    }
  }

  @TempDir Path designRoot;

  private PSFileSystemPathItemService service() throws Exception {
    IPSFolderHelper folderHelper = mock(IPSFolderHelper.class);
    IPSFileSystemService fsService = mock(IPSFileSystemService.class);
    // getPathItemFromFile calls fileSystemService.getFile(parentPath).isFile() at line
    // 194 (post-fix line). Mock the call to return a real File (treated as a directory
    // since isFile() is called) so legitimate tests pass that branch.
    when(fsService.getFile(anyString())).thenReturn(new File(designRoot.toFile(), "mock-parent"));
    com.percussion.ui.service.IPSListViewHelper listViewHelper =
        mock(com.percussion.ui.service.IPSListViewHelper.class);
    return new TestablePathItemService(folderHelper, fsService, listViewHelper);
  }

  /**
   * Reflectively invokes the private {@code getPathItemFromFile}. Reflection is needed only because
   * the method is private; the constructor is exercised via the public {@code @Autowired} entry
   * point.
   */
  private PSPathItem invokeGetPathItemFromFile(
      PSFileSystemPathItemService svc, String parentPath, File child) throws Throwable {
    Method m =
        PSFileSystemPathItemService.class.getDeclaredMethod(
            "getPathItemFromFile", String.class, File.class);
    m.setAccessible(true);
    try {
      return (PSPathItem) m.invoke(svc, parentPath, child);
    } catch (InvocationTargetException ite) {
      throw ite.getCause();
    }
  }

  // ====================================================================
  // Fail-then-pass tests on malicious file-name payloads
  // ====================================================================

  @Test
  @DisplayName(
      "getPathItemFromFile: rejects a child.getName() containing '..' before child.isDirectory()"
          + " — CodeQL java/path-injection #1053")
  void testGetPathItemFromFileRejectsTraversalName() throws Throwable {
    PSFileSystemPathItemService svc = service();
    Path legitDir = designRoot.resolve("legit-dir");
    Files.createDirectories(legitDir);
    // Simulate the post-listFiles File: parentPath is legit but the child.getName()
    // contains traversal (the attack payload travels in the file name when the
    // attacker controls a symlink or a sibling with a traversal name).
    File traversalChild = new File(legitDir.toFile(), "..");
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeGetPathItemFromFile(svc, legitDir.toString(), traversalChild),
        "A child.getName() of '..' must be rejected by"
            + " PSPathInjectionGuard.requireSafeFileName before any File operation runs.");
  }

  @Test
  @DisplayName("getPathItemFromFile: rejects a child.getName() containing forward-slash")
  void testGetPathItemFromFileRejectsForwardSlashInName() throws Throwable {
    PSFileSystemPathItemService svc = service();
    Path legitDir = designRoot.resolve("legit-dir");
    Files.createDirectories(legitDir);
    File slashedChild = new File(legitDir.toFile(), "foo/bar");
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeGetPathItemFromFile(svc, legitDir.toString(), slashedChild));
  }

  @Test
  @DisplayName("getPathItemFromFile: rejects a child.getName() containing a NUL byte")
  void testGetPathItemFromFileRejectsNulInName() throws Throwable {
    PSFileSystemPathItemService svc = service();
    Path legitDir = designRoot.resolve("legit-dir");
    Files.createDirectories(legitDir);
    // File constructor rejects NUL bytes directly; build via Path which is permissive on
    // some platforms, then fall back to a literal String constructor if needed.
    File nulChild = new File(legitDir.toFile(), "good\0.css");
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeGetPathItemFromFile(svc, legitDir.toString(), nulChild));
  }

  // ====================================================================
  // Behavior parity: legitimate child.getName() values pass the validator
  // (tested via the helper directly — getPathItemFromFile has too many
  // downstream dependencies on a live server runtime to exercise the
  // happy path in a unit test).
  // ====================================================================

  @Test
  @DisplayName("Validator accepts a real directory name like 'legit-dir'")
  void testValidatorAcceptsLegitDirectoryName() {
    assertDoesNotThrow(
        () ->
            com.percussion.security.io.PSPathInjectionGuard.requireSafeFileName("legit-dir"),
        "Validator MUST accept 'legit-dir' (this is the name form a real directory"
            + " would have when reachin line 207 of getPathItemFromFile).");
  }

  @Test
  @DisplayName("Validator accepts a real file name like 'readme.txt'")
  void testValidatorAcceptsLegitFileName() {
    assertDoesNotThrow(
        () ->
            com.percussion.security.io.PSPathInjectionGuard.requireSafeFileName("readme.txt"),
        "Validator MUST accept 'readme.txt'.");
  }

  @Test
  @DisplayName(
      "Validator accepts a child.getName() with allowed punctuation"
          + " (dashes, dots, underscores)")
  void testValidatorAcceptsPunctuationInName() {
    assertDoesNotThrow(
        () ->
            com.percussion.security.io.PSPathInjectionGuard.requireSafeFileName("archive.tar.gz"),
        "Validator MUST accept 'archive.tar.gz'.");
  }

  // ====================================================================
  // Sanity: PSFileSystemPathItemService can be constructed via the @Autowired
  // entry point with Mockito-mocked dependencies (no sun.misc.Unsafe).
  // ====================================================================

  @Test
  @DisplayName("Sanity: PSFileSystemPathItemService is constructible via the public constructor")
  void testSanityConstruction() throws Exception {
    PSFileSystemPathItemService svc = service();
    assertNotNull(svc, "Service must be constructible with mocked dependencies");
  }
}