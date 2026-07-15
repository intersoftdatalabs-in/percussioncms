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
package com.percussion.pagemanagement.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-833 / v8.1.7 PR #834: sitewide framework gadget URLs are wrapped in
 * script/link tags via processFrameworkContent.
 */
class PSPageUtilsProcessFrameworkContentTest {

  @Test
  void processFrameworkContentWrapsJsAndCssUrls() {
    PSPageUtils utils = new PSPageUtils();

    assertEquals("", utils.processFrameworkContent(null));
    assertEquals("", utils.processFrameworkContent(""));
    assertEquals("", utils.processFrameworkContent("   "));

    assertEquals(
        "<script src=\"https://code.jquery.com/jquery-3.7.1.min.js\"></script>",
        utils.processFrameworkContent(
            "<script src=\"https://code.jquery.com/jquery-3.7.1.min.js\"></script>"));
    assertEquals(
        "console.log(\"hello\");", utils.processFrameworkContent("console.log(\"hello\");"));

    assertEquals(
        "<script src=\"https://code.jquery.com/jquery-3.7.1.min.js\"></script>",
        utils.processFrameworkContent("https://code.jquery.com/jquery-3.7.1.min.js"));
    assertEquals(
        "<script src=\"http://code.jquery.com/jquery-3.7.1.js?v=2\"></script>",
        utils.processFrameworkContent("http://code.jquery.com/jquery-3.7.1.js?v=2"));
    assertEquals(
        "<script src=\"//code.jquery.com/ui/1.13.3/jquery-ui.min.js\"></script>",
        utils.processFrameworkContent("//code.jquery.com/ui/1.13.3/jquery-ui.min.js"));
    assertEquals(
        "<script src=\"/js/custom.js\"></script>", utils.processFrameworkContent("/js/custom.js"));

    assertEquals(
        "<link rel=\"stylesheet\" href=\"https://code.jquery.com/ui/1.13.3/themes/smoothness/jquery-ui.css\" />",
        utils.processFrameworkContent(
            "https://code.jquery.com/ui/1.13.3/themes/smoothness/jquery-ui.css"));
    assertEquals(
        "<link rel=\"stylesheet\" href=\"//example.com/styles.css?foo=bar#baz\" />",
        utils.processFrameworkContent("//example.com/styles.css?foo=bar#baz"));

    assertEquals(
        "https://example.com/image.png",
        utils.processFrameworkContent("https://example.com/image.png"));
  }


  @Test
  void assemblyVmUsesProcessFrameworkContent() throws Exception {
    Path root = resolveRepoRoot();
    Path vm =
        root.resolve(
            "system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm");
    if (!Files.isRegularFile(vm)) {
      fail("expected " + vm);
    }
    String text = Files.readString(vm, StandardCharsets.UTF_8);
    assertTrue(text.contains("processFrameworkContent"));
    assertTrue(text.contains("siteAdditionalHeadContent"));
    assertTrue(text.contains("siteAfterBodyOpenContent"));
    assertTrue(text.contains("siteBeforeBodyCloseContent"));
    long count = text.split("processFrameworkContent", -1).length - 1;
    assertEquals(3, count, "three sitewide framework injection points");
  }


  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path candidate = cwd.resolve("../..").normalize();
    if (Files.isDirectory(candidate.resolve("system"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("system"))) {
      return cwd;
    }
    fail("could not resolve monorepo root");
    return cwd;
  }
}
