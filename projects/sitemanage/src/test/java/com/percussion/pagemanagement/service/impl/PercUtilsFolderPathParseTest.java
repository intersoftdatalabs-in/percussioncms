package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regression for GH-867 / v8.1.7 PR #872: folder open path uses PathItem, not lastClickPath. */
class PercUtilsFolderPathParseTest {
  @Test
  void openUsesPathItemNotLastClickPath() throws Exception {
    Path root = resolve();
    for (String rel :
        List.of(
            "WebUI/war/plugins/perc_utils.js", "WebUI/src/main/webapp/cm/plugins/perc_utils.js")) {
      Path p = root.resolve(rel);
      if (!Files.isRegularFile(p)) fail(p.toString());
      String js = Files.readString(p, StandardCharsets.UTF_8);
      assertTrue(js.contains("result.PathItem.path.split"), rel);
      assertTrue(
          js.contains("pth[pth.length - 1] === \"\""), rel + " trims trailing empty segment");
      // Defensive guard against a SUCCESS payload that lacks a PathItem
      // (kilo-code-bot WARNING on PR #1246).
      assertTrue(
          js.contains("result && result.PathItem && result.PathItem.path"),
          rel + " must guard the result.PathItem dereference");
      // Should not prefer lastClickPath for the new folder path
      assertFalse(
          js.contains("var pth = $.perc_finder().lastClickPath;"),
          rel + " must not seed path from lastClickPath");
    }
  }

  private static Path resolve() {
    // Prefer Surefire's `basedir` system property -- it points at this
    // module's directory (`projects/sitemanage`) regardless of the host CWD,
    // so the test resolves the same monorepo root on every runner (CLI,
    // CI, IDE). Fall back to relative CWD traversal for `mvn test` invocations
    // outside Surefire where `basedir` is not set.
    String basedir = System.getProperty("basedir");
    if (basedir != null && !basedir.isEmpty()) {
      Path fromSurefire = Path.of(basedir).resolve("../..").normalize();
      if (isMonorepoRoot(fromSurefire)) {
        return fromSurefire;
      }
    }
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path up = cwd.resolve("../..").normalize();
    if (isMonorepoRoot(up)) return up;
    if (isMonorepoRoot(cwd)) return cwd;
    fail(
        "could not resolve monorepo root (Surefire basedir="
            + basedir
            + ", cwd="
            + cwd
            + ") -- expected a directory containing both WebUI/ and system/");
    return cwd;
  }

  private static boolean isMonorepoRoot(Path p) {
    return Files.isDirectory(p.resolve("WebUI")) && Files.isDirectory(p.resolve("system"));
  }
}
