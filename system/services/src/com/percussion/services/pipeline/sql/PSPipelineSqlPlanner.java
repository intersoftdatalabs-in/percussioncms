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
import com.percussion.services.pipeline.model.BackendJoinIr;
import com.percussion.services.pipeline.model.BackendTableRefIr;
import com.percussion.services.pipeline.model.BackendTankStageIr;
import com.percussion.services.pipeline.model.MapperStageIr;
import com.percussion.services.pipeline.model.MappingEntryIr;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.model.SelectorStageIr;
import com.percussion.services.pipeline.model.UpdaterStageIr;
import com.percussion.services.pipeline.model.WhereClauseIr;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 *   <li>QUERY multi-table SELECT uses IR join edges ({@code backendTank.joins}) with ANSI JOIN
 *       ({@code INNER}/{@code LEFT}/{@code RIGHT}/{@code FULL OUTER}). Join edges with classic
 *       translators, disconnected graphs, or multi-table tanks without edges are rejected.
 *   <li>INSERT/UPDATE/DELETE remain single-table only.
 * </ul>
 */
public final class PSPipelineSqlPlanner {

  private static final Pattern SAFE_IDENT = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
  private static final Pattern NAMED_PARAM = Pattern.compile(":([A-Za-z_][A-Za-z0-9_]*)");

  /** Relational operators allowed in generated WHERE (case-insensitive match after normalize). */
  private static final Set<String> BINARY_OPS =
      Set.of("=", "<>", "!=", "<", "<=", ">", ">=", "LIKE", "NOT LIKE");

  private static final Set<String> UNARY_OPS = Set.of("IS NULL", "IS NOT NULL");

  /**
   * Rejection message when join inventory is present without executable join edges (legacy IR) or
   * multi-table tank has no graph. Keep stable for tests and API clients.
   */
  public static final String JOIN_PRODUCT_LIMIT_MESSAGE =
      "Product limit: multi-table join SQL requires backendTank.joins edges"
          + " (joinCount alone is not enough). Provide join graph IR, use a single backend table,"
          + " or the selector nativeStatement escape hatch for hand-authored SELECT with joins.";

  /** Rejection when a join edge carries a classic extension translator. */
  public static final String JOIN_TRANSLATOR_LIMIT_MESSAGE =
      "Product limit: join edges with classic translators are not supported by the generated"
          + " planner; use selector nativeStatement for translated join predicates.";

  private PSPipelineSqlPlanner() {}

