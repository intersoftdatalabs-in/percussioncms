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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.service.IPSSitePublishService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * Covers issue #4598: malformed (non-GUID) item ids must surface {@link IllegalArgumentException}
 * (400 via the bus {@code runtimeExceptionMapper}) instead of a 500 on each item endpoint
 * (publish-now, takedown, stage, publishingActions).
 *
 * <p>Guid parsing fails with {@link NumberFormatException}; the shared {@code PSItemSummaryService}
 * wraps it in a {@link DataServiceLoadException}, which the adapter previously rethrew as a bare
 * {@code WebApplicationException} (500). Wellformed-unknown ids ({@code PSNotFoundException}, no
 * {@code NumberFormatException} in the cause chain) keep the existing path untouched for #4596.
 */
class PSSitePublishServiceWebAdapterMalformedIdTest {

  private static final String MALICIOUS_ID = "<script>alert(1)</script>";

  private PSSitePublishServiceWebAdapter adapterWith(IPSSitePublishService publishService) {
    return new PSSitePublishServiceWebAdapter(publishService);
  }

  /** Mimics the shared item-summary path: guid {@code NumberFormatException} wrapped checked. */
  private static DataServiceLoadException wrappedMalformedId(String id) {
    return new DataServiceLoadException(
        "Failed to load: " + id, new NumberFormatException("For input string: \"" + id + "\""));
  }

  private static IPSSitePublishService publishThrowingOnPublish(Throwable failure)
      throws Exception {
    IPSSitePublishService publishService = mock(IPSSitePublishService.class);
    when(publishService.publish(isNull(), any(), anyString(), anyBoolean(), isNull()))
        .thenThrow(failure);
    return publishService;
  }

  private static void assertInvalidItemId(IllegalArgumentException e) {
    assertEquals("Invalid item id", e.getMessage());
    assertFalse(e.getMessage().contains(MALICIOUS_ID));
    assertFalse(e.getMessage().contains("<script>"));
  }

  @Test
  void publishNowMapsWrappedNumberFormatToInvalidItemId() throws Exception {
    IPSSitePublishService publishService =
        publishThrowingOnPublish(wrappedMalformedId(MALICIOUS_ID));
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> adapterWith(publishService).publishPage(MALICIOUS_ID));
    assertInvalidItemId(e);
  }

  @Test
  void takedownMapsWrappedNumberFormatToInvalidItemId() throws Exception {
    IPSSitePublishService publishService =
        publishThrowingOnPublish(wrappedMalformedId(MALICIOUS_ID));
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> adapterWith(publishService).takeDownPage(MALICIOUS_ID));
    assertInvalidItemId(e);
  }

  @Test
  void stageMapsWrappedNumberFormatToInvalidItemId() throws Exception {
    IPSSitePublishService publishService =
        publishThrowingOnPublish(wrappedMalformedId(MALICIOUS_ID));
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> adapterWith(publishService).publishPageToStaging(MALICIOUS_ID));
    assertInvalidItemId(e);
  }

  @Test
  void publishingActionsMapsWrappedNumberFormatToInvalidItemId() throws Exception {
    IPSSitePublishService publishService = mock(IPSSitePublishService.class);
    when(publishService.getPublishingActions(anyString()))
        .thenThrow(wrappedMalformedId(MALICIOUS_ID));
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> adapterWith(publishService).getPublishingActions(MALICIOUS_ID));
    assertInvalidItemId(e);
  }

  @Test
  void rawNumberFormatIsSanitizedToFixedMessage() throws Exception {
    IPSSitePublishService publishService = mock(IPSSitePublishService.class);
    when(publishService.getPublishingActions(anyString()))
        .thenThrow(new NumberFormatException("For input string: \"" + MALICIOUS_ID + "\""));
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> adapterWith(publishService).getPublishingActions(MALICIOUS_ID));
    assertInvalidItemId(e);
  }

  @Test
  void blankIdStaysIllegalArgument() throws Exception {
    IPSSitePublishService publishService = mock(IPSSitePublishService.class);
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> adapterWith(publishService).publishPage("  "));
    assertInvalidItemId(e);
  }

  @Test
  void dataServiceFailureWithoutNumberFormatKeepsExistingPath() throws Exception {
    IPSSitePublishService publishService =
        publishThrowingOnPublish(new PSDataServiceException("db down"));
    assertThrows(
        WebApplicationException.class,
        () -> adapterWith(publishService).publishPage("1-101-708"));
  }

  @Test
  void notFoundWithoutNumberFormatKeepsExistingPath() throws Exception {
    IPSSitePublishService publishService = mock(IPSSitePublishService.class);
    when(publishService.getPublishingActions(anyString()))
        .thenThrow(new PSNotFoundException("Item not found for id: 1-101-999"));
    Response response = adapterWith(publishService).getPublishingActions("1-101-999");
    assertEquals(404, response.getStatus());
    assertEquals("Item not found", response.getEntity());
  }

  @Test
  void isMalformedItemIdDetectsCauseChain() {
    assertFalse(PSSitePublishServiceWebAdapter.isMalformedItemId(null));
    assertFalse(
        PSSitePublishServiceWebAdapter.isMalformedItemId(new PSDataServiceException("db down")));
    assertFalse(
        PSSitePublishServiceWebAdapter.isMalformedItemId(
            new PSNotFoundException("missing")));
    assertTrue(
        PSSitePublishServiceWebAdapter.isMalformedItemId(
            new NumberFormatException("For input string: \"nope\"")));
    assertTrue(
        PSSitePublishServiceWebAdapter.isMalformedItemId(wrappedMalformedId("nope")));
    assertTrue(
        PSSitePublishServiceWebAdapter.isMalformedItemId(
            new PSDataServiceException("wrap", wrappedMalformedId("nope"))));
  }
}
