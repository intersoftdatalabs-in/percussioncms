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

package com.percussion.theme;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pagemanagement.data.PSRegionTree;
import com.percussion.pagemanagement.data.PSRegionTreeTest;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.theme.data.PSRegionCSS;
import com.percussion.theme.data.PSRegionCSS.Property;
import com.percussion.theme.data.PSThemeSummary;
import com.percussion.theme.service.impl.PSRegionCSSFileService;
import com.percussion.theme.service.impl.PSThemeService;
import com.percussion.util.PSPurgableTempFile;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;

/** Unit tests for {@link PSRegionCSSFileService}. // REFACTORED: CP-JAVA11 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSRegionCSSFileServiceTest {

  private PSThemeService themeService;
  private PSRegionCSSFileService cssService;
  private List<PSPurgableTempFile> tempFiles;
  private static final int SAMPLE_SIZE = 3;

  @BeforeEach
  public void setup() {
    themeService = new PSThemeService();
    themeService.setThemesRootDirectory("src/test/resources/themes");
    themeService.setThemesRootRelativeUrl("/Rhythmyx/web_resources/themes");
    themeService.setThemesTempRootDirectory("src/test/resources/themes.tmp");
    themeService.setThemesTempRootRelativeUrl("/Rhythmyx/sys_resources/temp/themes");

    cssService = new PSRegionCSSFileService();
    tempFiles = new ArrayList<>();
  }

  @AfterEach
  public void tearDown() {
    for (var f : tempFiles) {
      f.delete();
    }
  }

  @Test
  public void testRead() throws Exception {
    var regions = readFromSampleFile();
    assertEquals(SAMPLE_SIZE, regions.size());
    assertEquals(regions, getSampleRegions(), "Region CSS file must equal sample data");

    var writeList = writeToTempFileThenRead(regions);
    assertEquals(regions, writeList, "Region list, write then read must be equal");
  }

  @Test
  public void testFind() throws Exception {
    var regionCSS = cssService.findRegionCSS("container", "header", getSampleFilePath());
    assertNotNull(regionCSS, "Sample file must contain \"container\" & \"header\" region CSS");

    regionCSS = cssService.findRegionCSS("container", "container", getSampleFilePath());
    assertNotNull(regionCSS, "Sample file must contain \"container\" & \"container\" region CSS");

    regionCSS = cssService.findRegionCSS("container", "unkown", getSampleFilePath());
    assertNull(regionCSS, "Sample file must not contain \"container\" & \"unkown\" region CSS");
  }

  @Test
  public void testSave() throws Exception {
    var container = createTestRegionCSS("container", "container");

    var tempFile = copySampleToTempFile();
    var regionCSS =
        cssService.findRegionCSS(
            container.getOuterRegionName(), container.getRegionName(), tempFile.getAbsolutePath());
    assertNotNull(regionCSS, "Sample file must contain \"container\" & \"container\" region CSS");

    // test save as update
    var regions = cssService.read(tempFile.getAbsolutePath());
    assertEquals(SAMPLE_SIZE, regions.size());
    cssService.save(container, tempFile.getAbsolutePath());
    regions = cssService.read(tempFile.getAbsolutePath());
    assertEquals(SAMPLE_SIZE, regions.size(), "The save is an update operation");

    // test save as add
    container.setRegionName("middle");
    regionCSS =
        cssService.findRegionCSS(
            container.getOuterRegionName(), container.getRegionName(), tempFile.getAbsolutePath());
    assertNull(regionCSS, "Region CSS does not contain \"container\" & \"middle\" region CSS");

    cssService.save(container, tempFile.getAbsolutePath());
    regions = cssService.read(tempFile.getAbsolutePath());
    assertEquals(SAMPLE_SIZE + 1, regions.size(), "The save is an add operation");
  }

  @Test
  public void testSort() throws Exception {
    var tempFile = copySampleToTempFile();
    var middle = createTestRegionCSS("container", "middle");
    cssService.save(middle, tempFile.getAbsolutePath());
    var regions = cssService.read(tempFile.getAbsolutePath());
    assertEquals(SAMPLE_SIZE + 1, regions.size(), "Just added a region");

    var sortRegions = readFromSampleFile();
    sortRegions.add(middle);
    Collections.sort(sortRegions);

    assertEquals(sortRegions, regions, "The regions should be sorted");
  }

  @Test
  public void testDelete() throws Exception {
    final String OUTER = "container";
    final String MIDDLE = "middle";
    var tempFile = copySampleToTempFile();
    var regionCSS = cssService.findRegionCSS(OUTER, MIDDLE, tempFile.getAbsolutePath());
    assertNull(regionCSS, "Region CSS does not contain \"container\" & \"middle\" region CSS");

    // delete nothing if not exist
    var regions = cssService.read(tempFile.getAbsolutePath());
    assertEquals(SAMPLE_SIZE, regions.size());
    cssService.delete(OUTER, MIDDLE, tempFile.getAbsolutePath());
    regions = cssService.read(tempFile.getAbsolutePath());
    assertEquals(SAMPLE_SIZE, regions.size(), "There was nothing to delete");

    final String HEADER = "header";

    // delete an existing region
    regionCSS = cssService.findRegionCSS(OUTER, HEADER, tempFile.getAbsolutePath());
    assertNotNull(regionCSS, "Sample file must contain \"container\" & \"header\" region CSS");

    cssService.delete(OUTER, HEADER, tempFile.getAbsolutePath());
    regionCSS = cssService.findRegionCSS(OUTER, HEADER, tempFile.getAbsolutePath());
    assertNull(regionCSS, "Deleted \"container\" & \"header\" region CSS");
  }

  @Test
  public void testCopy() throws Exception {
    var tempFile = copySampleToTempFile();
    var tempCss = createTempCssFile();

    cssService.copyFile(tempFile.getAbsolutePath(), tempCss.getAbsolutePath());

    // validate the copy result
    var srcText = getStringFileFile(tempFile.getAbsolutePath());
    var targetText = getStringFileFile(tempCss.getAbsolutePath());

    assertEquals(srcText, targetText);
  }

  private String getStringFileFile(String path) throws IOException {
    try (Reader reader = new FileReader(path)) {
      return IOUtils.toString(reader);
    }
  }

  @Test
  public void testCreateEmptyRegionCSS() throws Exception {
    var tempCss = createTempCssFile();
    tempCss.delete();

    assertFalse(tempCss.exists());

    cssService.copyFile(null, tempCss.getAbsolutePath());

    var content = getStringFileFile(tempCss.getAbsolutePath());
    assertEquals(
        ".percDummyRule{/* Dummy rule for correct HTML's LINK tag rendering during editing a"
            + " template */}",
        content);
    assertTrue(tempCss.exists());
  }

  private PSRegionCSS createTestRegionCSS(String outer, String region) {
    var regionCSS = new PSRegionCSS(outer, region);
    var p = new Property("height", "11px");
    var props = new ArrayList<Property>();
    props.add(p);
    regionCSS.setProperties(props);

    return regionCSS;
  }

  private List<PSRegionCSS> readFromSampleFile()
      throws IPSDataService.DataServiceNotFoundException,
          IPSDataService.DataServiceLoadException,
          PSValidationException {
    return cssService.read(getSampleFilePath());
  }

  private String getSampleFilePath()
      throws IPSDataService.DataServiceLoadException,
          PSValidationException,
          IPSDataService.DataServiceNotFoundException {
    var summary = themeService.find("test");
    assertNotNull(summary.getRegionCssFilePath(), "Region CSS file");

    return getRegionCssFile(summary);
  }

  private List<PSRegionCSS> writeToTempFileThenRead(List<PSRegionCSS> regions)
      throws IOException, IPSDataService.PSThemeNotFoundException {
    var tempCss = new PSPurgableTempFile("temp", "css", null);
    try {
      cssService.write(tempCss.getAbsolutePath(), regions);
      return cssService.read(tempCss.getAbsolutePath());
    } finally {
      tempCss.delete();
    }
  }

  private PSPurgableTempFile copySampleToTempFile()
      throws IOException,
          IPSDataService.DataServiceLoadException,
          PSValidationException,
          IPSDataService.DataServiceNotFoundException {
    var tempCss = createTempCssFile();
    var regions = readFromSampleFile();
    cssService.write(tempCss.getAbsolutePath(), regions);
    return tempCss;
  }

  private String getRegionCssFile(PSThemeSummary summary) {
    return themeService.getThemesRootDirectory() + "/" + summary.getRegionCssFilePath();
  }

  private PSPurgableTempFile createTempCssFile() throws IOException {
    var tempCss = new PSPurgableTempFile("temp", "css", null);
    tempFiles.add(tempCss);
    return tempCss;
  }

  private PSRegionTree getRegionTree() throws Exception {
    var tree = PSRegionTreeTest.loadRegionTree();

    var names = Arrays.asList(nameChildren);
    var regionNames = PSRegionTreeTest.getRegionIds(tree.getDescendentRegions());
    assertEquals(names, regionNames);

    return tree;
  }

  private final String[] nameChildren =
      new String[] {
        "container", "header", "middle", "leftsidebar", "content", "rightsidebar", "footer"
      };

  @Test
  public void testMergeRegionCSSFile() throws Exception {
    // simply copy resource to target as the merged result
    var tree = getRegionTree();
    validateSourceMergeToTarget(
        tree, getSampleRegions_2(), getSampleRegions(), getSampleRegions_2());
    validateSourceMergeToTarget(
        tree, getSampleRegions_2(), getSampleRegions_3(), getSampleRegions_2());

    // empty tree, no change to the target
    tree = new PSRegionTree();
    validateSourceMergeToTarget(
        tree, getSampleRegions_2(), getSampleRegions_3(), getSampleRegions_3());

    // empty source, no change to the target
    tree = getRegionTree();
    validateSourceMergeToTarget(
        tree, new ArrayList<>(), getSampleRegions_3(), getSampleRegions_3());

    // merged result is mixing the source into target
    validateSourceMergeToTarget(
        tree, getSampleRegions_4(), getSampleRegions_2(), getSampleRegions_4_merge_2());
  }

  private void validateSourceMergeToTarget(
      PSRegionTree tree,
      List<PSRegionCSS> src,
      List<PSRegionCSS> target,
      List<PSRegionCSS> finalRegions)
      throws Exception {
    var tempCss = createTempCssFile();
    cssService.write(tempCss.getAbsolutePath(), target);

    var tempCss2 = createTempCssFile();
    cssService.write(tempCss2.getAbsolutePath(), src);

    cssService.mergeFile(tree, tempCss2.getAbsolutePath(), tempCss.getAbsolutePath());

    var mergedRegions = cssService.read(tempCss.getAbsolutePath());
    assertEquals(finalRegions, mergedRegions, "The merge should have copied source to target");
  }

  private List<PSRegionCSS> getSampleRegions() {
    var regions = new ArrayList<PSRegionCSS>();

    var r = new PSRegionCSS("container", "container");
    addProperty(r, "font-family", "Verdana");
    addProperty(r, "font-size", "11px");
    addProperty(r, "font-weight", "normal");
    regions.add(r);

    r = new PSRegionCSS("container", "header");
    addProperty(r, "font-family", "Times,\"Times New Roman\",Georgia,serif");
    addProperty(r, "font-size", "22px");
    regions.add(r);

    r = new PSRegionCSS("container", "left");
    addProperty(r, "height", "100px");
    regions.add(r);

    return regions;
  }

  private List<PSRegionCSS> getSampleRegions_2() {
    var regions = new ArrayList<PSRegionCSS>();

    var r = new PSRegionCSS("container", "container");
    addProperty(r, "font-family", "Verdana");
    addProperty(r, "font-weight", "normal");
    regions.add(r);

    r = new PSRegionCSS("container", "header");
    addProperty(r, "font-family", "Times,\"Times New Roman\",Georgia,serif");
    regions.add(r);

    r = new PSRegionCSS("container", "left");
    addProperty(r, "height", "100px");
    regions.add(r);

    return regions;
  }

  private List<PSRegionCSS> getSampleRegions_3() {
    var regions = new ArrayList<PSRegionCSS>();

    var r = new PSRegionCSS("container", "header");
    addProperty(r, "font-family", "Times,\"Times New Roman\",Georgia,serif");
    addProperty(r, "font-size", "22px");
    regions.add(r);

    r = new PSRegionCSS("container", "left");
    addProperty(r, "height", "100px");
    regions.add(r);

    return regions;
  }

  private List<PSRegionCSS> getSampleRegions_4() {
    var regions = new ArrayList<PSRegionCSS>();

    var r = new PSRegionCSS("container", "container");
    addProperty(r, "font-weight", "normal");
    regions.add(r);

    r = new PSRegionCSS("container", "header");
    addProperty(r, "font-family", "Times,\"Times New Roman\",Georgia,serif");
    addProperty(r, "font-size", "22px");
    regions.add(r);

    r = new PSRegionCSS("container", "left");
    addProperty(r, "font-size", "22px");
    regions.add(r);

    return regions;
  }

  /**
   * Gets the result of merging {@link #getSampleRegions_4()} into {@link #getSampleRegions_2()}.
   *
   * @return the merged result.
   */
  private List<PSRegionCSS> getSampleRegions_4_merge_2() {
    var regions = new ArrayList<PSRegionCSS>();

    // from getSampleRegions_4()
    var r = new PSRegionCSS("container", "container");
    addProperty(r, "font-weight", "normal");
    regions.add(r);

    // from getSampleRegions_4()
    r = new PSRegionCSS("container", "header");
    addProperty(r, "font-family", "Times,\"Times New Roman\",Georgia,serif");
    addProperty(r, "font-size", "22px");
    regions.add(r);

    // from getSampleRegions_2
    r = new PSRegionCSS("container", "left");
    addProperty(r, "height", "100px");
    regions.add(r);

    return regions;
  }

  private void addProperty(PSRegionCSS r, String pname, String pvalue) {
    var p = new PSRegionCSS.Property(pname, pvalue);
    r.getProperties().add(p);
  }
}
