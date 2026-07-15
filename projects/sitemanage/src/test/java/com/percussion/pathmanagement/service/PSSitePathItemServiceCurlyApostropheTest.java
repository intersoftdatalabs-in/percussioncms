package com.percussion.pathmanagement.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Regression for GH-879 / v8.1.7 PR #931: path error messages use curly apostrophes. */
class PSSitePathItemServiceCurlyApostropheTest {
  @Test
  void sitePathMessagesUseCurlyApostrophe() throws Exception {
    Path root = resolveRoot();
    Path java =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSSitePathItemService.java");
    if (!Files.isRegularFile(java)) fail(java.toString());
    String src = Files.readString(java, StandardCharsets.UTF_8);
    assertTrue(src.contains("\u2019"), "must use U+2019 curly apostrophe in error messages");
    assertFalse(src.contains("can't find the site"), "must not use straight apostrophe in can't");
    assertFalse(src.contains("We're sorry"), "must not use straight apostrophe in We're");
  }

  private static Path resolveRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path up = cwd.resolve("../..").normalize();
    if (Files.isDirectory(up.resolve("projects/sitemanage"))) return up;
    if (Files.isDirectory(cwd.resolve("projects/sitemanage"))) return cwd;
    fail("no root");
    return cwd;
  }
}
