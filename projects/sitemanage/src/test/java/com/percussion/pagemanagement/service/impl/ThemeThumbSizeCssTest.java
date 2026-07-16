package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regression for GH-876 / v8.1.7 PR #896: theme gallery thumbs constrained. */
class ThemeThumbSizeCssTest {
  @Test
  void galleryImagesConstrained() throws Exception {
    Path root = resolve();
    for (String rel :
        List.of(
            "WebUI/war/css/perc_css_editor.css",
            "WebUI/src/main/webapp/cm/css/perc_css_editor.css",
            "WebUI/src/main/webapp/cm/app/css/legacy/perc_css_editor.css")) {
      Path p = root.resolve(rel);
      if (!Files.isRegularFile(p)) fail(p.toString());
      String css = Files.readString(p, StandardCharsets.UTF_8);
      assertTrue(css.contains(".perc-css-gallery-item img"), rel);
      assertTrue(css.contains("max-width: 150px"), rel);
      assertTrue(css.contains("max-height: 120px"), rel);
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
