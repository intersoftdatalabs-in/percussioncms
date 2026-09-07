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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.pipeline.http.PSPipelineHttpAdapter;
import com.percussion.services.pipeline.http.PSPipelineHttpUrl;
import com.percussion.services.pipeline.model.BackendTankStageIr;
import com.percussion.services.pipeline.model.FilterGroupIr;
import com.percussion.services.pipeline.model.MapperStageIr;
import com.percussion.services.pipeline.model.MappingEntryIr;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.model.PipelineStagesIr;
import com.percussion.services.pipeline.model.SelectorStageIr;
import com.percussion.services.pipeline.sql.IPSPipelineSqlAdapter;
import com.percussion.services.pipeline.sql.PSPipelineSqlPlan;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Slice C HTTP datasource: loopback fetch, bundled fixture, URL guard, persist round-trip. */
@DisplayName("Pipeline HTTP adapter (Slice C)")
class PSPipelineHttpAdapterTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("URL guard: loopback ok; cloud, userinfo, file rejected")
  void requireSafe_rejectsCloudCredentialsAndFile() throws Exception {
    assertTrue(
        PSPipelineHttpUrl.isBundledFixture(
            PSPipelineHttpUrl.requireSafe(PSPipelineHttpUrl.BUNDLED_FIXTURE_URL)));

    PSPipelineIrException cloud =
        assertThrows(
            PSPipelineIrException.class,
            () -> PSPipelineHttpUrl.requireSafe("https://erp.example/api/items"));
    assertTrue(cloud.getMessage().toLowerCase().contains("loopback"), cloud.getMessage());

    PSPipelineIrException userinfo =
        assertThrows(
            PSPipelineIrException.class,
            () -> PSPipelineHttpUrl.requireSafe("http://user:secret@127.0.0.1/items"));
    assertTrue(userinfo.getMessage().toLowerCase().contains("userinfo"), userinfo.getMessage());

    assertThrows(
        PSPipelineIrException.class, () -> PSPipelineHttpUrl.requireSafe("file:///tmp/items.json"));
    assertThrows(PSPipelineIrException.class, () -> PSPipelineHttpUrl.requireSafe(""));
  }

  @Test
  @DisplayName("bundled fixture execute maps sku/name rows")
  void execute_bundledFixture_mapsRows() throws Exception {
    IPSPipelineIrService ir = new PSPipelineIrService(tempDir);
    PipelineIrDocument doc = httpDoc("httpApp", "items", PSPipelineHttpUrl.BUNDLED_FIXTURE_URL);
    ir.save(doc);

    IPSPipelineRuntimeService runtime =
        new PSPipelineRuntimeService(ir, throwingSql());
    PipelineExecuteResult result =
        runtime.execute("httpApp", "items", PipelineExecuteRequest.empty());

    assertEquals("http-query", result.getOperation());
    assertEquals(2, result.getRowCount());
    assertEquals("SKU-1", result.getRows().get(0).get("sku"));
    assertEquals("Loopback Widget", result.getRows().get(0).get("name"));
    assertFalse(result.getRows().isEmpty());
  }

  @Test
  @DisplayName("nested AND/OR filter group keeps matching bundled fixture rows")
  void execute_bundledFixture_nestedFilterGroup() throws Exception {
    IPSPipelineIrService ir = new PSPipelineIrService(tempDir);
    PipelineIrDocument doc = httpDoc("httpFilterApp", "items", PSPipelineHttpUrl.BUNDLED_FIXTURE_URL);
    MappingEntryIr qty = new MappingEntryIr();
    qty.setDocumentField("qty");
    qty.setBackend("qty");
    MapperStageIr mapper = doc.findResource("items").getStages().getMapper();
    List<MappingEntryIr> mappings = new ArrayList<>(mapper.getMappings());
    mappings.add(qty);
    mapper.setMappings(mappings);

    FilterGroupIr sku = predicate("sku", "=", "SKU-1");
    FilterGroupIr nested = new FilterGroupIr();
    nested.setType(FilterGroupIr.TYPE_GROUP);
    nested.setOp(FilterGroupIr.OP_OR);
    nested.setChildren(List.of(predicate("qty", "=", "3"), predicate("qty", "=", "99")));
    FilterGroupIr root = new FilterGroupIr();
    root.setType(FilterGroupIr.TYPE_GROUP);
    root.setOp(FilterGroupIr.OP_AND);
    root.setChildren(List.of(sku, nested));
    SelectorStageIr selector = new SelectorStageIr();
    selector.setPresent(true);
    selector.setMethod(SelectorStageIr.METHOD_WHERE);
    selector.setFilterGroup(root);
    doc.findResource("items").getStages().setSelector(selector);
    ir.save(doc);

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(ir, throwingSql());
    PipelineExecuteResult result =
        runtime.execute("httpFilterApp", "items", PipelineExecuteRequest.empty());
    assertEquals(1, result.getRowCount());
    assertEquals("SKU-1", result.getRows().get(0).get("sku"));
    assertFalse(result.getRows().stream().anyMatch(r -> "SKU-2".equals(r.get("sku"))));
  }

  @Test
  @DisplayName("malformed nested filter group is rejected on execute")
  void execute_malformedFilterGroup() throws Exception {
    IPSPipelineIrService ir = new PSPipelineIrService(tempDir);
    PipelineIrDocument doc = httpDoc("badFilterApp", "items", PSPipelineHttpUrl.BUNDLED_FIXTURE_URL);
    FilterGroupIr root = new FilterGroupIr();
    root.setType(FilterGroupIr.TYPE_GROUP);
    root.setOp(FilterGroupIr.OP_AND);
    SelectorStageIr selector = new SelectorStageIr();
    selector.setPresent(true);
    selector.setFilterGroup(root);
    doc.findResource("items").getStages().setSelector(selector);
    ir.save(doc);
    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(ir, throwingSql());
    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class,
            () -> runtime.execute("badFilterApp", "items", PipelineExecuteRequest.empty()));
    assertTrue(ex.getMessage().toLowerCase().contains("child"), ex.getMessage());
  }

  @Test
  @DisplayName("loopback HttpServer JSON array is fetched and mapped")
  void execute_loopbackHttpServer() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(Executors.newCachedThreadPool());
    byte[] body =
        "[{\"sku\":\"LIVE-1\",\"name\":\"Live Item\"}]".getBytes(StandardCharsets.UTF_8);
    server.createContext(
        "/items.json",
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
          }
        });
    server.start();
    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/items.json";
      IPSPipelineIrService ir = new PSPipelineIrService(tempDir);
      PipelineIrDocument doc = httpDoc("liveApp", "liveItems", url);
      ir.save(doc);

      IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(ir, throwingSql());
      PipelineExecuteResult result =
          runtime.execute("liveApp", "liveItems", PipelineExecuteRequest.empty());
      assertEquals(1, result.getRowCount());
      assertEquals("LIVE-1", result.getRows().get(0).get("sku"));
      assertEquals("Live Item", result.getRows().get(0).get("name"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  @DisplayName("open redirect from loopback is refused")
  void execute_refusesRedirect() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(Executors.newCachedThreadPool());
    server.createContext(
        "/redir",
        exchange -> {
          exchange.getResponseHeaders().add("Location", "http://example.com/evil.json");
          exchange.sendResponseHeaders(302, -1);
          exchange.close();
        });
    server.start();
    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/redir";
      PipelineIrDocument doc = httpDoc("redirApp", "r", url);
      IPSPipelineRuntimeService runtime =
          new PSPipelineRuntimeService(new PSPipelineIrService(tempDir), throwingSql());
      PSPipelineIrException ex =
          assertThrows(
              PSPipelineIrException.class,
              () -> runtime.execute(doc, doc.findResource("r"), PipelineExecuteRequest.empty()));
      assertTrue(ex.getMessage().toLowerCase().contains("redirect"), ex.getMessage());
    } finally {
      server.stop(0);
    }
  }

  @Test
  @DisplayName("IR JSON round-trip preserves HTTP adapterType and url")
  void irJson_roundTripHttpTank() throws Exception {
    IPSPipelineIrService ir = new PSPipelineIrService(tempDir);
    PipelineIrDocument doc = httpDoc("persistApp", "items", PSPipelineHttpUrl.BUNDLED_FIXTURE_URL);
    ir.save(doc);
    PipelineIrDocument loaded = ir.load("persistApp").orElseThrow();
    BackendTankStageIr tank = loaded.findResource("items").getStages().getBackendTank();
    assertEquals(BackendTankStageIr.ADAPTER_HTTP, tank.getAdapterType());
    assertEquals(PSPipelineHttpUrl.BUNDLED_FIXTURE_URL, tank.getUrl());
    assertTrue(tank.isHttpAdapter());
  }

  @Test
  @DisplayName("classic mapper with unmatched columns falls back to HTTP JSON fields")
  void mapRows_fallsBackWhenClassicMappingsMiss() throws Exception {
    PipelineIrDocument doc = httpDoc("mapApp", "items", PSPipelineHttpUrl.BUNDLED_FIXTURE_URL);
    MappingEntryIr classic = new MappingEntryIr();
    classic.setDocumentField("link/@url");
    classic.setBackend("PSX_MISSING.URL");
    doc.findResource("items").getStages().getMapper().setMappings(List.of(classic));
    IPSPipelineRuntimeService runtime =
        new PSPipelineRuntimeService(new PSPipelineIrService(tempDir), throwingSql());
    PipelineExecuteResult result =
        runtime.execute(doc, doc.findResource("items"), PipelineExecuteRequest.empty());
    assertEquals("SKU-1", result.getRows().get(0).get("sku"));
    assertEquals("Loopback Widget", result.getRows().get(0).get("name"));
  }

  @Test
  @DisplayName("parseRows: object without rows becomes a document")
  void parseRows_singleObject() throws Exception {
    List<Map<String, Object>> rows = PSPipelineHttpAdapter.parseRows("{\"sku\":\"X\",\"name\":\"Y\"}");
    assertEquals(1, rows.size());
    assertEquals("X", rows.get(0).get("sku"));
  }

  private static FilterGroupIr predicate(String left, String operator, String right) {
    FilterGroupIr p = new FilterGroupIr();
    p.setType(FilterGroupIr.TYPE_PREDICATE);
    p.setLeftKind("COLUMN");
    p.setLeft(left);
    p.setOperator(operator);
    p.setRightKind("LITERAL");
    p.setRight(right);
    return p;
  }

  private static PipelineIrDocument httpDoc(String app, String resource, String url) {
    PipelineIrDocument doc = new PipelineIrDocument();
    doc.setSource(PipelineIrDocument.SOURCE_NATIVE);
    doc.getApp().setName(app);
    PipelineResourceIr res = new PipelineResourceIr();
    res.setName(resource);
    res.setKind(PipelineResourceIr.KIND_QUERY);
    PipelineStagesIr stages = new PipelineStagesIr();
    BackendTankStageIr tank = new BackendTankStageIr();
    tank.setPresent(true);
    tank.setAdapterType(BackendTankStageIr.ADAPTER_HTTP);
    tank.setUrl(url);
    tank.setHttpMethod("GET");
    stages.setBackendTank(tank);
    MapperStageIr mapper = new MapperStageIr();
    mapper.setPresent(true);
    MappingEntryIr sku = new MappingEntryIr();
    sku.setDocumentField("sku");
    sku.setBackend("sku");
    MappingEntryIr name = new MappingEntryIr();
    name.setDocumentField("name");
    name.setBackend("name");
    mapper.setMappings(List.of(sku, name));
    stages.setMapper(mapper);
    res.setStages(stages);
    doc.getResources().add(res);
    return doc;
  }

  private static IPSPipelineSqlAdapter throwingSql() {
    return new IPSPipelineSqlAdapter() {
      @Override
      public List<Map<String, Object>> query(PSPipelineSqlPlan plan) {
        throw new AssertionError("SQL adapter must not run for HTTP resources");
      }

      @Override
      public int update(PSPipelineSqlPlan plan) {
        throw new AssertionError("SQL adapter must not run for HTTP resources");
      }

      @Override
      public int updateAll(List<PSPipelineSqlPlan> plans, String transactionMode) {
        throw new AssertionError("SQL adapter must not run for HTTP resources");
      }
    };
  }
}
