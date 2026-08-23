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

import com.percussion.services.virtualsite.VirtualSiteConfig.SqlSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.VersionSpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Virtual Site source: JDBC query against in-memory H2 ({@code jdbc:h2:mem:}).
 *
 * <p>Required result columns (after optional {@code sql.columns} mapping): {@code id}, {@code
 * title}, {@code body}. Optional: {@code path}, {@code order}, {@code version}. Missing required
 * columns or blank {@code id}/{@code title} fail closed with {@link VirtualSiteException}.
 *
 * <p>This slice does not open Oracle, MySQL, SQL Server, or remote H2 ({@code tcp}/{@code ssl}/file)
 * URLs. {@code INIT=}/{@code RUNSCRIPT} in the JDBC URL are rejected.
 *
 * <p>Stateless: {@link #discover} and {@link #load} always re-run the current SELECT. No row cache
 * is kept on the instance or in statics.
 *
 * <p>Optional {@code sql.queryFile} is resolved with portable NIO {@link Path} under the site root.
 */
public class PSSqlDatabaseVirtualSiteSource implements IPSVirtualSiteSource {

  private static final String H2_DRIVER = "org.h2.Driver";
  private static final String MEM_PREFIX = "jdbc:h2:mem:";
  private static final Pattern MEM_NAME =
      Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]*$");
  private static final Pattern FORBIDDEN_URL_TOKEN =
      Pattern.compile(
          "(?i)(init\\s*=|runscript|password\\s*=|user\\s*=|jdbc:h2:tcp:|jdbc:h2:ssl:|"
              + "jdbc:h2:file:|jdbc:h2:zip:|jdbc:oracle:|jdbc:mysql:|jdbc:mariadb:|"
              + "jdbc:sqlserver:|jdbc:postgresql:|jdbc:derby:)");
  private static final Pattern SELECT_ONLY =
      Pattern.compile("(?is)^select\\b.+");
  private static final Pattern FORBIDDEN_SQL_TOKEN =
      Pattern.compile(
          "(?i)\\b(csvread|csvwrite|file_read|file_write|call|script|into|merge|update|delete|"
              + "insert|drop|alter|create|grant|truncate|execute|runscript)\\b");

  @Override
  public String sourceType() {
    return VirtualSiteSourceType.SQL_DATABASE.wireName();
  }

  @Override
  public List<VirtualItemRef> discover(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    List<LoadedRow> rows = loadAllRows(config);
    List<VirtualItemRef> refs = new ArrayList<>(rows.size());
    for (LoadedRow row : rows) {
      refs.add(row.ref());
    }
    refs.sort(
        Comparator.comparing(VirtualItemRef::versionId)
            .thenComparingInt(VirtualItemRef::order)
            .thenComparing(r -> r.relativePath().toString().replace('\\', '/')));
    return refs;
  }

  @Override
  public VirtualItem load(VirtualSiteConfig config, VirtualItemRef ref)
      throws IOException, VirtualSiteException {
    if (ref == null) {
      throw new VirtualSiteException("SQL item ref is required");
    }
    List<LoadedRow> rows = loadAllRows(config);
    for (LoadedRow row : rows) {
      if (row.ref().versionId().equals(ref.versionId()) && row.ref().id().equals(ref.id())) {
        return new VirtualItem(row.ref(), row.frontmatter(), row.body(), row.sourcePath());
      }
    }
    throw new VirtualSiteException(
        "Unknown SQL page id '" + ref.id() + "' in version " + ref.versionId());
  }

  private static List<LoadedRow> loadAllRows(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    if (config == null) {
      throw new VirtualSiteException("Virtual Site config is required");
    }
    Path root = config.root();
    if (root == null || !PSVirtualSiteHelper.isSafeRootPath(root)) {
      throw new VirtualSiteException("SQL site root is missing or unsafe");
    }
    SqlSpec spec = config.sql();
    if (spec == null) {
      throw new VirtualSiteException(
          "sql-database requires a sql: mapping in _config.yaml (jdbcUrl plus query or queryFile)");
    }
    String jdbcUrl = requireSafeH2MemUrl(spec.jdbcUrl());
    String sql = resolveQuery(config.root().normalize(), spec);
    Path sourcePath = resolveSourcePath(config.root().normalize(), spec);
    requireH2Driver();

    List<LoadedRow> rows = new ArrayList<>();
    Map<String, Path> seenIds = new HashMap<>();
    try (Connection conn = openH2(jdbcUrl, spec);
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      Map<String, Integer> columns = columnIndex(rs);
      requireMappedColumn(columns, spec.idColumn(), "id");
      requireMappedColumn(columns, spec.titleColumn(), "title");
      requireMappedColumn(columns, spec.bodyColumn(), "body");
      int rowNum = 0;
      while (rs.next()) {
        rowNum++;
        LoadedRow loaded = toLoadedRow(rs, columns, spec, config, sourcePath, rowNum);
        String idKey = loaded.ref().versionId() + "\0" + loaded.ref().id();
        Path previous = seenIds.put(idKey, loaded.ref().relativePath());
        if (previous != null) {
          throw new VirtualSiteException(
              "Duplicate SQL id '"
                  + loaded.ref().id()
                  + "' in version "
                  + loaded.ref().versionId()
                  + ": "
                  + previous
                  + " and "
                  + loaded.ref().relativePath());
        }
        rows.add(loaded);
      }
    } catch (VirtualSiteException e) {
      throw e;
    } catch (SQLException e) {
      throw new VirtualSiteException(
          "sql-database query failed against " + redactJdbcUrl(jdbcUrl) + ": " + e.getMessage(),
          e);
    }
    return rows;
  }

  private static LoadedRow toLoadedRow(
      ResultSet rs,
      Map<String, Integer> columns,
      SqlSpec spec,
      VirtualSiteConfig config,
      Path sourcePath,
      int rowNum)
      throws SQLException, VirtualSiteException {
    String where = "SQL row " + rowNum;
    String id = cell(rs, columns, spec.idColumn()).trim();
    String title = cell(rs, columns, spec.titleColumn()).trim();
    String body = cell(rs, columns, spec.bodyColumn());
    String pathRaw = optionalCell(rs, columns, spec.pathColumn());
    String orderRaw = optionalCell(rs, columns, spec.orderColumn());
    String versionRaw = optionalCell(rs, columns, spec.versionColumn());
    if (id.isEmpty()) {
      throw new VirtualSiteException("SQL 'id' is required in " + where);
    }
    if (title.isEmpty()) {
      throw new VirtualSiteException("SQL 'title' is required in " + where);
    }
    VersionSpec version = resolveVersion(config, versionRaw, where);
    int order = parseOrder(orderRaw, where);
    Path relative =
        PSCsvFilesystemVirtualSiteSource.resolvePagePath(
            pathRaw, id, version.path(), where);
    VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, order, title);
    VirtualFrontmatter fm =
        new VirtualFrontmatter(id, title, "", version.id(), true, order, List.of(), false);
    return new LoadedRow(ref, fm, body != null ? body : "", sourcePath);
  }

  private static VersionSpec resolveVersion(
      VirtualSiteConfig config, String versionRaw, String where) throws VirtualSiteException {
    List<VersionSpec> versions = config.versions();
    if (versions == null || versions.isEmpty()) {
      throw new VirtualSiteException("sql-database config must declare at least one version");
    }
    if (versionRaw == null || versionRaw.isBlank()) {
      for (VersionSpec v : versions) {
        if (v.defaultVersion()) {
          return v;
        }
      }
      return versions.get(0);
    }
    String wanted = versionRaw.trim();
    for (VersionSpec v : versions) {
      if (wanted.equals(v.id())) {
        return v;
      }
    }
    throw new VirtualSiteException(
        "SQL version '" + wanted + "' is not declared in _config.yaml (" + where + ")");
  }

  private static int parseOrder(String raw, String where) throws VirtualSiteException {
    if (raw == null || raw.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      throw new VirtualSiteException("SQL 'order' must be an integer in " + where, e);
    }
  }

  static String requireSafeH2MemUrl(String jdbcUrl) throws VirtualSiteException {
    if (jdbcUrl == null || jdbcUrl.isBlank()) {
      throw new VirtualSiteException("sql-database jdbcUrl is required");
    }
    String url = jdbcUrl.trim();
    if (FORBIDDEN_URL_TOKEN.matcher(url).find()) {
      throw new VirtualSiteException(
          "sql-database jdbcUrl must be in-memory H2 (jdbc:h2:mem:…). Oracle, MySQL, SQL Server,"
              + " remote H2, file H2, INIT/RUNSCRIPT, and URL user/password are not allowed.");
    }
    if (!url.regionMatches(true, 0, MEM_PREFIX, 0, MEM_PREFIX.length())) {
      throw new VirtualSiteException(
          "sql-database jdbcUrl must start with jdbc:h2:mem: (in-memory H2 only). Rejected: "
              + redactJdbcUrl(url));
    }
    String rest = url.substring(MEM_PREFIX.length());
    int semi = rest.indexOf(';');
    String name = semi < 0 ? rest : rest.substring(0, semi);
    if (name.isBlank() || !MEM_NAME.matcher(name).matches()) {
      throw new VirtualSiteException(
          "sql-database H2 memory name must be alphanumeric (optional _-). Rejected: "
              + redactJdbcUrl(url));
    }
    if (semi >= 0) {
      String params = rest.substring(semi + 1);
      if (FORBIDDEN_URL_TOKEN.matcher(params).find() || params.toLowerCase(Locale.ROOT).contains("init")) {
        throw new VirtualSiteException(
            "sql-database jdbcUrl must not include INIT, RUNSCRIPT, or credentials.");
      }
    }
    return url;
  }

  static String requireSelectOnly(String sql) throws VirtualSiteException {
    if (sql == null || sql.isBlank()) {
      throw new VirtualSiteException("sql-database query is required (inline query or queryFile)");
    }
    String trimmed = stripSqlComments(sql).trim();
    if (trimmed.endsWith(";")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
    }
    if (trimmed.indexOf(';') >= 0) {
      throw new VirtualSiteException("sql-database query must be a single SELECT (no extra statements)");
    }
    if (!SELECT_ONLY.matcher(trimmed).matches()) {
      throw new VirtualSiteException("sql-database query must be a single SELECT statement");
    }
    if (FORBIDDEN_SQL_TOKEN.matcher(trimmed).find()) {
      throw new VirtualSiteException(
          "sql-database query must be a read-only SELECT (no DML, CALL, SCRIPT, CSVREAD, or INTO)");
    }
    return trimmed;
  }

  static Path resolveQueryFile(Path root, String queryFile) throws VirtualSiteException {
    if (queryFile == null || queryFile.isBlank()) {
      throw new VirtualSiteException("sql-database queryFile is blank");
    }
    if (queryFile.indexOf('\0') >= 0) {
      throw new VirtualSiteException("sql-database queryFile must not contain NUL");
    }
    String logical = queryFile.trim().replace('\\', '/');
    if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
      throw new VirtualSiteException("sql-database queryFile must be relative to the site root");
    }
    Path safeRoot = root.normalize();
    Path resolved;
    try {
      Path relative = Path.of("");
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0 || seg.indexOf(':') >= 0) {
          throw new VirtualSiteException(
              "sql-database queryFile must not contain '..', drive, or NUL segments");
        }
        relative = relative.resolve(seg);
      }
      if (relative.getNameCount() == 0 || relative.toString().isEmpty()) {
        throw new VirtualSiteException("sql-database queryFile is empty after normalize");
      }
      resolved = safeRoot.resolve(relative).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException("sql-database queryFile is not a valid path", e);
    }
    if (!resolved.startsWith(safeRoot)
        || !PSVirtualSiteHelper.isSafeRootPath(resolved)
        || remainingParent(resolved)) {
      throw new VirtualSiteException("sql-database queryFile escapes the site root");
    }
    return resolved;
  }

  private static String resolveQuery(Path root, SqlSpec spec)
      throws IOException, VirtualSiteException {
    boolean hasFile = spec.queryFile() != null && !spec.queryFile().isBlank();
    boolean hasQuery = spec.query() != null && !spec.query().isBlank();
    if (hasFile && hasQuery) {
      throw new VirtualSiteException(
          "sql-database must set either query or queryFile, not both");
    }
    if (hasFile) {
      Path file = resolveQueryFile(root, spec.queryFile());
      if (!Files.isRegularFile(file)) {
        throw new VirtualSiteException("sql-database queryFile not found: " + file.getFileName());
      }
      return requireSelectOnly(Files.readString(file, StandardCharsets.UTF_8));
    }
    return requireSelectOnly(spec.query());
  }

  private static Path resolveSourcePath(Path root, SqlSpec spec) throws VirtualSiteException {
    if (spec.queryFile() != null && !spec.queryFile().isBlank()) {
      return resolveQueryFile(root, spec.queryFile());
    }
    Path config = root.resolve(VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE).normalize();
    if (config.startsWith(root)) {
      return config;
    }
    return root;
  }

  private static Connection openH2(String jdbcUrl, SqlSpec spec) throws SQLException {
    if (spec.user() == null || spec.user().isBlank()) {
      return DriverManager.getConnection(jdbcUrl);
    }
    String password = spec.password() != null ? spec.password() : "";
    return DriverManager.getConnection(jdbcUrl, spec.user(), password);
  }

  private static void requireH2Driver() throws VirtualSiteException {
    try {
      Class.forName(H2_DRIVER);
    } catch (ClassNotFoundException e) {
      throw new VirtualSiteException("sql-database requires the H2 JDBC driver on the classpath", e);
    }
  }

  private static Map<String, Integer> columnIndex(ResultSet rs) throws SQLException {
    ResultSetMetaData md = rs.getMetaData();
    Map<String, Integer> idx = new LinkedHashMap<>();
    for (int i = 1; i <= md.getColumnCount(); i++) {
      String label = md.getColumnLabel(i);
      if (label == null || label.isBlank()) {
        label = md.getColumnName(i);
      }
      if (label != null && !label.isBlank()) {
        idx.put(label.trim().toLowerCase(Locale.ROOT), i);
      }
    }
    return idx;
  }

  private static void requireMappedColumn(
      Map<String, Integer> columns, String physical, String logical) throws VirtualSiteException {
    if (!columns.containsKey(physical.toLowerCase(Locale.ROOT))) {
      throw new VirtualSiteException(
          "sql-database result is missing required '"
              + logical
              + "' column (mapped to '"
              + physical
              + "')");
    }
  }

  private static String cell(ResultSet rs, Map<String, Integer> columns, String physical)
      throws SQLException {
    Integer idx = columns.get(physical.toLowerCase(Locale.ROOT));
    if (idx == null) {
      return "";
    }
    String value = rs.getString(idx);
    return value != null ? value : "";
  }

  private static String optionalCell(ResultSet rs, Map<String, Integer> columns, String physical)
      throws SQLException {
    if (physical == null || !columns.containsKey(physical.toLowerCase(Locale.ROOT))) {
      return "";
    }
    return cell(rs, columns, physical);
  }

  static String redactJdbcUrl(String jdbcUrl) {
    if (jdbcUrl == null || jdbcUrl.isBlank()) {
      return "";
    }
    String url = jdbcUrl.trim();
    int semi = url.indexOf(';');
    return semi < 0 ? url : url.substring(0, semi);
  }

  /**
   * Remove SQL line comments ({@code --}) and block comments without truncating those
   * markers when they appear inside string literals or quoted identifiers.
   */
  static String stripSqlComments(String sql) {
    if (sql == null || sql.isEmpty()) {
      return sql;
    }
    StringBuilder out = new StringBuilder(sql.length());
    int i = 0;
    int n = sql.length();
    while (i < n) {
      char c = sql.charAt(i);
      char next = i + 1 < n ? sql.charAt(i + 1) : 0;
      if (c == '\'') {
        i = copyQuotedSql(sql, i, '\'', out);
        continue;
      }
      if (c == '"') {
        i = copyQuotedSql(sql, i, '"', out);
        continue;
      }
      if (c == '-' && next == '-') {
        i += 2;
        while (i < n) {
          char d = sql.charAt(i);
          if (d == '\n' || d == '\r') {
            break;
          }
          i++;
        }
        continue;
      }
      if (c == '/' && next == '*') {
        i += 2;
        while (i + 1 < n && !(sql.charAt(i) == '*' && sql.charAt(i + 1) == '/')) {
          i++;
        }
        i = i + 1 < n ? i + 2 : n;
        out.append(' ');
        continue;
      }
      out.append(c);
      i++;
    }
    return out.toString();
  }

  private static int copyQuotedSql(String sql, int start, char quote, StringBuilder out) {
    int i = start;
    int n = sql.length();
    out.append(sql.charAt(i));
    i++;
    while (i < n) {
      char d = sql.charAt(i);
      out.append(d);
      i++;
      if (d == quote) {
        if (i < n && sql.charAt(i) == quote) {
          out.append(quote);
          i++;
        } else {
          break;
        }
      }
    }
    return i;
  }

  private static boolean looksAbsoluteWindows(String logical) {
    return logical.length() >= 2
        && Character.isLetter(logical.charAt(0))
        && logical.charAt(1) == ':';
  }

  private static boolean remainingParent(Path path) {
    for (Path part : path) {
      if ("..".equals(part.toString())) {
        return true;
      }
    }
    return false;
  }

  private record LoadedRow(
      VirtualItemRef ref, VirtualFrontmatter frontmatter, String body, Path sourcePath) {}
}
