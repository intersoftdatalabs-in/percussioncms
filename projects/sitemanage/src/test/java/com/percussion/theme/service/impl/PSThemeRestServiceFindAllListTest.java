package com.percussion.theme.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Regression for GH-868 / v8.1.7 PR #870: findAll returns wrapped list type. */
class PSThemeRestServiceFindAllListTest {
  @Test
  void findAllReturnsThemeSummaryList() throws Exception {
    Path root = resolve();
    Path java =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeRestService.java");
    Path js = root.resolve("WebUI/war/views/PercCSSGalleryView.js");
    String src = Files.readString(java, StandardCharsets.UTF_8);
    assertTrue(src.contains("PSThemeSummaryList findAll()"), "return type must be PSThemeSummaryList");
    assertTrue(src.contains("new PSThemeSummaryList("), "must wrap themeService.findAll()");
    String gallery = Files.readString(js, StandardCharsets.UTF_8);
    assertTrue(gallery.contains("ThemeSummary || data"), "gallery must tolerate single/array summary");
  }

  private static Path resolve() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path up = cwd.resolve("../..").normalize();
    if (Files.isDirectory(up.resolve("projects/sitemanage"))) return up;
    if (Files.isDirectory(cwd.resolve("projects/sitemanage"))) return cwd;
    fail("root");
    return cwd;
  }
}
