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

package com.percussion.services.pipeline.sql;

import com.percussion.services.pipeline.PSPipelineIrException;
import com.percussion.services.pipeline.model.BackendTableRefIr;
import com.percussion.services.pipeline.model.MapperStageIr;
import com.percussion.services.pipeline.model.MappingEntryIr;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.model.SelectorStageIr;
import com.percussion.services.pipeline.model.UpdaterStageIr;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds parameterized {@link PSPipelineSqlPlan} instances from pipeline IR resources.
 *
 * <p><strong>Security defaults:</strong>
 *
 * <ul>
 *   <li>Request values are always bound as JDBC parameters — never concatenated into SQL.
 *   <li>Generated identifiers (table/column) are restricted to {@code [A-Za-z_][A-Za-z0-9_]*}.
 *   <li>Native SQL (selector {@code nativeStatement}) is an escape hatch: single {@code SELECT}
 *       only, named {@code :param} placeholders; multi-statement and non-SELECT rejected.
 * </ul>
 */
public final class PSPipelineSqlPlanner {

  private static final Pattern SAFE_IDENT = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
  private static final Pattern NAMED_PARAM = Pattern.compile(":([A-Za-z_][A-Za-z0-9_]*)");

  private PSPipelineSqlPlanner() {}

  /**
   * Plan a QUERY resource.
   *
   * <p>Strategy:
   *
   * <ol>
   *   <li>If selector uses {@code nativeStatement}, compile named-parameter SELECT.
   *   <li>Else build projection from single backend table + COLUMN mapper mappings; optional WHERE
   *       from request params that match mapped columns / JSON field names.
   * </ol>
   */
  public static PSPipelineSqlPlan planQuery(
      PipelineResourceIr resource, PipelineExecuteRequest request) throws PSPipelineIrException {
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(request, "request");
    if (!PipelineResourceIr.KIND_QUERY.equals(resource.getKind())) {
      throw new PSPipelineIrException(
          "Resource kind is not QUERY: " + resource.getKind() + " (" + resource.getName() + ")");
    }

    SelectorStageIr selector =
        resource.getStages() != null ? resource.getStages().getSelector() : null;
    if (selector != null
        && selector.isPresent()
        && SelectorStageIr.METHOD_NATIVE.equals(selector.getMethod())
        && selector.getNativeStatement() != null
        && !selector.getNativeStatement().isBlank()) {
      return planNativeSelect(selector.getNativeStatement(), request);
    }

    return planGeneratedSelect(resource, request);
  }

