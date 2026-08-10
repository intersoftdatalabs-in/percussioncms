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

package com.percussion.services.pipeline;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.services.pipeline.hooks.IPSPipelinePostExecuteHook;
import com.percussion.services.pipeline.hooks.IPSPipelinePreExecuteHook;
import com.percussion.services.pipeline.model.BackendJoinIr;
import com.percussion.services.pipeline.model.BackendTableRefIr;
import com.percussion.services.pipeline.model.BackendTankStageIr;
import com.percussion.services.pipeline.model.MapperStageIr;
import com.percussion.services.pipeline.model.MappingEntryIr;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.model.PipelineStagesIr;
import com.percussion.services.pipeline.model.SelectorStageIr;
import com.percussion.services.pipeline.model.UpdaterStageIr;
import com.percussion.services.pipeline.model.WhereClauseIr;
import com.percussion.services.pipeline.sql.PSJdbcPipelineSqlAdapter;
import com.percussion.services.pipeline.sql.PSPipelineSqlPlan;
import com.percussion.services.pipeline.sql.PSPipelineSqlPlanner;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * H2-backed integration tests for Slice A pipeline runtime: query SQL adapter, JSON I/O, pre/post
 * hooks, and minimal insert path.
 */
@DisplayName("Pipeline runtime service (Slice A SQL + JSON + hooks)")
class PSPipelineRuntimeServiceTest {

  @TempDir Path tempDir;

  private String jdbcUrl;
  private IPSPipelineIrService irService;
  private PSJdbcPipelineSqlAdapter sqlAdapter;

  @BeforeEach
  void setUp() throws Exception {
    jdbcUrl = "jdbc:h2:mem:pipeline_rt_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
    try (Connection c = DriverManager.getConnection(jdbcUrl);
        Statement st = c.createStatement()) {
      // Quote identifiers: VALUE (and others) are reserved in modern H2.
      st.execute(
          "CREATE TABLE \"PSX_ADMINLOOKUP\" ("
              + "\"TYPE\" VARCHAR(64), \"NAME\" VARCHAR(128), \"LOOKUPVALUE\" VARCHAR(256))");
      st.execute(
          "INSERT INTO \"PSX_ADMINLOOKUP\" (\"TYPE\", \"NAME\", \"LOOKUPVALUE\") VALUES "
              + "('workflow', 'wf1', '1'),"
              + "('workflow', 'wf2', '2'),"
              + "('locale', 'en-us', 'en')");
      st.execute(
          "CREATE TABLE \"PSX_LOOKUP_EXTRA\" ("
              + "\"LOOKUP_NAME\" VARCHAR(128), \"NOTE\" VARCHAR(256))");
      st.execute(
          "INSERT INTO \"PSX_LOOKUP_EXTRA\" (\"LOOKUP_NAME\", \"NOTE\") VALUES "
              + "('wf1', 'note-one'),"
              + "('wf2', 'note-two')");
    }
    irService = new PSPipelineIrService(tempDir);
    sqlAdapter =
        new PSJdbcPipelineSqlAdapter(
            () -> {
              try {
                return DriverManager.getConnection(jdbcUrl);
              } catch (java.sql.SQLException e) {
                throw new IllegalStateException("H2 connection failed", e);
              }
            });
  }

  @AfterEach
  void tearDown() throws Exception {
    try (Connection c = DriverManager.getConnection(jdbcUrl);
        Statement st = c.createStatement()) {
      st.execute("SHUTDOWN");
    } catch (Exception ignored) {
      // mem DB may already be gone
    }
  }

  @Test
  @DisplayName("query: generated SELECT + JSON params filter against H2")
  void executeQuery_generatedSelectWithParams() throws Exception {
    PipelineIrDocument doc = nativeQueryDoc("lookupApp", "DatasetQ");
    irService.save(doc);

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    PipelineExecuteRequest req = PipelineExecuteRequest.ofParams(Map.of("TYPE", "workflow"));

    PipelineExecuteResult result = runtime.execute("lookupApp", "DatasetQ", req);

    assertEquals("lookupApp", result.getAppName());
    assertEquals("DatasetQ", result.getResourceName());
    assertEquals(PipelineResourceIr.KIND_QUERY, result.getKind());
    assertEquals("query", result.getOperation());
    assertEquals(2, result.getRowCount());
    assertEquals(2, result.getRows().size());
    assertEquals("workflow", result.getRows().get(0).get("Type"));
    assertTrue(
        result.getRows().stream().allMatch(r -> "workflow".equals(String.valueOf(r.get("Type")))));

    String json = PSPipelineExecuteJsonCodec.toJson(result);
    assertTrue(json.contains("\"rowCount\""));
    assertTrue(json.contains("workflow"));
    PipelineExecuteResult roundTrip = PSPipelineExecuteJsonCodec.resultFromJson(json);
    assertEquals(result.getRowCount(), roundTrip.getRowCount());
  }

  @Test
  @DisplayName("query: load saved IR and execute end-to-end")
  void execute_fromSavedIr() throws Exception {
    PipelineIrDocument doc = nativeQueryDoc("savedApp", "R1");
    irService.save(doc);

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    PipelineExecuteResult result =
        runtime.execute("savedApp", "R1", PipelineExecuteRequest.empty());

    assertEquals(3, result.getRowCount());
    assertEquals(3, result.getRows().size());
  }

