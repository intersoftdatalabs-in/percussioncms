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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Guards the percCalendarTwo snippet template against unbalanced Velocity
 * block directives (issue #7034, PR #7061 review).
 *
 * <p>A duplicated {@code #foreach($googleSource in $googleCalendarArray)}
 * with a single {@code #end} nested the loops and stole a later {@code #end},
 * emitting N^2 buttons or failing to parse. The guard counts block openers
 * ({@code #if}, {@code #foreach}, {@code #macro}) against {@code #end} and
 * pins the source-filter loop to a single copy.
 */
class PercCalendarTwoSnippetDirectiveBalanceTest {

  /** Snippet template, relative to the Packages payload root. */
  private static final String SNIPPET_REL =
      "perc.widget.calendar/widgets/percCalendarTwo/templates/percCalendarTwoSnippet.vm";

  /** Block directives that must each be closed by an {@code #end}. */
  private static final Pattern OPENER = Pattern.compile("#(if|foreach|macro)\\b");

  /** Block closer. */
  private static final Pattern CLOSER = Pattern.compile("#end\\b");

  /** The source-filter loop signature — duplicated verbatim by the #7034 defect. */
  private static final Pattern LOOP_OPENER = Pattern.compile("^\\s*#foreach\\(.*\\)\\s*$");

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

  private static String codeOnly(String line) {
    int comment = line.indexOf("##");
    return comment < 0 ? line : line.substring(0, comment);
  }

  private static int count(Pattern pattern, String text) {
    Matcher matcher = pattern.matcher(text);
    int found = 0;
    while (matcher.find()) {
      found++;
    }
    return found;
  }

  @Test
  void blockDirectivesAreBalanced() throws IOException {
    Path snippet = packagesRoot().resolve(SNIPPET_REL);
    assertTrue(Files.isRegularFile(snippet), "snippet template missing: " + snippet);
    String content = Files.readString(snippet, StandardCharsets.UTF_8);
    StringBuilder code = new StringBuilder();
    for (String line : content.split("\\R")) {
      code.append(codeOnly(line)).append('\n');
    }
    int openers = count(OPENER, code.toString());
    int closers = count(CLOSER, code.toString());
    assertTrue(openers > 0, "expected block directives in " + SNIPPET_REL);
    assertEquals(
        openers,
        closers,
        SNIPPET_REL + " has unbalanced Velocity blocks: " + openers + " openers vs " + closers + " #end");
  }

  @Test
  void loopOpenerIsNotDuplicated() throws IOException {
    Path snippet = packagesRoot().resolve(SNIPPET_REL);
    assertTrue(Files.isRegularFile(snippet), "snippet template missing: " + snippet);
    String content = Files.readString(snippet, StandardCharsets.UTF_8);
    List<String> codeLines = new ArrayList<>();
    for (String line : content.split("\\R")) {
      String code = codeOnly(line).trim();
      if (!code.isEmpty()) {
        codeLines.add(code);
      }
    }
    for (int i = 1; i < codeLines.size(); i++) {
      String line = codeLines.get(i);
      if (LOOP_OPENER.matcher(line).matches() && line.equals(codeLines.get(i - 1))) {
        fail(SNIPPET_REL + " repeats a #foreach opener with no #end between: " + line);
      }
    }
  }
}
