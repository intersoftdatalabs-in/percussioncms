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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.virtualsite.VirtualSiteConfig.LlmsSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PSLlmsTxtVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadFromLocalLlmsFixture() throws Exception {
    Path root = writeSite(tempDir.resolve("llms-file"), "llms.txt");
    Files.writeString(
        root.resolve("llms.txt"),
        llmsLink("Quickstart", "docs/quickstart.md", "Hello-from-llms"),
        StandardCharsets.UTF_8);
    PSLlmsTxtVirtualSiteSource source = new PSLlmsTxtVirtualSiteSource();
    assertEquals(VirtualSiteSourceType.LLMS_TXT.wireName(), source.sourceType());
    VirtualSiteConfig cfg = config(root, "llms.txt");
    List<VirtualItemRef> refs = source.discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("Quickstart-1", refs.get(0).id());
    assertEquals("Quickstart", refs.get(0).title());
    assertEquals(Path.of("8.2", "Quickstart-1.html"), refs.get(0).relativePath());
    VirtualItem item = source.load(cfg, refs.get(0));
    assertEquals("", item.frontmatter().description());
    assertTrue(item.markdownBody().contains("Hello-from-llms"), item.markdownBody());
    assertTrue(item.markdownBody().contains("docs/quickstart.md"), item.markdownBody());
    assertEquals("llms.txt", item.absolutePath().getFileName().toString());
  }

  @Test
  void omittedLlmsMappingDefaultsToLlmsTxt() throws Exception {
    Path root = writeSite(tempDir.resolve("default-llms"), null);
    Files.writeString(
        root.resolve(PSLlmsTxtVirtualSiteSource.DEFAULT_LLMS_FILE),
        llmsLink("Guide", "guide.md", "notes"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "llms-docs");
    List<VirtualItemRef> refs = new PSLlmsTxtVirtualSiteSource().discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("Guide-1", refs.get(0).id());
  }

  @Test
  void headingOnlyFixtureStillEmitsOnePage() throws Exception {
    Path root = writeSite(tempDir.resolve("heading-only"), "llms.txt");
    Files.writeString(
        root.resolve("llms.txt"),
        "# Product docs\n\n> operators: no LLM links configured yet\n",
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs = new PSLlmsTxtVirtualSiteSource().discover(config(root, "llms.txt"));
    assertEquals(1, refs.size());
    assertEquals("llms", refs.get(0).id());
    assertEquals("Product docs", refs.get(0).title());
    VirtualItem item =
        new PSLlmsTxtVirtualSiteSource().load(config(root, "llms.txt"), refs.get(0));
    assertTrue(item.markdownBody().contains("no LLM links configured yet"), item.markdownBody());
  }

  @Test
  void blankLinkTitleFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("blank-title"), "llms.txt");
    Files.writeString(
        root.resolve("llms.txt"),
        "- [](docs/page.md): notes\n",
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSLlmsTxtVirtualSiteSource().discover(config(root, "llms.txt")));
    assertTrue(ex.getMessage().contains("title"), ex.getMessage());
  }

  @Test
  void emptyLlmsFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("empty-llms"), "llms.txt");
    Files.writeString(root.resolve("llms.txt"), "   \n", StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSLlmsTxtVirtualSiteSource().discover(config(root, "llms.txt")));
    assertTrue(ex.getMessage().toLowerCase().contains("empty"), ex.getMessage());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("unknown-id"), "llms.txt");
    Files.writeString(
        root.resolve("llms.txt"),
        llmsLink("Quickstart", "docs/quickstart.md", ""),
        StandardCharsets.UTF_8);
    PSLlmsTxtVirtualSiteSource source = new PSLlmsTxtVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "llms.txt");
    source.discover(cfg);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.html"), 0, "x");
    VirtualSiteException ex = assertThrows(VirtualSiteException.class, () -> source.load(cfg, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void llmsUrlIsRejected() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-url"), "llms.txt");
    Files.writeString(
        root.resolve("llms.txt"),
        llmsLink("Quickstart", "docs/quickstart.md", ""),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        new VirtualSiteConfig(
            root,
            "Llms Docs",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(),
            "llms-docs",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new LlmsSpec("https://example.com/llms.txt", "llms.txt"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> new PSLlmsTxtVirtualSiteSource().discover(cfg));
    assertTrue(ex.getMessage().contains("llms.url"), ex.getMessage());
    assertTrue(
        ex.getMessage().toLowerCase().contains("live") || ex.getMessage().contains("remote"),
        ex.getMessage());
  }

  @Test
  void remoteLinkHrefFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-href"), "llms.txt");
    Files.writeString(
        root.resolve("llms.txt"),
        """
        # Product
        - [API](https://example.com/api.md): live
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSLlmsTxtVirtualSiteSource().discover(config(root, "llms.txt")));
    assertTrue(ex.getMessage().toLowerCase().contains("link"), ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("live"), ex.getMessage());
  }

  @Test
  void localLinkHrefIsDocumentedNotFetched() throws Exception {
    Path root = writeSite(tempDir.resolve("local-href"), "llms.txt");
    Files.writeString(
        root.resolve("llms.txt"),
        llmsLink("API", "docs/api.md", "local notes"),
        StandardCharsets.UTF_8);
    VirtualItem item =
        new PSLlmsTxtVirtualSiteSource()
            .load(
                config(root, "llms.txt"),
                new PSLlmsTxtVirtualSiteSource().discover(config(root, "llms.txt")).get(0));
    assertTrue(item.markdownBody().contains("Link: docs/api.md"), item.markdownBody());
    assertTrue(item.markdownBody().contains("local notes"), item.markdownBody());
  }

  @Test
  void pathTraversalAndAbsolutePathFailClosed() {
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSLlmsTxtVirtualSiteSource.resolvePagePath(
                    "../outside.html", "esc", "8.2", "test"));
    assertTrue(rel.getMessage().contains("path"), rel.getMessage());
    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSLlmsTxtVirtualSiteSource.resolvePagePath(
                    "/etc/passwd.html", "esc", "8.2", "test"));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());
  }

  @Test
  void llmsFileTraversalFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSLlmsTxtVirtualSiteSource.resolveLlmsFile(
                    tempDir.resolve("q-escape"), "../outside.txt"));
    assertTrue(ex.getMessage().contains("file") || ex.getMessage().contains(".."), ex.getMessage());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsAbsoluteLlmsFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSLlmsTxtVirtualSiteSource.resolveLlmsFile(
                    tempDir.resolve("win-root"), "C:/docs/evil.txt"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixAbsoluteLlmsFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSLlmsTxtVirtualSiteSource.resolveLlmsFile(
                    tempDir.resolve("unix-root"), "/etc/passwd.txt"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  void loadRereadsLocalFileWithoutCache() throws Exception {
    Path root = writeSite(tempDir.resolve("live"), "llms.txt");
    Path llms = root.resolve("llms.txt");
    Files.writeString(
        llms, llmsLink("First", "docs/a.md", "token-AAA"), StandardCharsets.UTF_8);
    PSLlmsTxtVirtualSiteSource source = new PSLlmsTxtVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "llms.txt");
    VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(first.markdownBody().contains("token-AAA"));
    Files.writeString(
        llms, llmsLink("Second", "docs/b.md", "token-BBB"), StandardCharsets.UTF_8);
    VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
    assertEquals("Second", second.frontmatter().title());
    assertTrue(second.markdownBody().contains("token-BBB"));
    assertFalse(second.markdownBody().contains("token-AAA"));
  }

  @Test
  void factorySelectsLlmsTxtAndLeavesPeersUnchanged() throws Exception {
    IPSVirtualSiteSource source =
        PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.LLMS_TXT);
    assertInstanceOf(PSLlmsTxtVirtualSiteSource.class, source);
    assertEquals("llms-txt", source.sourceType());
    IPSVirtualSiteSource byName = PSVirtualSiteSourceFactory.createFromWireName("llms-txt");
    assertInstanceOf(PSLlmsTxtVirtualSiteSource.class, byName);
    assertInstanceOf(
        PSGitFilesystemVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("git-filesystem"));
    assertInstanceOf(
        PSCsvFilesystemVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("csv-filesystem"));
    assertInstanceOf(
        PSSqlDatabaseVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("sql-database"));
    assertInstanceOf(
        PSHttpJsonVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("http-json"));
    assertInstanceOf(
        PSObjectStorageVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("object-storage"));
    assertInstanceOf(
        PSRssAtomVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("rss-atom"));
    assertInstanceOf(
        PSIcalendarVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("icalendar"));
    assertInstanceOf(
        PSSitemapXmlVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("sitemap-xml"));
    assertInstanceOf(
        PSRobotsTxtVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("robots-txt"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("llms-txt"));
    assertTrue(ex.getMessage().contains("robots-txt"));
  }

  @Test
  void buildServiceFactoryWiresLlmsTxtAndEmitsHtml() throws Exception {
    Path root = writeSite(tempDir.resolve("build-llms"), "llms.txt");
    Files.writeString(
        root.resolve("llms.txt"),
        llmsLink("Quickstart", "docs/quickstart.md", "Hello-from-llms"),
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.LLMS_TXT);
    assertInstanceOf(PSLlmsTxtVirtualSiteSource.class, service.source());

    PSVirtualSiteBuildResult result = service.build(root, out, "llms-docs");
    assertTrue(result.pageCount() > 0);
    Path html = out.resolve("8.2").resolve("Quickstart-1.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("Quickstart"), body);
    assertTrue(body.contains("Hello-from-llms"), body);
  }

  @Test
  void secondBuildAfterLlmsAndConfigEditEmitsUpdatedHtmlWithoutRestart() throws Exception {
    Path root = writeSite(tempDir.resolve("llms-rebuild"), "llms.txt");
    writeLlmsYaml(root, "First Site Title", "llms.txt");
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${siteTitle}</h1><h2>${pageTitle}</h2>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path llms = root.resolve("llms.txt");
    Files.writeString(
        llms, llmsLink("First", "docs/a.md", "unique-token-AAA"), StandardCharsets.UTF_8);

    Path out = tempDir.resolve("llms-rebuild-out");
    PSLlmsTxtVirtualSiteSource source = new PSLlmsTxtVirtualSiteSource();
    PSVirtualSiteBuildService service =
        new PSVirtualSiteBuildService(source, new PSInMemoryVirtualParticipantService());

    PSVirtualSiteBuildResult first = service.build(root, out, "llms-docs");
    assertEquals(1, first.pageCount());
    Path firstHtmlPath = out.resolve("8.2").resolve("First-1.html");
    assertTrue(Files.isRegularFile(firstHtmlPath), "missing " + firstHtmlPath);
    String firstHtml = Files.readString(firstHtmlPath, StandardCharsets.UTF_8);
    assertTrue(firstHtml.contains("First Site Title"), firstHtml);
    assertTrue(firstHtml.contains("unique-token-AAA"), firstHtml);

    Files.writeString(
        llms, llmsLink("Second", "docs/b.md", "unique-token-BBB"), StandardCharsets.UTF_8);
    writeLlmsYaml(root, "Second Site Title", "llms.txt");

    VirtualSiteConfig reloaded =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "llms-docs");
    assertEquals("Second Site Title", reloaded.siteTitle());
    assertEquals("llms.txt", reloaded.llms().file());
    List<VirtualItemRef> refs = source.discover(reloaded);
    assertEquals(1, refs.size());
    assertEquals("Second-1", refs.get(0).id());
    VirtualItem loaded = source.load(reloaded, refs.get(0));
    assertEquals("Second", loaded.frontmatter().title());
    assertTrue(loaded.markdownBody().contains("unique-token-BBB"), loaded.markdownBody());
    assertFalse(loaded.markdownBody().contains("unique-token-AAA"), loaded.markdownBody());

    PSVirtualSiteBuildResult second = service.build(root, out, "llms-docs");
    assertEquals(1, second.pageCount());
    Path secondHtmlPath = out.resolve("8.2").resolve("Second-1.html");
    assertTrue(Files.isRegularFile(secondHtmlPath), "missing " + secondHtmlPath);
    String secondHtml = Files.readString(secondHtmlPath, StandardCharsets.UTF_8);
    assertTrue(secondHtml.contains("Second Site Title"), secondHtml);
    assertTrue(secondHtml.contains("unique-token-BBB"), secondHtml);
    assertFalse(
        Files.exists(firstHtmlPath), "stale " + firstHtmlPath + " should be cleared on full rebuild");
    assertFalse(secondHtml.contains("unique-token-AAA"), secondHtml);
    assertFalse(secondHtml.contains("First Site Title"), secondHtml);
    assertNotEquals(firstHtml, secondHtml);
  }

  @Test
  void yamlLoaderParsesLlmsSpec() throws Exception {
    Path root = writeSite(tempDir.resolve("yaml-llms"), "custom-llms.txt");
    Files.writeString(
        root.resolve("custom-llms.txt"),
        llmsLink("Guide", "guide.md", ""),
        StandardCharsets.UTF_8);
    VirtualSiteConfig loaded =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "llms-docs");
    assertEquals("custom-llms.txt", loaded.llms().file());
  }

  @Test
  void omittedPathDefaultsToIdHtmlUnderVersion() throws Exception {
    Path relative = PSLlmsTxtVirtualSiteSource.resolvePagePath("", "plain-id", "8.2", "test");
    assertEquals(Path.of("8.2", "plain-id.html"), relative);
  }

  @Test
  void slugForPathStripsUnsafeChars() {
    assertEquals("Quickstart", PSLlmsTxtVirtualSiteSource.slugForPath("Quickstart"));
    assertEquals("bot-name", PSLlmsTxtVirtualSiteSource.slugForPath("bot name"));
    assertEquals("llms", PSLlmsTxtVirtualSiteSource.slugForPath(":::"));
  }

  private static Path writeSite(Path root, String file) throws Exception {
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    String llmsBlock;
    if (file != null && !file.isBlank()) {
      llmsBlock =
          """
          llms:
            file: %s
          """
              .formatted(file);
    } else {
      llmsBlock = "";
    }
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: Llms Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        %s
        """
            .formatted(llmsBlock),
        StandardCharsets.UTF_8);
    return root;
  }

  private static void writeLlmsYaml(Path root, String siteTitle, String file) throws Exception {
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: %s
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        llms:
          file: %s
        """
            .formatted(siteTitle, file),
        StandardCharsets.UTF_8);
  }

  private static VirtualSiteConfig config(Path root, String file) {
    LlmsSpec spec = file != null ? new LlmsSpec(null, file) : null;
    return new VirtualSiteConfig(
        root,
        "Llms Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "llms-docs",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        spec);
  }

  private static String llmsLink(String title, String href, String notes) {
    if (notes == null || notes.isBlank()) {
      return """
          # Product
          - [%s](%s)
          """
          .formatted(title, href);
    }
    return """
        # Product
        - [%s](%s): %s
        """
        .formatted(title, href, notes);
  }
}
