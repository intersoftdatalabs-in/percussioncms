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

import com.percussion.theme.data.PSThemeSummary;
import com.percussion.theme.service.impl.PSThemeService;
import org.junit.jupiter.api.*;

/** Unit tests for {@link PSThemeService}. // REFACTORED: CP-JAVA11 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSThemeServiceTest {

  private PSThemeService themeService;

  @BeforeEach
  public void setup() {
    themeService = new PSThemeService();
    themeService.setThemesRootDirectory("src/test/resources/themes");
    themeService.setThemesRootRelativeUrl("/Rhythmyx/web_resources/themes");
    themeService.setThemesTempRootDirectory("src/test/resources/themes.tmp");
    themeService.setThemesTempRootRelativeUrl("/Rhythmyx/sys_resources/temp/themes");
  }

  @Test
  public void testFind() throws Exception {
    var summary = themeService.find("test");
    assertNotNull(summary);
    assertNotNull(summary.getName());
    assertEquals("test", summary.getName());
    assertEquals("test/perc_theme.css", summary.getCssFilePath(), "CSS filename");
    assertNotNull(summary.getRegionCssFilePath(), "Region CSS file");

    summary = themeService.find("more-than-one-css");
    assertNotNull(summary);
    assertNotNull(summary.getName());
    assertEquals("more-than-one-css", summary.getName());
    assertEquals(
        "more-than-one-css/more-than-one-css.css", summary.getCssFilePath(), "CSS filename");
    assertNotNull(summary.getRegionCssFilePath(), "Region CSS file");
  }

  @Test
  public void testFindAll() throws Exception {
    var sums = themeService.findAll();
    assertNotNull(sums);
    assertFalse(sums.isEmpty());
    assertEquals(4, sums.size());
    for (var sum : sums) {
      if (sum.getName().equals("test")) {
        assertNotNull(sum.getThumbUrl());
        assertNotNull(sum.getCssFilePath());
        assertNotNull(sum.getRegionCssFilePath(), "Region CSS file");
      } else if (sum.getName().equals("more-than-one-css")) {
        assertNotNull(sum.getThumbUrl());
        assertNotNull(sum.getCssFilePath());
        assertNotNull(sum.getRegionCssFilePath(), "Region CSS file");
      } else if (sum.getName().equals("more-than-one-thumb-images")) {
        assertNotNull(sum.getThumbUrl());
        assertNotNull(sum.getCssFilePath());
        assertNotNull(sum.getRegionCssFilePath(), "Region CSS file");
      } else if (sum.getName().equals("no-thumb-image")) {
        assertNull(sum.getThumbUrl());
        assertNotNull(sum.getCssFilePath());
        assertNull(sum.getRegionCssFilePath(), "There is no Region CSS file");
      }
    }
  }

  @Test
  public void testLoad() throws Exception {
    var theme = themeService.load("test");
    assertNotNull(theme, "theme");
    assertNotNull(theme.getCSS(), "css");

    theme = themeService.load("more-than-one-css");
    assertNotNull(theme, "theme");
    assertNotNull(theme.getCSS(), "css");
  }

  @Test
  public void testCreate() throws Exception {
    PSThemeSummary newSum = null;
    try {
      var sum = themeService.find("test");
      newSum = themeService.create("mynewtheme", sum.getName());
      assertNotNull(newSum);
      assertTrue(newSum.getCssFilePath().startsWith(newSum.getName()));
      assertNotNull(newSum.getRegionCssFilePath(), "Region CSS file");

      var theme = themeService.load(sum.getName());
      var newTheme = themeService.load(newSum.getName());
      assertEquals(newTheme.getCSS(), theme.getCSS());

      Exception thrown =
          assertThrows(
              Exception.class,
              () -> {
                themeService.create("shouldnotbecreated", "doesnotexist");
              });
      assertNotNull(thrown);
    } finally {
      if (newSum != null) {
        themeService.delete(newSum.getName());
      }
    }
  }

  @Test
  public void testDelete() throws Exception {
    var sum = themeService.find("test");
    var newSum = themeService.create("mynewtheme", sum.getName());
    themeService.delete(newSum.getName());
    Exception thrown =
        assertThrows(
            Exception.class,
            () -> {
              themeService.find(newSum.getName());
            });
    assertNotNull(thrown);

    // issue CM-276: deleting a non-existent theme should not throw
    assertDoesNotThrow(() -> themeService.delete(newSum.getName()));
  }
}