  @Test
  @DisplayName("query: nativeStatement parameterized escape hatch")
  void executeQuery_nativeStatement() throws Exception {
    PipelineIrDocument doc = new PipelineIrDocument();
    doc.setSource(PipelineIrDocument.SOURCE_NATIVE);
    doc.getApp().setName("nativeApp");
    PipelineResourceIr res = new PipelineResourceIr();
    res.setName("NativeQ");
    res.setKind(PipelineResourceIr.KIND_QUERY);
    PipelineStagesIr stages = new PipelineStagesIr();
    SelectorStageIr selector = new SelectorStageIr();
    selector.setPresent(true);
    selector.setMethod(SelectorStageIr.METHOD_NATIVE);
    selector.setNativeStatement(
        "SELECT \"TYPE\" AS \"Type\", \"NAME\" AS \"Name\" FROM \"PSX_ADMINLOOKUP\""
            + " WHERE \"TYPE\" = :type");
    stages.setSelector(selector);
    // still need tank/mapper empty ok for native path
    res.setStages(stages);
    doc.getResources().add(res);

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    PipelineExecuteResult result =
        runtime.execute(doc, res, PipelineExecuteRequest.ofParams(Map.of("type", "locale")));

    assertEquals(1, result.getRowCount());
    assertEquals("locale", result.getRows().get(0).get("Type"));
    assertEquals("en-us", result.getRows().get(0).get("Name"));
  }

  @Test
  @DisplayName("security: native multi-statement and non-SELECT rejected")
  void nativeSql_rejectsUnsafe() {
    assertThrows(
        PSPipelineIrException.class,
        () ->
            PSPipelineSqlPlanner.planQuery(
                nativeOnlyResource("SELECT 1; DROP TABLE X"), PipelineExecuteRequest.empty()));
    assertThrows(
        PSPipelineIrException.class,
        () ->
            PSPipelineSqlPlanner.planQuery(
                nativeOnlyResource("DELETE FROM PSX_ADMINLOOKUP"), PipelineExecuteRequest.empty()));
  }

  @Test
  @DisplayName("security: native keyword inside string literal is not rejected")
  void nativeSql_allowsKeywordInsideStringLiteral() throws Exception {
    PSPipelineSqlPlan plan =
        PSPipelineSqlPlanner.planQuery(
            nativeOnlyResource("SELECT * FROM t WHERE name = 'EXEC sp'"),
            PipelineExecuteRequest.empty());
    assertNotNull(plan);
    assertTrue(
        plan.getSql().contains("'EXEC sp'") || plan.getSql().toUpperCase().contains("SELECT"));
  }

  @Test
  @DisplayName("generated SELECT dedupes case-variant params onto one WHERE bind")
  void generatedSelect_dedupesCaseVariantParams() throws Exception {
    PipelineIrDocument doc = nativeQueryDoc("dedupeApp", "D1");
    PipelineResourceIr res = doc.findResource("D1");
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("TYPE", "workflow");
    params.put("type", "locale"); // same mapped column; first entry wins
    PSPipelineSqlPlan plan =
        PSPipelineSqlPlanner.planQuery(res, PipelineExecuteRequest.ofParams(params));
    String sql = plan.getSql();
    // Only one equality on TYPE column
    int idx = sql.indexOf("\"TYPE\" = ?");
    assertTrue(idx >= 0, sql);
    assertEquals(-1, sql.indexOf("\"TYPE\" = ?", idx + 1), sql);
    assertEquals(1, plan.getParameters().size());
    assertEquals("workflow", plan.getParameters().get(0));
  }

  @Test
  @DisplayName("where-clause IR: PARAM filter executes against H2 (classic import shape)")
  void executeQuery_whereClauseIrParam() throws Exception {
    PipelineIrDocument doc = nativeQueryDoc("whereIrApp", "W1");
    PipelineResourceIr res = doc.findResource("W1");
    SelectorStageIr selector = res.getStages().getSelector();
    WhereClauseIr clause = new WhereClauseIr();
    clause.setLeftKind(WhereClauseIr.KIND_COLUMN);
    clause.setLeft("PSX_ADMINLOOKUP.TYPE");
    clause.setOperator("=");
    clause.setRightKind(WhereClauseIr.KIND_PARAM);
    clause.setRight("sys_key");
    clause.setBooleanOp(WhereClauseIr.BOOL_AND);
    clause.setOmitWhenNull(false);
    selector.setWhereClauses(List.of(clause));
    irService.save(doc);

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    PipelineExecuteResult result =
        runtime.execute(
            "whereIrApp", "W1", PipelineExecuteRequest.ofParams(Map.of("sys_key", "locale")));

    assertEquals(1, result.getRowCount());
    assertEquals("locale", result.getRows().get(0).get("Type"));
  }

