/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.cookieconsent.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.pubserver.IPSPubServerService;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for Home-gadget cookie consent totals: missing DTS indexer must return empty JSON, not
 * HTTP 500.
 */
@ExtendWith(MockitoExtension.class)
class PSCookieConsentServiceTest {

  @Mock private IPSDeliveryInfoService deliveryService;
  @Mock private IPSPubServerService pubServerService;

  private PSCookieConsentService service;

  @BeforeEach
  void setUp() throws Exception {
    service = new PSCookieConsentService(deliveryService);
    Field pubField = PSCookieConsentService.class.getDeclaredField("pubServerService");
    pubField.setAccessible(true);
    pubField.set(service, pubServerService);
  }

  @Test
  void getAllCookieConsentTotals_nullDeliveryServer_returnsEmptyJson() {
    when(deliveryService.findByService(PSDeliveryInfo.SERVICE_INDEXER)).thenReturn(null);

    String json = service.getAllCookieConsentTotals();

    assertEquals(PSCookieConsentService.EMPTY_TOTALS_JSON, json);
  }

  @Test
  void getCookieConsentForSite_nullDeliveryServer_returnsEmptyJson() throws Exception {
    when(pubServerService.getDefaultAdminURL("Demo")).thenReturn("https://dts.example/admin");
    when(deliveryService.findByService(eq(PSDeliveryInfo.SERVICE_INDEXER), isNull(), anyString()))
        .thenReturn(null);

    String json = service.getCookieConsentForSite("Demo");

    assertEquals(PSCookieConsentService.EMPTY_TOTALS_JSON, json);
  }

  @Test
  void getCookieConsentForSite_blankSiteName_throwsWebApplicationException() {
    assertThrows(WebApplicationException.class, () -> service.getCookieConsentForSite(" "));
  }

  @Test
  void getCookieConsentForSite_nullSiteName_throwsWebApplicationException() {
    assertThrows(WebApplicationException.class, () -> service.getCookieConsentForSite(null));
  }

  @Test
  void getCookieConsentForSite_pubServerLookupFailure_throwsWebApplicationException()
      throws Exception {
    when(pubServerService.getDefaultAdminURL(any()))
        .thenThrow(new IPSPubServerService.PSPubServerServiceException("no site"));

    assertThrows(WebApplicationException.class, () -> service.getCookieConsentForSite("Missing"));
  }
}
