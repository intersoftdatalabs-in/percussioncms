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

package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-785 / v8.1.7 PR #786: View Metadata dialog must render tags as plain text and
 * always initialize the category checkbox tree (even when PercNavigationManager is unavailable in
 * the metadata iframe).
 */
class ViewMetadataTagListControlTest {

  private static final Path TAG_XSL =
      Path.of(
          "modules/perc-packages/src/main/resources/Packages/perc.Baseline"
              + "/SupportFile-rx_resources/stylesheets/controls/percTagListControl.xsl");

  private static final Path SYS_TEMPLATES =
      Path.of(
          "system/cms/content/applications/sys_resources/ApplicationFiles/stylesheets/sys_Templates.xsl");

  @Test
  void readOnlyTagListRendersPlainTextNotInputs() throws Exception {
    String xsl = read(TAG_XSL);
    // Isolate the isReadOnly template body
    Matcher m =
        Pattern.compile(
                "match=\"Control\\[@name='percTagListControl' and @isReadOnly='yes'\\]\"[\\s\\S]*?</xsl:template>",
                Pattern.MULTILINE)
            .matcher(xsl);
    assertTrue(m.find(), "expected isReadOnly percTagListControl template");
    String body = m.group();
    assertFalse(
        body.contains("<input"),
        "read-only tags must not emit <input> (View Metadata should show plain text)");
    assertTrue(body.contains("datadisplay"), "read-only tags must use datadisplay wrapper");
    assertTrue(
        body.contains("xsl:value-of") || body.contains("<xsl:value-of"),
        "read-only tags must output Value text");
    assertTrue(
        body.contains("not(position()=last())\">, </xsl:if>")
            || body.contains("not(position()=last())\">, "),
        "selected tags must be joined with comma+space");
  }

  @Test
  void checkBoxTreeJsAlwaysInitializesWithSafeSiteName() throws Exception {
    String xsl = read(SYS_TEMPLATES);
    // The CDATA block for sys_CheckBoxTreeJS must always call perc_checkboxTree
    assertTrue(
        xsl.contains("encodeURIComponent(siteName)"),
        "siteName must be encodeURIComponent'd for query string safety");
    assertTrue(
        xsl.contains("parent.$.PercNavigationManager"),
        "must still attempt PercNavigationManager when present");
    assertTrue(
        xsl.contains("try {") && xsl.contains("} catch (e) {"),
        "PercNavigationManager access must be try/catch guarded for iframe contexts");
    // Must not gate tree init solely on parent.$ being defined
    assertFalse(
        Pattern.compile(
                "if\\s*\\(\\s*typeof\\s+parent\\.\\$\\s*!==\\s*'undefined'\\s*\\)\\s*\\{\\s*"
                    + "var\\s+siteName\\s*=\\s*parent\\.\\$\\.PercNavigationManager\\.getSiteName\\s*\\(\\s*\\)\\s*;")
            .matcher(xsl)
            .find(),
        "must not use the old pattern that only inits the tree when parent.$ is defined");
    assertTrue(
        xsl.contains("$('#' + paramName + '-tree').perc_checkboxTree(opts)"),
        "perc_checkboxTree must always be invoked");
  }

  private static String read(Path rel) throws Exception {
    Path p = resolveRepoRoot().resolve(rel);
    if (!Files.isRegularFile(p)) {
      fail("expected file at " + p.toAbsolutePath());
    }
    return Files.readString(p, StandardCharsets.UTF_8);
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path candidate = cwd.resolve("../..").normalize();
    if (Files.isDirectory(candidate.resolve("system"))
        && Files.isDirectory(candidate.resolve("modules"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("system")) && Files.isDirectory(cwd.resolve("modules"))) {
      return cwd;
    }
    fail("could not resolve monorepo root; tried " + candidate + " and " + cwd);
    return cwd;
  }
}