  @Test
  @DisplayName("where-clause IR: LIKE + LITERAL bind; omitWhenNull skips missing param")
  void planQuery_whereClauseIrLikeAndOmit() throws Exception {
    PipelineIrDocument doc = nativeQueryDoc("likeApp", "L1");
    PipelineResourceIr res = doc.findResource("L1");
    SelectorStageIr selector = res.getStages().getSelector();

    WhereClauseIr typeLike = new WhereClauseIr();
    typeLike.setLeftKind(WhereClauseIr.KIND_COLUMN);
    typeLike.setLeft("TYPE");
    typeLike.setOperator("LIKE");
    typeLike.setRightKind(WhereClauseIr.KIND_LITERAL);
    typeLike.setRight("work%");
    typeLike.setBooleanOp(WhereClauseIr.BOOL_AND);

    WhereClauseIr optionalName = new WhereClauseIr();
    optionalName.setLeftKind(WhereClauseIr.KIND_COLUMN);
    optionalName.setLeft("NAME");
    optionalName.setOperator("=");
    optionalName.setRightKind(WhereClauseIr.KIND_PARAM);
    optionalName.setRight("nameFilter");
    optionalName.setOmitWhenNull(true);
    optionalName.setBooleanOp(WhereClauseIr.BOOL_AND);

    selector.setWhereClauses(List.of(typeLike, optionalName));

    PSPipelineSqlPlan plan = PSPipelineSqlPlanner.planQuery(res, PipelineExecuteRequest.empty());
    String sql = plan.getSql();
    assertTrue(sql.contains("\"TYPE\" LIKE ?"), sql);
    assertFalse(sql.contains("\"NAME\" = ?"), "optional param must not appear in WHERE: " + sql);
    assertEquals(List.of("work%"), plan.getParameters());

    // With param present, both predicates appear
    PSPipelineSqlPlan plan2 =
        PSPipelineSqlPlanner.planQuery(
            res, PipelineExecuteRequest.ofParams(Map.of("nameFilter", "wf1")));
    assertTrue(plan2.getSql().contains("\"TYPE\" LIKE ?"), plan2.getSql());
    assertTrue(plan2.getSql().contains("\"NAME\" = ?"), plan2.getSql());
    assertTrue(plan2.getSql().contains(" AND "), plan2.getSql());
    assertEquals(List.of("work%", "wf1"), plan2.getParameters());
  }

  @Test
  @DisplayName("where-clause IR: OR connector and missing required PARAM rejected")
  void planQuery_whereClauseIrOrAndMissingParam() {
    PipelineIrDocument doc = nativeQueryDoc("orApp", "O1");
    PipelineResourceIr res = doc.findResource("O1");
    SelectorStageIr selector = res.getStages().getSelector();

    WhereClauseIr a = new WhereClauseIr();
    a.setLeftKind(WhereClauseIr.KIND_COLUMN);
    a.setLeft("TYPE");
    a.setOperator("=");
    a.setRightKind(WhereClauseIr.KIND_LITERAL);
    a.setRight("workflow");
    a.setBooleanOp(WhereClauseIr.BOOL_OR);

    WhereClauseIr b = new WhereClauseIr();
    b.setLeftKind(WhereClauseIr.KIND_COLUMN);
    b.setLeft("TYPE");
    b.setOperator("=");
    b.setRightKind(WhereClauseIr.KIND_PARAM);
    b.setRight("altType");
    b.setOmitWhenNull(false);

    selector.setWhereClauses(List.of(a, b));

    PSPipelineIrException missing =
        assertThrows(
            PSPipelineIrException.class,
            () -> PSPipelineSqlPlanner.planQuery(res, PipelineExecuteRequest.empty()));
    assertTrue(missing.getMessage().contains("altType"), missing.getMessage());
  }

