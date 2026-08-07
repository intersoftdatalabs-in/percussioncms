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
import com.percussion.services.pipeline.sql.PSJdbcPipelineSqlAdapter;
import com.percussion.services.pipeline.sql.PSPipelineSqlPlan;
import com.percussion.services.pipeline.sql.PSPipelineSqlPlanner;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
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
    PipelineExecuteRequest req =
        PipelineExecuteRequest.ofParams(Map.of("TYPE", "workflow"));

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
        runtime.execute(
            doc, res, PipelineExecuteRequest.ofParams(Map.of("type", "locale")));

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
                nativeOnlyResource("SELECT 1; DROP TABLE X"),
                PipelineExecuteRequest.empty()));
    assertThrows(
        PSPipelineIrException.class,
        () ->
            PSPipelineSqlPlanner.planQuery(
                nativeOnlyResource("DELETE FROM PSX_ADMINLOOKUP"),
                PipelineExecuteRequest.empty()));
  }

  @Test
  @DisplayName("security: native keyword inside string literal is not rejected")
  void nativeSql_allowsKeywordInsideStringLiteral() throws Exception {
    PSPipelineSqlPlan plan =
        PSPipelineSqlPlanner.planQuery(
            nativeOnlyResource("SELECT * FROM t WHERE name = 'EXEC sp'"),
            PipelineExecuteRequest.empty());
    assertNotNull(plan);
    assertTrue(plan.getSql().contains("'EXEC sp'") || plan.getSql().toUpperCase().contains("SELECT"));
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
        new PSPipelineRuntimeService(
            irService, sqlAdapter, List.of(preHook), List.of(postHook));

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
  @DisplayName("JSON request codec parses params")
  void requestJsonCodec() throws Exception {
    PipelineExecuteRequest req =
        PSPipelineExecuteJsonCodec.requestFromJson("{\"params\":{\"TYPE\":\"workflow\"}}");
    assertEquals("workflow", req.getParams().get("TYPE"));
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

  private static PipelineIrDocument nativeUpdateDoc(String appName, String resourceName) {
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
    updater.setAllowInsert(true);
    updater.setAllowUpdate(false);
    updater.setAllowDelete(false);
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
