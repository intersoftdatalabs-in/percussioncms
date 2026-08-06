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
package com.percussion.recycle.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.percussion.recycle.data.PSEmptyRecycleResult;
import com.percussion.recycle.service.IPSEmptyRecycleService;
import com.percussion.recycle.service.IPSEmptyRecycleService.PSEmptyRecycleException;
import com.percussion.recycle.service.IPSEmptyRecycleService.PSEmptyRecycleNotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PSRecycleRestServiceTest {

  @Mock private IPSEmptyRecycleService emptyRecycleService;

  private PSRecycleRestService rest;

  @BeforeEach
  void setUp() {
    rest = new PSRecycleRestService(emptyRecycleService);
  }

  @Test
  void emptyDelete_returnsServiceResult() throws Exception {
    PSEmptyRecycleResult expected = new PSEmptyRecycleResult();
    expected.setAlreadyEmpty(true);
    when(emptyRecycleService.emptyRecyclingBin()).thenReturn(expected);

    PSEmptyRecycleResult actual = rest.emptyRecyclingBinDelete();
    assertSame(expected, actual);
  }

  @Test
  void emptyPost_returnsServiceResult() throws Exception {
    PSEmptyRecycleResult expected = new PSEmptyRecycleResult();
    expected.setPurgedFolderCount(2);
    when(emptyRecycleService.emptyRecyclingBin()).thenReturn(expected);

    PSEmptyRecycleResult actual = rest.emptyRecyclingBinPost();
    assertEquals(2, actual.getPurgedFolderCount());
  }

  @Test
  void empty_whenNotAuthorized_returns403WithoutLeakingDetails() throws Exception {
    when(emptyRecycleService.emptyRecyclingBin())
        .thenThrow(new PSEmptyRecycleNotAuthorizedException("secret internal reason XYZ"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> rest.emptyRecyclingBinDelete());
    assertEquals(403, ex.getResponse().getStatus());
    String body = String.valueOf(ex.getResponse().getEntity());
    assertTrue(body.contains("Admin"));
    assertTrue(!body.contains("XYZ"));
  }

  @Test
  void empty_whenServiceFails_returns500GenericMessage() throws Exception {
    when(emptyRecycleService.emptyRecyclingBin())
        .thenThrow(new PSEmptyRecycleException("db host=internal-secret:5432"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> rest.emptyRecyclingBinPost());
    assertEquals(500, ex.getResponse().getStatus());
    String body = String.valueOf(ex.getResponse().getEntity());
    assertTrue(body.contains("Failed to empty"));
    assertTrue(!body.contains("internal-secret"));
  }
}
