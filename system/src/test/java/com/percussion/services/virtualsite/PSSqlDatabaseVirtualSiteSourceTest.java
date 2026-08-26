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

import com.percussion.services.virtualsite.VirtualSiteConfig.SqlSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PSSqlDatabaseVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadFromInMemoryH2() throws Exception {
    String jdbc = newMemUrl();
    seedPages(
        jdbc,
        """
        INSERT INTO pages (id, title, body, path, sort_order) VALUES
        ('sql-home', 'Home', 'Welcome **SQL**.', 'index.md', 1),
        ('sql-install', 'Install', 'See [Home](id:sql-home).', 'getting-started/install.md', 2)
        """);
    Path root = writeSite(tempDir.resolve("sql-site"), jdbc, inlineSelect(), null);

    PSSqlDatabaseVirtualSiteSource source = new PSSqlDatabaseVirtualSiteSource();
    assertEquals(VirtualSiteSourceType.SQL_DATABASE.wireName(), source.sourceType());

    VirtualSiteConfig config = config(root, jdbc, inlineSelect(), null);
    List<VirtualItemRef> refs = source.discover(config);
    assertEquals(2, refs.size());
    assertEquals("sql-home", refs.get(0).id());
    assertEquals("Home", refs.get(0).title());
    assertEquals(1, refs.get(0).order());
    assertEquals(Path.of("8.2", "index.md"), refs.get(0).relativePath());
    assertEquals(Path.of("8.2", "getting-started", "install.md"), refs.get(1).relativePath());

    VirtualItem home = source.load(config, refs.get(0));
    assertEquals("sql-home", home.frontmatter().id());
    assertEquals("Home", home.frontmatter().title());
    assertTrue(home.markdownBody().contains("Welcome **SQL**."));
    VirtualItem install = source.load(config, refs.get(1));
    assertTrue(install.markdownBody().contains("id:sql-home"));
  }

  @Test
  void omittedPathDefaultsToIdMarkdownUnderVersion() throws Exception {
    String jdbc = newMemUrl();
    seedPages(
        jdbc,
        "INSERT INTO pages (id, title, body) VALUES ('plain-id', 'Plain', 'Body text')");
    Path root = writeSite(tempDir.resolve("default-path"), jdbc, "SELECT id, title, body FROM pages", null);
    List<VirtualItemRef> refs =
        new PSSqlDatabaseVirtualSiteSource()
            .discover(config(root, jdbc, "SELECT id, title, body FROM pages", null));
    assertEquals(1, refs.size());
    assertEquals(Path.of("8.2", "plain-id.md"), refs.get(0).relativePath());
  }

  @Test
  void mappedColumnsAssembleLikeCsv() throws Exception {
    String jdbc = newMemUrl();
    try (Connection c = DriverManager.getConnection(jdbc, "sa", "");
        Statement st = c.createStatement()) {
      st.execute(
          "CREATE TABLE catalog (page_key VARCHAR(64), headline VARCHAR(256), markdown CLOB)");
      st.execute(
          "INSERT INTO catalog (page_key, headline, markdown) VALUES"
              + " ('mapped-home', 'Mapped', 'Hello from mapped columns.')");
    }
    SqlSpec spec =
        new SqlSpec(
            jdbc,
            "sa",
            "",
            "SELECT page_key, headline, markdown FROM catalog",
            "",
            "page_key",
            "headline",
            "markdown",
            "path",
            "order",
            "version");
    Path root = writeSite(tempDir.resolve("mapped"), jdbc, spec.query(), null);
    VirtualSiteConfig config =
        new VirtualSiteConfig(
            root,
            "SQL Docs",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(),
            "sql-docs",
            spec);
    List<VirtualItemRef> refs = new PSSqlDatabaseVirtualSiteSource().discover(config);
    assertEquals(1, refs.size());
    assertEquals("mapped-home", refs.get(0).id());
    VirtualItem item = new PSSqlDatabaseVirtualSiteSource().load(config, refs.get(0));
    assertTrue(item.markdownBody().contains("Hello from mapped columns."));
  }

  @Test
  void missingRequiredColumnFailsClosed() throws Exception {
    String jdbc = newMemUrl();
    seedPages(jdbc, "INSERT INTO pages (id, title, body) VALUES ('x', 'Y', 'z')");
    Path root = writeSite(tempDir.resolve("missing-col"), jdbc, "SELECT id, title FROM pages", null);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                new PSSqlDatabaseVirtualSiteSource()
                    .discover(config(root, jdbc, "SELECT id, title FROM pages", null)));
    assertTrue(ex.getMessage().contains("body"), ex.getMessage());
  }

  @Test
  void blankIdOrTitleFailsClosed() throws Exception {
    String jdbc = newMemUrl();
    seedPages(jdbc, "INSERT INTO pages (id, title, body) VALUES ('', 'MissingId', 'body')");
    Path root = writeSite(tempDir.resolve("blank-id"), jdbc, inlineSelect(), null);
    VirtualSiteException idEx =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSSqlDatabaseVirtualSiteSource().discover(config(root, jdbc, inlineSelect(), null)));
    assertTrue(idEx.getMessage().contains("id"), idEx.getMessage());

    String jdbc2 = newMemUrl();
    seedPages(jdbc2, "INSERT INTO pages (id, title, body) VALUES ('has-id', '', 'body')");
    Path root2 = writeSite(tempDir.resolve("blank-title"), jdbc2, inlineSelect(), null);
    VirtualSiteException titleEx =
        assertThrows(
            VirtualSiteException.class,
            () ->
                new PSSqlDatabaseVirtualSiteSource()
                    .discover(config(root2, jdbc2, inlineSelect(), null)));
    assertTrue(titleEx.getMessage().contains("title"), titleEx.getMessage());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    String jdbc = newMemUrl();
    seedPages(jdbc, "INSERT INTO pages (id, title, body) VALUES ('known', 'Known', 'Body')");
    Path root = writeSite(tempDir.resolve("unknown-id"), jdbc, inlineSelect(), null);
    PSSqlDatabaseVirtualSiteSource source = new PSSqlDatabaseVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, jdbc, inlineSelect(), null);
    source.discover(cfg);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.md"), 0, "x");
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> source.load(cfg, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void pathTraversalAndAbsolutePathFailClosed() throws Exception {
    String jdbc = newMemUrl();
    seedPages(
        jdbc,
        "INSERT INTO pages (id, title, body, path) VALUES ('esc', 'Esc', 'body', '../outside.md')");
    Path root = writeSite(tempDir.resolve("escape"), jdbc, inlineSelect(), null);
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSSqlDatabaseVirtualSiteSource().discover(config(root, jdbc, inlineSelect(), null)));
    assertTrue(rel.getMessage().contains("path"), rel.getMessage());

    String jdbcAbs = newMemUrl();
    seedPages(
        jdbcAbs,
        "INSERT INTO pages (id, title, body, path) VALUES ('esc', 'Esc', 'body', '/etc/passwd.md')");
    Path rootAbs = writeSite(tempDir.resolve("abs"), jdbcAbs, inlineSelect(), null);
    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () ->
                new PSSqlDatabaseVirtualSiteSource()
                    .discover(config(rootAbs, jdbcAbs, inlineSelect(), null)));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());
  }

  @Test
  void duplicateIdsFailClosed() throws Exception {
    String jdbc = newMemUrl();
    seedPages(
        jdbc,
        """
        INSERT INTO pages (id, title, body) VALUES
        ('same', 'A', 'a'),
        ('same', 'B', 'b')
        """);
    Path root = writeSite(tempDir.resolve("dup"), jdbc, inlineSelect(), null);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSSqlDatabaseVirtualSiteSource().discover(config(root, jdbc, inlineSelect(), null)));
    assertTrue(ex.getMessage().toLowerCase().contains("duplicate"), ex.getMessage());
  }

  @Test
  void loadRerunsQueryWithoutCache() throws Exception {
    String jdbc = newMemUrl();
    seedPages(jdbc, "INSERT INTO pages (id, title, body) VALUES ('live', 'First', 'token-AAA')");
    Path root = writeSite(tempDir.resolve("live"), jdbc, inlineSelect(), null);
    PSSqlDatabaseVirtualSiteSource source = new PSSqlDatabaseVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, jdbc, inlineSelect(), null);
    VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(first.markdownBody().contains("token-AAA"));

    try (Connection c = DriverManager.getConnection(jdbc, "sa", "");
        Statement st = c.createStatement()) {
      st.execute("UPDATE pages SET title='Second', body='token-BBB' WHERE id='live'");
    }
    VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
    assertEquals("Second", second.frontmatter().title());
    assertTrue(second.markdownBody().contains("token-BBB"));
    assertFalse(second.markdownBody().contains("token-AAA"));
  }

  @Test
  void secondBuildAfterSqlConfigAndQueryFileEditEmitsUpdatedHtmlWithoutRestart() throws Exception {
    String jdbc = newMemUrl();
    seedPages(
        jdbc,
        "INSERT INTO pages (id, title, body, path, sort_order) VALUES"
            + " ('live-home', 'First Title', 'First body unique-token-AAA', 'index.md', 1)");
    Path root = writeSite(tempDir.resolve("sql-rebuild"), jdbc, null, "pages.sql");
    Path queryFile = root.resolve("pages.sql");
    Files.writeString(
        queryFile,
        "SELECT id, title, body, path, sort_order AS \"order\" FROM pages\n",
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${siteTitle}</h1><h2>${pageTitle}</h2>${content}</body></html>",
        StandardCharsets.UTF_8);
    writeSqlYaml(root, jdbc, "First Site Title", "pages.sql");

    Path out = tempDir.resolve("sql-rebuild-out");
    PSSqlDatabaseVirtualSiteSource source = new PSSqlDatabaseVirtualSiteSource();
    PSVirtualSiteBuildService service =
        new PSVirtualSiteBuildService(source, new PSInMemoryVirtualParticipantService());

    PSVirtualSiteBuildResult first = service.build(root, out, "sql-docs");
    assertEquals(1, first.pageCount());
    Path html = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String firstHtml = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(firstHtml.contains("First Site Title"), firstHtml);
    assertTrue(firstHtml.contains("First Title"), firstHtml);
    assertTrue(firstHtml.contains("unique-token-AAA"), firstHtml);

    Files.writeString(
        queryFile,
        "SELECT id, 'Second Title' AS title, 'Second body unique-token-BBB' AS body, path,"
            + " sort_order AS \"order\" FROM pages\n",
        StandardCharsets.UTF_8);
    writeSqlYaml(root, jdbc, "Second Site Title", "pages.sql");

    VirtualSiteConfig reloaded =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "sql-docs");
    assertEquals("Second Site Title", reloaded.siteTitle());
    VirtualItem loaded = source.load(reloaded, source.discover(reloaded).get(0));
    assertEquals("Second Title", loaded.frontmatter().title());
    assertTrue(loaded.markdownBody().contains("unique-token-BBB"), loaded.markdownBody());
    assertFalse(loaded.markdownBody().contains("unique-token-AAA"), loaded.markdownBody());

    PSVirtualSiteBuildResult second = service.build(root, out, "sql-docs");
    assertEquals(1, second.pageCount());
    String secondHtml = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(secondHtml.contains("Second Site Title"), secondHtml);
    assertTrue(secondHtml.contains("Second Title"), secondHtml);
    assertTrue(secondHtml.contains("unique-token-BBB"), secondHtml);
    assertFalse(secondHtml.contains("unique-token-AAA"), secondHtml);
    assertFalse(secondHtml.contains("First Title"), secondHtml);
    assertFalse(secondHtml.contains("First Site Title"), secondHtml);
    assertNotEquals(firstHtml, secondHtml);
  }

  @Test
  void invalidOrderFailsClosed() throws Exception {
    String jdbc = newMemUrl();
    Class.forName("org.h2.Driver");
    try (Connection c = DriverManager.getConnection(jdbc, "sa", "");
        Statement st = c.createStatement()) {
      st.execute(
          "CREATE TABLE pages (id VARCHAR(128), title VARCHAR(256), body CLOB, path VARCHAR(512),"
              + " sort_order VARCHAR(32))");
      st.execute(
          "INSERT INTO pages (id, title, body, sort_order) VALUES ('x', 'T', 'b', 'not-an-int')");
    }
    Path root = writeSite(tempDir.resolve("bad-order"), jdbc, inlineSelect(), null);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSSqlDatabaseVirtualSiteSource().discover(config(root, jdbc, inlineSelect(), null)));
    assertTrue(ex.getMessage().contains("order"), ex.getMessage());
  }

  @Test
  void queryFileIsPortableNioPath() throws Exception {
    String jdbc = newMemUrl();
    seedPages(jdbc, "INSERT INTO pages (id, title, body) VALUES ('from-file', 'File', 'From query file.')");
    Path root = writeSite(tempDir.resolve("qfile"), jdbc, null, "pages.sql");
    Files.writeString(
        root.resolve("pages.sql"),
        "SELECT id, title, body FROM pages\n",
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSSqlDatabaseVirtualSiteSource().discover(config(root, jdbc, null, "pages.sql"));
    assertEquals(1, refs.size());
    assertEquals("from-file", refs.get(0).id());
    VirtualItem item =
        new PSSqlDatabaseVirtualSiteSource()
            .load(config(root, jdbc, null, "pages.sql"), refs.get(0));
    assertTrue(item.markdownBody().contains("From query file."));
    assertEquals("pages.sql", item.absolutePath().getFileName().toString());
  }

  @Test
  void queryFileTraversalFailsClosed() throws Exception {
    String jdbc = newMemUrl();
    seedPages(jdbc, "INSERT INTO pages (id, title, body) VALUES ('x', 'X', 'y')");
    Path root = writeSite(tempDir.resolve("q-escape"), jdbc, inlineSelect(), null);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                new PSSqlDatabaseVirtualSiteSource()
                    .discover(config(root, jdbc, null, "../outside.sql")));
    assertTrue(
        ex.getMessage().contains("queryFile") || ex.getMessage().contains(".."), ex.getMessage());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsAbsoluteQueryFileRejected() throws Exception {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSSqlDatabaseVirtualSiteSource.resolveQueryFile(
                    tempDir.resolve("win-root"), "C:/docs/evil.sql"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixAbsoluteQueryFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSSqlDatabaseVirtualSiteSource.resolveQueryFile(
                    tempDir.resolve("unix-root"), "/etc/passwd.sql"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  void rejectsOracleMysqlSqlServerAndRemoteH2() {
    assertThrows(
        VirtualSiteException.class,
        () -> PSSqlDatabaseVirtualSiteSource.requireSafeH2MemUrl("jdbc:oracle:thin:@localhost:1521:orcl"));
    assertThrows(
        VirtualSiteException.class,
        () -> PSSqlDatabaseVirtualSiteSource.requireSafeH2MemUrl("jdbc:mysql://localhost:3306/cms"));
    assertThrows(
        VirtualSiteException.class,
        () ->
            PSSqlDatabaseVirtualSiteSource.requireSafeH2MemUrl(
                "jdbc:sqlserver://localhost:1433;databaseName=cms"));
    assertThrows(
        VirtualSiteException.class,
        () -> PSSqlDatabaseVirtualSiteSource.requireSafeH2MemUrl("jdbc:h2:tcp://localhost/~/test"));
    assertThrows(
        VirtualSiteException.class,
        () -> PSSqlDatabaseVirtualSiteSource.requireSafeH2MemUrl("jdbc:h2:file:./data/cms"));
    assertThrows(
        VirtualSiteException.class,
        () ->
            PSSqlDatabaseVirtualSiteSource.requireSafeH2MemUrl(
                "jdbc:h2:mem:evil;INIT=RUNSCRIPT FROM 'foo.sql'"));
  }

  @Test
  void requireSelectOnlyPreservesCommentMarkersInsideStringLiterals() throws Exception {
    String dashes = "SELECT 'hello -- world' AS title, body FROM pages";
    assertEquals(dashes, PSSqlDatabaseVirtualSiteSource.requireSelectOnly(dashes));
    String block = "SELECT '/* not a comment */' AS title, body FROM pages";
    assertEquals(block, PSSqlDatabaseVirtualSiteSource.requireSelectOnly(block));
    String doubled = "SELECT 'it''s -- fine' AS title FROM pages";
    assertEquals(doubled, PSSqlDatabaseVirtualSiteSource.requireSelectOnly(doubled));
    assertEquals(
        "SELECT id FROM pages",
        PSSqlDatabaseVirtualSiteSource.requireSelectOnly("SELECT id FROM pages -- trailing"));
    String strippedBlock =
        PSSqlDatabaseVirtualSiteSource.requireSelectOnly("SELECT id /* skip */ FROM pages");
    assertTrue(strippedBlock.matches("(?is)SELECT id\\s+FROM pages"), strippedBlock);
    assertTrue(
        PSSqlDatabaseVirtualSiteSource.stripSqlComments("SELECT 'a -- b' FROM pages")
            .contains("'a -- b'"));
  }

  @Test
  void requireSelectOnlyAllowsReadOnlyWithCte() throws Exception {
    String cte =
        "WITH src AS (SELECT id, title, body FROM pages) SELECT id, title, body FROM src";
    assertEquals(cte, PSSqlDatabaseVirtualSiteSource.requireSelectOnly(cte));
  }

  @Test
  void resolveQueryFileDoesNotTreatColonAsDriveInNestedSegment() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("colon-root"));
    try {
      Path resolved =
          PSSqlDatabaseVirtualSiteSource.resolveQueryFile(root, "queries/my:pages.sql");
      assertTrue(resolved.startsWith(root.normalize()));
      assertTrue(resolved.getFileName().toString().contains("pages.sql"));
    } catch (VirtualSiteException e) {
      // Windows Path rejects ':' in a file name; Unix accepts it. Never the old
      // per-segment "drive" guard.
      assertFalse(e.getMessage().contains("drive"), e.getMessage());
      assertTrue(e.getMessage().toLowerCase().contains("valid path"), e.getMessage());
    }
  }

  @Test
  void missingQueryFileFailsClosedWithResolvedPath() throws Exception {
    Path root = writeSite(tempDir.resolve("missing-qfile"), newMemUrl(), null, "queries/nope.sql");
    VirtualSiteConfig cfg = config(root, newMemUrl(), null, "queries/nope.sql");
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> new PSSqlDatabaseVirtualSiteSource().discover(cfg));
    String msg = ex.getMessage();
    assertTrue(msg.toLowerCase().contains("queryfile"), msg);
    assertTrue(msg.contains("nope.sql"), msg);
    assertTrue(
        msg.contains(root.toAbsolutePath().normalize().toString()) || msg.contains("queries"),
        msg);
  }

  @Test
  void rejectsNonSelectQuery() {
    VirtualSiteException insert =
        assertThrows(
            VirtualSiteException.class,
            () -> PSSqlDatabaseVirtualSiteSource.requireSelectOnly("INSERT INTO pages VALUES (1)"));
    assertTrue(insert.getMessage().toLowerCase().contains("select"), insert.getMessage());
    VirtualSiteException multi =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSSqlDatabaseVirtualSiteSource.requireSelectOnly(
                    "SELECT id FROM pages; DROP TABLE pages"));
    assertTrue(multi.getMessage().toLowerCase().contains("select"), multi.getMessage());
    VirtualSiteException csvRead =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSSqlDatabaseVirtualSiteSource.requireSelectOnly(
                    "SELECT * FROM CSVREAD('pages.csv')"));
    assertTrue(csvRead.getMessage().toLowerCase().contains("select"), csvRead.getMessage());
  }

  @Test
  void factorySelectsSqlDatabase() throws Exception {
    IPSVirtualSiteSource source =
        PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.SQL_DATABASE);
    assertInstanceOf(PSSqlDatabaseVirtualSiteSource.class, source);
    assertEquals("sql-database", source.sourceType());
    IPSVirtualSiteSource byName = PSVirtualSiteSourceFactory.createFromWireName("sql-database");
    assertInstanceOf(PSSqlDatabaseVirtualSiteSource.class, byName);
    IPSVirtualSiteSource git = PSVirtualSiteSourceFactory.createFromWireName("git-filesystem");
    assertInstanceOf(PSGitFilesystemVirtualSiteSource.class, git);
    IPSVirtualSiteSource csv = PSVirtualSiteSourceFactory.createFromWireName("csv-filesystem");
    assertInstanceOf(PSCsvFilesystemVirtualSiteSource.class, csv);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("sql-database"));
    assertTrue(ex.getMessage().contains("http-json"));
    assertTrue(ex.getMessage().contains("object-storage"));
  }

  @Test
  void buildServiceFactoryWiresSqlAndEmitsHtml() throws Exception {
    String jdbc = newMemUrl();
    seedPages(
        jdbc,
        "INSERT INTO pages (id, title, body, path) VALUES ('home', 'SQL Home', 'Hello from SQL.', 'index.md')");
    Path root = writeSite(tempDir.resolve("build-sql"), jdbc, inlineSelect(), null);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.SQL_DATABASE);
    assertInstanceOf(PSSqlDatabaseVirtualSiteSource.class, service.source());

    PSVirtualSiteBuildResult result = service.build(root, out, "sql-docs");
    assertEquals(1, result.pageCount());
    Path html = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("SQL Home"), body);
    assertTrue(body.contains("Hello from SQL"), body);
  }

  @Test
  void missingSqlMappingFailsClosed() throws Exception {
    Path root = tempDir.resolve("no-sql");
    Files.createDirectories(root.resolve("8.2"));
    VirtualSiteConfig cfg =
        new VirtualSiteConfig(
            root,
            "SQL Docs",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(),
            "sql-docs",
            null);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> new PSSqlDatabaseVirtualSiteSource().discover(cfg));
    assertTrue(ex.getMessage().contains("sql:"), ex.getMessage());
  }

  @Test
  void yamlLoaderParsesSqlSpec() throws Exception {
    String jdbc = newMemUrl();
    Path root = writeSite(tempDir.resolve("yaml-sql"), jdbc, inlineSelect(), null);
    VirtualSiteConfig loaded =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "sql-docs");
    assertEquals(jdbc, loaded.sql().jdbcUrl());
    assertEquals("sa", loaded.sql().user());
    assertTrue(loaded.sql().query().toLowerCase().contains("select"));
  }

  private static String newMemUrl() {
    return "jdbc:h2:mem:vsql_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
  }

  private static String inlineSelect() {
    return "SELECT id, title, body, path, sort_order AS \"order\" FROM pages";
  }

  private static void seedPages(String jdbc, String insertSql) throws Exception {
    Class.forName("org.h2.Driver");
    try (Connection c = DriverManager.getConnection(jdbc, "sa", "");
        Statement st = c.createStatement()) {
      st.execute(
          "CREATE TABLE pages ("
              + "id VARCHAR(128), title VARCHAR(256), body CLOB, path VARCHAR(512),"
              + " sort_order INT)");
      st.execute(insertSql);
    }
  }

  private static void writeSqlYaml(Path root, String jdbc, String siteTitle, String queryFile)
      throws Exception {
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
        sql:
          jdbcUrl: "%s"
          user: sa
          password: ""
          queryFile: %s
        """
            .formatted(siteTitle, jdbc, queryFile),
        StandardCharsets.UTF_8);
  }

  private static Path writeSite(Path root, String jdbc, String query, String queryFile)
      throws Exception {
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    String sqlBlock;
    if (queryFile != null && !queryFile.isBlank()) {
      sqlBlock =
          """
          sql:
            jdbcUrl: "%s"
            user: sa
            password: ""
            queryFile: %s
          """
              .formatted(jdbc, queryFile);
    } else {
      sqlBlock =
          """
          sql:
            jdbcUrl: "%s"
            user: sa
            password: ""
            query: |
              %s
          """
              .formatted(jdbc, query);
    }
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: SQL Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        %s
        """
            .formatted(sqlBlock),
        StandardCharsets.UTF_8);
    return root;
  }

  private static VirtualSiteConfig config(
      Path root, String jdbc, String query, String queryFile) {
    SqlSpec spec =
        new SqlSpec(jdbc, "sa", "", query, queryFile, "id", "title", "body", "path", "order", "version");
    return new VirtualSiteConfig(
        root,
        "SQL Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "sql-docs",
        spec);
  }
}
