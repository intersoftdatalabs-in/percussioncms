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
   * Resolve the mutation operation for an UPDATE resource from request + updater flags.
   *
   * <p>If {@code request.operation} is set, it must be allowed by the updater. If omitted and
   * exactly one of insert/update/delete is allowed, that operation is chosen. If multiple are
   * allowed and operation is omitted, a clear API error is raised.
   *
   * @return one of {@link PipelineExecuteRequest#OP_INSERT}, {@link
   *     PipelineExecuteRequest#OP_UPDATE}, {@link PipelineExecuteRequest#OP_DELETE}
   */
  public static String resolveMutationOperation(
      PipelineResourceIr resource, PipelineExecuteRequest request) throws PSPipelineIrException {
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(request, "request");
    if (!PipelineResourceIr.KIND_UPDATE.equals(resource.getKind())) {
      throw new PSPipelineIrException(
          "Resource kind is not UPDATE: " + resource.getKind() + " (" + resource.getName() + ")");
    }
    UpdaterStageIr updater =
        resource.getStages() != null ? resource.getStages().getUpdater() : null;
    if (updater == null || !updater.isPresent()) {
      throw new PSPipelineIrException(
          "UPDATE resource has no updater stage: " + resource.getName());
    }
    boolean ins = updater.isAllowInsert();
    boolean upd = updater.isAllowUpdate();
    boolean del = updater.isAllowDelete();
    if (!ins && !upd && !del) {
      throw new PSPipelineIrException(
          "UPDATE resource allows none of insert/update/delete: " + resource.getName());
    }

    String op = request.getOperation();
    if (op != null && !op.isBlank()) {
      String normalized = op.trim().toLowerCase(Locale.ROOT);
      switch (normalized) {
        case PipelineExecuteRequest.OP_INSERT:
          if (!ins) {
            throw new PSPipelineIrException(
                "UPDATE resource does not allow insert (updater.allowInsert=false): "
                    + resource.getName());
          }
          return PipelineExecuteRequest.OP_INSERT;
        case PipelineExecuteRequest.OP_UPDATE:
          if (!upd) {
            throw new PSPipelineIrException(
                "UPDATE resource does not allow update (updater.allowUpdate=false): "
                    + resource.getName());
          }
          return PipelineExecuteRequest.OP_UPDATE;
        case PipelineExecuteRequest.OP_DELETE:
          if (!del) {
            throw new PSPipelineIrException(
                "UPDATE resource does not allow delete (updater.allowDelete=false): "
                    + resource.getName());
          }
          return PipelineExecuteRequest.OP_DELETE;
        default:
          throw new PSPipelineIrException(
              "Unknown mutation operation '"
                  + op
                  + "' (expected insert, update, or delete): "
                  + resource.getName());
      }
    }

    int allowed = (ins ? 1 : 0) + (upd ? 1 : 0) + (del ? 1 : 0);
    if (allowed > 1) {
      throw new PSPipelineIrException(
          "UPDATE resource allows multiple mutations; request.operation is required"
              + " (insert/update/delete): "
              + resource.getName());
    }
    if (ins) {
      return PipelineExecuteRequest.OP_INSERT;
    }
    if (upd) {
      return PipelineExecuteRequest.OP_UPDATE;
    }
    return PipelineExecuteRequest.OP_DELETE;
  }

  /**
   * Plan INSERT statements when updater allows insert and request has rows (or params as a single
   * row).
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
          "UPDATE resource does not allow insert (updater.allowInsert=false): "
              + resource.getName());
    }

    String table = requireSingleTable(resource);
    List<ColumnMapping> mappings = columnMappings(resource);
    if (mappings.isEmpty()) {
      throw new PSPipelineIrException(
          "UPDATE resource has no COLUMN mappings for insert: " + resource.getName());
    }

    List<Map<String, Object>> rows = mutationRows(request, "Insert");

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

  /**
   * Plan UPDATE statements when updater allows update.
   *
   * <p>Each row produces {@code UPDATE table SET col=? ... WHERE key=? AND ...}. Key columns come
   * from {@code request.keyColumns}, or — when empty — from {@code request.params} keys that map to
   * columns (params supply shared WHERE values applied to every row). SET columns are mapped
   * columns present on the row that are not keys.
   */
  public static List<PSPipelineSqlPlan> planUpdates(
      PipelineResourceIr resource, PipelineExecuteRequest request) throws PSPipelineIrException {
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(request, "request");
    if (!PipelineResourceIr.KIND_UPDATE.equals(resource.getKind())) {
      throw new PSPipelineIrException(
          "Resource kind is not UPDATE: " + resource.getKind() + " (" + resource.getName() + ")");
    }
    UpdaterStageIr updater =
        resource.getStages() != null ? resource.getStages().getUpdater() : null;
    if (updater == null || !updater.isPresent() || !updater.isAllowUpdate()) {
      throw new PSPipelineIrException(
          "UPDATE resource does not allow update (updater.allowUpdate=false): "
              + resource.getName());
    }

    String table = requireSingleTable(resource);
    List<ColumnMapping> mappings = columnMappings(resource);
    if (mappings.isEmpty()) {
      throw new PSPipelineIrException(
          "UPDATE resource has no COLUMN mappings for update: " + resource.getName());
    }

    KeySpec keys = resolveKeySpec(mappings, request, "Update");
    List<Map<String, Object>> rows = mutationRows(request, "Update");

    List<PSPipelineSqlPlan> plans = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      List<String> setCols = new ArrayList<>();
      List<Object> setValues = new ArrayList<>();
      for (ColumnMapping m : mappings) {
        if (keys.keyColumnNames.contains(m.column.toUpperCase(Locale.ROOT))) {
          continue;
        }
        Object v = findValue(row, m);
        if (v != null) {
          setCols.add(m.column);
          setValues.add(v);
        }
      }
      if (setCols.isEmpty()) {
        throw new PSPipelineIrException(
            "Update row has no SET values (non-key mapped columns required)");
      }

      List<Object> whereValues = new ArrayList<>();
      List<String> whereCols = new ArrayList<>();
      for (String keyCol : keys.orderedKeyColumns) {
        Object kv = resolveKeyValue(row, keyCol, keys.sharedParams, mappings);
        if (kv == null && !hasKeyPresent(row, keyCol, keys.sharedParams, mappings)) {
          throw new PSPipelineIrException(
              "Update row missing key column value for WHERE: " + keyCol);
        }
        whereCols.add(keyCol);
        whereValues.add(kv);
      }

      StringBuilder sql = new StringBuilder("UPDATE ").append(quoteIdent(table)).append(" SET ");
      for (int i = 0; i < setCols.size(); i++) {
        if (i > 0) {
          sql.append(", ");
        }
        sql.append(quoteIdent(setCols.get(i))).append(" = ?");
      }
      sql.append(" WHERE ");
      for (int i = 0; i < whereCols.size(); i++) {
        if (i > 0) {
          sql.append(" AND ");
        }
        sql.append(quoteIdent(whereCols.get(i))).append(" = ?");
      }

      List<Object> binds = new ArrayList<>(setValues.size() + whereValues.size());
      binds.addAll(setValues);
      binds.addAll(whereValues);
      plans.add(
          new PSPipelineSqlPlan(
              PSPipelineSqlPlan.Kind.UPDATE, sql.toString(), binds, "update:" + table));
    }
    return plans;
  }

  /**
   * Plan DELETE statements when updater allows delete.
   *
   * <p>WHERE is built from {@code keyColumns} (preferred) or mapped columns present on each row /
   * shared {@code params}. Unrestricted deletes (empty WHERE) are rejected.
   */
  public static List<PSPipelineSqlPlan> planDeletes(
      PipelineResourceIr resource, PipelineExecuteRequest request) throws PSPipelineIrException {
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(request, "request");
    if (!PipelineResourceIr.KIND_UPDATE.equals(resource.getKind())) {
      throw new PSPipelineIrException(
          "Resource kind is not UPDATE: " + resource.getKind() + " (" + resource.getName() + ")");
    }
    UpdaterStageIr updater =
        resource.getStages() != null ? resource.getStages().getUpdater() : null;
    if (updater == null || !updater.isPresent() || !updater.isAllowDelete()) {
      throw new PSPipelineIrException(
          "UPDATE resource does not allow delete (updater.allowDelete=false): "
              + resource.getName());
    }

    String table = requireSingleTable(resource);
    List<ColumnMapping> mappings = columnMappings(resource);
    if (mappings.isEmpty()) {
      throw new PSPipelineIrException(
          "UPDATE resource has no COLUMN mappings for delete: " + resource.getName());
    }

    boolean keysExplicit = hasExplicitKeys(request);
    KeySpec keys =
        keysExplicit
            ? resolveKeySpec(mappings, request, "Delete")
            : new KeySpec(List.of(), Set.of(), Map.of());
    List<Map<String, Object>> rows = mutationRows(request, "Delete");

    List<PSPipelineSqlPlan> plans = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      List<String> whereCols = new ArrayList<>();
      List<Object> whereValues = new ArrayList<>();
      if (keysExplicit) {
        for (String keyCol : keys.orderedKeyColumns) {
          Object kv = resolveKeyValue(row, keyCol, keys.sharedParams, mappings);
          if (kv == null && !hasKeyPresent(row, keyCol, keys.sharedParams, mappings)) {
            throw new PSPipelineIrException(
                "Delete row missing key column value for WHERE: " + keyCol);
          }
          whereCols.add(keyCol);
          whereValues.add(kv);
        }
      } else {
        // No keyColumns/params: WHERE from mapped columns present on this row only
        for (ColumnMapping m : mappings) {
          if (!containsKeyForMapping(row, m)) {
            continue;
          }
          whereCols.add(m.column);
          whereValues.add(findValue(row, m));
        }
      }
      if (whereCols.isEmpty()) {
        throw new PSPipelineIrException(
            "Delete requires a non-empty WHERE (keyColumns, params, or row key values): "
                + resource.getName());
      }

      StringBuilder sql =
          new StringBuilder("DELETE FROM ").append(quoteIdent(table)).append(" WHERE ");
      for (int i = 0; i < whereCols.size(); i++) {
        if (i > 0) {
          sql.append(" AND ");
        }
        sql.append(quoteIdent(whereCols.get(i))).append(" = ?");
      }
      plans.add(
          new PSPipelineSqlPlan(
              PSPipelineSqlPlan.Kind.UPDATE, sql.toString(), whereValues, "delete:" + table));
    }
    return plans;
  }

  private static boolean hasExplicitKeys(PipelineExecuteRequest request) {
    return (request.getKeyColumns() != null && !request.getKeyColumns().isEmpty())
        || (request.getParams() != null && !request.getParams().isEmpty());
  }

  private static List<Map<String, Object>> mutationRows(
      PipelineExecuteRequest request, String label) throws PSPipelineIrException {
    List<Map<String, Object>> rows = new ArrayList<>();
    if (request.getRows() != null && !request.getRows().isEmpty()) {
      rows.addAll(request.getRows());
    } else if (request.getParams() != null && !request.getParams().isEmpty()) {
      rows.add(new LinkedHashMap<>(request.getParams()));
    } else {
      throw new PSPipelineIrException(label + " requires request.rows or request.params");
    }
    return rows;
  }

  private static KeySpec resolveKeySpec(
      List<ColumnMapping> mappings, PipelineExecuteRequest request, String label)
      throws PSPipelineIrException {
    List<String> ordered = new ArrayList<>();
    Set<String> upper = new HashSet<>();
    Map<String, Object> shared =
        request.getParams() != null ? request.getParams() : Map.of();

    if (request.getKeyColumns() != null && !request.getKeyColumns().isEmpty()) {
      for (String raw : request.getKeyColumns()) {
        if (raw == null || raw.isBlank()) {
          continue;
        }
        ColumnMapping match = findMappingForParam(mappings, raw.trim());
        if (match == null) {
          throw new PSPipelineIrException(
              label + " keyColumns entry does not match a mapped column: " + raw);
        }
        String colKey = match.column.toUpperCase(Locale.ROOT);
        if (upper.add(colKey)) {
          ordered.add(match.column);
        }
      }
    } else if (!shared.isEmpty()) {
      // Shared params keys that map to columns become WHERE keys
      for (Map.Entry<String, Object> e : shared.entrySet()) {
        ColumnMapping match = findMappingForParam(mappings, e.getKey());
        if (match == null) {
          continue;
        }
        String colKey = match.column.toUpperCase(Locale.ROOT);
        if (upper.add(colKey)) {
          ordered.add(match.column);
        }
      }
    }

    if (ordered.isEmpty()) {
      throw new PSPipelineIrException(
          label
              + " requires keyColumns or mapped params for WHERE (unrestricted mutation rejected): "
              + "provide request.keyColumns or request.params matching mapped columns");
    }
    return new KeySpec(ordered, upper, shared);
  }

  private static Object resolveKeyValue(
      Map<String, Object> row,
      String keyCol,
      Map<String, Object> sharedParams,
      List<ColumnMapping> mappings) {
    ColumnMapping m = findMappingForParam(mappings, keyCol);
    if (m != null) {
      Object fromRow = findValue(row, m);
      if (fromRow != null || containsKeyForMapping(row, m)) {
        return fromRow;
      }
    }
    // Shared params (case-insensitive)
    Object fromShared = findParamIgnoreCase(sharedParams, keyCol);
    if (fromShared != null || containsKeyIgnoreCase(sharedParams, keyCol)) {
      return fromShared;
    }
    if (m != null) {
      Object byJson = findParamIgnoreCase(sharedParams, m.jsonField);
      if (byJson != null || containsKeyIgnoreCase(sharedParams, m.jsonField)) {
        return byJson;
      }
    }
    return null;
  }

  private static boolean hasKeyPresent(
      Map<String, Object> row,
      String keyCol,
      Map<String, Object> sharedParams,
      List<ColumnMapping> mappings) {
    ColumnMapping m = findMappingForParam(mappings, keyCol);
    if (m != null && containsKeyForMapping(row, m)) {
      return true;
    }
    if (containsKeyIgnoreCase(sharedParams, keyCol)) {
      return true;
    }
    return m != null && containsKeyIgnoreCase(sharedParams, m.jsonField);
  }

  private static boolean containsKeyForMapping(Map<String, Object> row, ColumnMapping m) {
    if (row.containsKey(m.column) || row.containsKey(m.jsonField)) {
      return true;
    }
    for (String k : row.keySet()) {
      if (k != null
          && (k.equalsIgnoreCase(m.column) || k.equalsIgnoreCase(m.jsonField))) {
        return true;
      }
    }
    return false;
  }

  private static final class KeySpec {
    final List<String> orderedKeyColumns;
    final Set<String> keyColumnNames;
    final Map<String, Object> sharedParams;

    KeySpec(
        List<String> orderedKeyColumns,
        Set<String> keyColumnNames,
        Map<String, Object> sharedParams) {
      this.orderedKeyColumns = orderedKeyColumns;
      this.keyColumnNames = keyColumnNames;
      this.sharedParams = sharedParams != null ? sharedParams : Map.of();
    }
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
