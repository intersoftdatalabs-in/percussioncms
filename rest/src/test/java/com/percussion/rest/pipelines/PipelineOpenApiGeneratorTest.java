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

package com.percussion.rest.pipelines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.pipeline.model.BackendTankStageIr;
import com.percussion.services.pipeline.model.MapperStageIr;
import com.percussion.services.pipeline.model.MappingEntryIr;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class PipelineOpenApiGeneratorTest {

  @Test
  public void toSpecDocumentsExecutePathForQueryResource() {
    PipelineIrDocument ir = sampleIr();
    Map<String, Object> spec = PipelineOpenApiGenerator.toSpec(ir);
    assertEquals("3.0.3", spec.get("openapi"));
    @SuppressWarnings("unchecked")
    Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
    String path = "/pipelines/lookupApp/resources/DatasetQ/execute";
    assertTrue(paths.containsKey(path), "expected resource execute path");
    @SuppressWarnings("unchecked")
    Map<String, Object> item = (Map<String, Object>) paths.get(path);
    assertTrue(item.containsKey("post"));
    String yaml = PipelineOpenApiGenerator.toYaml(spec);
    assertTrue(yaml.contains("openapi:"));
    assertTrue(yaml.contains(path));
    assertFalse(yaml.contains("../"));
    String json = PipelineOpenApiGenerator.toJson(spec);
    assertTrue(json.contains("\"openapi\""));
    assertTrue(json.contains(path));
  }

  @Test
  public void skipsUnsafeResourceNames() {
    PipelineIrDocument ir = new PipelineIrDocument();
    ir.getApp().setName("lookupApp");
    PipelineResourceIr evil = new PipelineResourceIr();
    evil.setName("../secret");
    evil.setKind(PipelineResourceIr.KIND_QUERY);
    ir.getResources().add(evil);
    PipelineResourceIr ok = new PipelineResourceIr();
    ok.setName("safeRes");
    ok.setKind(PipelineResourceIr.KIND_QUERY);
    ir.getResources().add(ok);
    Map<String, Object> spec = PipelineOpenApiGenerator.toSpec(ir);
    @SuppressWarnings("unchecked")
    Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
    assertEquals(1, paths.size());
    assertTrue(paths.containsKey("/pipelines/lookupApp/resources/safeRes/execute"));
  }

  @Test
  public void normalizeFormatDefaultsYamlAndRejectsUnknown() {
    assertEquals("yaml", PipelineOpenApiGenerator.normalizeFormat(null));
    assertEquals("yaml", PipelineOpenApiGenerator.normalizeFormat(" YML "));
    assertEquals("json", PipelineOpenApiGenerator.normalizeFormat("JSON"));
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> PipelineOpenApiGenerator.normalizeFormat("xml"));
    assertEquals("OpenAPI format must be yaml or json", ex.getMessage());
  }

  @Test
  public void includesMapperFieldsAndHttpAdapterInDescription() {
    PipelineIrDocument ir = sampleIr();
    PipelineResourceIr res = ir.getResources().get(0);
    BackendTankStageIr tank = new BackendTankStageIr();
    tank.setPresent(true);
    tank.setAdapterType(BackendTankStageIr.ADAPTER_HTTP);
    tank.setUrl("http://127.0.0.1/pipeline-http-fixture");
    res.getStages().setBackendTank(tank);
    MapperStageIr mapper = new MapperStageIr();
    mapper.setPresent(true);
    MappingEntryIr m = new MappingEntryIr();
    m.setDocumentField("sku");
    mapper.getMappings().add(m);
    res.getStages().setMapper(mapper);

    Map<String, Object> spec = PipelineOpenApiGenerator.toSpec(ir);
    @SuppressWarnings("unchecked")
    Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
    @SuppressWarnings("unchecked")
    Map<String, Object> item =
        (Map<String, Object>) paths.get("/pipelines/lookupApp/resources/DatasetQ/execute");
    @SuppressWarnings("unchecked")
    Map<String, Object> post = (Map<String, Object>) item.get("post");
    String desc = String.valueOf(post.get("description"));
    assertTrue(desc.contains("HTTP"));
    assertTrue(desc.contains("sku"));
  }

  private static PipelineIrDocument sampleIr() {
    PipelineIrDocument ir = new PipelineIrDocument();
    ir.setSource(PipelineIrDocument.SOURCE_NATIVE);
    ir.getApp().setName("lookupApp");
    PipelineResourceIr res = new PipelineResourceIr();
    res.setName("DatasetQ");
    res.setKind(PipelineResourceIr.KIND_QUERY);
    res.setRequestPage("query.html");
    ir.getResources().add(res);
    return ir;
  }
}
