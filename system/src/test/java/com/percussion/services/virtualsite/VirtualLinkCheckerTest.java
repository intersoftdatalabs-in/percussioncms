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
package com.percussion.services.virtualsite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Residual unit coverage for {@link VirtualLinkChecker} public helpers and path resolution.
 */
class VirtualLinkCheckerTest {

  private static final Map<String, String> IDS =
      Map.of(
          "home", "8.2/index.html",
          "install-overview", "8.2/getting-started/install.html");

  private static final Set<String> PATHS =
      Set.of("8.2/index.html", "8.2/getting-started/index.html", "8.2/getting-started/install.html");

  @Test
  void emptyOrNullBodyYieldsNoProblems() {
    assertTrue(VirtualLinkChecker.checkPage("s", "8.2", "8.2/index.md", null, IDS, PATHS).isEmpty());
    assertTrue(VirtualLinkChecker.checkPage("s", "8.2", "8.2/index.md", "", IDS, PATHS).isEmpty());
  }

  @Test
  void skipsExternalMailtoAndProtocolRelative() {
    String body =
        "See [web](https://example.com/docs) [http](http://example.com) "
            + "[mail](mailto:docs@example.com) [proto](//cdn.example.com/x) [frag](#section)";
    List<String> problems =
        VirtualLinkChecker.checkPage("s", "8.2", "8.2/index.md", body, IDS, PATHS);
    assertTrue(problems.isEmpty(), problems::toString);
  }

  @Test
  void reportsMissingRelativeMarkdownPath() {
    List<String> problems =
        VirtualLinkChecker.checkPage(
            "s",
            "8.2",
            "8.2/index.md",
            "See [missing](no-such-page.md)",
            IDS,
            PATHS);
    assertEquals(1, problems.size());
    assertTrue(problems.get(0).contains("no-such-page.md"), problems.get(0));
    assertTrue(problems.get(0).contains("8.2/no-such-page.html"), problems.get(0));
  }

  @Test
  void acceptsRelativePathThatResolvesToPublishedHtml() {
    List<String> problems =
        VirtualLinkChecker.checkPage(
            "s",
            "8.2",
            "8.2/index.md",
            "See [Install](getting-started/install.md#setup \"Install\")",
            IDS,
            PATHS);
    assertTrue(problems.isEmpty(), problems::toString);
  }

  @Test
  void reportsMissingStableId() {
    List<String> problems =
        VirtualLinkChecker.checkPage(
            "help",
            "8.2",
            "8.2/index.md",
            "Jump [x](id:does-not-exist)",
            IDS,
            PATHS);
    assertEquals(1, problems.size());
    assertTrue(problems.get(0).contains("does-not-exist"), problems.get(0));
    assertTrue(problems.get(0).contains("help"), problems.get(0));
  }

  @Test
  void acceptsKnownStableId() {
    List<String> problems =
        VirtualLinkChecker.checkPage(
            "s",
            "8.2",
            "8.2/index.md",
            "Jump [Install](id:install-overview)",
            IDS,
            PATHS);
    assertTrue(problems.isEmpty(), problems::toString);
  }

  @Test
  void stripsAngleBracketTargetsAndQuotedTitles() {
    List<String> problems =
        VirtualLinkChecker.checkPage(
            "s",
            "8.2",
            "8.2/index.md",
            "See [a](<getting-started/install.md> \"t\")",
            IDS,
            PATHS);
    assertTrue(problems.isEmpty(), problems::toString);
  }

  @Test
  void resolveRelativeHandlesDotDotAndRootSlash() {
    assertEquals(
        "8.2/getting-started/install.html",
        VirtualLinkChecker.resolveRelative(
            "8.2/getting-started/index.md", "../getting-started/install.html"));
    assertEquals(
        "admin/index.html",
        VirtualLinkChecker.resolveRelative("8.2/index.md", "/admin/index.html"));
    assertEquals(
        "a/b",
        VirtualLinkChecker.normalizePosix("a/./b/../b"));
  }

  @Test
  void pathSetNormalizesBackslashes() {
    Set<String> set = VirtualLinkChecker.pathSet(List.of("8.2\\index.html", "a/b.html"));
    assertTrue(set.contains("8.2/index.html"));
    assertTrue(set.contains("a/b.html"));
    assertEquals(2, set.size());
  }
}
