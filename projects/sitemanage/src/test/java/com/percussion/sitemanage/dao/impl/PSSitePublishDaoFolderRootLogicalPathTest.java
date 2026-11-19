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
package com.percussion.sitemanage.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.util.PSPathUtil;
import java.io.File;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * CMS-logical folder-root construction for site publishing (issue #6729).
 *
 * <p>The {@code FOLDER_ROOT_BASE} join ({@code PSPathUtil.SITES_ROOT + "/"}) concatenates a CMS
 * content-repository path ({@code //Sites/<name>}, consumed via {@code IPSSite#setFolderRoot} and
 * parsed by {@code PSPathUtils.getSiteFromPath}), never an OS filesystem path — so it is
 * intentionally not {@code Path.resolve}/{@code File.separator}. These tests pin the forward-slash
 * logical-path contract on every OS, including Windows where the filesystem separator is {@code \}.
 */
@Tag("UnitTest")
class PSSitePublishDaoFolderRootLogicalPathTest {

  private static String folderRootBase() throws Exception {
    Field f = PSSitePublishDao.class.getDeclaredField("FOLDER_ROOT_BASE");
    f.setAccessible(true);
    return (String) f.get(null);
  }

  @Test
  void folderRootBaseUsesRepositorySlash() throws Exception {
    assertEquals("//Sites/", folderRootBase());
  }

  @Test
  void folderRootBaseIsIndependentOfOsSeparator() throws Exception {
    // Windows/Unix parity: the join must stay "/" even where File.separator is "\".
    assertEquals(PSPathUtil.SITES_ROOT + "/", folderRootBase());
    assertFalse(folderRootBase().contains("\\"));
    assertFalse(folderRootBase().endsWith(File.separator + File.separator));
  }

  @Test
  void constructedFolderRootIsUnderSiteFolderRoot() throws Exception {
    String folderRoot = folderRootBase() + "my-site";
    assertEquals("//Sites/my-site", folderRoot);
    assertTrue(PSPathUtil.isPathUnderSiteFolderRoot(folderRoot));
  }

  @Test
  void constructedFolderRootRoundTripsSiteName() throws Exception {
    assertEquals("my-site", PSPathUtils.getSiteFromPath(folderRootBase() + "my-site"));
    assertEquals(
        "My Site_2", PSPathUtils.getSiteFromPath(folderRootBase() + "My Site_2"));
  }

  @Test
  void constructedFolderRootIsRepositoryPathNotFilesystemPath() throws Exception {
    // A filesystem-style join would produce backslashes on Windows and break
    // getSiteFromPath, which only parses "/" separators.
    String folderRoot = folderRootBase() + "my-site";
    assertEquals("my-site", PSPathUtils.getSiteFromPath(folderRoot));
    assertTrue(folderRoot.startsWith(PSPathUtil.SITES_ROOT + "/"));
  }
}
