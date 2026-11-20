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

package com.percussion.rest.explorer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class ItemIdentityResourceTest {

  @Mock private IItemIdentityAdaptor adaptor;
  @Mock private UriInfo uriInfo;

  private ItemIdentityResource resource;

  @BeforeEach
  void init() {
    resource = new ItemIdentityResource(adaptor);
    resource.setUriInfo(uriInfo);
    org.mockito.Mockito.lenient()
        .when(uriInfo.getBaseUri())
        .thenReturn(UriBuilder.fromUri("http://localhost/rest").build());
  }

  @Test
  void batchDelegatesNormalizedIds() {
    ItemIdentity identity = new ItemIdentity("42");
    identity.setCheckoutStatus("Checked Out By Me");
    when(adaptor.lookupBatch(any(), eq(List.of("42")))).thenReturn(List.of(identity));
    List<ItemIdentity> out =
        resource.lookupBatch(new ItemIdentityBatchRequest(Arrays.asList(" 42 ", "42", " ")));
    assertEquals(1, out.size());
    assertEquals("Checked Out By Me", out.get(0).getCheckoutStatus());
    verify(adaptor).lookupBatch(any(), eq(List.of("42")));
  }

  @Test
  void batchNullAdaptorResultIsEmptyList() {
    when(adaptor.lookupBatch(any(), eq(List.of("42")))).thenReturn(null);
    assertTrue(resource.lookupBatch(new ItemIdentityBatchRequest(List.of("42"))).isEmpty());
  }

  @Test
  void batchMissingBodyIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.lookupBatch(null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void batchBlankIdsAre400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.lookupBatch(new ItemIdentityBatchRequest(Arrays.asList(" ", null))));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void batchOverCapIs400() {
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < ItemIdentityResource.MAX_IDS + 1; i++) {
      ids.add("id-" + i);
    }
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.lookupBatch(new ItemIdentityBatchRequest(ids)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void batchHugeListFailsFast400() {
    // Review #6814: the raw-size cap must reject before any per-element work,
    // so a hostile/buggy 10k-id payload fails fast instead of grinding.
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < 10_000; i++) {
      ids.add("id-" + i);
    }
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.lookupBatch(new ItemIdentityBatchRequest(ids)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void batchDedupesPreservingOrder() {
    // Review #6814: duplicates collapse via LinkedHashSet, first-seen order kept.
    ItemIdentity identity = new ItemIdentity("1");
    when(adaptor.lookupBatch(any(), eq(List.of("1", "2")))).thenReturn(List.of(identity));
    List<ItemIdentity> out =
        resource.lookupBatch(new ItemIdentityBatchRequest(Arrays.asList("1", "2", "1", " 2 ")));
    assertEquals(1, out.size());
    verify(adaptor).lookupBatch(any(), eq(List.of("1", "2")));
  }

  @Test
  void batchAdaptorFailureIs500() {
    when(adaptor.lookupBatch(any(), eq(List.of("42"))))
        .thenThrow(new IllegalStateException("down"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.lookupBatch(new ItemIdentityBatchRequest(List.of("42"))));
    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  void batchPropagatesAdaptorStatus() {
    when(adaptor.lookupBatch(any(), eq(List.of("42"))))
        .thenThrow(new WebApplicationException("forbidden", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.lookupBatch(new ItemIdentityBatchRequest(List.of("42"))));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void batchEmptyIdsIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.lookupBatch(new ItemIdentityBatchRequest(Collections.emptyList())));
    assertEquals(400, ex.getResponse().getStatus());
  }
}