  /**
   * Plan a QUERY resource.
   *
   * <p>Strategy:
   *
   * <ol>
   *   <li>If selector uses {@code nativeStatement}, compile named-parameter SELECT.
   *   <li>Else build projection from single backend table + COLUMN mapper mappings; WHERE from
   *       selector where-clause IR when present, otherwise request params that match mapped columns
   *       / JSON field names (equality only).
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
    BackendTankStageIr tank = requireBackendTank(resource);
    List<BackendTableRefIr> tables = tank.getTables();
    if (tables == null || tables.isEmpty()) {
      throw new PSPipelineIrException(
          "Pipeline SQL planner requires at least one backend table. Resource: "
              + resource.getName());
    }
    List<BackendJoinIr> joins =
        tank.getJoins() != null ? tank.getJoins() : List.of();

    // Multi-table without edges, or legacy joinCount without edges → reject.
    if (tables.size() > 1 && joins.isEmpty()) {
      throw new PSPipelineIrException(
          JOIN_PRODUCT_LIMIT_MESSAGE + " Resource: " + resource.getName());
    }
    if (joins.isEmpty() && tank.getJoinCount() > 0) {
      throw new PSPipelineIrException(
          JOIN_PRODUCT_LIMIT_MESSAGE + " Resource: " + resource.getName());
    }
    // Single table with stray join edges is invalid.
    if (tables.size() == 1 && !joins.isEmpty()) {
      throw new PSPipelineIrException(
          "Backend tank has join edges but only one table; remove joins or add tables. Resource: "
              + resource.getName());
    }

    List<ColumnMapping> mappings = columnMappings(resource);
    if (mappings.isEmpty()) {
      throw new PSPipelineIrException(
          "QUERY resource has no COLUMN mappings to project: " + resource.getName());
    }

    SelectorStageIr selector =
        resource.getStages() != null ? resource.getStages().getSelector() : null;
    boolean distinct = selector != null && selector.isUnique();
    boolean multiTable = !joins.isEmpty();

    StringBuilder sql = new StringBuilder("SELECT ");
    if (distinct) {
      sql.append("DISTINCT ");
    }
    for (int i = 0; i < mappings.size(); i++) {
      ColumnMapping m = mappings.get(i);
      if (i > 0) {
        sql.append(", ");
      }
      sql.append(columnSql(m, multiTable)).append(" AS ").append(quoteIdent(m.jsonField));
    }

    String planLabel;
    List<QualifiedColumn> deferredJoinEquals = List.of();
    if (!multiTable) {
      String table = tableName(tables.get(0));
      sql.append(" FROM ").append(quoteIdent(table));
      planLabel = "generatedSelect:" + table;
    } else {
      deferredJoinEquals = appendMultiTableFrom(sql, tables, joins, resource.getName());
      planLabel = "generatedSelect:join:" + tables.size() + "t/" + joins.size() + "j";
    }

    List<Object> binds = new ArrayList<>();
    Map<String, Object> params =
        request.getParams() != null ? request.getParams() : Map.of();

    List<WhereClauseIr> irClauses =
        selector != null && selector.getWhereClauses() != null
            ? selector.getWhereClauses()
            : List.of();
    boolean whereStarted = false;
    if (!irClauses.isEmpty()) {
      whereStarted = appendWhereFromIr(sql, binds, irClauses, params, resource.getName(), multiTable);
    } else if (!params.isEmpty()) {
      whereStarted = appendWhereFromRequestParams(sql, binds, mappings, params, multiTable);
    }
    appendDeferredJoinEquals(sql, deferredJoinEquals, whereStarted);

    return new PSPipelineSqlPlan(PSPipelineSqlPlan.Kind.QUERY, sql.toString(), binds, planLabel);
  }

  /**
   * Emit {@code FROM t0 a0 JOIN t1 a1 ON ...} for a connected join graph. Returns equality
   * predicates for join edges that connect already-included tables (cycles), to fold into WHERE.
   */
  private static List<QualifiedColumn> appendMultiTableFrom(
      StringBuilder sql,
      List<BackendTableRefIr> tables,
      List<BackendJoinIr> joins,
      String resourceName)
      throws PSPipelineIrException {
    Map<String, BackendTableRefIr> byAlias = new LinkedHashMap<>();
    for (BackendTableRefIr t : tables) {
      if (t == null) {
        continue;
      }
      String alias = tableAlias(t);
      String key = alias.toUpperCase(Locale.ROOT);
      if (byAlias.containsKey(key)) {
        throw new PSPipelineIrException(
            "Duplicate backend table alias '" + alias + "' on resource: " + resourceName);
      }
      byAlias.put(key, t);
    }
    if (byAlias.isEmpty()) {
      throw new PSPipelineIrException(
          "Pipeline SQL planner requires at least one backend table. Resource: " + resourceName);
    }

    List<ResolvedJoin> resolved = new ArrayList<>();
    for (BackendJoinIr edge : joins) {
      if (edge == null) {
        continue;
      }
      if (edge.isTranslatorPresent()) {
        throw new PSPipelineIrException(
            JOIN_TRANSLATOR_LIMIT_MESSAGE + " Resource: " + resourceName);
      }
      resolved.add(resolveJoin(edge, byAlias, resourceName));
    }
    if (resolved.isEmpty()) {
      throw new PSPipelineIrException(
          JOIN_PRODUCT_LIMIT_MESSAGE + " Resource: " + resourceName);
    }

    // Root = first table in tank order.
    BackendTableRefIr root = byAlias.values().iterator().next();
    String rootAlias = tableAlias(root);
    sql.append(" FROM ");
    appendTableRef(sql, root);

    Set<String> included = new LinkedHashSet<>();
    included.add(rootAlias.toUpperCase(Locale.ROOT));

    List<ResolvedJoin> remaining = new ArrayList<>(resolved);
    List<ResolvedJoin> deferred = new ArrayList<>();

    while (!remaining.isEmpty()) {
      boolean progress = false;
      Iterator<ResolvedJoin> it = remaining.iterator();
      while (it.hasNext()) {
        ResolvedJoin j = it.next();
        boolean leftIn = included.contains(j.leftAlias.toUpperCase(Locale.ROOT));
        boolean rightIn = included.contains(j.rightAlias.toUpperCase(Locale.ROOT));
        if (leftIn && rightIn) {
          deferred.add(j);
          it.remove();
          progress = true;
        } else if (leftIn && !rightIn) {
          appendAnsiJoin(sql, j.joinType, byAlias.get(j.rightAlias.toUpperCase(Locale.ROOT)), j);
          included.add(j.rightAlias.toUpperCase(Locale.ROOT));
          it.remove();
          progress = true;
        } else if (rightIn && !leftIn) {
          // Introduce left table; flip outer-join direction to preserve semantics.
          String flipped = flipJoinType(j.joinType);
          ResolvedJoin flippedJoin =
              new ResolvedJoin(
                  flipped, j.rightAlias, j.rightColumn, j.leftAlias, j.leftColumn);
          appendAnsiJoin(sql, flipped, byAlias.get(j.leftAlias.toUpperCase(Locale.ROOT)), flippedJoin);
          included.add(j.leftAlias.toUpperCase(Locale.ROOT));
          it.remove();
          progress = true;
        }
      }
      if (!progress) {
        throw new PSPipelineIrException(
            "Disconnected join graph: cannot attach remaining join edges from root table '"
                + rootAlias
                + "' on resource: "
                + resourceName);
      }
    }

    if (included.size() != byAlias.size()) {
      throw new PSPipelineIrException(
          "Join graph does not include all backend tables on resource: " + resourceName);
    }

    // Cycle edges (both tables already in FROM): fold into WHERE as equality pairs.
    List<QualifiedColumn> deferredMarker = new ArrayList<>();
    for (ResolvedJoin d : deferred) {
      deferredMarker.add(new QualifiedColumn(d.leftAlias, d.leftColumn));
      deferredMarker.add(new QualifiedColumn(d.rightAlias, d.rightColumn));
    }
    return deferredMarker;
  }

