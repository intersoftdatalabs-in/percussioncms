package com.percussion.theme.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-868 / v8.1.7 PR #870: findAll returns wrapped list type and gallery normalizes
 * ThemeSummary payloads on both WebUI copies.
 */
class PSThemeRestServiceFindAllListTest {

  @Test
  void galleryJsNormalizesThemeSummaryOnBothCopies() throws Exception {
    Path root = resolve();
    for (String rel :
        List.of(
            "WebUI/war/views/PercCSSGalleryView.js",
            "WebUI/src/main/webapp/cm/views/PercCSSGalleryView.js")) {
      Path js = root.resolve(rel);
      if (!Files.isRegularFile(js)) fail(js.toString());
      String gallery = Files.readString(js, StandardCharsets.UTF_8);
      assertTrue(
          gallery.contains("ThemeSummary || data"), rel + " must normalize ThemeSummary payload");
      assertTrue(gallery.contains("Array.isArray(themes)"), rel + " must coerce non-array");
    }
  }

  @Test
  void restServiceFindAllReturnsThemeSummaryListWithErrorHandling() throws Exception {
    Path root = resolve();
    Path java =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeRestService.java");
    String src = Files.readString(java, StandardCharsets.UTF_8);
    assertTrue(src.contains("PSThemeSummaryList findAll()"));
    assertTrue(src.contains("new PSThemeSummaryList("));
    // findAll body should log and rethrow as WebApplicationException like sibling methods
    int findAll = src.indexOf("public PSThemeSummaryList findAll()");
    int nextMethod = src.indexOf("@GET", findAll + 1);
    String body = src.substring(findAll, nextMethod > 0 ? nextMethod : findAll + 400);
    assertTrue(body.contains("WebApplicationException"), "findAll should convert failures to WAE");
    assertTrue(body.contains("log.error"), "findAll should log errors");
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