  @Test
  @DisplayName("product limit: joinCount > 0 without join edges rejected")
  void planQuery_rejectsJoinCountWithoutEdges() {
    PipelineIrDocument doc = nativeQueryDoc("joinApp", "J1");
    PipelineResourceIr res = doc.findResource("J1");
    res.getStages().getBackendTank().setJoinCount(1);
    res.getStages().getBackendTank().setJoins(List.of());

    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class,
            () -> PSPipelineSqlPlanner.planQuery(res, PipelineExecuteRequest.empty()));
    assertTrue(
        ex.getMessage().contains(PSPipelineSqlPlanner.JOIN_PRODUCT_LIMIT_MESSAGE)
            || ex.getMessage().contains("backendTank.joins"),
        ex.getMessage());
    assertTrue(ex.getMessage().contains("J1"), ex.getMessage());
  }

  @Test
  @DisplayName("product limit: multi-table tank without join edges rejected")
  void planQuery_rejectsMultiTableWithoutJoin() {
    PipelineIrDocument doc = nativeQueryDoc("mtApp", "M1");
    PipelineResourceIr res = doc.findResource("M1");
    BackendTableRefIr t2 = new BackendTableRefIr();
    t2.setTable("OTHER");
    t2.setAlias("OTHER");
    List<BackendTableRefIr> tables = new ArrayList<>(res.getStages().getBackendTank().getTables());
    tables.add(t2);
    res.getStages().getBackendTank().setTables(tables);
    res.getStages().getBackendTank().setJoinCount(0);
    res.getStages().getBackendTank().setJoins(List.of());

    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class,
            () -> PSPipelineSqlPlanner.planQuery(res, PipelineExecuteRequest.empty()));
    assertTrue(
        ex.getMessage().contains(PSPipelineSqlPlanner.JOIN_PRODUCT_LIMIT_MESSAGE)
            || ex.getMessage().contains("joins"),
        ex.getMessage());
  }

  @Test
  @DisplayName("join-graph: INNER JOIN generates parameterized multi-table SELECT")
  void planQuery_innerJoinSql() throws Exception {
    PipelineIrDocument doc = nativeJoinQueryDoc("innerPlan", "IJ", BackendJoinIr.TYPE_INNER);
    PipelineResourceIr res = doc.findResource("IJ");

    PSPipelineSqlPlan plan =
        PSPipelineSqlPlanner.planQuery(
            res, PipelineExecuteRequest.ofParams(Map.of("TYPE", "workflow")));

    String sql = plan.getSql();
    assertTrue(sql.contains("INNER JOIN"), sql);
    assertTrue(sql.contains("\"PSX_ADMINLOOKUP\" \"L\""), sql);
    assertTrue(sql.contains("\"PSX_LOOKUP_EXTRA\" \"X\""), sql);
    assertTrue(sql.contains("ON \"L\".\"NAME\" = \"X\".\"LOOKUP_NAME\""), sql);
    assertTrue(sql.contains("\"L\".\"TYPE\" = ?") || sql.contains("\"TYPE\" = ?"), sql);
    assertEquals(List.of("workflow"), plan.getParameters());
    assertTrue(plan.getDescription().contains("join"), plan.getDescription());
  }

  @Test
  @DisplayName("join-graph: INNER JOIN executes against H2")
  void executeQuery_innerJoinH2() throws Exception {
    PipelineIrDocument doc = nativeJoinQueryDoc("innerH2", "IJH", BackendJoinIr.TYPE_INNER);
    irService.save(doc);

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    PipelineExecuteResult result =
        runtime.execute(
            "innerH2", "IJH", PipelineExecuteRequest.ofParams(Map.of("TYPE", "workflow")));

    assertEquals(2, result.getRowCount());
    assertTrue(
        result.getRows().stream().allMatch(r -> String.valueOf(r.get("Type")).equals("workflow")));
    assertTrue(
        result.getRows().stream()
            .map(r -> String.valueOf(r.get("Note")))
            .anyMatch(n -> n.startsWith("note-")));
  }

  @Test
  @DisplayName("join-graph: LEFT OUTER JOIN executes against H2 (orphan left row kept)")
  void executeQuery_leftOuterJoinH2() throws Exception {
    PipelineIrDocument doc = nativeJoinQueryDoc("leftH2", "LJH", BackendJoinIr.TYPE_LEFT);
    irService.save(doc);

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    // locale has no matching PSX_LOOKUP_EXTRA row — INNER would drop it; LEFT keeps it.
    PipelineExecuteResult result =
        runtime.execute("leftH2", "LJH", PipelineExecuteRequest.ofParams(Map.of("TYPE", "locale")));

    assertEquals(1, result.getRowCount());
    assertEquals("locale", result.getRows().get(0).get("Type"));
    assertEquals("en-us", result.getRows().get(0).get("Name"));
    assertNull(result.getRows().get(0).get("Note"));
  }

  @Test
  @DisplayName("join-graph: translator edges rejected")
  void planQuery_rejectsJoinTranslator() {
    PipelineIrDocument doc = nativeJoinQueryDoc("trApp", "TR", BackendJoinIr.TYPE_INNER);
    PipelineResourceIr res = doc.findResource("TR");
    res.getStages().getBackendTank().getJoins().get(0).setTranslatorPresent(true);

    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class,
            () -> PSPipelineSqlPlanner.planQuery(res, PipelineExecuteRequest.empty()));
    assertTrue(
        ex.getMessage().contains(PSPipelineSqlPlanner.JOIN_TRANSLATOR_LIMIT_MESSAGE)
            || ex.getMessage().contains("translator"),
        ex.getMessage());
  }

  @Test
  @DisplayName("where-clause IR executes LIKE against H2 end-to-end")
  void executeQuery_whereClauseIrLikeH2() throws Exception {
    PipelineIrDocument doc = nativeQueryDoc("likeH2", "LH");
    PipelineResourceIr res = doc.findResource("LH");
    WhereClauseIr clause = new WhereClauseIr();
    clause.setLeftKind(WhereClauseIr.KIND_COLUMN);
    clause.setLeft("NAME");
    clause.setOperator("LIKE");
    clause.setRightKind(WhereClauseIr.KIND_LITERAL);
    clause.setRight("wf%");
    res.getStages().getSelector().setWhereClauses(List.of(clause));
    irService.save(doc);

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    PipelineExecuteResult result = runtime.execute("likeH2", "LH", PipelineExecuteRequest.empty());

    assertEquals(2, result.getRowCount());
    assertTrue(
        result.getRows().stream().allMatch(r -> String.valueOf(r.get("Name")).startsWith("wf")));
  }

  @Test
  @DisplayName("execute rejects resource not owned by document")
  void execute_rejectsForeignResource() {
    PipelineIrDocument doc = nativeQueryDoc("ownApp", "R1");
    PipelineIrDocument other = nativeQueryDoc("otherApp", "R1");
    PipelineResourceIr foreign = other.findResource("R1");
    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    assertThrows(
        PSPipelineIrException.class,
        () -> runtime.execute(doc, foreign, PipelineExecuteRequest.empty()));
  }

  @Test
  @DisplayName("pre and post Java hooks fire on execute path")
  void hooks_preAndPostInvoked() throws Exception {
    PipelineIrDocument doc = nativeQueryDoc("hookApp", "HQ");
    AtomicInteger pre = new AtomicInteger();
    AtomicInteger post = new AtomicInteger();

    IPSPipelinePreExecuteHook preHook =
        ctx -> {
          pre.incrementAndGet();
          ctx.addTrace("pre:test");
          // inject filter via mutable request
          ctx.getRequest().getParams().put("TYPE", "locale");
        };
    IPSPipelinePostExecuteHook postHook =
        (ctx, result) -> {
          post.incrementAndGet();
          ctx.addTrace("post:test");
          result.getMeta().put("hooked", true);
        };

    IPSPipelineRuntimeService runtime =
        new PSPipelineRuntimeService(irService, sqlAdapter, List.of(preHook), List.of(postHook));

    PipelineExecuteResult result =
        runtime.execute(doc, doc.findResource("HQ"), PipelineExecuteRequest.empty());

    assertEquals(1, pre.get());
    assertEquals(1, post.get());
    assertEquals(1, result.getRowCount());
    assertEquals("locale", result.getRows().get(0).get("Type"));
    assertTrue(result.getHookTrace().contains("pre:test"));
    assertTrue(result.getHookTrace().contains("post:test"));
    assertEquals(true, result.getMeta().get("hooked"));
  }

  @Test
  @DisplayName("update: minimal INSERT via SQL adapter")
  void executeInsert_minimal() throws Exception {
    PipelineIrDocument doc = nativeUpdateDoc("updApp", "Ins");
    irService.save(doc);

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("TYPE", "community");
    row.put("NAME", "c1");
    row.put("LOOKUPVALUE", "99");
    PipelineExecuteRequest req = new PipelineExecuteRequest();
    req.setRows(List.of(row));

    PipelineExecuteResult result = runtime.execute("updApp", "Ins", req);
    assertEquals("insert", result.getOperation());
    assertEquals(1, result.getAffectedRows());

    // verify via query resource
    PipelineIrDocument qdoc = nativeQueryDoc("q2", "Q2");
    PipelineExecuteResult q =
        runtime.execute(
            qdoc,
            qdoc.findResource("Q2"),
            PipelineExecuteRequest.ofParams(Map.of("TYPE", "community")));
    assertEquals(1, q.getRowCount());
    assertEquals("c1", q.getRows().get(0).get("Name"));
  }

  @Test
  @DisplayName("update: UPDATE when allowUpdate; SET non-keys WHERE keyColumns")
  void executeUpdate_whenAllowed() throws Exception {
    PipelineIrDocument doc = nativeUpdateDocFlags("updAll", "Upd", true, true, false);
    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);

    Map<String, Object> row = new LinkedHashMap<>();
    row.put("TYPE", "workflow");
    row.put("NAME", "wf1");
    row.put("LOOKUPVALUE", "42");
    PipelineExecuteRequest req = new PipelineExecuteRequest();
    req.setOperation(PipelineExecuteRequest.OP_UPDATE);
    req.setKeyColumns(List.of("TYPE", "NAME"));
    req.setRows(List.of(row));

    PipelineExecuteResult result = runtime.execute(doc, doc.findResource("Upd"), req);
    assertEquals("update", result.getOperation());
    assertEquals(1, result.getAffectedRows());

    PipelineIrDocument qdoc = nativeQueryDoc("qUpd", "Q");
    PipelineExecuteResult q =
        runtime.execute(
            qdoc,
            qdoc.findResource("Q"),
            PipelineExecuteRequest.ofParams(Map.of("TYPE", "workflow", "NAME", "wf1")));
    assertEquals(1, q.getRowCount());
    assertEquals("42", String.valueOf(q.getRows().get(0).get("Value")));
  }

  @Test
  @DisplayName("update: DELETE when allowDelete")
  void executeDelete_whenAllowed() throws Exception {
    PipelineIrDocument doc = nativeUpdateDocFlags("delApp", "Del", false, false, true);
    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);

    Map<String, Object> row = new LinkedHashMap<>();
    row.put("TYPE", "locale");
    row.put("NAME", "en-us");
    PipelineExecuteRequest req = new PipelineExecuteRequest();
    req.setKeyColumns(List.of("TYPE", "NAME"));
    req.setRows(List.of(row));

    PipelineExecuteResult result = runtime.execute(doc, doc.findResource("Del"), req);
    assertEquals("delete", result.getOperation());
    assertEquals(1, result.getAffectedRows());

    PipelineIrDocument qdoc = nativeQueryDoc("qDel", "Q");
    PipelineExecuteResult q =
        runtime.execute(
            qdoc,
            qdoc.findResource("Q"),
            PipelineExecuteRequest.ofParams(Map.of("TYPE", "locale")));
    assertEquals(0, q.getRowCount());
  }

  @Test
  @DisplayName("update: reject UPDATE when allowUpdate=false with clear API error")
  void executeUpdate_rejectedWhenNotAllowed() {
    PipelineIrDocument doc = nativeUpdateDoc("noUpd", "InsOnly");
    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);

    PipelineExecuteRequest req = new PipelineExecuteRequest();
    req.setOperation(PipelineExecuteRequest.OP_UPDATE);
    req.setKeyColumns(List.of("TYPE", "NAME"));
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("TYPE", "workflow");
    row.put("NAME", "wf1");
    row.put("LOOKUPVALUE", "x");
    req.setRows(List.of(row));

    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class,
            () -> runtime.execute(doc, doc.findResource("InsOnly"), req));
    assertTrue(
        ex.getMessage().contains("does not allow update"),
        () -> "message should mention allowUpdate: " + ex.getMessage());
  }

  @Test
  @DisplayName("update: reject DELETE when allowDelete=false with clear API error")
  void executeDelete_rejectedWhenNotAllowed() {
    PipelineIrDocument doc = nativeUpdateDoc("noDel", "InsOnly");
    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);

    PipelineExecuteRequest req = new PipelineExecuteRequest();
    req.setOperation(PipelineExecuteRequest.OP_DELETE);
    req.setKeyColumns(List.of("TYPE", "NAME"));
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("TYPE", "workflow");
    row.put("NAME", "wf1");
    req.setRows(List.of(row));

    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class,
            () -> runtime.execute(doc, doc.findResource("InsOnly"), req));
    assertTrue(
        ex.getMessage().contains("does not allow delete"),
        () -> "message should mention allowDelete: " + ex.getMessage());
  }

  @Test
  @DisplayName("update: multi-flag resource requires request.operation")
  void execute_multiFlagRequiresOperation() {
    PipelineIrDocument doc = nativeUpdateDocFlags("multi", "M", true, true, true);
    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("TYPE", "x");
    row.put("NAME", "y");
    row.put("LOOKUPVALUE", "z");
    PipelineExecuteRequest req = new PipelineExecuteRequest();
    req.setRows(List.of(row));

    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class, () -> runtime.execute(doc, doc.findResource("M"), req));
    assertTrue(
        ex.getMessage().contains("request.operation is required"),
        () -> "message: " + ex.getMessage());
  }

  @Test
  @DisplayName("multi-row transactionMode=all commits all plans together")
  void multiRow_transactionAll_commits() throws Exception {
    PipelineIrDocument doc = nativeUpdateDocFlags("txAll", "Ins", true, false, false);
    doc.findResource("Ins").setTransactionMode("all");
    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);

    Map<String, Object> r1 = new LinkedHashMap<>();
    r1.put("TYPE", "tx");
    r1.put("NAME", "a");
    r1.put("LOOKUPVALUE", "1");
    Map<String, Object> r2 = new LinkedHashMap<>();
    r2.put("TYPE", "tx");
    r2.put("NAME", "b");
    r2.put("LOOKUPVALUE", "2");
    PipelineExecuteRequest req = new PipelineExecuteRequest();
    req.setRows(List.of(r1, r2));

    PipelineExecuteResult result = runtime.execute(doc, doc.findResource("Ins"), req);
    assertEquals(2, result.getAffectedRows());
    assertEquals("all", result.getMeta().get("transactionMode"));

    PipelineIrDocument qdoc = nativeQueryDoc("qTx", "Q");
    PipelineExecuteResult q =
        runtime.execute(
            qdoc, qdoc.findResource("Q"), PipelineExecuteRequest.ofParams(Map.of("TYPE", "tx")));
    assertEquals(2, q.getRowCount());
  }

  @Test
  @DisplayName("multi-row transactionMode=all rolls back entire batch on failure")
  void multiRow_transactionAll_rollsBackOnFailure() throws Exception {
    // Seed one row; second insert in batch targets a not-null constrained table via dual plans:
    // use DELETE of non-existent is fine; force failure with a plan against missing column via
    // adapter updateAll directly after a successful first insert under same TX would need bad SQL.
    // Instead: use two inserts then a third plan that fails — execute via adapter with mixed plans.
    try (Connection c = DriverManager.getConnection(jdbcUrl);
        Statement st = c.createStatement()) {
      st.execute(
          "CREATE TABLE \"TX_STRICT\" (\"ID\" VARCHAR(32) PRIMARY KEY, \"VAL\" VARCHAR(32) NOT"
              + " NULL)");
    }

    List<Object> okBinds = new ArrayList<>();
    okBinds.add("k1");
    okBinds.add("v1");
    List<Object> failBinds = new ArrayList<>();
    failBinds.add("k2");
    failBinds.add(null);
    List<PSPipelineSqlPlan> plans =
        List.of(
            new PSPipelineSqlPlan(
                PSPipelineSqlPlan.Kind.UPDATE,
                "INSERT INTO \"TX_STRICT\" (\"ID\", \"VAL\") VALUES (?, ?)",
                okBinds,
                "ok1"),
            new PSPipelineSqlPlan(
                PSPipelineSqlPlan.Kind.UPDATE,
                "INSERT INTO \"TX_STRICT\" (\"ID\", \"VAL\") VALUES (?, ?)",
                failBinds,
                "fail-null"));

    PSPipelineIrException ex =
        assertThrows(PSPipelineIrException.class, () -> sqlAdapter.updateAll(plans, "all"));
    assertTrue(
        ex.getMessage().toLowerCase().contains("rolled back")
            || ex.getMessage().toLowerCase().contains("batch failed"),
        () -> "expected rollback message: " + ex.getMessage());

    try (Connection c = DriverManager.getConnection(jdbcUrl);
        Statement st = c.createStatement();
        var rs = st.executeQuery("SELECT COUNT(*) FROM \"TX_STRICT\"")) {
      assertTrue(rs.next());
      assertEquals(0, rs.getInt(1), "first insert must roll back with the failed batch");
    }
  }

  @Test
  @DisplayName("multi-row transactionMode=row commits successful plans before a failure")
  void multiRow_transactionRow_commitsPriorPlans() throws Exception {
    try (Connection c = DriverManager.getConnection(jdbcUrl);
        Statement st = c.createStatement()) {
      st.execute(
          "CREATE TABLE \"TX_ROW\" (\"ID\" VARCHAR(32) PRIMARY KEY, \"VAL\" VARCHAR(32) NOT NULL)");
    }

    List<Object> okBinds = new ArrayList<>();
    okBinds.add("r1");
    okBinds.add("ok");
    List<Object> failBinds = new ArrayList<>();
    failBinds.add("r2");
    failBinds.add(null);
    List<PSPipelineSqlPlan> plans =
        List.of(
            new PSPipelineSqlPlan(
                PSPipelineSqlPlan.Kind.UPDATE,
                "INSERT INTO \"TX_ROW\" (\"ID\", \"VAL\") VALUES (?, ?)",
                okBinds,
                "ok1"),
            new PSPipelineSqlPlan(
                PSPipelineSqlPlan.Kind.UPDATE,
                "INSERT INTO \"TX_ROW\" (\"ID\", \"VAL\") VALUES (?, ?)",
                failBinds,
                "fail-null"));

    assertThrows(PSPipelineIrException.class, () -> sqlAdapter.updateAll(plans, "row"));

    try (Connection c = DriverManager.getConnection(jdbcUrl);
        Statement st = c.createStatement();
        var rs = st.executeQuery("SELECT COUNT(*) FROM \"TX_ROW\"")) {
      assertTrue(rs.next());
      assertEquals(1, rs.getInt(1), "row mode keeps prior committed plans");
    }
  }

  @Test
  @DisplayName("JSON request codec parses params")
  void requestJsonCodec() throws Exception {
    PipelineExecuteRequest req =
        PSPipelineExecuteJsonCodec.requestFromJson("{\"params\":{\"TYPE\":\"workflow\"}}");
    assertEquals("workflow", req.getParams().get("TYPE"));
  }

  @Test
  @DisplayName("JSON request codec parses operation and keyColumns")
  void requestJsonCodec_operationAndKeys() throws Exception {
    PipelineExecuteRequest req =
        PSPipelineExecuteJsonCodec.requestFromJson(
            "{\"operation\":\"update\",\"keyColumns\":[\"TYPE\",\"NAME\"],"
                + "\"rows\":[{\"TYPE\":\"workflow\",\"NAME\":\"wf1\",\"LOOKUPVALUE\":\"1\"}]}");
    assertEquals("update", req.getOperation());
    assertEquals(List.of("TYPE", "NAME"), req.getKeyColumns());
    assertEquals(1, req.getRows().size());
  }

  @Test
  @DisplayName("missing app / resource errors")
  void execute_missing() {
    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    assertThrows(
        PSPipelineIrException.class,
        () -> runtime.execute("nope", "r", PipelineExecuteRequest.empty()));
  }

  private static PipelineResourceIr nativeOnlyResource(String sql) {
    PipelineResourceIr res = new PipelineResourceIr();
    res.setName("N");
    res.setKind(PipelineResourceIr.KIND_QUERY);
    SelectorStageIr selector = new SelectorStageIr();
    selector.setPresent(true);
    selector.setMethod(SelectorStageIr.METHOD_NATIVE);
    selector.setNativeStatement(sql);
    PipelineStagesIr stages = new PipelineStagesIr();
    stages.setSelector(selector);
    res.setStages(stages);
    return res;
  }

  private static PipelineIrDocument nativeQueryDoc(String appName, String resourceName) {
    PipelineIrDocument doc = new PipelineIrDocument();
    doc.setSource(PipelineIrDocument.SOURCE_NATIVE);
    doc.getApp().setName(appName);

    PipelineResourceIr res = new PipelineResourceIr();
    res.setName(resourceName);
    res.setKind(PipelineResourceIr.KIND_QUERY);

    PipelineStagesIr stages = new PipelineStagesIr();

    BackendTankStageIr tank = new BackendTankStageIr();
    tank.setPresent(true);
    BackendTableRefIr table = new BackendTableRefIr();
    table.setTable("PSX_ADMINLOOKUP");
    table.setAlias("PSX_ADMINLOOKUP");
    tank.setTables(List.of(table));
    stages.setBackendTank(tank);

    MapperStageIr mapper = new MapperStageIr();
    mapper.setPresent(true);
    mapper.setMappings(
        List.of(
            mapping("Properties/@Type", "PSX_ADMINLOOKUP.TYPE"),
            mapping("Properties/@Name", "PSX_ADMINLOOKUP.NAME"),
            mapping("Properties/@Value", "PSX_ADMINLOOKUP.LOOKUPVALUE")));
    stages.setMapper(mapper);

    SelectorStageIr selector = new SelectorStageIr();
    selector.setPresent(true);
    selector.setMethod(SelectorStageIr.METHOD_WHERE);
    stages.setSelector(selector);

    res.setStages(stages);
    doc.getResources().add(res);
    return doc;
  }

  /**
   * Two-table QUERY IR: {@code L} = PSX_ADMINLOOKUP, {@code X} = PSX_LOOKUP_EXTRA, join on L.NAME =
   * X.LOOKUP_NAME.
   */
  private static PipelineIrDocument nativeJoinQueryDoc(
      String appName, String resourceName, String joinType) {
    PipelineIrDocument doc = new PipelineIrDocument();
    doc.setSource(PipelineIrDocument.SOURCE_NATIVE);
    doc.getApp().setName(appName);

    PipelineResourceIr res = new PipelineResourceIr();
    res.setName(resourceName);
    res.setKind(PipelineResourceIr.KIND_QUERY);

    PipelineStagesIr stages = new PipelineStagesIr();

    BackendTankStageIr tank = new BackendTankStageIr();
    tank.setPresent(true);
    BackendTableRefIr left = new BackendTableRefIr();
    left.setTable("PSX_ADMINLOOKUP");
    left.setAlias("L");
    BackendTableRefIr right = new BackendTableRefIr();
    right.setTable("PSX_LOOKUP_EXTRA");
    right.setAlias("X");
    tank.setTables(List.of(left, right));

    BackendJoinIr join = new BackendJoinIr();
    join.setJoinType(joinType);
    join.setLeft("L.NAME");
    join.setRight("X.LOOKUP_NAME");
    tank.setJoins(List.of(join));
    tank.setJoinCount(1);
    stages.setBackendTank(tank);

    MapperStageIr mapper = new MapperStageIr();
    mapper.setPresent(true);
    mapper.setMappings(
        List.of(
            mapping("Properties/@Type", "L.TYPE"),
            mapping("Properties/@Name", "L.NAME"),
            mapping("Properties/@Note", "X.NOTE")));
    stages.setMapper(mapper);

    SelectorStageIr selector = new SelectorStageIr();
    selector.setPresent(true);
    selector.setMethod(SelectorStageIr.METHOD_WHERE);
    stages.setSelector(selector);

    res.setStages(stages);
    doc.getResources().add(res);
    return doc;
  }

  private static PipelineIrDocument nativeUpdateDoc(String appName, String resourceName) {
    return nativeUpdateDocFlags(appName, resourceName, true, false, false);
  }

  private static PipelineIrDocument nativeUpdateDocFlags(
      String appName,
      String resourceName,
      boolean allowInsert,
      boolean allowUpdate,
      boolean allowDelete) {
    PipelineIrDocument doc = new PipelineIrDocument();
    doc.setSource(PipelineIrDocument.SOURCE_NATIVE);
    doc.getApp().setName(appName);

    PipelineResourceIr res = new PipelineResourceIr();
    res.setName(resourceName);
    res.setKind(PipelineResourceIr.KIND_UPDATE);

    PipelineStagesIr stages = new PipelineStagesIr();

    BackendTankStageIr tank = new BackendTankStageIr();
    tank.setPresent(true);
    BackendTableRefIr table = new BackendTableRefIr();
    table.setTable("PSX_ADMINLOOKUP");
    tank.setTables(List.of(table));
    stages.setBackendTank(tank);

    MapperStageIr mapper = new MapperStageIr();
    mapper.setPresent(true);
    mapper.setMappings(
        List.of(
            mapping("Properties/@Type", "PSX_ADMINLOOKUP.TYPE"),
            mapping("Properties/@Name", "PSX_ADMINLOOKUP.NAME"),
            mapping("Properties/@Value", "PSX_ADMINLOOKUP.LOOKUPVALUE")));
    stages.setMapper(mapper);

    UpdaterStageIr updater = new UpdaterStageIr();
    updater.setPresent(true);
    updater.setAllowInsert(allowInsert);
    updater.setAllowUpdate(allowUpdate);
    updater.setAllowDelete(allowDelete);
    stages.setUpdater(updater);

    res.setStages(stages);
    doc.getResources().add(res);
    return doc;
  }

  private static MappingEntryIr mapping(String docField, String backend) {
    MappingEntryIr m = new MappingEntryIr();
    m.setDocumentField(docField);
    m.setBackend(backend);
    m.setBackendKind(MappingEntryIr.BACKEND_KIND_COLUMN);
    return m;
  }
}
