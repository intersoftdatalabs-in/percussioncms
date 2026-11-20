/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.packages.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Guards the adjudicated {@code mkd-code-review} {@code secrets.heuristic} FP
 * on the percCalendarTwo editor twins (issue #6777, ledger row
 * {@code mkd-6777}).
 *
 * <p>The heuristic fires on the property name {@code apiKey}, but the value is
 * the operator-typed Google Calendar browser key flowing exclusively through
 * editor-DOM {@code .val()} reads at runtime — restore in {@code renderEditor}
 * and collect in {@code getData}. No key literal exists in either twin (same
 * FP class as #6728/#52/#50: secret-worded names holding no secret).
 */
class PercCalendarTwoApiKeyHeuristicGuardTest {

  /** Both editor twins, relative to the Packages payload root. */
  private static final List<String> TWIN_RELS =
      List.of(
          "perc.widget.calendar/SupportFile-rx_resources/widgets/percCalendarTwo/js/percCalendarTwo.js",
          "perc.widget.calendar/sys__UserDependency--rx_resources/widgets/percCalendarTwo/js/percCalendarTwo.js");

  private static final String SUPPRESSION_MARKER = "mkd-code-review[secrets.heuristic]";

  /** Credential-shaped literals that must never appear in the twins. */
  private static final List<Pattern> CREDENTIAL_LITERALS =
      List.of(
          Pattern.compile("AIza[0-9A-Za-z_\\-]{10,}"),
          Pattern.compile("BEGIN [A-Z ]*PRIVATE KEY"),
          Pattern.compile("(ghp_|gho_|github_pat_|AKIA|ASIA|xox[bap]-|sk-live-)\\S+"),
          Pattern.compile("apiKey\\s*[:=]\\s*['\"][^'\"]+['\"]"),
          Pattern.compile("['\"][A-Za-z0-9+/=_\\-.]{40,}['\"]"));

  private static Path packagesRoot() {
    List<Path> candidates =
        List.of(
            Path.of(System.getProperty("user.dir"), "src", "main", "resources", "Packages"),
            Path.of(
                System.getProperty("user.dir"),
                "modules",
                "perc-packages",
                "src",
                "main",
                "resources",
                "Packages"));
    for (Path candidate : candidates) {
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
    }
    fail("Packages payload root not found under user.dir=" + System.getProperty("user.dir"));
    throw new IllegalStateException("unreachable");
  }

  private static List<String> linesOf(Path file) throws IOException {
    return List.of(Files.readString(file, StandardCharsets.UTF_8).split("\\R"));
  }

  @Test
  void apiKeyFlowsOnlyThroughDomReads() throws IOException {
    Path root = packagesRoot();
    for (String rel : TWIN_RELS) {
      List<String> apiKeyLines = new ArrayList<>();
      for (String line : linesOf(root.resolve(rel))) {
        if (line.contains("apiKey")) {
          apiKeyLines.add(line);
        }
      }
      assertEquals(
          2, apiKeyLines.size(), rel + " apiKey surface drifted — expected the restore + collect reads only");
      for (String line : apiKeyLines) {
        assertTrue(
            line.contains(".val("),
            rel + " apiKey must flow through an editor-DOM .val() read, no literal: " + line.trim());
      }
    }
  }

  @Test
  void noCredentialLiteralInTwins() throws IOException {
    Path root = packagesRoot();
    for (String rel : TWIN_RELS) {
      String content = Files.readString(root.resolve(rel), StandardCharsets.UTF_8);
      for (Pattern credential : CREDENTIAL_LITERALS) {
        assertTrue(
            !credential.matcher(content).find(),
            rel + " ships credential-shaped literal matching " + credential);
      }
    }
  }

  @Test
  void adjudicationSuppressionPinned() throws IOException {
    Path root = packagesRoot();
    for (String rel : TWIN_RELS) {
      int apiKeyLines = 0;
      int suppressedApiKeyLines = 0;
      for (String line : linesOf(root.resolve(rel))) {
        if (line.contains("apiKey")) {
          apiKeyLines++;
          if (line.contains(SUPPRESSION_MARKER)) {
            suppressedApiKeyLines++;
          }
        }
      }
      assertTrue(apiKeyLines > 0, rel + " has no apiKey line to pin");
      assertEquals(
          apiKeyLines,
          suppressedApiKeyLines,
          rel + " every apiKey sink must carry the mkd-6777 same-line suppression");
    }
  }
}
