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
            "WebUI/war/plugins/perc_utils.js",
            "WebUI/src/main/webapp/cm/plugins/perc_utils.js")) {
      Path p = root.resolve(rel);
      if (!Files.isRegularFile(p)) fail(p.toString());
      String js = Files.readString(p, StandardCharsets.UTF_8);
      assertTrue(js.contains("result.PathItem.path.split"), rel);
      assertTrue(js.contains("pth[pth.length - 1] === \"\""), rel + " trims trailing empty segment");
      // Should not prefer lastClickPath for the new folder path
      assertFalse(
          js.contains("var pth = $.perc_finder().lastClickPath;"),
          rel + " must not seed path from lastClickPath");
    }
  }

  private static Path resolve() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path up = cwd.resolve("../..").normalize();
    if (Files.isDirectory(up.resolve("WebUI"))) return up;
    if (Files.isDirectory(cwd.resolve("WebUI"))) return cwd;
    fail("root");
    return cwd;
  }
}
