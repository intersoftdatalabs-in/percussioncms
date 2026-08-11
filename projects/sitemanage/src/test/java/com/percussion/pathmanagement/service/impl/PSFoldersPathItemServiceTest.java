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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.pathmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import com.percussion.share.dao.IPSFolderHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for classic {@code //Folders} path mapping (#3044).
 *
 * <p>Does not require a running CMS: {@link IPSFolderHelper#concatPath} is stubbed with the same
 * portable join used by production ({@code /} path segments).</p>
 */
public class PSFoldersPathItemServiceTest {

  private TestFoldersPathItemService service;
  private IPSFolderHelper folderHelper;

  @BeforeEach
  public void setUp() {
    folderHelper = Mockito.mock(IPSFolderHelper.class);
    Mockito.when(folderHelper.concatPath(Mockito.anyString(), Mockito.anyString()))
        .thenAnswer(
            inv -> {
              String start = inv.getArgument(0);
              String end = inv.getArgument(1);
              if (end == null || end.isEmpty() || "/".equals(end)) {
                return start;
              }
              String rel = end.startsWith("/") ? end.substring(1) : end;
              // Strip trailing slash from relative segment for stable concat.
              if (rel.endsWith("/")) {
                rel = rel.substring(0, rel.length() - 1);
              }
              if (start.endsWith("/")) {
                return start + rel;
              }
              return start + "/" + rel;
            });
    service = new TestFoldersPathItemService(folderHelper);
  }

  @Test
  public void rootNameIsFolders() {
    assertEquals("Folders", service.getRootName());
  }

  @Test
  public void fullFolderPathAtRootIsClassicDoubleSlashFolders() throws Exception {
    assertEquals(
        PSFoldersPathItemService.FOLDERS_ROOT, service.exposeFullFolderPath("/"));
  }

  @Test
  public void fullFolderPathConcatenatesRelativeChildren() throws Exception {
    // PathMatch always normalizes with a trailing slash before calling the service.
    assertEquals(
        "//Folders/$System$", service.exposeFullFolderPath("/$System$/"));
    assertEquals(
        "//Folders/$System$/Assets/uploads",
        service.exposeFullFolderPath("/$System$/Assets/uploads/"));
  }

  @Test
  public void fullFolderPathRejectsNullOrBlank() {
    assertThrows(Exception.class, () -> service.exposeFullFolderPath(null));
    assertThrows(Exception.class, () -> service.exposeFullFolderPath(""));
  }

  @Test
  public void folderRootConstantMatchesClassicRepositoryRoot() {
    assertEquals("//Folders", PSFoldersPathItemService.FOLDERS_ROOT);
    assertEquals("/Folders", PSFoldersPathItemService.FOLDERS_FINDER_ROOT);
    assertTrue(PSFoldersPathItemService.FOLDERS_ROOT.startsWith("//"));
  }

  @Test
  public void pathUtilsRoundTripForFoldersRoot() {
    assertEquals(
        "/Folders",
        PSPathUtils.getFinderPath(PSFoldersPathItemService.FOLDERS_ROOT));
    assertEquals(
        "//Folders/legacy",
        PSPathUtils.getFolderPath("/Folders/legacy"));
    assertEquals(
        "/Folders/legacy",
        PSPathUtils.getFinderPath("//Folders/legacy"));
  }

  /**
   * Test double that wires only {@link IPSFolderHelper} and exposes protected path mapping for
   * assertions.
   */
  private static final class TestFoldersPathItemService extends PSFoldersPathItemService {
    TestFoldersPathItemService(IPSFolderHelper folderHelper) {
      super(folderHelper, null, null, null, null, null, null, null, null, null);
    }

    String exposeFullFolderPath(String path) throws PSPathNotFoundServiceException {
      return getFullFolderPath(path);
    }
  }
}
