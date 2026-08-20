/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
// REFACTORED: CP-JAVA11

package com.percussion.pathmanagement.service;

import static com.percussion.test.TestAssertions.*;

import com.percussion.pathmanagement.service.impl.PSAssetPathItemService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.pathmanagement.service.impl.PSSitePathItemService;
import org.junit.jupiter.api.Test;

public class PSPathUtilsTest {

  /**
   * Tests the {@link PSPathUtils#getFinderPath(String)} and {@link
   * PSPathUtils#getFolderPath(String)} methods.
   */
  @Test
  public void testGetPath() {
    var asset = "testAsset";
    var site = "testSite";
    var assetFolderPath = PSAssetPathItemService.ASSET_ROOT + '/' + asset;
    var siteFolderPath = PSSitePathItemService.SITE_ROOT + '/' + site;
    var assetFinderPath = PSPathUtils.ASSETS_FINDER_ROOT + '/' + asset;
    var siteFinderPath = PSPathUtils.SITES_FINDER_ROOT + '/' + site;
    var folderPath = "//path";
    var finderPath = "/path";

    assertEquals(assetFinderPath, PSPathUtils.getFinderPath(assetFolderPath));
    assertEquals(siteFinderPath, PSPathUtils.getFinderPath(siteFolderPath));
    assertEquals(assetFolderPath, PSPathUtils.getFolderPath(assetFinderPath));
    assertEquals(siteFolderPath, PSPathUtils.getFolderPath(siteFinderPath));
    assertEquals(finderPath, PSPathUtils.getFinderPath(folderPath));
    assertEquals(folderPath, PSPathUtils.getFolderPath(finderPath));
    assertEquals(finderPath, PSPathUtils.getFinderPath("////path"));
    assertEquals(folderPath, PSPathUtils.getFolderPath("////path"));

    // Classic //Folders root (#3044): finder form is /Folders; folder form is //Folders.
    assertEquals("/Folders", PSPathUtils.getFinderPath("//Folders"));
    assertEquals("//Folders", PSPathUtils.getFolderPath("/Folders"));
    assertEquals("/Folders/$System$", PSPathUtils.getFinderPath("//Folders/$System$"));
    assertEquals("//Folders/$System$", PSPathUtils.getFolderPath("/Folders/$System$"));
  }

  /**
   * Public REST copy/folder dest paths from Explorer are finder or
   * single-slash repository form. {@link PSPathUtils#getFolderPath} must
   * yield {@code //} or folderHelper throws "Path must start with '//'"
   * (#3647).
   */
  /**
   * Pathmanagement moveItem source/dest from Explorer are finder or
   * single-slash repository form. Same {@link PSPathUtils#getFolderPath}
   * conversion as copy (#3655).
   */
  @Test
  public void testGetFolderPathMoveFolder() {
    assertEquals(
        "//Folders/$System$/Assets/qa3655/src",
        PSPathUtils.getFolderPath("/Assets/qa3655/src"));
    assertEquals(
        "//Folders/$System$/Assets/qa3655/dst",
        PSPathUtils.getFolderPath("/Assets/qa3655/dst"));
    assertEquals(
        "//Folders/$System$/Assets/qa3655/src",
        PSPathUtils.getFolderPath("/Folders/$System$/Assets/qa3655/src"));
    assertEquals(
        "//Folders/$System$/Assets/qa3655/src",
        PSPathUtils.getFolderPath("//Folders/$System$/Assets/qa3655/src"));
    assertEquals("//Sites/Help", PSPathUtils.getFolderPath("/Sites/Help"));
  }

  @Test
  public void testGetFolderPathCopyFolderDest() {
    assertEquals(
        "//Folders/$System$/Assets/qa/dst",
        PSPathUtils.getFolderPath("/Assets/qa/dst"));
    assertEquals(
        "//Folders/$System$/Assets/qa/dst",
        PSPathUtils.getFolderPath("/Folders/$System$/Assets/qa/dst"));
    assertEquals(
        "//Folders/$System$/Assets/qa/dst",
        PSPathUtils.getFolderPath("//Folders/$System$/Assets/qa/dst"));
    assertEquals("//Sites/Help", PSPathUtils.getFolderPath("/Sites/Help"));
  }

