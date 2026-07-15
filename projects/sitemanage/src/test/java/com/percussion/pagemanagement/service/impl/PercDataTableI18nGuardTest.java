package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regression for GH-910 / v8.1.7 PR #911: PercDataTable must not require global I18N. */
class PercDataTableI18nGuardTest {
  @Test
  void guardsMissingI18n() throws Exception {
    Path root = resolve();
    for (String rel :
        List.of(
            "WebUI/war/widgets/PercDataTable/PercDataTable.js",
            "WebUI/src/main/webapp/cm/widgets/PercDataTable/PercDataTable.js")) {
      Path p = root.resolve(rel);
      if (!Files.isRegularFile(p)) fail(p.toString());
      String js = Files.readString(p, StandardCharsets.UTF_8);
      assertTrue(js.contains("typeof I18N === \"undefined\""), rel + " must guard I18N");
      assertTrue(js.contains("No Pages Found"), rel + " must include fallback label");
    }
  }

  private static Path resolve() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path up = cwd.resolve("../..").normalize();
    if (Files.isDirectory(up.resolve("WebUI"))) return up;
    if (Files.isDirectory(cwd.resolve("WebUI"))) return cwd;
    fail("could not resolve monorepo root (Surefire basedir projects/sitemanage expected)");
    return cwd;
  }
}
