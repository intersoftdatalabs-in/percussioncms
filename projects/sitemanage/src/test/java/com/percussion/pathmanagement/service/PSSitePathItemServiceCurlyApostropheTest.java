package com.percussion.pathmanagement.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-879 / v8.1.7 PR #931 (curly apostrophes) after the #913 / #1255 TMX migration.
 *
 * <p>User-facing site/page path errors are loaded from {@code CmsUi.tmx}; EN segments must use
 * U+2019 so SecureStringUtils HTML sanitization does not render {@code &#39;}. TMX keys may still
 * contain straight apostrophes (stable {@code tuid} strings); only the translated EN text is
 * required to use curly apostrophes.
 */
class PSSitePathItemServiceCurlyApostropheTest {
  private static final char CURLY = '\u2019';

  @Test
  void sitePathMessagesUseCurlyApostrophe() throws Exception {
    Path root = resolveRoot();
    Path java =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSSitePathItemService.java");
    Path tmx = root.resolve("modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx");
    if (!Files.isRegularFile(java)) {
      fail(java.toString());
    }
    if (!Files.isRegularFile(tmx)) {
      fail(tmx.toString());
    }

    String src = Files.readString(java, StandardCharsets.UTF_8);
    // Service must load messages from TMX rather than hardcoding EN user text.
    assertTrue(src.contains("PSI18NTranslationKeyValues"));
    // Must not hardcode straight-apostrophe user-facing English outside of i18n key strings.
    // Keys look like perc.ui.pathmanagement@Oops...; reject a bare throw message form.
    assertFalse(
        src.contains("? \"Oops.  We can"),
        "must not hardcode EN path-not-found text with straight apostrophe");

    String xml = Files.readString(tmx, StandardCharsets.UTF_8);
    assertTrue(
        xml.contains("We can" + CURLY + "t find the site"),
        "must use U+2019 curly apostrophe in TMX EN site-not-found message");
    assertTrue(
        xml.contains("We" + CURLY + "re sorry"),
        "must use U+2019 curly apostrophe in TMX EN page-not-found message");
  }

  private static Path resolveRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path up = cwd.resolve("../..").normalize();
    if (Files.isDirectory(up.resolve("projects/sitemanage"))) return up;
    if (Files.isDirectory(cwd.resolve("projects/sitemanage"))) return cwd;
    fail("could not resolve monorepo root (Surefire basedir projects/sitemanage expected)");
    return cwd;
  }
}
