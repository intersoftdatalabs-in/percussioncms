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

package com.percussion.theme.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pagemanagement.data.PSRegionTree;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("PSRegionCSSFileService Path Traversal and Sanitization Security Tests")
public class PSRegionCSSFileServiceTest {

  private PSRegionCSSFileService service;
  private File allowedRoot;

  @BeforeEach
  void setUp(@TempDir File tempDir) {
    service = new PSRegionCSSFileService();
    allowedRoot = tempDir;
    service.setAllowedRoots(allowedRoot);
  }

  @Test
  @DisplayName("Should reject targetPath in mergeFile if it escapes the allowed roots")
  void testMergeFileRejectsTraversalTargetPath() throws IOException {
    File srcFile = new File(allowedRoot, "src.css");
    assertTrue(srcFile.createNewFile());

    String badTargetPath = new File(allowedRoot, "../traversal_target.css").getCanonicalPath();
    PSRegionTree dummyTree = new PSRegionTree();

    assertThrows(
        IllegalArgumentException.class,
        () -> service.mergeFile(dummyTree, srcFile.getAbsolutePath(), badTargetPath),
        "mergeFile should reject targetPath escaping allowed root");
  }

  @Test
  @DisplayName("Should reject srcPath in getSourceFile if it escapes the allowed roots")
  void testGetSourceFileRejectsTraversal() throws IOException {
    String badSrcPath = new File(allowedRoot, "../traversal_src.css").getCanonicalPath();

    assertThrows(
        IllegalArgumentException.class,
        () -> service.getSourceFile(badSrcPath),
        "getSourceFile should reject path escaping allowed root");
  }

  @Test
  @DisplayName("Should reject targetPath in getTargetFile if it escapes the allowed roots")
  void testGetTargetFileRejectsTraversal() throws IOException {
    String badTargetPath = new File(allowedRoot, "../traversal_target.css").getCanonicalPath();

    assertThrows(
        IllegalArgumentException.class,
        () -> service.getTargetFile(badTargetPath),
        "getTargetFile should reject path escaping allowed root");
  }

  @Test
  @DisplayName("Should reject filePath in writeContent if it escapes the allowed roots")
  void testWriteContentRejectsTraversal() throws IOException {
    String badFilePath = new File(allowedRoot, "../traversal_write.css").getCanonicalPath();

    assertThrows(
        IllegalArgumentException.class,
        () -> service.writeContent(badFilePath, "content"),
        "writeContent should reject path escaping allowed root");
  }

  @Test
  @DisplayName("Should reject filePath in getContentFromFile if it escapes the allowed roots")
  void testGetContentFromFileRejectsTraversal() throws IOException {
    String badFilePath = new File(allowedRoot, "../traversal_read.css").getCanonicalPath();

    assertThrows(
        IllegalArgumentException.class,
        () -> service.getContentFromFile(badFilePath),
        "getContentFromFile should reject path escaping allowed root");
  }

  @Test
  @DisplayName("Should allow operations on paths inside allowed roots")
  void testAllowedPathSucceeds() throws Exception {
    File validFile = new File(allowedRoot, "valid.css");
    String validPath = validFile.getAbsolutePath();

    // Verify writeContent works
    service.writeContent(validPath, ".test { color: red; }");
    assertTrue(validFile.exists());

    // Verify getContentFromFile works
    String content = service.getContentFromFile(validPath);
    assertEquals(".test { color: red; }", content);

    // Verify getSourceFile works
    File srcFile = service.getSourceFile(validPath);
    assertNotNull(srcFile);
    assertEquals(validFile.getCanonicalPath(), srcFile.getCanonicalPath());

    // Verify getTargetFile works
    File targetFile = service.getTargetFile(validPath);
    assertNotNull(targetFile);
    assertEquals(validFile.getCanonicalPath(), targetFile.getCanonicalPath());
  }
}
