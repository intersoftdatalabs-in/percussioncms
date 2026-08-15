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
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * FastForward rff* ObjectStore editors and their content-type icons must ship.
 * Editors live under {@code system/FastForward/.../Editors}; icons must be
 * copied to {@code rx_resources/images/ContentTypeIcons} (7.3.2 {@code
 * installFastForward.xml}).
 */
@Tag("UnitTest")
class FastForwardContentTypeIconsPackagingTest {

  private static final Path FF_EDITORS =
      Path.of(
          "..",
          "..",
          "system",
          "FastForward",
          "Core",
          "Content",
          "Applications",
          "Editors");

  private static final Path FF_ICONS =
      Path.of(
          "..",
          "..",
          "system",
          "FastForward",
          "Core",
          "Content",
          "Applications",
          "rx_resources",
          "ApplicationFiles",
          "images",
          "ContentTypeIcons");

  private static final Path DIST_FILES =
      Path.of("src/main/resources/installDistributionFiles.xml");

  private static final List<String> RFF_EDITORS =
      List.of(
          "psx_cerffAutoIndex.xml",
          "psx_cerffBrief.xml",
          "psx_cerffCalendar.xml",
          "psx_cerffContacts.xml",
          "psx_cerffEvent.xml",
          "psx_cerffExternalLink.xml",
          "psx_cerffFile.xml",
          "psx_cerffGeneric.xml",
          "psx_cerffGenericWord.xml",
          "psx_cerffHome.xml",
          "psx_cerffImage.xml",
          "psx_cerffPressRelease.xml");

  private static final List<String> RFF_ICONS =
      List.of(
          "rffAutoIndex.gif",
          "rffBrief.gif",
          "rffCalendar.gif",
          "rffContacts.gif",
          "rffEvent.gif",
          "rffExternalLink.gif",
          "rffGeneric.gif",
          "rffGenericWord.gif",
          "rffHome.gif",
          "rffPressRelease.gif",
          "percNavImage.gif",
          "percNavon.gif",
          "percNavTree.gif");

  @Test
  void objectStoreEditorsExistInFastForwardSource() {
    assertTrue(Files.isDirectory(FF_EDITORS), FF_EDITORS.toAbsolutePath().toString());
    for (String name : RFF_EDITORS) {
      try (var walk = Files.walk(FF_EDITORS)) {
        boolean found =
            walk.filter(Files::isRegularFile)
                .anyMatch(p -> name.equalsIgnoreCase(p.getFileName().toString()));
        assertTrue(found, "missing FastForward ObjectStore editor " + name);
      } catch (Exception e) {
        throw new AssertionError("walk failed for " + name, e);
      }
    }
  }

  @Test
  void contentTypeIconsExistInFastForwardSource() {
    assertTrue(Files.isDirectory(FF_ICONS), FF_ICONS.toAbsolutePath().toString());
    for (String name : RFF_ICONS) {
      assertTrue(
          Files.isRegularFile(FF_ICONS.resolve(name)),
          "missing FastForward content-type icon " + name);
    }
  }

  @Test
  void installDistributionFilesCopiesFastForwardIconsToRxResources() throws Exception {
    String xml = Files.readString(DIST_FILES, StandardCharsets.UTF_8);
    assertTrue(
        xml.contains("ApplicationFiles/images") && xml.contains("rx_resources/images"),
        "installDistributionFiles must copy FastForward ContentTypeIcons into rx_resources/images");
  }
}
