package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Regression for GH-910 / v8.1.7 PR #911: PercDataTable must not require global I18N. */
class PercDataTableI18nGuardTest {
  // The src copy is pretty-printed (newlines + indentation around the ternary
  // operator) while the war copy is minified to a single line. Collapse
  // arbitrary whitespace between the guard and the fallback so both layouts
  // match the same assertion.
  private static final Pattern GUARDED_FALLBACK =
      Pattern.compile(
          "typeof\\s+I18N\\s*===\\s*\"undefined\"\\s*\\?\\s*\"No Pages Found\"");

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
      // Assert the JS string LITERAL "No Pages Found" (with the surrounding
      // quotes). The pre-fix source only contained the substring
      // `No Pages Found` inside the longer TMX key
      // `perc.ui.workflow.status.gadget@No Pages Found`, so an unquoted
      // `js.contains("No Pages Found")` would have passed even on the
      // unguarded code. The quoted literal exists only in the new ternary
      // fallback that the guard selects when I18N is undefined.
      assertTrue(
          js.contains("\"No Pages Found\""),
          rel + " must include the literal \"No Pages Found\" fallback");
      // Verify the guard context: the `typeof I18N === "undefined"` check
      // must guard the `No Pages Found` literal in the same ternary
      // expression. Without this, the two preceding assertions could be
      // satisfied by an unrelated `typeof I18N === "undefined"` branch
      // elsewhere in the file. The regex permits arbitrary whitespace
      // between the `?` and the fallback so it matches both the
      // pretty-printed src copy and the minified war copy.
      assertTrue(
          GUARDED_FALLBACK.matcher(js).find(),
          rel + " must wire the fallback into the typeof-I18N-undefined ternary");
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
