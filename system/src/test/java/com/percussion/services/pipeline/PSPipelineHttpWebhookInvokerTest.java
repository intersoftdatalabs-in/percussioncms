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

import com.percussion.services.pipeline.http.PSPipelineHttpUrl;
import com.percussion.services.pipeline.model.BackendTankStageIr;
import com.percussion.services.pipeline.model.MapperStageIr;
import com.percussion.services.pipeline.model.MappingEntryIr;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.model.PipelineStagesIr;
import com.percussion.services.pipeline.model.PipelineWebhookHooksIr;
import com.percussion.services.pipeline.sql.IPSPipelineSqlAdapter;
import com.percussion.services.pipeline.sql.PSPipelineSqlPlan;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Slice C HTTP webhook hooks: bundled fixture, loopback POST, skip missing URL, SSRF. */
@DisplayName("Pipeline HTTP webhook hooks (Slice C)")
class PSPipelineHttpWebhookInvokerTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("bundled webhook fixture: Test invoke shows real status/body")
  void execute_bundledWebhookFixture_recordsStatusAndBody() throws Exception {
    IPSPipelineIrService ir = new PSPipelineIrService(tempDir);
    PipelineIrDocument doc =
        httpDocWithHooks(
            "hookApp",
            "items",
            PSPipelineHttpUrl.BUNDLED_FIXTURE_URL,
            PSPipelineHttpUrl.BUNDLED_WEBHOOK_FIXTURE_URL,
            PSPipelineHttpUrl.BUNDLED_WEBHOOK_FIXTURE_URL);
    ir.save(doc);

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(ir, throwingSql());
    PipelineExecuteResult result =
        runtime.execute("hookApp", "items", PipelineExecuteRequest.empty());

    assertEquals("http-query", result.getOperation());
    assertEquals(2, result.getRowCount());
    assertEquals(200, result.getMeta().get("preWebhookStatus"));
    assertEquals(200, result.getMeta().get("postWebhookStatus"));
    String preBody = String.valueOf(result.getMeta().get("preWebhookBody"));
    String postBody = String.valueOf(result.getMeta().get("postWebhookBody"));
    assertTrue(preBody.contains("hook-ok"), preBody);
    assertTrue(preBody.contains("pipeline-webhook"), preBody);
    assertTrue(postBody.contains("hook-ok"), postBody);
    assertTrue(
        result.getHookTrace().stream().anyMatch(t -> t.contains("pre:webhook") && t.contains("200")),
        String.valueOf(result.getHookTrace()));
    assertTrue(
        result.getHookTrace().stream()
            .anyMatch(t -> t.contains("post:webhook") && t.contains("200")),
        String.valueOf(result.getHookTrace()));
    assertFalse(String.valueOf(result.getHookTrace()).contains("invented"));
  }

  @Test
  @DisplayName("missing webhook URL is skip, not a fake delivery")
  void execute_missingWebhookUrl_skips() throws Exception {
    IPSPipelineIrService ir = new PSPipelineIrService(tempDir);
    PipelineIrDocument doc =
        httpDocWithHooks("skipApp", "items", PSPipelineHttpUrl.BUNDLED_FIXTURE_URL, null, null);
    ir.save(doc);

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(ir, throwingSql());
    PipelineExecuteResult result =
        runtime.execute("skipApp", "items", PipelineExecuteRequest.empty());

    assertEquals(2, result.getRowCount());
    assertFalse(result.getMeta().containsKey("preWebhookStatus"));
    assertFalse(result.getMeta().containsKey("postWebhookStatus"));
    assertTrue(
        result.getHookTrace() == null
            || result.getHookTrace().stream().noneMatch(t -> t.contains("webhook status=")));
  }

  @Test
  @DisplayName("IR JSON round-trip preserves webhook hook URLs")
  void irJson_roundTripWebhookHooks() throws Exception {
    IPSPipelineIrService ir = new PSPipelineIrService(tempDir);
    PipelineIrDocument doc =
        httpDocWithHooks(
            "persistHooks",
            "items",
            PSPipelineHttpUrl.BUNDLED_FIXTURE_URL,
            PSPipelineHttpUrl.BUNDLED_WEBHOOK_FIXTURE_URL,
            null);
    ir.save(doc);
    PipelineIrDocument loaded = ir.load("persistHooks").orElseThrow();
    PipelineWebhookHooksIr hooks = loaded.findResource("items").getWebhookHooks();
    assertEquals(PSPipelineHttpUrl.BUNDLED_WEBHOOK_FIXTURE_URL, hooks.getPreUrl());
    assertTrue(loaded.findResource("items").presentStageInventory().contains("webhookHooks"));
  }

  @Test
  @DisplayName("cloud webhook URL fail-closes on execute")
  void execute_rejectsCloudWebhookUrl() throws Exception {
    PipelineIrDocument doc =
        httpDocWithHooks(
            "cloudHook",
            "items",
            PSPipelineHttpUrl.BUNDLED_FIXTURE_URL,
            "https://hooks.example/catch",
            null);
    IPSPipelineRuntimeService runtime =
        new PSPipelineRuntimeService(new PSPipelineIrService(tempDir), throwingSql());
    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class,
            () -> runtime.execute(doc, doc.findResource("items"), PipelineExecuteRequest.empty()));
    assertTrue(ex.getMessage().toLowerCase().contains("loopback"), ex.getMessage());
  }

  @Test
  @DisplayName("loopback HttpServer receives POST and status/body are recorded")
  void execute_loopbackHttpServer_postsAndRecords() throws Exception {
    AtomicInteger hits = new AtomicInteger();
    AtomicReference<String> lastBody = new AtomicReference<>("");
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(Executors.newCachedThreadPool());
    byte[] ack = "{\"received\":true,\"echo\":\"live-hook\"}".getBytes(StandardCharsets.UTF_8);
    server.createContext(
        "/hook",
        exchange -> {
          hits.incrementAndGet();
          lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, ack.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(ack);
          }
        });
    server.start();
    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
      IPSPipelineIrService ir = new PSPipelineIrService(tempDir);
      PipelineIrDocument doc =
          httpDocWithHooks("liveHook", "items", PSPipelineHttpUrl.BUNDLED_FIXTURE_URL, url, url);
      ir.save(doc);
      IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(ir, throwingSql());
      PipelineExecuteResult result =
          runtime.execute("liveHook", "items", PipelineExecuteRequest.empty());
      assertEquals(2, hits.get());
      assertTrue(lastBody.get().contains("\"phase\""), lastBody.get());
      assertEquals(200, result.getMeta().get("postWebhookStatus"));
      assertTrue(String.valueOf(result.getMeta().get("postWebhookBody")).contains("live-hook"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  @DisplayName("open redirect from loopback webhook is refused")
  void execute_webhookRefusesRedirect() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(Executors.newCachedThreadPool());
    server.createContext(
        "/redir",
        exchange -> {
          exchange.getResponseHeaders().add("Location", "http://example.com/evil");
          exchange.sendResponseHeaders(302, -1);
          exchange.close();
        });
    server.start();
    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/redir";
      PipelineIrDocument doc =
          httpDocWithHooks(
              "redirHook", "items", PSPipelineHttpUrl.BUNDLED_FIXTURE_URL, url, null);
      IPSPipelineRuntimeService runtime =
          new PSPipelineRuntimeService(new PSPipelineIrService(tempDir), throwingSql());
      PSPipelineIrException ex =
          assertThrows(
              PSPipelineIrException.class,
              () -> runtime.execute(doc, doc.findResource("items"), PipelineExecuteRequest.empty()));
      assertTrue(ex.getMessage().toLowerCase().contains("redirect"), ex.getMessage());
    } finally {
      server.stop(0);
    }
  }

  private static PipelineIrDocument httpDocWithHooks(
      String app, String resource, String httpUrl, String preUrl, String postUrl) {
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
    tank.setUrl(httpUrl);
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
    if (preUrl != null || postUrl != null) {
      PipelineWebhookHooksIr hooks = new PipelineWebhookHooksIr();
      hooks.setPreUrl(preUrl);
      hooks.setPostUrl(postUrl);
      hooks.setHttpMethod(PipelineWebhookHooksIr.METHOD_POST);
      res.setWebhookHooks(hooks);
    }
    doc.getResources().add(res);
    return doc;
  }

  private static IPSPipelineSqlAdapter throwingSql() {
    return new IPSPipelineSqlAdapter() {
      @Override
      public List<java.util.Map<String, Object>> query(PSPipelineSqlPlan plan) {
        throw new AssertionError("SQL adapter must not run for HTTP tests");
      }

      @Override
      public int update(PSPipelineSqlPlan plan) {
        throw new AssertionError("SQL adapter must not run for HTTP tests");
      }

      @Override
      public int updateAll(List<PSPipelineSqlPlan> plans, String transactionMode) {
        throw new AssertionError("SQL adapter must not run for HTTP tests");
      }
    };
  }
}
