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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.apikeys.ApiKeyAssignment;
import jakarta.ws.rs.WebApplicationException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * JSON-registry API key scopes: Admin gate, upsert round-trip, fail-closed validation, and
 * corrupt-registry tolerance (#6481).
 */
@Tag("UnitTest")
class ApiKeyScopeAdaptorTest {

  @TempDir Path tempDir;

  private AtomicReference<Path> registryFile;
  private ApiKeyScopeAdaptor adaptor;

  @BeforeEach
  void setUp() {
    registryFile = new AtomicReference<>(tempDir.resolve("apikey-scopes.json"));
    adaptor = new ApiKeyScopeAdaptor(registryFile::get, () -> true);
  }

  @Test
  void emptyRegistryListsNoKeys() {
    assertTrue(adaptor.listKeys().isEmpty());
    assertNull(adaptor.getKey("missing"));
  }

  @Test
  void saveKeyRoundTripPersistsAcrossInstances() {
    ApiKeyAssignment body = new ApiKeyAssignment();
    body.setDisplayName("Sync job");
    body.setScopes(List.of("publish:status", "content:read", "content:read"));

    ApiKeyAssignment stored = adaptor.saveKey("sync", body);

    assertEquals("sync", stored.getName());
    assertEquals(List.of("content:read", "publish:status"), stored.getScopes());
    assertNull(stored.getPresetId());
    assertTrue(Files.isRegularFile(registryFile.get()));

    ApiKeyScopeAdaptor reloaded =
        new ApiKeyScopeAdaptor(registryFile::get, () -> true);
    ApiKeyAssignment again = reloaded.getKey("sync");
    assertEquals("Sync job", again.getDisplayName());
    assertEquals(List.of("content:read", "publish:status"), again.getScopes());

    assertEquals(1, reloaded.listKeys().size());
  }

  @Test
  void saveKeyPresetMatchRecomputed() {
    ApiKeyAssignment body = new ApiKeyAssignment();
    body.setScopes(
        List.of("content:read", "asset:read", "template:read", "site:read", "publish:status"));

    ApiKeyAssignment stored = adaptor.saveKey("dash", body);

    assertEquals("readonly", stored.getPresetId());
  }

  @Test
  void saveKeyUnknownScopeRejected() {
    ApiKeyAssignment body = new ApiKeyAssignment();
    body.setScopes(List.of("content:read", "nope"));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.saveKey("sync", body));
    assertTrue(ex.getMessage().contains("nope"));
    assertTrue(adaptor.listKeys().isEmpty());
  }

  @Test
  void saveKeyBlankNameRejected() {
    ApiKeyAssignment body = new ApiKeyAssignment();
    body.setScopes(List.of("content:read"));

    assertThrows(IllegalArgumentException.class, () -> adaptor.saveKey("  ", body));
    assertThrows(IllegalArgumentException.class, () -> adaptor.saveKey("../evil", body));
  }

  @Test
  void saveKeyBodyNameMismatchRejected() {
    ApiKeyAssignment body = new ApiKeyAssignment();
    body.setName("other");
    body.setScopes(List.of("content:read"));

    assertThrows(IllegalArgumentException.class, () -> adaptor.saveKey("sync", body));
  }

  @Test
  void nonAdminForbiddenOnEveryMethod() {
    ApiKeyScopeAdaptor locked = new ApiKeyScopeAdaptor(registryFile::get, () -> false);

    assertEquals(403, assertThrows(WebApplicationException.class, locked::listScopes)
        .getResponse().getStatus());
    assertEquals(403, assertThrows(WebApplicationException.class, locked::listPresets)
        .getResponse().getStatus());
    assertEquals(403, assertThrows(WebApplicationException.class, locked::listKeys)
        .getResponse().getStatus());
    assertEquals(403, assertThrows(WebApplicationException.class, () -> locked.getKey("x"))
        .getResponse().getStatus());
    ApiKeyAssignment body = new ApiKeyAssignment();
    body.setScopes(List.of("content:read"));
    assertEquals(403, assertThrows(WebApplicationException.class, () -> locked.saveKey("x", body))
        .getResponse().getStatus());
  }

  @Test
  void corruptRegistryReadsAsEmpty() throws Exception {
    Files.writeString(registryFile.get(), "{not json", StandardCharsets.UTF_8);

    assertTrue(adaptor.listKeys().isEmpty());
    assertNull(adaptor.getKey("sync"));
  }

  @Test
  void unknownStoredScopesDroppedOnRead() throws Exception {
    Files.writeString(
        registryFile.get(),
        "{\"keys\":{\"sync\":{\"displayName\":\"s\",\"scopes\":[\"content:read\",\"nope\"]}}}",
        StandardCharsets.UTF_8);

    ApiKeyAssignment again = adaptor.getKey("sync");

    assertEquals(List.of("content:read"), again.getScopes());
  }

  @Test
  void catalogListsKnownScopesAndPresets() {
    assertEquals(13, adaptor.listScopes().size());
    assertEquals(6, adaptor.listPresets().size());
    assertEquals("content:read", adaptor.listScopes().get(0).getKey());
    assertEquals("readonly", adaptor.listPresets().get(0).getId());
  }
}
