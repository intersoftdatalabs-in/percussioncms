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

package com.percussion.rest.apikeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class ApiKeysResourceTest {

  private IApiKeyScopeAdaptor adaptor;
  private ApiKeysResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IApiKeyScopeAdaptor.class);
    resource = new ApiKeysResource(adaptor);
  }

  @Test
  public void listScopesDelegatesToAdaptor() {
    when(adaptor.listScopes()).thenReturn(ApiKeyScopeCatalog.scopes());

    List<ApiKeyScope> out = resource.listScopes();

    assertEquals(ApiKeyScopeCatalog.scopes().size(), out.size());
    assertEquals("content:read", out.get(0).getKey());
    verify(adaptor).listScopes();
  }

  @Test
  public void listScopesWrapsFailures() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listScopes()).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listScopes());
    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  public void listScopesWithoutInjectionIs503() {
    ApiKeysResource bare = new ApiKeysResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listScopes);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void listPresetsDelegatesToAdaptor() {
    when(adaptor.listPresets()).thenReturn(ApiKeyScopeCatalog.presets());

    List<ApiKeyScopePreset> out = resource.listPresets();

    assertEquals(6, out.size());
    assertEquals("readonly", out.get(0).getId());
    verify(adaptor).listPresets();
  }

  @Test
  public void listKeysEmptyWhenNoneRegistered() {
    when(adaptor.listKeys()).thenReturn(List.of());

    assertEquals(0, resource.listKeys().size());
    verify(adaptor).listKeys();
  }

  @Test
  public void getKeyMissingIs404() {
    when(adaptor.getKey(eq("missing"))).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getKey("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void saveKeyDelegatesAndReturnsStored() {
    ApiKeyAssignment body = new ApiKeyAssignment();
    body.setScopes(List.of("content:read"));
    ApiKeyAssignment stored = new ApiKeyAssignment();
    stored.setName("sync");
    stored.setScopes(List.of("content:read"));
    stored.setPresetId(null);
    when(adaptor.saveKey(eq("sync"), any())).thenReturn(stored);

    ApiKeyAssignment out = resource.saveKey("sync", body);

    assertEquals("sync", out.getName());
    verify(adaptor).saveKey(eq("sync"), any());
  }

  @Test
  public void saveKeyNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.saveKey("sync", null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void saveKeyUnknownScopeIs400() {
    when(adaptor.saveKey(eq("sync"), any()))
        .thenThrow(new IllegalArgumentException("Unknown scope: nope"));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.saveKey("sync", new ApiKeyAssignment()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void saveKeyForbiddenPassthroughIs403() {
    when(adaptor.saveKey(eq("sync"), any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.saveKey("sync", new ApiKeyAssignment()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void catalogMatchPresetFindsReadonly() {
    assertEquals(
        "readonly",
        ApiKeyScopeCatalog.matchPreset(
            List.of("content:read", "asset:read", "template:read", "site:read", "publish:status")));
  }

  @Test
  public void catalogMatchPresetNullForCustom() {
    assertNull(ApiKeyScopeCatalog.matchPreset(List.of("content:read", "publish")));
  }

  @Test
  public void catalogEveryPresetScopeIsKnown() {
    for (ApiKeyScopePreset preset : ApiKeyScopeCatalog.presets()) {
      for (String scope : preset.getScopes()) {
        assertEquals(true, ApiKeyScopeCatalog.isKnownScope(scope), "preset " + preset.getId());
      }
    }
  }
}
