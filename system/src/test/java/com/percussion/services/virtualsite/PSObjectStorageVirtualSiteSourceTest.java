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

import com.percussion.services.virtualsite.VirtualSiteConfig.ObjectsSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PSObjectStorageVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadMarkdownHtmlAndJsonCatalog() throws Exception {
    Path root = writeSite(tempDir.resolve("obj-site"), null);
    Path version = root.resolve("8.2");
    Files.writeString(
        version.resolve("index.md"),
        """
        ---
        id: home
        title: Home
        order: 1
        ---
        Welcome **object**.
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        version.resolve("install.html"),
        "<html><head><title>Install</title></head><body><p>See home.</p></body></html>",
        StandardCharsets.UTF_8);
    Files.writeString(
        version.resolve("pages.json"),
        """
        {"pages":[{"id":"plain","title":"Plain","body":"From catalog.","order":3}]}
        """,
        StandardCharsets.UTF_8);

    PSObjectStorageVirtualSiteSource source = new PSObjectStorageVirtualSiteSource();
    assertEquals(VirtualSiteSourceType.OBJECT_STORAGE.wireName(), source.sourceType());

    VirtualSiteConfig config = config(root, null);
    List<VirtualItemRef> refs = source.discover(config);
    assertEquals(3, refs.size());
    // order 0 (html stem) then frontmatter order 1 then catalog order 3
    assertEquals("install", refs.get(0).id());
    assertEquals("Install", refs.get(0).title());
    assertEquals(Path.of("8.2", "install.html"), refs.get(0).relativePath());
    assertEquals("home", refs.get(1).id());
    assertEquals("Home", refs.get(1).title());
    assertEquals(Path.of("8.2", "index.md"), refs.get(1).relativePath());
    assertEquals("plain", refs.get(2).id());
    assertEquals(Path.of("8.2", "plain.html"), refs.get(2).relativePath());

    VirtualItem html = source.load(config, refs.get(0));
    assertTrue(html.markdownBody().contains("See home."));
    VirtualItem home = source.load(config, refs.get(1));
    assertEquals("home", home.frontmatter().id());
    assertTrue(home.markdownBody().contains("Welcome **object**."));
    VirtualItem catalog = source.load(config, refs.get(2));
    assertTrue(catalog.markdownBody().contains("From catalog."));
    assertEquals("pages.json", catalog.absolutePath().getFileName().toString());
  }

  @Test
  void filenameStemUsedWhenFrontmatterOmitted() throws Exception {
    Path root = writeSite(tempDir.resolve("stem-site"), null);
    Files.writeString(
        root.resolve("8.2").resolve("getting-started.md"),
        "# Getting started\nBody without fences.\n",
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs = new PSObjectStorageVirtualSiteSource().discover(config(root, null));
    assertEquals(1, refs.size());
    assertEquals("getting-started", refs.get(0).id());
    assertEquals("Getting started", refs.get(0).title());
    VirtualItem item = new PSObjectStorageVirtualSiteSource().load(config(root, null), refs.get(0));
    assertTrue(item.markdownBody().contains("Body without fences."));
  }

  @Test
  void objectsKeysListLimitsDiscovery() throws Exception {
    Path root = writeSite(tempDir.resolve("keys-site"), List.of("8.2/keep.md"));
    Path version = root.resolve("8.2");
    Files.writeString(
        version.resolve("keep.md"),
        """
        ---
        id: keep
        title: Keep
        ---
        kept
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        version.resolve("skip.md"),
        """
        ---
        id: skip
        title: Skip
        ---
        skipped
        """,
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSObjectStorageVirtualSiteSource().discover(config(root, List.of("8.2/keep.md")));
    assertEquals(1, refs.size());
    assertEquals("keep", refs.get(0).id());
  }

  @Test
  void missingListedKeyFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("missing-key"), List.of("8.2/gone.md"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                new PSObjectStorageVirtualSiteSource()
                    .discover(config(root, List.of("8.2/gone.md"))));
    assertTrue(ex.getMessage().toLowerCase().contains("not found"), ex.getMessage());
  }

  @Test
  void blankIdOrTitleInJsonFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("blank-id"), null);
    Files.writeString(
        root.resolve("8.2").resolve("pages.json"),
        """
        {"pages":[{"id":"","title":"MissingId","body":"x"}]}
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException idEx =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSObjectStorageVirtualSiteSource().discover(config(root, null)));
    assertTrue(idEx.getMessage().contains("id"), idEx.getMessage());

    Files.writeString(
        root.resolve("8.2").resolve("pages.json"),
        """
        {"pages":[{"id":"has-id","title":"","body":"x"}]}
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException titleEx =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSObjectStorageVirtualSiteSource().discover(config(root, null)));
    assertTrue(titleEx.getMessage().contains("title"), titleEx.getMessage());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("unknown-id"), null);
    Files.writeString(
        root.resolve("8.2").resolve("only.md"),
        """
        ---
        id: known
        title: Known
        ---
        Body
        """,
        StandardCharsets.UTF_8);
    PSObjectStorageVirtualSiteSource source = new PSObjectStorageVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, null);
    source.discover(cfg);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.md"), 0, "x");
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> source.load(cfg, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void pathTraversalAndAbsoluteKeysFailClosed() {
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () -> PSObjectStorageVirtualSiteSource.resolveObjectKey(tempDir, "../outside.md"));
    assertTrue(rel.getMessage().contains(".."), rel.getMessage());

    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () -> PSObjectStorageVirtualSiteSource.resolveObjectKey(tempDir, "/etc/passwd"));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());

    VirtualSiteException win =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSObjectStorageVirtualSiteSource.resolveObjectKey(tempDir, "C:/docs/evil.md"));
    assertTrue(win.getMessage().toLowerCase().contains("relative"), win.getMessage());
  }

  @Test
  void jsonPathTraversalFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("json-escape"), null);
    Files.writeString(
        root.resolve("8.2").resolve("pages.json"),
        """
        {"pages":[{"id":"esc","title":"Esc","body":"x","path":"../outside.html"}]}
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSObjectStorageVirtualSiteSource().discover(config(root, null)));
    assertTrue(ex.getMessage().contains("path"), ex.getMessage());
  }

  @Test
  void duplicateIdsFailClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("dup"), null);
    Path version = root.resolve("8.2");
    Files.writeString(
        version.resolve("a.md"),
        """
        ---
        id: same
        title: A
        ---
        a
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        version.resolve("b.md"),
        """
        ---
        id: same
        title: B
        ---
        b
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSObjectStorageVirtualSiteSource().discover(config(root, null)));
    assertTrue(ex.getMessage().toLowerCase().contains("duplicate"), ex.getMessage());
  }

  @Test
  void loadRereadsCurrentObjectBytesWithoutCache() throws Exception {
    Path root = writeSite(tempDir.resolve("live"), null);
    Path md = root.resolve("8.2").resolve("live.md");
    Files.writeString(
        md,
        """
        ---
        id: live
        title: First
        ---
        token-AAA
        """,
        StandardCharsets.UTF_8);
    PSObjectStorageVirtualSiteSource source = new PSObjectStorageVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, null);
    VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(first.markdownBody().contains("token-AAA"));

    Files.writeString(
        md,
        """
        ---
        id: live
        title: Second
        ---
        token-BBB
        """,
        StandardCharsets.UTF_8);
    VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
    assertEquals("Second", second.frontmatter().title());
    assertTrue(second.markdownBody().contains("token-BBB"));
    assertFalse(second.markdownBody().contains("token-AAA"));
  }

  @Test
  void secondBuildAfterObjectKeyAndConfigEditEmitsUpdatedHtmlWithoutRestart() throws Exception {
    Path root = writeSite(tempDir.resolve("obj-rebuild"), null);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${siteTitle}</h1><h2>${pageTitle}</h2>${content}</body></html>",
        StandardCharsets.UTF_8);
    writeObjYaml(root, "First Site Title", null);
    Path md = root.resolve("8.2").resolve("index.md");
    Path htmlKey = root.resolve("8.2").resolve("install.html");
    Path json = root.resolve("8.2").resolve("pages.json");
    Files.writeString(
        md,
        """
        ---
        id: live-home
        title: First Title
        ---
        unique-token-AAA-md
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        htmlKey,
        "<html><head><title>First Install</title></head><body>unique-token-AAA-html</body></html>",
        StandardCharsets.UTF_8);
    Files.writeString(
        json,
        """
        {"pages":[{"id":"plain","title":"First Catalog","body":"unique-token-AAA-json"}]}
        """,
        StandardCharsets.UTF_8);

    Path out = tempDir.resolve("obj-rebuild-out");
    PSObjectStorageVirtualSiteSource source = new PSObjectStorageVirtualSiteSource();
    PSVirtualSiteBuildService service =
        new PSVirtualSiteBuildService(source, new PSInMemoryVirtualParticipantService());

    PSVirtualSiteBuildResult first = service.build(root, out, "obj-docs");
    assertEquals(3, first.pageCount());
    Path homeHtml = out.resolve("8.2").resolve("index.html");
    Path installHtml = out.resolve("8.2").resolve("install.html");
    Path catalogHtml = out.resolve("8.2").resolve("plain.html");
    assertTrue(Files.isRegularFile(homeHtml), "missing " + homeHtml);
    assertTrue(Files.isRegularFile(installHtml), "missing " + installHtml);
    assertTrue(Files.isRegularFile(catalogHtml), "missing " + catalogHtml);
    String firstHome = Files.readString(homeHtml, StandardCharsets.UTF_8);
    String firstInstall = Files.readString(installHtml, StandardCharsets.UTF_8);
    String firstCatalog = Files.readString(catalogHtml, StandardCharsets.UTF_8);
    assertTrue(firstHome.contains("First Site Title"), firstHome);
    assertTrue(firstHome.contains("First Title"), firstHome);
    assertTrue(firstHome.contains("unique-token-AAA-md"), firstHome);
    assertTrue(firstInstall.contains("First Install"), firstInstall);
    assertTrue(firstInstall.contains("unique-token-AAA-html"), firstInstall);
    assertTrue(firstCatalog.contains("First Catalog"), firstCatalog);
    assertTrue(firstCatalog.contains("unique-token-AAA-json"), firstCatalog);

    Files.writeString(
        md,
        """
        ---
        id: live-home
        title: Second Title
        ---
        unique-token-BBB-md
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        htmlKey,
        "<html><head><title>Second Install</title></head><body>unique-token-BBB-html</body></html>",
        StandardCharsets.UTF_8);
    Files.writeString(
        json,
        """
        {"pages":[{"id":"plain","title":"Second Catalog","body":"unique-token-BBB-json"}]}
        """,
        StandardCharsets.UTF_8);
    writeObjYaml(root, "Second Site Title", null);

    VirtualSiteConfig reloaded =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "obj-docs");
    assertEquals("Second Site Title", reloaded.siteTitle());
    List<VirtualItemRef> refs = source.discover(reloaded);
    assertEquals(3, refs.size());
    VirtualItem loadedMd = source.load(reloaded, findRef(refs, "live-home"));
    assertEquals("Second Title", loadedMd.frontmatter().title());
    assertTrue(loadedMd.markdownBody().contains("unique-token-BBB-md"), loadedMd.markdownBody());
    assertFalse(loadedMd.markdownBody().contains("unique-token-AAA-md"), loadedMd.markdownBody());
    VirtualItem loadedHtml = source.load(reloaded, findRef(refs, "install"));
    assertEquals("Second Install", loadedHtml.frontmatter().title());
    assertTrue(
        loadedHtml.markdownBody().contains("unique-token-BBB-html"), loadedHtml.markdownBody());
    VirtualItem loadedJson = source.load(reloaded, findRef(refs, "plain"));
    assertEquals("Second Catalog", loadedJson.frontmatter().title());
    assertTrue(
        loadedJson.markdownBody().contains("unique-token-BBB-json"), loadedJson.markdownBody());

    PSVirtualSiteBuildResult second = service.build(root, out, "obj-docs");
    assertEquals(3, second.pageCount());
    String secondHome = Files.readString(homeHtml, StandardCharsets.UTF_8);
    String secondInstall = Files.readString(installHtml, StandardCharsets.UTF_8);
    String secondCatalog = Files.readString(catalogHtml, StandardCharsets.UTF_8);
    assertTrue(secondHome.contains("Second Site Title"), secondHome);
    assertTrue(secondHome.contains("Second Title"), secondHome);
    assertTrue(secondHome.contains("unique-token-BBB-md"), secondHome);
    assertFalse(secondHome.contains("unique-token-AAA-md"), secondHome);
    assertFalse(secondHome.contains("First Title"), secondHome);
    assertFalse(secondHome.contains("First Site Title"), secondHome);
    assertTrue(secondInstall.contains("Second Install"), secondInstall);
    assertTrue(secondInstall.contains("unique-token-BBB-html"), secondInstall);
    assertFalse(secondInstall.contains("unique-token-AAA-html"), secondInstall);
    assertTrue(secondCatalog.contains("Second Catalog"), secondCatalog);
    assertTrue(secondCatalog.contains("unique-token-BBB-json"), secondCatalog);
    assertFalse(secondCatalog.contains("unique-token-AAA-json"), secondCatalog);
    assertNotEquals(firstHome, secondHome);
    assertNotEquals(firstInstall, secondInstall);
    assertNotEquals(firstCatalog, secondCatalog);
  }

  @Test
  void skipsThemeAndDotFiles() throws Exception {
    Path root = writeSite(tempDir.resolve("skip-hidden"), null);
    Files.writeString(
        root.resolve("8.2").resolve("page.md"),
        """
        ---
        id: page
        title: Page
        ---
        ok
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("secret.md"),
        """
        ---
        id: secret
        title: Secret
        ---
        no
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("8.2").resolve(".hidden.md"),
        """
        ---
        id: hidden
        title: Hidden
        ---
        no
        """,
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs = new PSObjectStorageVirtualSiteSource().discover(config(root, null));
    assertEquals(1, refs.size());
    assertEquals("page", refs.get(0).id());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsTempRootAndLogicalKey() throws Exception {
    Path root = writeSite(tempDir.resolve("win-root"), List.of("8.2/getting-started/install.md"));
    assertTrue(root.isAbsolute());
    assertTrue(root.toString().contains(":"));
    Path nested = root.resolve("8.2").resolve("getting-started");
    Files.createDirectories(nested);
    Files.writeString(
        nested.resolve("install.md"),
        """
        ---
        id: install
        title: Install
        ---
        windows
        """,
        StandardCharsets.UTF_8);
    Path resolved =
        PSObjectStorageVirtualSiteSource.resolveObjectKey(
            root, "8.2\\getting-started\\install.md");
    assertEquals(nested.resolve("install.md").normalize(), resolved.normalize());
    List<VirtualItemRef> refs =
        new PSObjectStorageVirtualSiteSource()
            .discover(config(root, List.of("8.2/getting-started/install.md")));
    assertEquals(1, refs.size());
    assertEquals(Path.of("8.2", "getting-started", "install.md"), refs.get(0).relativePath());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixTempRootAndLogicalKey() throws Exception {
    Path root = writeSite(tempDir.resolve("unix-root"), List.of("8.2/getting-started/install.md"));
    assertTrue(root.isAbsolute());
    assertTrue(root.toString().startsWith("/"));
    Path nested = root.resolve("8.2").resolve("getting-started");
    Files.createDirectories(nested);
    Files.writeString(
        nested.resolve("install.md"),
        """
        ---
        id: install
        title: Install
        ---
        unix
        """,
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSObjectStorageVirtualSiteSource()
            .discover(config(root, List.of("8.2/getting-started/install.md")));
    assertEquals(1, refs.size());
    assertEquals(Path.of("8.2", "getting-started", "install.md"), refs.get(0).relativePath());
  }

  @Test
  void omittedJsonPathDefaultsToIdHtmlUnderVersion() throws Exception {
    Path relative =
        PSObjectStorageVirtualSiteSource.resolvePagePath("", "plain-id", "8.2", "test");
    assertEquals(Path.of("8.2", "plain-id.html"), relative);
  }

  @Test
  void factorySelectsObjectStorageAndLeavesPeersUnchanged() throws Exception {
    IPSVirtualSiteSource source =
        PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.OBJECT_STORAGE);
    assertInstanceOf(PSObjectStorageVirtualSiteSource.class, source);
    assertEquals("object-storage", source.sourceType());
    IPSVirtualSiteSource byName = PSVirtualSiteSourceFactory.createFromWireName("object-storage");
    assertInstanceOf(PSObjectStorageVirtualSiteSource.class, byName);
    IPSVirtualSiteSource git = PSVirtualSiteSourceFactory.createFromWireName("git-filesystem");
    assertInstanceOf(PSGitFilesystemVirtualSiteSource.class, git);
    IPSVirtualSiteSource csv = PSVirtualSiteSourceFactory.createFromWireName("csv-filesystem");
    assertInstanceOf(PSCsvFilesystemVirtualSiteSource.class, csv);
    IPSVirtualSiteSource sql = PSVirtualSiteSourceFactory.createFromWireName("sql-database");
    assertInstanceOf(PSSqlDatabaseVirtualSiteSource.class, sql);
    IPSVirtualSiteSource http = PSVirtualSiteSourceFactory.createFromWireName("http-json");
    assertInstanceOf(PSHttpJsonVirtualSiteSource.class, http);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("object-storage"));
    assertTrue(ex.getMessage().contains("http-json"));
  }

  @Test
  void buildServiceFactoryWiresObjectStorageAndEmitsHtml() throws Exception {
    Path root = writeSite(tempDir.resolve("build-obj"), null);
    Files.writeString(
        root.resolve("8.2").resolve("index.md"),
        """
        ---
        id: home
        title: Object Home
        ---
        Hello from objects.
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.OBJECT_STORAGE);
    assertInstanceOf(PSObjectStorageVirtualSiteSource.class, service.source());

    PSVirtualSiteBuildResult result = service.build(root, out, "obj-docs");
    assertEquals(1, result.pageCount());
    Path html = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("Object Home"), body);
    assertTrue(body.contains("Hello from objects"), body);
  }

  @Test
  void yamlLoaderParsesObjectsKeys() throws Exception {
    Path root = writeSite(tempDir.resolve("yaml-obj"), List.of("8.2/index.md"));
    Files.writeString(
        root.resolve("8.2").resolve("index.md"),
        """
        ---
        id: home
        title: Home
        ---
        body
        """,
        StandardCharsets.UTF_8);
    VirtualSiteConfig loaded =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "obj-docs");
    assertTrue(loaded.objects().hasKeys());
    assertEquals("8.2/index.md", loaded.objects().keys().get(0));
  }

  private static Path writeSite(Path root, List<String> keys) throws Exception {
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    writeObjYaml(root, "Object Docs", keys);
    return root;
  }

  private static void writeObjYaml(Path root, String siteTitle, List<String> keys)
      throws Exception {
    String objectsBlock = "";
    if (keys != null && !keys.isEmpty()) {
      StringBuilder sb = new StringBuilder("objects:\n  keys:\n");
      for (String key : keys) {
        sb.append("    - ").append(key).append('\n');
      }
      objectsBlock = sb.toString();
    }
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
        %s
        """
            .formatted(siteTitle, objectsBlock),
        StandardCharsets.UTF_8);
  }

  private static VirtualItemRef findRef(List<VirtualItemRef> refs, String id) {
    return refs.stream()
        .filter(r -> id.equals(r.id()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("missing ref " + id + " in " + refs));
  }

  private static VirtualSiteConfig config(Path root, List<String> keys) {
    ObjectsSpec spec = keys != null ? new ObjectsSpec(keys) : null;
    return new VirtualSiteConfig(
        root,
        "Object Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "obj-docs",
        null,
        null,
        spec);
  }
}
