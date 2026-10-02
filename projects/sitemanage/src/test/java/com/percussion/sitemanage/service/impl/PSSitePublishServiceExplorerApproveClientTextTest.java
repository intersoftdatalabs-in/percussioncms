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
package com.percussion.sitemanage.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.percussion.sitemanage.service.IPSSitePublishService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/** Explorer incremental approve must not copy exception text into the HTTP body. */
class PSSitePublishServiceExplorerApproveClientTextTest {

  private static final String SECRET = "jdbc:secret-internal-detail";

  @Test
  void statusBodyUsesFixedTextNotExceptionMessage() throws Exception {
    IPSSitePublishService publishService = mock(IPSSitePublishService.class);
    doThrow(new PSIncrementalQueueStatusException(400, SECRET))
        .when(publishService)
        .approveExplorerItemToIncrementalQueue("301");
    PSSitePublishServiceWebAdapter adapter = new PSSitePublishServiceWebAdapter(publishService);

    Response response = adapter.approveExplorerItemToIncrementalQueue("301");

    assertEquals(400, response.getStatus());
    String body = String.valueOf(response.getEntity());
    assertEquals("Request could not be completed", body);
    assertFalse(body.contains(SECRET));
    assertFalse(body.contains("jdbc"));
  }

  @Test
  void publishExceptionDoesNotEchoMessage() throws Exception {
    IPSSitePublishService publishService = mock(IPSSitePublishService.class);
    doThrow(new IPSSitePublishService.PSSitePublishException(SECRET))
        .when(publishService)
        .approveExplorerItemToIncrementalQueue("301");
    PSSitePublishServiceWebAdapter adapter = new PSSitePublishServiceWebAdapter(publishService);

    WebApplicationException thrown =
        assertThrows(
            WebApplicationException.class, () -> adapter.approveExplorerItemToIncrementalQueue("301"));
    assertFalse(String.valueOf(thrown.getMessage()).contains(SECRET));
  }

  @Test
  void unapproveStatusBodyUsesFixedTextNotExceptionMessage() throws Exception {
    IPSSitePublishService publishService = mock(IPSSitePublishService.class);
    doThrow(new PSIncrementalQueueStatusException(409, SECRET))
        .when(publishService)
        .unapproveExplorerItemOnIncrementalQueue("301");
    PSSitePublishServiceWebAdapter adapter = new PSSitePublishServiceWebAdapter(publishService);

    Response response = adapter.unapproveExplorerItemOnIncrementalQueue("301");

    assertEquals(409, response.getStatus());
    String body = String.valueOf(response.getEntity());
    assertFalse(body.contains(SECRET));
    assertFalse(body.contains("jdbc"));
  }

  @Test
  void removeStatusBodyUsesFixedTextNotExceptionMessage() throws Exception {
    IPSSitePublishService publishService = mock(IPSSitePublishService.class);
    doThrow(new PSIncrementalQueueStatusException(409, SECRET))
        .when(publishService)
        .removeExplorerItemFromIncrementalQueue("301");
    PSSitePublishServiceWebAdapter adapter = new PSSitePublishServiceWebAdapter(publishService);

    Response response = adapter.removeExplorerItemFromIncrementalQueue("301");

    assertEquals(409, response.getStatus());
    String body = String.valueOf(response.getEntity());
    assertFalse(body.contains(SECRET));
    assertFalse(body.contains("jdbc"));
  }
}
