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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PSVirtualSiteBuildServiceTest {

  @TempDir Path tempDir;

  @Test
  void buildsSampleTreeAndRegistersParticipants() throws Exception {
    Path sample = resolveSampleDocs();
    Path out = tempDir.resolve("site-out");
    Path meta = tempDir.resolve("meta");

    PSInMemoryVirtualParticipantService participants =
        new PSInMemoryVirtualParticipantService(meta);
    PSVirtualSiteBuildService service =
        new PSVirtualSiteBuildService(new PSGitFilesystemVirtualSiteSource(), participants);

    PSVirtualSiteBuildResult result = service.build(sample, out, "sample");

    assertEquals(3, result.pageCount());
    assertFalse(result.hasLinkProblems(), () -> result.linkProblems().toString());

    Path home = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(home), "missing " + home);
    String html = Files.readString(home, StandardCharsets.UTF_8);
    assertTrue(html.contains("Sample Docs"), html);
    assertTrue(html.contains("Welcome") || html.contains("welcome"), html);
    // Body + nav links must be site-root HTML (not source .md)
    assertTrue(html.contains("/8.2/getting-started/index.html"), html);
    assertFalse(html.contains("getting-started/index.md"), html);
    assertTrue(html.contains("href=\"/8.2/getting-started/install.html\""), html);
    assertTrue(html.contains("class=\"vs-toc\""), html);
    assertTrue(html.contains("id=\"sample-overview\""), html);
    assertTrue(html.contains("href=\"#sample-overview\""), html);
    assertTrue(html.contains("id=\"sample-details\""), html);
    assertTrue(html.contains("href=\"#sample-details\""), html);

    Path install = out.resolve("8.2").resolve("getting-started").resolve("install.html");
    assertTrue(Files.isRegularFile(install));

    Optional<VirtualParticipant> p = participants.find("sample", "install-overview");
    assertTrue(p.isPresent());
    assertEquals("8.2/getting-started/install.html", p.get().publishedPath().replace('\\', '/'));

    participants.flush("sample");
    assertTrue(Files.isRegularFile(meta.resolve("participants-sample.jsonl")));

    // Second build replaces site registry (stale ids gone; current ids retained)
    participants.upsert(
        new VirtualParticipant("sample", "stale-only", "8.2", "8.2/stale.html", "8.2/stale.md"));
    PSVirtualSiteBuildResult second = service.build(sample, out, "sample");
    assertEquals(3, second.pageCount());
    assertTrue(participants.find("sample", "install-overview").isPresent());
    assertTrue(
        participants.find("sample", "stale-only").isEmpty(),
        "full rebuild must clear site then re-register only current pages");

    // Link report is always written next to static HTML for operator review
    Path linkReport = out.resolve("link-report.txt");
    assertTrue(Files.isRegularFile(linkReport), "missing link-report.txt");
    String report = Files.readString(linkReport, StandardCharsets.UTF_8);
    assertTrue(report.contains("OK"), report);
    assertTrue(result.writtenFiles().contains("link-report.txt"), result.writtenFiles()::toString);
  }

  @Test
  void secondBuildAfterMarkdownEditEmitsUpdatedHtmlWithoutRestart() throws Exception {
    Path siteRoot = tempDir.resolve("live-site");
    Path versionDir = siteRoot.resolve("8.2");
    Files.createDirectories(versionDir);
    Files.createDirectories(siteRoot.resolve("_theme"));
    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: Live Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: 8.2
            default: true
        theme:
          layout: page.html
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        siteRoot.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path indexMd = versionDir.resolve("index.md");
    Files.writeString(
        indexMd,
        """
        ---
        id: live-home
        title: First Title
        ---

        First body unique-token-AAA.
        """,
        StandardCharsets.UTF_8);

    Path out = tempDir.resolve("live-out");
    PSInMemoryVirtualParticipantService participants =
        new PSInMemoryVirtualParticipantService(tempDir.resolve("live-meta"));
    PSGitFilesystemVirtualSiteSource source = new PSGitFilesystemVirtualSiteSource();
    PSVirtualSiteBuildService service = new PSVirtualSiteBuildService(source, participants);

    PSVirtualSiteBuildResult first = service.build(siteRoot, out, "live");
    assertEquals(1, first.pageCount());
    Path html = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String firstHtml = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(firstHtml.contains("First Title"), firstHtml);
    assertTrue(firstHtml.contains("unique-token-AAA"), firstHtml);

    Files.writeString(
        indexMd,
        """
        ---
        id: live-home
        title: Second Title
        ---

        Second body unique-token-BBB.
        """,
        StandardCharsets.UTF_8);

    VirtualSiteConfig config =
        VirtualSiteConfigLoader.load(siteRoot, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "live");
    var refs = source.discover(config);
    assertEquals(1, refs.size());
    VirtualItem loaded = source.load(config, refs.get(0));
    assertEquals("Second Title", loaded.frontmatter().title());
    assertTrue(loaded.markdownBody().contains("unique-token-BBB"), loaded.markdownBody());

    PSVirtualSiteBuildResult second = service.build(siteRoot, out, "live");
    assertEquals(1, second.pageCount());
    String secondHtml = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(secondHtml.contains("Second Title"), secondHtml);
    assertTrue(secondHtml.contains("unique-token-BBB"), secondHtml);
    assertFalse(secondHtml.contains("unique-token-AAA"), secondHtml);
    assertFalse(secondHtml.contains("First Title"), secondHtml);
    assertNotEquals(firstHtml, secondHtml);
  }

  @Test
  void linkReportListsProblemsWhenBrokenLinksPresent() throws Exception {
    Path siteRoot = tempDir.resolve("broken-site");
    Path versionDir = siteRoot.resolve("8.2");
    Files.createDirectories(versionDir);
    Files.createDirectories(siteRoot.resolve("_theme"));
    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: Broken
        versions:
          - id: "8.2"
            label: "8.2"
            path: 8.2
            default: true
        theme:
          layout: page.html
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        siteRoot.resolve("_theme").resolve("page.html"),
        "<html><body>{{content}}</body></html>",
        StandardCharsets.UTF_8);
    Files.writeString(
        versionDir.resolve("index.md"),
        """
        ---
        id: broken-home
        title: Broken Home
        ---

        See [missing](nope.md).
        """,
        StandardCharsets.UTF_8);

    Path out = tempDir.resolve("broken-out");
    PSVirtualSiteBuildResult result =
        new PSVirtualSiteBuildService().build(siteRoot, out, "broken");

    assertTrue(result.hasLinkProblems(), "expected link problems");
    Path linkReport = out.resolve("link-report.txt");
    assertTrue(Files.isRegularFile(linkReport));
    String report = Files.readString(linkReport, StandardCharsets.UTF_8);
    assertTrue(report.contains("nope.md") || report.contains("nope.html"), report);
    assertFalse(report.contains("OK: no link problems"), report);
  }

  @Test
  void linkCheckerReportsMissingId() {
    var problems =
        VirtualLinkChecker.checkPage(
            "s",
            "8.2",
            "8.2/index.md",
            "See [x](id:missing-page)",
            java.util.Map.of("home", "8.2/index.html"),
            java.util.Set.of("8.2/index.html"));
    assertEquals(1, problems.size());
    assertTrue(problems.get(0).contains("missing-page"));
  }

  @Test
  void resolveHrefIsPortable() throws Exception {
    Path base = tempDir.resolve("out");
    Path resolved = PSVirtualSiteBuildService.resolveHref(base, "8.2/getting-started/install.html");
    assertEquals(
        base.resolve("8.2").resolve("getting-started").resolve("install.html").normalize(),
        resolved);
  }

  @Test
  void requireSafeBuildRootRejectsTraversalAndEmpty() throws Exception {
    assertThrows(
        VirtualSiteException.class,
        () -> PSVirtualSiteBuildService.requireSafeBuildRoot(Path.of("")));
    assertThrows(
        VirtualSiteException.class,
        () -> PSVirtualSiteBuildService.requireSafeBuildRoot(Path.of(".")));
    assertThrows(
        VirtualSiteException.class,
        () -> PSVirtualSiteBuildService.requireSafeBuildRoot(Path.of("a/../../etc")));
    Path ok = tempDir.resolve("safe-out");
    assertEquals(ok.normalize(), PSVirtualSiteBuildService.requireSafeBuildRoot(ok));
  }

  @Test
  void resolveHrefRejectsParentAndAbsoluteSegments() {
    Path base = tempDir.resolve("out");
    assertThrows(
        VirtualSiteException.class,
        () -> PSVirtualSiteBuildService.resolveHref(base, "../escape.html"));
    assertThrows(
        VirtualSiteException.class,
        () -> PSVirtualSiteBuildService.resolveHref(base, "a/../../escape.html"));
    assertThrows(
        VirtualSiteException.class,
        () -> PSVirtualSiteBuildService.resolveHref(base, "C:/windows/evil.html"));
  }

  @Test
  void buildRejectsUnsafeOutputRoot() throws Exception {
    Path sample = resolveSampleDocs();
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSVirtualSiteBuildService().build(sample, Path.of("."), "sample"));
    assertTrue(ex.getMessage().contains("outputRoot"), ex.getMessage());
  }

  private static Path resolveSampleDocs() throws Exception {
    URL url =
        PSVirtualSiteBuildServiceTest.class
            .getClassLoader()
            .getResource("virtualsite/sample-docs/_config.yaml");
    if (url == null) {
      throw new IllegalStateException("sample-docs fixture missing from test classpath");
    }
    Path config = Path.of(url.toURI());
    return config.getParent();
  }
}