  private static void appendDeferredJoinEquals(
      StringBuilder sql, List<QualifiedColumn> deferredPairs, boolean whereAlreadyStarted) {
    if (deferredPairs == null || deferredPairs.isEmpty()) {
      return;
    }
    // pairs: (left0, right0, left1, right1, ...)
    List<String> parts = new ArrayList<>();
    for (int i = 0; i + 1 < deferredPairs.size(); i += 2) {
      QualifiedColumn left = deferredPairs.get(i);
      QualifiedColumn right = deferredPairs.get(i + 1);
      parts.add(qualifiedColumnSql(left) + " = " + qualifiedColumnSql(right));
    }
    if (parts.isEmpty()) {
      return;
    }
    if (whereAlreadyStarted) {
      sql.append(" AND ");
    } else {
      sql.append(" WHERE ");
    }
    sql.append(String.join(" AND ", parts));
  }

  private static ResolvedJoin resolveJoin(
      BackendJoinIr edge, Map<String, BackendTableRefIr> byAlias, String resourceName)
      throws PSPipelineIrException {
    QualifiedColumn left = parseBackendColumn(edge.getLeft() != null ? edge.getLeft() : "");
    QualifiedColumn right = parseBackendColumn(edge.getRight() != null ? edge.getRight() : "");
    if (left.tableAlias == null || right.tableAlias == null) {
      throw new PSPipelineIrException(
          "Join edge columns must be qualified as alias.column on resource: " + resourceName);
    }
    String leftKey = left.tableAlias.toUpperCase(Locale.ROOT);
    String rightKey = right.tableAlias.toUpperCase(Locale.ROOT);
    if (!byAlias.containsKey(leftKey) || !byAlias.containsKey(rightKey)) {
      throw new PSPipelineIrException(
          "Join edge references unknown table alias (left="
              + left.tableAlias
              + ", right="
              + right.tableAlias
              + ") on resource: "
              + resourceName);
    }
    if (leftKey.equals(rightKey)) {
      throw new PSPipelineIrException(
          "Join edge cannot join a table to itself on resource: " + resourceName);
    }
    String type = normalizeJoinType(edge.getJoinType());
    return new ResolvedJoin(type, left.tableAlias, left.column, right.tableAlias, right.column);
  }

