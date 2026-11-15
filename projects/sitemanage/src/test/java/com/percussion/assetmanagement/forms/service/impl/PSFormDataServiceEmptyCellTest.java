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
package com.percussion.assetmanagement.forms.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryClientException;
import com.percussion.delivery.client.PSDeliveryClient;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.pubserver.IPSPubServerService;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.net.ConnectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Empty-cell behavior for forms REST used by Home gadgets: a CMS-only H2 cell has no DTS publish
 * server, so forms calls must return empty payloads (HTTP 200), not HTTP 500.
 */
@ExtendWith(MockitoExtension.class)
class PSFormDataServiceEmptyCellTest {

  @Mock private IPSDeliveryInfoService deliveryService;
  @Mock private IPSPubServerService pubServerService;

  private PSFormDataService formDataService;

  @BeforeEach
  void setUp() throws Exception {
    formDataService = new PSFormDataService(deliveryService);
    // pubServerService is an @Autowired @Lazy field with no setter; wire the mock directly.
    Field field = PSFormDataService.class.getDeclaredField("pubServerService");
    field.setAccessible(true);
    field.set(formDataService, pubServerService);
  }

  private void stubNoDeliveryServer() throws Exception {
    when(pubServerService.getDefaultAdminURL("Site")).thenReturn(null);
    when(deliveryService.findByService(eq(PSDeliveryInfo.SERVICE_FORMS), isNull(), isNull()))
        .thenReturn(null);
  }

  @Test
  void getAllFormData_noDeliveryServer_returnsEmptyList() throws Exception {
    stubNoDeliveryServer();

    var result = formDataService.getAllFormData("Site");

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void clearFormData_noDeliveryServer_isNoOp() throws Exception {
    stubNoDeliveryServer();

    assertDoesNotThrow(() -> formDataService.clearFormData("form", "Site"));
  }

  @Test
  void exportFormData_noDeliveryServer_returnsEmptyString() throws Exception {
    stubNoDeliveryServer();

    assertEquals("", formDataService.exportFormData("Site", "form"));
  }

  @Test
  void getAllFormData_unreachableDeliveryServer_returnsEmptyList() throws Exception {
    // Fresh CMS-only H2 cell: a default delivery record exists but nothing listens
    // (issue #6399). Gadgets expect an empty payload, not HTTP 500.
    PSFormDataService spy = spyWithFailingDeliveryClient(new ConnectException("Connection refused"));

    var result = spy.getAllFormData("Site");

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void getAllFormData_nonTransportFailure_stillThrows() throws Exception {
    // Narrowing the catch must not mask real outages (issue #6399): parse/config
    // failures still surface as HTTP 500.
    PSFormDataService spy =
        spyWithFailingDeliveryClient(new PSDeliveryClientException("Error parsing JSON response"));

    assertThrows(WebApplicationException.class, () -> spy.getAllFormData("Site"));
  }

  @Test
  void getAllFormData_clientUnreachableMessage_returnsEmptyList() throws Exception {
    // Production shape on a CMS-only cell (server.log, issue #6399): PSDeliveryClient
    // throws its "Unable to connect ..." message WITHOUT a cause chain.
    PSFormDataService spy =
        spyWithFailingDeliveryClient(
            new PSDeliveryClientException(
                "Unable to connect to delivery server at: {}.https://localhost:8443."));

    var result = spy.getAllFormData("Site");

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  private PSFormDataService spyWithFailingDeliveryClient(Throwable failure) throws Exception {
    var server = new PSDeliveryInfo("https://localhost:8443");
    when(pubServerService.getDefaultAdminURL("Site")).thenReturn(null);
    when(deliveryService.findByService(eq(PSDeliveryInfo.SERVICE_FORMS), isNull(), isNull()))
        .thenReturn(server);

    PSDeliveryClient deliveryClient = mock(PSDeliveryClient.class);
    PSDeliveryClientException transportFailure =
        failure instanceof PSDeliveryClientException dce
            ? dce
            : new PSDeliveryClientException(failure);
    when(deliveryClient.getJsonObject(any(PSDeliveryActionOptions.class)))
        .thenThrow(transportFailure);
    PSFormDataService spy = spy(formDataService);
    doReturn(deliveryClient).when(spy).createDeliveryClient();
    return spy;
  }
}