  /**
   * Plan a minimal UPDATE-resource insert when updater allows insert and request has rows (or
   * params as a single row).
   */
  public static List<PSPipelineSqlPlan> planInserts(
      PipelineResourceIr resource, PipelineExecuteRequest request) throws PSPipelineIrException {
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(request, "request");
    if (!PipelineResourceIr.KIND_UPDATE.equals(resource.getKind())) {
      throw new PSPipelineIrException(
          "Resource kind is not UPDATE: " + resource.getKind() + " (" + resource.getName() + ")");
    }
    UpdaterStageIr updater =
        resource.getStages() != null ? resource.getStages().getUpdater() : null;
    if (updater == null || !updater.isPresent() || !updater.isAllowInsert()) {
      throw new PSPipelineIrException(
          "UPDATE resource does not allow insert: " + resource.getName());
    }

    String table = requireSingleTable(resource);
    List<ColumnMapping> mappings = columnMappings(resource);
    if (mappings.isEmpty()) {
      throw new PSPipelineIrException(
          "UPDATE resource has no COLUMN mappings for insert: " + resource.getName());
    }

    List<Map<String, Object>> rows = new ArrayList<>();
    if (request.getRows() != null && !request.getRows().isEmpty()) {
      rows.addAll(request.getRows());
    } else if (request.getParams() != null && !request.getParams().isEmpty()) {
      rows.add(new LinkedHashMap<>(request.getParams()));
    } else {
      throw new PSPipelineIrException("Insert requires request.rows or request.params");
    }

    List<PSPipelineSqlPlan> plans = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      List<String> cols = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      for (ColumnMapping m : mappings) {
        Object v = findValue(row, m);
        if (v != null) {
          cols.add(m.column);
          values.add(v);
        }
      }
      if (cols.isEmpty()) {
        throw new PSPipelineIrException("Insert row has no values matching mapped columns");
      }
      StringBuilder sql = new StringBuilder("INSERT INTO ").append(quoteIdent(table)).append(" (");
      for (int i = 0; i < cols.size(); i++) {
        if (i > 0) {
          sql.append(", ");
        }
        sql.append(quoteIdent(cols.get(i)));
      }
      sql.append(") VALUES (");
      for (int i = 0; i < cols.size(); i++) {
        if (i > 0) {
          sql.append(", ");
        }
        sql.append('?');
      }
      sql.append(')');
      plans.add(
          new PSPipelineSqlPlan(
              PSPipelineSqlPlan.Kind.UPDATE, sql.toString(), values, "insert:" + table));
    }
    return plans;
  }

  private static PSPipelineSqlPlan planNativeSelect(String nativeSql, PipelineExecuteRequest request)
      throws PSPipelineIrException {
    String trimmed = nativeSql.trim();
    if (trimmed.indexOf(';') >= 0) {
      throw new PSPipelineIrException(
          "Native SQL escape hatch rejects multi-statement / semicolon SQL");
    }
    String upper = trimmed.toUpperCase(Locale.ROOT);
    if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
      throw new PSPipelineIrException(
          "Native SQL escape hatch allows only SELECT/WITH statements (got: "
              + firstToken(trimmed)
              + ")");
    }
    // Reject obvious mutation keywords as whole words outside string literals
    // (e.g. WHERE name = 'EXEC sp' must not false-positive on EXEC inside quotes).
    String upperOutsideLiterals = stripSqlStringLiterals(trimmed).toUpperCase(Locale.ROOT);
    if (containsWord(upperOutsideLiterals, "INSERT")
        || containsWord(upperOutsideLiterals, "UPDATE")
        || containsWord(upperOutsideLiterals, "DELETE")
        || containsWord(upperOutsideLiterals, "DROP")
        || containsWord(upperOutsideLiterals, "ALTER")
        || containsWord(upperOutsideLiterals, "TRUNCATE")
        || containsWord(upperOutsideLiterals, "MERGE")
        || containsWord(upperOutsideLiterals, "EXEC")
        || containsWord(upperOutsideLiterals, "EXECUTE")
        || containsWord(upperOutsideLiterals, "CALL")) {
      throw new PSPipelineIrException("Native SQL escape hatch rejected unsafe keyword in statement");
    }

    List<Object> binds = new ArrayList<>();
    Matcher m = NAMED_PARAM.matcher(trimmed);
    StringBuffer jdbc = new StringBuffer();
    Map<String, Object> params =
        request.getParams() != null ? request.getParams() : Map.of();
    while (m.find()) {
      String name = m.group(1);
      Object value = findParamIgnoreCase(params, name);
      if (value == null && !params.containsKey(name) && findParamKey(params, name) == null) {
        // allow explicit null if key present under different case handled above
        if (!containsKeyIgnoreCase(params, name)) {
          throw new PSPipelineIrException("Missing request param for native SQL placeholder :" + name);
        }
      }
      binds.add(value);
      m.appendReplacement(jdbc, "?");
    }
    m.appendTail(jdbc);

    return new PSPipelineSqlPlan(
        PSPipelineSqlPlan.Kind.QUERY, jdbc.toString(), binds, "nativeSelect");
  }

  private static PSPipelineSqlPlan planGeneratedSelect(
      PipelineResourceIr resource, PipelineExecuteRequest request) throws PSPipelineIrException {
    String table = requireSingleTable(resource);
    List<ColumnMapping> mappings = columnMappings(resource);
    if (mappings.isEmpty()) {
      throw new PSPipelineIrException(
          "QUERY resource has no COLUMN mappings to project: " + resource.getName());
    }

    StringBuilder sql = new StringBuilder("SELECT ");
    for (int i = 0; i < mappings.size(); i++) {
      ColumnMapping m = mappings.get(i);
      if (i > 0) {
        sql.append(", ");
      }
      sql.append(quoteIdent(m.column)).append(" AS ").append(quoteIdent(m.jsonField));
    }
    sql.append(" FROM ").append(quoteIdent(table));

    List<Object> binds = new ArrayList<>();
    Map<String, Object> params =
        request.getParams() != null ? request.getParams() : Map.of();
    if (!params.isEmpty()) {
      List<String> whereParts = new ArrayList<>();
      // Deduplicate by mapped column so case variants of the same param (TYPE vs type) do not
      // produce duplicate WHERE "TYPE" = ? AND "TYPE" = ? with conflicting binds.
      Set<String> usedColumns = new HashSet<>();
      for (Map.Entry<String, Object> e : params.entrySet()) {
        ColumnMapping match = findMappingForParam(mappings, e.getKey());
        if (match == null) {
          // Unknown params are ignored for generated WHERE (hooks may use them); only mapped cols
          // become filters.
          continue;
        }
        String colKey = match.column.toUpperCase(Locale.ROOT);
        if (!usedColumns.add(colKey)) {
          continue;
        }
        whereParts.add(quoteIdent(match.column) + " = ?");
        binds.add(e.getValue());
      }
      if (!whereParts.isEmpty()) {
        sql.append(" WHERE ");
        sql.append(String.join(" AND ", whereParts));
      }
    }

    return new PSPipelineSqlPlan(
        PSPipelineSqlPlan.Kind.QUERY, sql.toString(), binds, "generatedSelect:" + table);
  }

  private static String requireSingleTable(PipelineResourceIr resource) throws PSPipelineIrException {
    if (resource.getStages() == null
        || resource.getStages().getBackendTank() == null
        || !resource.getStages().getBackendTank().isPresent()) {
      throw new PSPipelineIrException("Resource missing backend tank: " + resource.getName());
    }
    if (resource.getStages().getBackendTank().getJoinCount() > 0) {
      throw new PSPipelineIrException(
          "Joined backend tanks are not supported in Slice A runtime: " + resource.getName());
    }
    List<BackendTableRefIr> tables = resource.getStages().getBackendTank().getTables();
    if (tables == null || tables.size() != 1) {
      throw new PSPipelineIrException(
          "Slice A runtime requires exactly one backend table: " + resource.getName());
    }
    String table = tables.get(0).getTable();
    if (table == null || table.isBlank()) {
      table = tables.get(0).getAlias();
    }
    return requireSafeIdent(table, "table");
  }

  private static List<ColumnMapping> columnMappings(PipelineResourceIr resource)
      throws PSPipelineIrException {
    MapperStageIr mapper =
        resource.getStages() != null ? resource.getStages().getMapper() : null;
    if (mapper == null || !mapper.isPresent() || mapper.getMappings() == null) {
      return List.of();
    }
    List<ColumnMapping> out = new ArrayList<>();
    for (MappingEntryIr entry : mapper.getMappings()) {
      if (entry == null) {
        continue;
      }
      if (!MappingEntryIr.BACKEND_KIND_COLUMN.equals(entry.getBackendKind())
          && entry.getBackendKind() != null
          && !entry.getBackendKind().isBlank()
          && !MappingEntryIr.BACKEND_KIND_COLUMN.equalsIgnoreCase(entry.getBackendKind())) {
        continue;
      }
      String backend = entry.getBackend();
      if (backend == null || backend.isBlank()) {
        continue;
      }
      String column = columnFromBackend(backend);
      String jsonField = jsonFieldFromDocument(entry.getDocumentField(), column);
      out.add(new ColumnMapping(column, jsonField, entry.getDocumentField()));
    }
    return out;
  }

  static String columnFromBackend(String backend) throws PSPipelineIrException {
    String b = backend.trim();
    int dot = b.lastIndexOf('.');
    String col = dot >= 0 ? b.substring(dot + 1) : b;
    return requireSafeIdent(col, "column");
  }

  static String jsonFieldFromDocument(String documentField, String fallbackColumn) {
    if (documentField == null || documentField.isBlank()) {
      return fallbackColumn;
    }
    String s = documentField.trim();
    int slash = s.lastIndexOf('/');
    if (slash >= 0) {
      s = s.substring(slash + 1);
    }
    if (s.startsWith("@")) {
      s = s.substring(1);
    }
    if (s.isBlank() || !SAFE_IDENT.matcher(s).matches()) {
      return fallbackColumn;
    }
    return s;
  }

  private static ColumnMapping findMappingForParam(List<ColumnMapping> mappings, String paramKey) {
    if (paramKey == null) {
      return null;
    }
    for (ColumnMapping m : mappings) {
      if (paramKey.equalsIgnoreCase(m.column)
          || paramKey.equalsIgnoreCase(m.jsonField)
          || (m.documentField != null && paramKey.equalsIgnoreCase(m.documentField))) {
        return m;
      }
    }
    return null;
  }

  private static Object findValue(Map<String, Object> row, ColumnMapping m) {
    if (row.containsKey(m.column)) {
      return row.get(m.column);
    }
    if (row.containsKey(m.jsonField)) {
      return row.get(m.jsonField);
    }
    for (Map.Entry<String, Object> e : row.entrySet()) {
      if (e.getKey() != null
          && (e.getKey().equalsIgnoreCase(m.column)
              || e.getKey().equalsIgnoreCase(m.jsonField))) {
        return e.getValue();
      }
    }
    return null;
  }

  private static Object findParamIgnoreCase(Map<String, Object> params, String name) {
    String key = findParamKey(params, name);
    return key != null ? params.get(key) : null;
  }

  private static String findParamKey(Map<String, Object> params, String name) {
    if (params.containsKey(name)) {
      return name;
    }
    for (String k : params.keySet()) {
      if (k != null && k.equalsIgnoreCase(name)) {
        return k;
      }
    }
    return null;
  }

  private static boolean containsKeyIgnoreCase(Map<String, Object> params, String name) {
    return findParamKey(params, name) != null;
  }

  private static String requireSafeIdent(String ident, String label) throws PSPipelineIrException {
    if (ident == null || !SAFE_IDENT.matcher(ident).matches()) {
      throw new PSPipelineIrException(
          "Unsafe or missing SQL " + label + " identifier: " + ident);
    }
    return ident;
  }

  /** Quote identifier with double-quotes (ANSI / H2). Ident already validated. */
  static String quoteIdent(String ident) {
    return "\"" + ident + "\"";
  }

  private static boolean containsWord(String upperSql, String word) {
    Pattern p = Pattern.compile("\\b" + word + "\\b");
    return p.matcher(upperSql).find();
  }

  /**
   * Removes single-quoted SQL string literals (including {@code ''} escapes) so keyword checks do
   * not false-positive on values such as {@code 'EXEC sp'}.
   */
  static String stripSqlStringLiterals(String sql) {
    if (sql == null || sql.isEmpty()) {
      return sql == null ? "" : sql;
    }
    StringBuilder out = new StringBuilder(sql.length());
    boolean inString = false;
    for (int i = 0; i < sql.length(); i++) {
      char c = sql.charAt(i);
      if (c == '\'') {
        if (inString) {
          // doubled quote inside a literal is an escaped apostrophe
          if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
            i++;
            continue;
          }
          inString = false;
        } else {
          inString = true;
        }
        continue;
      }
      if (!inString) {
        out.append(c);
      }
    }
    return out.toString();
  }

  private static String firstToken(String sql) {
    String[] parts = sql.trim().split("\\s+", 2);
    return parts.length > 0 ? parts[0] : "";
  }

  private static final class ColumnMapping {
    final String column;
    final String jsonField;
    final String documentField;

    ColumnMapping(String column, String jsonField, String documentField) {
      this.column = column;
      this.jsonField = jsonField;
      this.documentField = documentField;
    }
  }
}