  private static String normalizeJoinType(String raw) throws PSPipelineIrException {
    if (raw == null || raw.isBlank()) {
      return BackendJoinIr.TYPE_INNER;
    }
    String t = raw.trim().toUpperCase(Locale.ROOT);
    // Accept classic-style names too.
    if ("INNER".equals(t) || "INNERJOIN".equals(t)) {
      return BackendJoinIr.TYPE_INNER;
    }
    if ("LEFT".equals(t) || "LEFTOUTER".equals(t) || "LEFT_OUTER".equals(t) || "LEFT OUTER".equals(t)) {
      return BackendJoinIr.TYPE_LEFT;
    }
    if ("RIGHT".equals(t)
        || "RIGHTOUTER".equals(t)
        || "RIGHT_OUTER".equals(t)
        || "RIGHT OUTER".equals(t)) {
      return BackendJoinIr.TYPE_RIGHT;
    }
    if ("FULL".equals(t)
        || "FULLOUTER".equals(t)
        || "FULL_OUTER".equals(t)
        || "FULL OUTER".equals(t)) {
      return BackendJoinIr.TYPE_FULL;
    }
    throw new PSPipelineIrException("Unsupported join type: " + raw);
  }

  private static String flipJoinType(String type) {
    if (BackendJoinIr.TYPE_LEFT.equals(type)) {
      return BackendJoinIr.TYPE_RIGHT;
    }
    if (BackendJoinIr.TYPE_RIGHT.equals(type)) {
      return BackendJoinIr.TYPE_LEFT;
    }
    return type; // INNER / FULL are symmetric
  }

  private static void appendAnsiJoin(
      StringBuilder sql, String joinType, BackendTableRefIr newTable, ResolvedJoin on)
      throws PSPipelineIrException {
    if (newTable == null) {
      throw new PSPipelineIrException("Join introduces unknown table");
    }
    sql.append(' ').append(ansiJoinKeyword(joinType)).append(' ');
    appendTableRef(sql, newTable);
    sql.append(" ON ")
        .append(quoteIdent(on.leftAlias))
        .append('.')
        .append(quoteIdent(on.leftColumn))
        .append(" = ")
        .append(quoteIdent(on.rightAlias))
        .append('.')
        .append(quoteIdent(on.rightColumn));
  }

  private static String ansiJoinKeyword(String joinType) throws PSPipelineIrException {
    return switch (joinType) {
      case BackendJoinIr.TYPE_INNER -> "INNER JOIN";
      case BackendJoinIr.TYPE_LEFT -> "LEFT OUTER JOIN";
      case BackendJoinIr.TYPE_RIGHT -> "RIGHT OUTER JOIN";
      case BackendJoinIr.TYPE_FULL -> "FULL OUTER JOIN";
      default -> throw new PSPipelineIrException("Unsupported join type: " + joinType);
    };
  }

  private static void appendTableRef(StringBuilder sql, BackendTableRefIr t)
      throws PSPipelineIrException {
    String table = tableName(t);
    String alias = tableAlias(t);
    sql.append(quoteIdent(table)).append(' ').append(quoteIdent(alias));
  }