  /**
   * Public REST copy/item source is a listed item path (single-slash
   * {@code /Folders/$System$/Assets/…/item} or finder {@code /Assets/…/item}).
   * {@link PSPathUtils#getFolderPath} must still yield {@code //} so
   * {@code folderHelper.findItem} does not throw "Path must start with '//'"
   * (#3656).
   */
  @Test
  public void testGetFolderPathCopyItemSource() {
    assertEquals(
        "//Folders/$System$/Assets/src/item",
        PSPathUtils.getFolderPath("/Folders/$System$/Assets/src/item"));
    assertEquals(
        "//Folders/$System$/Assets/src/item",
        PSPathUtils.getFolderPath("/Assets/src/item"));
    assertEquals(
        "//Folders/$System$/Assets/src/item",
        PSPathUtils.getFolderPath("//Folders/$System$/Assets/src/item"));
  }

  /**
   * {@link PSPathUtils#toRepositoryPath} is the item-copy helper: same {@code //}
   * promotion as {@link PSPathUtils#getFolderPath} and must keep the leaf (#3656).
   */
  @Test
  public void testToRepositoryPathKeepsItemLeaf() {
    assertEquals(
        "//Folders/$System$/Assets/src/item",
        PSPathUtils.toRepositoryPath("/Folders/$System$/Assets/src/item"));
    assertEquals(
        "//Folders/$System$/Assets/src/item",
        PSPathUtils.toRepositoryPath("/Assets/src/item"));
    assertEquals(
        "//Folders/$System$/Assets/src/item",
        PSPathUtils.toRepositoryPath("//Folders/$System$/Assets/src/item"));
    assertEquals("//Sites/Help", PSPathUtils.toRepositoryPath("/Sites/Help"));
  }

  @Test
  public void testGetBasePath() {
    var trailing = "/Sites/mysite/mymain/mysecond/";
    var doubleleading = "//Sites/mysite/mymain/mysecond/";
    var page = "//Sites/mysite/mymain/mysecond/mypage";

    assertEquals("mymain/mysecond", PSPathUtils.getBaseFolderFromPath(trailing));
    assertEquals("mymain/mysecond", PSPathUtils.getBaseFolderFromPath(doubleleading));
    assertEquals("mymain/mysecond", PSPathUtils.getBaseFolderFromPath(page));
  }

  @Test
  public void testChopTrailingSlash() {
    var trailing = "/Sites/mysite/";
    var doubleTrail = "/Sites/mysite//";
    var notrail = "/Sites/mysite";

    assertEquals("/Sites/mysite", PSPathUtils.chopTrailingSlash(trailing));
    assertEquals("/Sites/mysite", PSPathUtils.chopTrailingSlash(doubleTrail));
    assertEquals("/Sites/mysite", PSPathUtils.chopTrailingSlash(notrail));
  }

  @Test
  public void testGetFolderName() {
    var trailing = "/Sites/mysite/myfolder/";
    var doubleTrail = "/Sites/mysite/myfolder//";
    var notrail = "/Sites/mysite/myfolder";

    assertEquals("myfolder", PSPathUtils.getFolderName(trailing));
    assertEquals("myfolder", PSPathUtils.getFolderName(doubleTrail));
    assertEquals("myfolder", PSPathUtils.getFolderName(notrail));
  }

  @Test
  public void testStripFolderName() {
    var trailing = "/Sites/mysite/myfolder/";
    var doubleTrail = "/Sites/mysite//myfolder//";
    var notrail = "//Sites/mysite/myfolder";

    assertEquals("/Sites/mysite", PSPathUtils.stripFolderNameFromPath(trailing));
    assertEquals("/Sites/mysite", PSPathUtils.stripFolderNameFromPath(doubleTrail));
    assertEquals("//Sites/mysite", PSPathUtils.stripFolderNameFromPath(notrail));
  }
}
