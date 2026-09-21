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
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Dist ANT copies TinyMCE overlay trees from perc-tinymce {@code target/classes}.
 * Missing {@code rx_resources} there fails {@code qa-rebuild-dist} / {@code qa-up}
 * (#4695). Guard the fileset path and the fail-fast message.
 */
@Tag("UnitTest")
class TinyMceRxResourcesPackagingTest {

  private static final Path DIST_FILES =
      Path.of("src/main/resources/installDistributionFiles.xml");

  private static final Path TINYMCE_RX_SRC =
      Path.of("..", "perc-tinymce", "src", "main", "resources", "META-INF", "resources",
          "rx_resources");

  @Test
  void antCopiesTinymceFromTargetClassesAndFailsIfMissing() throws Exception {
    String xml = Files.readString(DIST_FILES, StandardCharsets.UTF_8);
    assertTrue(
        xml.contains("perc-tinymce/target/classes/META-INF/resources/rx_resources"),
        "installDistributionFiles must copy perc-tinymce rx_resources from target/classes");
    assertTrue(
        xml.contains("qa-rebuild-tinymce"),
        "fail message must name qa-rebuild-tinymce so operators rebuild perc-tinymce first");
    assertTrue(
        Files.isDirectory(TINYMCE_RX_SRC),
        "perc-tinymce source rx_resources must exist so package can populate target/classes");
  }
}