  private static String tableName(BackendTableRefIr t) throws PSPipelineIrException {
    String table = t.getTable();
    if (table == null || table.isBlank()) {
      table = t.getAlias();
    }
    return requireSafeIdent(table, "table");
  }

  private static String tableAlias(BackendTableRefIr t) throws PSPipelineIrException {
    String alias = t.getAlias();
    if (alias == null || alias.isBlank()) {
      alias = t.getTable();
    }
    return requireSafeIdent(alias, "table alias");
  }

  private static final class ResolvedJoin {
    final String joinType;
    final String leftAlias;
    final String leftColumn;
    final String rightAlias;
    final String rightColumn;

    ResolvedJoin(
        String joinType,
        String leftAlias,
        String leftColumn,
        String rightAlias,
        String rightColumn) {
      this.joinType = joinType;
      this.leftAlias = leftAlias;
      this.leftColumn = leftColumn;
      this.rightAlias = rightAlias;
      this.rightColumn = rightColumn;
    }
  }

  private static final class QualifiedColumn {
    final String tableAlias; // nullable
    final String column;

    QualifiedColumn(String tableAlias, String column) {
      this.tableAlias = tableAlias;
      this.column = column;
    }
  }

  /**
   * Build WHERE from selector where-clause IR (classic import / native IR). Values are always JDBC
   * binds; identifiers are validated.
   *
   * @return true when a WHERE clause was appended
   */
  private static boolean appendWhereFromIr(
      StringBuilder sql,
      List<Object> binds,
      List<WhereClauseIr> clauses,
      Map<String, Object> params,
      String resourceName,
      boolean multiTable)
      throws PSPipelineIrException {
    List<String> parts = new ArrayList<>();
    // Connector after each emitted part (classic: boolean on a clause joins it to the next).
    List<String> trailingConnectors = new ArrayList<>();
    for (WhereClauseIr clause : clauses) {
      if (clause == null) {
        continue;
      }
      PredicateFrag frag = compileWherePredicate(clause, params, resourceName, multiTable);
      if (frag == null) {
        // omitWhenNull skipped this predicate — do not consume its boolean either
        continue;
      }
      parts.add(frag.sql);
      binds.addAll(frag.binds);
      trailingConnectors.add(frag.booleanOpAfter);
    }
    if (parts.isEmpty()) {
      return false;
    }
    sql.append(" WHERE ");
    for (int i = 0; i < parts.size(); i++) {
      if (i > 0) {
        // Use the previous clause's boolean as the join to this clause
        sql.append(' ').append(trailingConnectors.get(i - 1)).append(' ');
      }
      sql.append(parts.get(i));
    }
    return true;
  }

