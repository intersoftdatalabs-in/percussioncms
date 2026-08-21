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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PSCsvFilesystemVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadFromTempCsvTree() throws Exception {
    Path root = writeSite(tempDir.resolve("csv-site"));
    Path version = root.resolve("8.2");
    Files.writeString(
        version.resolve("pages.csv"),
        """
        id,title,body,path,order
        csv-home,Home,"Welcome **CSV**.",index.md,1
        csv-install,Install,"See [Home](id:csv-home).",getting-started/install.md,2
        """,
        StandardCharsets.UTF_8);

    PSCsvFilesystemVirtualSiteSource source = new PSCsvFilesystemVirtualSiteSource();
    assertEquals(VirtualSiteSourceType.CSV_FILESYSTEM.wireName(), source.sourceType());

    VirtualSiteConfig config = config(root);
    List<VirtualItemRef> refs = source.discover(config);
    assertEquals(2, refs.size());
    assertEquals("csv-home", refs.get(0).id());
    assertEquals("Home", refs.get(0).title());
    assertEquals(1, refs.get(0).order());
    assertEquals(
        Path.of("8.2", "index.md"),
        refs.get(0).relativePath());
    assertEquals(
        Path.of("8.2", "getting-started", "install.md"),
        refs.get(1).relativePath());

    VirtualItem home = source.load(config, refs.get(0));
    assertEquals("csv-home", home.frontmatter().id());
    assertEquals("Home", home.frontmatter().title());
    assertTrue(home.markdownBody().contains("Welcome **CSV**."));
    assertTrue(Files.isRegularFile(home.absolutePath()));
    assertEquals("pages.csv", home.absolutePath().getFileName().toString());

    VirtualItem install = source.load(config, refs.get(1));
    assertTrue(install.markdownBody().contains("id:csv-home"));
  }

  @Test
  void omittedPathDefaultsToIdMarkdownUnderVersion() throws Exception {
    Path root = writeSite(tempDir.resolve("default-path"));
    Files.writeString(
        root.resolve("8.2").resolve("pages.csv"),
        "id,title,body\nplain-id,Plain,Body text\n",
        StandardCharsets.UTF_8);

    List<VirtualItemRef> refs =
        new PSCsvFilesystemVirtualSiteSource().discover(config(root));
    assertEquals(1, refs.size());
    assertEquals(Path.of("8.2", "plain-id.md"), refs.get(0).relativePath());
  }

  @Test
  void missingRequiredColumnFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("missing-col"));
    Files.writeString(
        root.resolve("8.2").resolve("pages.csv"),
        "id,title\nx,Y\n",
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSCsvFilesystemVirtualSiteSource().discover(config(root)));
    assertTrue(ex.getMessage().contains("body"), ex.getMessage());
  }

  @Test
  void blankIdOrTitleFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("blank-id"));
    Files.writeString(
        root.resolve("8.2").resolve("pages.csv"),
        "id,title,body\n,MissingId,body\n",
        StandardCharsets.UTF_8);
    VirtualSiteException idEx =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSCsvFilesystemVirtualSiteSource().discover(config(root)));
    assertTrue(idEx.getMessage().contains("id"), idEx.getMessage());

    Path root2 = writeSite(tempDir.resolve("blank-title"));
    Files.writeString(
        root2.resolve("8.2").resolve("pages.csv"),
        "id,title,body\nhas-id,,body\n",
        StandardCharsets.UTF_8);
    VirtualSiteException titleEx =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSCsvFilesystemVirtualSiteSource().discover(config(root2)));
    assertTrue(titleEx.getMessage().contains("title"), titleEx.getMessage());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("unknown-id"));
    Files.writeString(
        root.resolve("8.2").resolve("pages.csv"),
        "id,title,body\nknown,Known,Body\n",
        StandardCharsets.UTF_8);
    PSCsvFilesystemVirtualSiteSource source = new PSCsvFilesystemVirtualSiteSource();
    VirtualSiteConfig config = config(root);
    source.discover(config);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.md"), 0, "x");
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> source.load(config, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void pathTraversalAndAbsolutePathFailClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("escape"));
    Files.writeString(
        root.resolve("8.2").resolve("pages.csv"),
        "id,title,body,path\nesc,Esc,body,../outside.md\n",
        StandardCharsets.UTF_8);
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSCsvFilesystemVirtualSiteSource().discover(config(root)));
    assertTrue(rel.getMessage().contains("path"), rel.getMessage());

    Path rootAbs = writeSite(tempDir.resolve("abs"));
    Files.writeString(
        rootAbs.resolve("8.2").resolve("pages.csv"),
        "id,title,body,path\nesc,Esc,body,/etc/passwd.md\n",
        StandardCharsets.UTF_8);
    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSCsvFilesystemVirtualSiteSource().discover(config(rootAbs)));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());

    Path rootWin = writeSite(tempDir.resolve("winabs"));
    Files.writeString(
        rootWin.resolve("8.2").resolve("pages.csv"),
        "id,title,body,path\nesc,Esc,body,C:/docs/evil.md\n",
        StandardCharsets.UTF_8);
    VirtualSiteException win =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSCsvFilesystemVirtualSiteSource().discover(config(rootWin)));
    assertTrue(win.getMessage().toLowerCase().contains("relative"), win.getMessage());
  }

  @Test
  void duplicateIdsFailClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("dup"));
    Path version = root.resolve("8.2");
    Files.writeString(
        version.resolve("a.csv"),
        "id,title,body\nsame,A,a\n",
        StandardCharsets.UTF_8);
    Files.writeString(
        version.resolve("b.csv"),
        "id,title,body\nsame,B,b\n",
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSCsvFilesystemVirtualSiteSource().discover(config(root)));
    assertTrue(ex.getMessage().toLowerCase().contains("duplicate"), ex.getMessage());
  }

  @Test
  void loadRereadsCurrentCsvBytesWithoutCache() throws Exception {
    Path root = writeSite(tempDir.resolve("live"));
    Path csv = root.resolve("8.2").resolve("pages.csv");
    Files.writeString(
        csv, "id,title,body\nlive,First,token-AAA\n", StandardCharsets.UTF_8);
    PSCsvFilesystemVirtualSiteSource source = new PSCsvFilesystemVirtualSiteSource();
    VirtualSiteConfig config = config(root);
    VirtualItem first = source.load(config, source.discover(config).get(0));
    assertTrue(first.markdownBody().contains("token-AAA"));

    Files.writeString(
        csv, "id,title,body\nlive,Second,token-BBB\n", StandardCharsets.UTF_8);
    VirtualItem second = source.load(config, source.discover(config).get(0));
    assertEquals("Second", second.frontmatter().title());
    assertTrue(second.markdownBody().contains("token-BBB"));
    assertFalse(second.markdownBody().contains("token-AAA"));
  }

  @Test
  void invalidOrderFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("bad-order"));
    Files.writeString(
        root.resolve("8.2").resolve("pages.csv"),
        "id,title,body,order\nx,T,b,not-an-int\n",
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSCsvFilesystemVirtualSiteSource().discover(config(root)));
    assertTrue(ex.getMessage().contains("order"), ex.getMessage());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsPathColumnAndTempRoot() throws Exception {
    Path root = writeSite(tempDir.resolve("win-root"));
    assertTrue(root.isAbsolute());
    assertTrue(root.toString().contains(":"));
    Files.writeString(
        root.resolve("8.2").resolve("pages.csv"),
        "id,title,body,path\nwin,Win,body,getting-started\\install\n",
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSCsvFilesystemVirtualSiteSource().discover(config(root));
    assertEquals(1, refs.size());
    assertEquals(Path.of("8.2", "getting-started", "install.md"), refs.get(0).relativePath());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixPathColumnAndTempRoot() throws Exception {
    Path root = writeSite(tempDir.resolve("unix-root"));
    assertTrue(root.isAbsolute());
    assertTrue(root.toString().startsWith("/"));
    Files.writeString(
        root.resolve("8.2").resolve("pages.csv"),
        "id,title,body,path\nunix,Unix,body,getting-started/install\n",
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSCsvFilesystemVirtualSiteSource().discover(config(root));
    assertEquals(1, refs.size());
    assertEquals(Path.of("8.2", "getting-started", "install.md"), refs.get(0).relativePath());
  }

  @Test
  void factorySelectsCsvFilesystem() throws Exception {
    IPSVirtualSiteSource source =
        PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.CSV_FILESYSTEM);
    assertTrue(source instanceof PSCsvFilesystemVirtualSiteSource);
    assertEquals("csv-filesystem", source.sourceType());
    IPSVirtualSiteSource git =
        PSVirtualSiteSourceFactory.createFromWireName("git-filesystem");
    assertTrue(git instanceof PSGitFilesystemVirtualSiteSource);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
  }

  @Test
  void buildServiceFactoryWiresCsvAndEmitsHtml() throws Exception {
    Path root = writeSite(tempDir.resolve("build-csv"));
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("8.2").resolve("pages.csv"),
        "id,title,body,path,order\nhome,CSV Home,Hello from CSV.,index.md,1\n",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.CSV_FILESYSTEM);
    assertTrue(service.source() instanceof PSCsvFilesystemVirtualSiteSource);

    PSVirtualSiteBuildResult result = service.build(root, out, "csv-docs");
    assertEquals(1, result.pageCount());
    Path html = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("CSV Home"), body);
    assertTrue(body.contains("Hello from CSV"), body);
  }

  @Test
  void buildServiceOptionalConfigYamlEmitsHtml() throws Exception {
    Path root = tempDir.resolve("build-csv-noconfig");
    Files.createDirectories(root.resolve("8.2"));
    Files.writeString(
        root.resolve("8.2").resolve("pages.csv"),
        "id,title,body,path\nhome,Bare CSV,Hello.,index.md\n",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-csv-noconfig-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.CSV_FILESYSTEM);
    PSVirtualSiteBuildResult result = service.build(root, out, "csv-docs");
    assertEquals(1, result.pageCount());
    Path html = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("Bare CSV"), body);
  }

  private static Path writeSite(Path root) throws Exception {
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: CSV Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: 8.2
            default: true
        theme:
          layout: page.html
        """,
        StandardCharsets.UTF_8);
    return root;
  }

  private static VirtualSiteConfig config(Path root) {
    return new VirtualSiteConfig(
        root,
        "CSV Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "csv-docs");
  }
}
