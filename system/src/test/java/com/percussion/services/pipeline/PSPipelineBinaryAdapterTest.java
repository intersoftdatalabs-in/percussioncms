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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.pipeline.binary.PSPipelineBinaryAdapter;
import com.percussion.services.pipeline.binary.PSPipelineBinaryPath;
import com.percussion.services.pipeline.model.PipelineBinaryPayload;
import com.percussion.services.pipeline.model.PipelineBinaryResourceIr;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.sql.IPSPipelineSqlAdapter;
import com.percussion.services.pipeline.sql.PSPipelineSqlPlan;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Slice D binary resource: path guard, bundled fixture, missing file is not invented. */
@DisplayName("Pipeline binary adapter (Slice D)")
class PSPipelineBinaryAdapterTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("path guard: bundled ok; cloud, credentials, traversal rejected")
  void requireSafe_rejectsCloudCredentialsAndTraversal() throws Exception {
    assertEquals(
        PSPipelineBinaryPath.BUNDLED_TOKEN,
        PSPipelineBinaryPath.requireSafe(PSPipelineBinaryPath.BUNDLED_TOKEN));
    assertEquals(
        PSPipelineBinaryPath.BUNDLED_FILENAME,
        PSPipelineBinaryPath.requireSafe(PSPipelineBinaryPath.BUNDLED_FILENAME));
    assertEquals("files/icon.png", PSPipelineBinaryPath.requireSafe("files/icon.png"));

    PSPipelineIrException cloud =
        assertThrows(
            PSPipelineIrException.class,
            () -> PSPipelineBinaryPath.requireSafe("https://cdn.example/blob.bin"));
    assertTrue(cloud.getMessage().toLowerCase().contains("cloud"), cloud.getMessage());

    PSPipelineIrException userinfo =
        assertThrows(
            PSPipelineIrException.class,
            () -> PSPipelineBinaryPath.requireSafe("https://user:secret@cdn.example/blob.bin"));
    assertTrue(userinfo.getMessage().toLowerCase().contains("userinfo"), userinfo.getMessage());

    assertThrows(
        PSPipelineIrException.class, () -> PSPipelineBinaryPath.requireSafe("../etc/passwd"));
    assertThrows(
        PSPipelineIrException.class, () -> PSPipelineBinaryPath.requireSafe("/tmp/secret.bin"));
    assertThrows(
        PSPipelineIrException.class, () -> PSPipelineBinaryPath.requireSafe("C:\\temp\\x.bin"));
    assertThrows(PSPipelineIrException.class, () -> PSPipelineBinaryPath.requireSafe(""));
    assertThrows(
        PSPipelineIrException.class,
        () -> PSPipelineBinaryPath.requireSafe("s3://bucket/key.bin"));

    PSPipelineIrException javascript =
        assertThrows(
            PSPipelineIrException.class,
            () -> PSPipelineBinaryPath.requireSafe("javascript:alert(1)"));
    assertTrue(javascript.getMessage().contains("javascript"), javascript.getMessage());

    assertEquals("files/userinfo.bin", PSPipelineBinaryPath.requireSafe("files/userinfo.bin"));
  }

  @Test
  @DisplayName("bundled fixture returns known bytes, not invented content")
  void retrieve_bundledFixtureBytes() throws Exception {
    PipelineResourceIr resource = binaryResource(PSPipelineBinaryPath.BUNDLED_TOKEN, "text/plain");
    PipelineBinaryPayload payload = new PSPipelineBinaryAdapter().retrieve("sys_cmpDocuments", resource);
    assertEquals("text/plain", payload.getContentType());
    assertEquals(
        PSPipelineBinaryAdapter.BUNDLED_FIXTURE_UTF8,
        new String(payload.getBytes(), StandardCharsets.UTF_8));
    assertTrue(payload.getByteLength() > 0);
    assertFalse(
        new String(payload.getBytes(), StandardCharsets.UTF_8)
            .startsWith("version https://git-lfs.github.com/"));
  }

  @Test
  @DisplayName("missing application-owned fixture is 404-style, not invented bytes")
  void retrieve_missingSandboxFile() {
    PipelineResourceIr resource = binaryResource("missing-fixture.bin", "application/octet-stream");
    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class,
            () -> new PSPipelineBinaryAdapter(tempDir).retrieve("sys_cmpDocuments", resource));
    assertTrue(ex.getMessage().toLowerCase().contains("not found"), ex.getMessage());
  }

  @Test
  @DisplayName("empty application-owned fixture is not invented")
  void retrieve_emptySandboxFile() throws Exception {
    Path appDir = tempDir.resolve("sys_cmpDocuments");
    Files.createDirectories(appDir);
    Files.write(appDir.resolve("empty.bin"), new byte[0]);
    PipelineResourceIr resource = binaryResource("empty.bin", "application/octet-stream");
    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class,
            () -> new PSPipelineBinaryAdapter(tempDir).retrieve("sys_cmpDocuments", resource));
    assertTrue(ex.getMessage().toLowerCase().contains("not found"), ex.getMessage());
  }

  @Test
  @DisplayName("application-owned relative file returns stored bytes")
  void retrieve_sandboxFile() throws Exception {
    Path appDir = tempDir.resolve("sys_cmpDocuments");
    Files.createDirectories(appDir.resolve("files"));
    byte[] stored = "APP-OWNED-BYTES".getBytes(StandardCharsets.UTF_8);
    Files.write(appDir.resolve("files").resolve("icon.bin"), stored);
    PipelineResourceIr resource = binaryResource("files/icon.bin", "application/octet-stream");
    PipelineBinaryPayload payload =
        new PSPipelineBinaryAdapter(tempDir).retrieve("sys_cmpDocuments", resource);
    assertArrayEquals(stored, payload.getBytes());
  }

  @Test
  @DisplayName("constructor maxBodyBytes rejects oversized sandbox fixture")
  void retrieve_exceedsConstructorMaxBodyBytes() throws Exception {
    Path appDir = tempDir.resolve("sys_cmpDocuments");
    Files.createDirectories(appDir);
    Files.write(appDir.resolve("big.bin"), "12345".getBytes(StandardCharsets.UTF_8));
    PipelineResourceIr resource = binaryResource("big.bin", "application/octet-stream");
    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class,
            () -> new PSPipelineBinaryAdapter(tempDir, 4).retrieve("sys_cmpDocuments", resource));
    assertTrue(ex.getMessage().toLowerCase().contains("size"), ex.getMessage());
  }

  @Test
  @DisplayName("perc.pipeline.binary.maxBodyBytes property is parsed")
  void resolveMaxBodyBytes_property() {
    String key = PSPipelineBinaryAdapter.MAX_BODY_BYTES_PROPERTY;
    String previous = System.getProperty(key);
    try {
      System.clearProperty(key);
      assertEquals(PSPipelineBinaryAdapter.DEFAULT_MAX_BODY_BYTES, PSPipelineBinaryAdapter.resolveMaxBodyBytes());
      System.setProperty(key, "2048");
      assertEquals(2048, PSPipelineBinaryAdapter.resolveMaxBodyBytes());
      System.setProperty(key, "0");
      assertEquals(PSPipelineBinaryAdapter.DEFAULT_MAX_BODY_BYTES, PSPipelineBinaryAdapter.resolveMaxBodyBytes());
      System.setProperty(key, "nope");
      assertEquals(PSPipelineBinaryAdapter.DEFAULT_MAX_BODY_BYTES, PSPipelineBinaryAdapter.resolveMaxBodyBytes());
    } finally {
      if (previous == null) {
        System.clearProperty(key);
      } else {
        System.setProperty(key, previous);
      }
    }
  }

  @Test
  @DisplayName("runtime retrieveBinary loads bundled fixture from native IR")
  void runtime_retrieveBinary_bundled() throws Exception {
    Path storeDir = tempDir.resolve("ir");
    Files.createDirectories(storeDir);
    PSPipelineIrService ir = new PSPipelineIrService(storeDir);
    PipelineIrDocument doc = new PipelineIrDocument();
    doc.setSource(PipelineIrDocument.SOURCE_NATIVE);
    doc.getApp().setName("sys_cmpDocuments");
    PipelineResourceIr resource = binaryResource(PSPipelineBinaryPath.BUNDLED_TOKEN, "text/plain");
    resource.setName("binaryFixture");
    resource.setKind(PipelineResourceIr.KIND_BINARY);
    doc.getResources().add(resource);
    ir.save(doc);

    PSPipelineRuntimeService runtime = new PSPipelineRuntimeService(ir, throwingSql());
    PipelineBinaryPayload payload = runtime.retrieveBinary("sys_cmpDocuments", "binaryFixture");
    assertEquals(
        PSPipelineBinaryAdapter.BUNDLED_FIXTURE_UTF8,
        new String(payload.getBytes(), StandardCharsets.UTF_8));
  }

  private static IPSPipelineSqlAdapter throwingSql() {
    return new IPSPipelineSqlAdapter() {
      @Override
      public List<Map<String, Object>> query(PSPipelineSqlPlan plan) {
        throw new UnsupportedOperationException("SQL not used");
      }

      @Override
      public int update(PSPipelineSqlPlan plan) {
        throw new UnsupportedOperationException("SQL not used");
      }

      @Override
      public int updateAll(List<PSPipelineSqlPlan> plans, String txMode) {
        throw new UnsupportedOperationException("SQL not used");
      }
    };
  }

  private static PipelineResourceIr binaryResource(String path, String contentType) {
    PipelineResourceIr resource = new PipelineResourceIr();
    resource.setName("binaryFixture");
    resource.setKind(PipelineResourceIr.KIND_BINARY);
    PipelineBinaryResourceIr binary = new PipelineBinaryResourceIr();
    binary.setPath(path);
    binary.setContentType(contentType);
    resource.setBinary(binary);
    return resource;
  }
}