  /**
   * @return null when predicate should be omitted (omitWhenNull + missing/null param)
   */
  private static PredicateFrag compileWherePredicate(
      WhereClauseIr clause, Map<String, Object> params, String resourceName, boolean multiTable)
      throws PSPipelineIrException {
    String opRaw = clause.getOperator();
    if (opRaw == null || opRaw.isBlank()) {
      throw new PSPipelineIrException(
          "Where-clause IR missing operator on resource: " + resourceName);
    }
    String op = normalizeOperator(opRaw);

    if (!WhereClauseIr.KIND_COLUMN.equals(clause.getLeftKind())) {
      throw new PSPipelineIrException(
          "Where-clause IR left side must be COLUMN (got "
              + clause.getLeftKind()
              + ") on resource: "
              + resourceName
              + " — use nativeStatement for non-column predicates");
    }
    String leftSql =
        backendColumnSql(clause.getLeft() != null ? clause.getLeft() : "", multiTable);

    String bool =
        clause.getBooleanOp() != null && !clause.getBooleanOp().isBlank()
            ? clause.getBooleanOp().trim().toUpperCase(Locale.ROOT)
            : WhereClauseIr.BOOL_AND;
    if (!WhereClauseIr.BOOL_AND.equals(bool) && !WhereClauseIr.BOOL_OR.equals(bool)) {
      throw new PSPipelineIrException(
          "Where-clause IR booleanOp must be AND or OR (got " + bool + "): " + resourceName);
    }

    if (UNARY_OPS.contains(op)) {
      return new PredicateFrag(leftSql + " " + op, List.of(), bool);
    }

    if (!BINARY_OPS.contains(op)) {
      throw new PSPipelineIrException(
          "Where-clause IR operator not supported by generated planner: "
              + opRaw
              + " on resource: "
              + resourceName
              + " (supported: =, <>, !=, <, <=, >, >=, LIKE, NOT LIKE, IS NULL, IS NOT NULL)");
    }

    // SQL uses <> for inequality
    String sqlOp = "!=".equals(op) ? "<>" : op;

    String rightKind = clause.getRightKind() != null ? clause.getRightKind() : WhereClauseIr.KIND_OTHER;
    if (WhereClauseIr.KIND_COLUMN.equals(rightKind)) {
      String rightSql =
          backendColumnSql(clause.getRight() != null ? clause.getRight() : "", multiTable);
      return new PredicateFrag(leftSql + " " + sqlOp + " " + rightSql, List.of(), bool);
    }
    if (WhereClauseIr.KIND_PARAM.equals(rightKind)) {
      String paramName = clause.getRight();
      if (paramName == null || paramName.isBlank()) {
        throw new PSPipelineIrException(
            "Where-clause IR PARAM right side missing name on resource: " + resourceName);
      }
      boolean present = containsKeyIgnoreCase(params, paramName);
      Object value = present ? findParamIgnoreCase(params, paramName) : null;
      if (clause.isOmitWhenNull() && (!present || value == null)) {
        return null;
      }
      if (!present) {
        throw new PSPipelineIrException(
            "Missing request param for where-clause IR placeholder: "
                + paramName
                + " (resource: "
                + resourceName
                + ")");
      }
      return new PredicateFrag(leftSql + " " + sqlOp + " ?", List.of(value), bool);
    }
    if (WhereClauseIr.KIND_LITERAL.equals(rightKind)) {
      // Always bind literals as parameters — never concatenate into SQL text.
      Object lit = clause.getRight();
      if (clause.isOmitWhenNull() && lit == null) {
        return null;
      }
      return new PredicateFrag(leftSql + " " + sqlOp + " ?", List.of(lit), bool);
    }
    throw new PSPipelineIrException(
        "Where-clause IR right side kind not executable ("
            + rightKind
            + ") on resource: "
            + resourceName
            + " — use nativeStatement for OTHER kinds");
  }

  private static String normalizeOperator(String opRaw) {
    String t = opRaw.trim().replaceAll("\\s+", " ");
    String upper = t.toUpperCase(Locale.ROOT);
    // Preserve symbolic ops; upper-case word ops
    if ("!=".equals(t) || "<>".equals(t) || "=".equals(t) || "<".equals(t) || ">".equals(t)
        || "<=".equals(t) || ">=".equals(t)) {
      return t;
    }
    return upper;
  }

