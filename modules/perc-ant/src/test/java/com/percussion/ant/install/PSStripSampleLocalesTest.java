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
package com.percussion.ant.install;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral tests for {@link PSStripSampleLocales} (issue #2303). Algorithm must match {@code
 * com.percussion.distribution.install.SampleSiteLocaleStrip}.
 */
@Tag("UnitTest")
public class PSStripSampleLocalesTest {

  @TempDir Path tempDir;

  @Test
  void stripLocaleBlocksRemovesLocaleTablesKeepsOthers() {
    String input =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<tables>\n"
            + "  <table name=\"CONTENTSTATUS\"><row><c>X</c></row></table>\n"
            + "  <table name=\"RXLOCALE\"><row><c>en-us</c></row></table>\n"
            + "  <table name=\"RXSITES\"><row><c>EI</c></row></table>\n"
            + "  <table name=\"RXLOCALEFORMAT\"><row><c>en-us</c><c>USD</c></row></table>\n"
            + "</tables>\n";

    String stripped = PSStripSampleLocales.stripLocaleBlocks(input);

    assertFalse(stripped.toUpperCase().contains("NAME=\"RXLOCALE\""));
    assertFalse(stripped.toUpperCase().contains("NAME=\"RXLOCALEFORMAT\""));
    assertTrue(stripped.contains("CONTENTSTATUS"));
    assertTrue(stripped.contains("RXSITES"));
  }

  @Test
  void taskWritesStagingFileWithoutModifyingSource() throws Exception {
    Path input = tempDir.resolve("RxffTableData.xml");
    Path staging = tempDir.resolve("RxffTableData.staging.xml");
    String body =
        "<tables>\n"
            + "  <table name=\"RXLOCALE\"><row><c>en-us</c></row></table>\n"
            + "  <table name=\"RXSITES\"><row><c>CI</c></row></table>\n"
            + "</tables>\n";
    Files.writeString(input, body, StandardCharsets.UTF_8);

    Project project = new Project();
    PSStripSampleLocales task = new PSStripSampleLocales();
    task.setProject(project);
    task.setInputFile(input.toString());
    task.setStagingFile(staging.toString());
    task.execute();

    assertTrue(Files.isRegularFile(staging));
    String staged = Files.readString(staging, StandardCharsets.UTF_8);
    assertFalse(staged.toUpperCase().contains("NAME=\"RXLOCALE\""));
    assertTrue(staged.contains("RXSITES"));
    // Source unchanged.
    assertTrue(Files.readString(input, StandardCharsets.UTF_8).contains("RXLOCALE"));
  }

  @Test
  void taskFailsWhenInputMissing() {
    Project project = new Project();
    PSStripSampleLocales task = new PSStripSampleLocales();
    task.setProject(project);
    task.setInputFile(tempDir.resolve("missing.xml").toString());
    task.setStagingFile(tempDir.resolve("out.xml").toString());
    assertThrows(BuildException.class, task::execute);
  }
}