  /**
   * @return true when a WHERE clause was appended
   */
  private static boolean appendWhereFromRequestParams(
      StringBuilder sql,
      List<Object> binds,
      List<ColumnMapping> mappings,
      Map<String, Object> params,
      boolean multiTable) {
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
      String colKey =
          (match.tableAlias != null ? match.tableAlias + "." : "")
              + match.column.toUpperCase(Locale.ROOT);
      if (!usedColumns.add(colKey)) {
        continue;
      }
      whereParts.add(columnSql(match, multiTable) + " = ?");
      binds.add(e.getValue());
    }
    if (whereParts.isEmpty()) {
      return false;
    }
    sql.append(" WHERE ");
    sql.append(String.join(" AND ", whereParts));
    return true;
  }

  private static final class PredicateFrag {
    final String sql;
    final List<Object> binds;
    /** Boolean connector that joins this fragment to the next (classic clause boolean). */
    final String booleanOpAfter;

    PredicateFrag(String sql, List<Object> binds, String booleanOpAfter) {
      this.sql = sql;
      this.binds = binds;
      this.booleanOpAfter = booleanOpAfter;
    }
  }

  private static BackendTankStageIr requireBackendTank(PipelineResourceIr resource)
      throws PSPipelineIrException {
    if (resource.getStages() == null
        || resource.getStages().getBackendTank() == null
        || !resource.getStages().getBackendTank().isPresent()) {
      throw new PSPipelineIrException("Resource missing backend tank: " + resource.getName());
    }
    return resource.getStages().getBackendTank();
  }

  /**
   * Mutations (INSERT/UPDATE/DELETE) require exactly one backend table and no join graph.
   */
  private static String requireSingleTable(PipelineResourceIr resource)
      throws PSPipelineIrException {
    BackendTankStageIr tank = requireBackendTank(resource);
    List<BackendJoinIr> joins = tank.getJoins() != null ? tank.getJoins() : List.of();
    if (tank.getJoinCount() > 0 || !joins.isEmpty()) {
      throw new PSPipelineIrException(
          "Pipeline SQL planner mutations require a single backend table without joins. Resource: "
              + resource.getName());
    }
    List<BackendTableRefIr> tables = tank.getTables();
    if (tables == null || tables.size() != 1) {
      throw new PSPipelineIrException(
          "Pipeline SQL planner requires exactly one backend table (got "
              + (tables == null ? 0 : tables.size())
              + "); multi-table tanks without joins are also unsupported. Resource: "
              + resource.getName());
    }
    return tableName(tables.get(0));
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
      QualifiedColumn qc = parseBackendColumn(backend);
      String jsonField = jsonFieldFromDocument(entry.getDocumentField(), qc.column);
      out.add(new ColumnMapping(qc.tableAlias, qc.column, jsonField, entry.getDocumentField()));
    }
    return out;
  }

  static String columnFromBackend(String backend) throws PSPipelineIrException {
    return parseBackendColumn(backend).column;
  }

  static QualifiedColumn parseBackendColumn(String backend) throws PSPipelineIrException {
    String b = backend.trim();
    int dot = b.lastIndexOf('.');
    if (dot >= 0) {
      // Prefer last segment as column; previous segment as table alias (alias.col or *.alias.col).
      String col = b.substring(dot + 1).trim();
      String prefix = b.substring(0, dot).trim();
      int prev = prefix.lastIndexOf('.');
      String alias = prev >= 0 ? prefix.substring(prev + 1).trim() : prefix;
      return new QualifiedColumn(
          requireSafeIdent(alias, "table alias"), requireSafeIdent(col, "column"));
    }
    return new QualifiedColumn(null, requireSafeIdent(b, "column"));
  }

  private static String backendColumnSql(String backend, boolean multiTable)
      throws PSPipelineIrException {
    QualifiedColumn qc = parseBackendColumn(backend);
    if (multiTable && qc.tableAlias != null) {
      return qualifiedColumnSql(qc);
    }
    return quoteIdent(qc.column);
  }

  private static String columnSql(ColumnMapping m, boolean multiTable) {
    if (multiTable && m.tableAlias != null) {
      return quoteIdent(m.tableAlias) + "." + quoteIdent(m.column);
    }
    return quoteIdent(m.column);
  }

  private static String qualifiedColumnSql(QualifiedColumn qc) {
    if (qc.tableAlias != null) {
      return quoteIdent(qc.tableAlias) + "." + quoteIdent(qc.column);
    }
    return quoteIdent(qc.column);
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
    final String tableAlias; // nullable
    final String column;
    final String jsonField;
    final String documentField;

    ColumnMapping(String tableAlias, String column, String jsonField, String documentField) {
      this.tableAlias = tableAlias;
      this.column = column;
      this.jsonField = jsonField;
      this.documentField = documentField;
    }
  }
}
