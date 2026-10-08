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
package com.percussion.publishingdesign.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.sitemanage.json.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for a name-only delivery-type update (#5203).
 *
 * <p>The SPA posts {@code {"deliveryType":{"name":"..."}}}. An omitted
 * {@code unpublishingRequiresAssembly} must not bind as explicit {@code false}.
 */
@Tag("UnitTest")
class PSDeliveryTypeSummaryJacksonTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(PSDeliveryTypeSummary.class);

  @Test
  void nameOnlyBodyOmitsBeanDescriptionAndAssemblyFlag() {
    String json = "{\"deliveryType\":{\"name\":\"Renamed\"}}";
    PSDeliveryTypeSummary body = mapper.readValue(json, PSDeliveryTypeSummary.class);
    assertEquals("Renamed", body.getName());
    assertNull(body.getBeanName());
    assertNull(body.getDescription());
    assertFalse(body.isUnpublishingRequiresAssembly());
    assertFalse(body.isUnpublishingRequiresAssemblySpecified());
  }

  @Test
  void flagOnlyFalseOmitsNameBeanAndDescription() {
    String json = "{\"deliveryType\":{\"unpublishingRequiresAssembly\":false}}";
    PSDeliveryTypeSummary body = mapper.readValue(json, PSDeliveryTypeSummary.class);
    assertNull(body.getName());
    assertNull(body.getBeanName());
    assertNull(body.getDescription());
    assertFalse(body.isUnpublishingRequiresAssembly());
    assertTrue(body.isUnpublishingRequiresAssemblySpecified());
  }

  @Test
  void explicitFalseAssemblyFlagIsSpecified() {
    String json =
        "{\"deliveryType\":{\"name\":\"Renamed\",\"unpublishingRequiresAssembly\":false}}";
    PSDeliveryTypeSummary body = mapper.readValue(json, PSDeliveryTypeSummary.class);
    assertFalse(body.isUnpublishingRequiresAssembly());
    assertTrue(body.isUnpublishingRequiresAssemblySpecified());
  }

  @Test
  void responseDoesNotWriteTheSpecifiedFlag() {
    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setDeliveryTypeId("5");
    body.setName("Renamed");
    body.setBeanName("sys_fileDeliveryHandler");
    body.setDescription("kept");
    body.setUnpublishingRequiresAssembly(true);

    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("\"deliveryType\""), json);
    assertTrue(json.contains("\"unpublishingRequiresAssembly\""), json);
    assertFalse(json.contains("Specified"), json);
    assertFalse(json.contains("unpublishingRequiresAssemblySpecified"), json);
  }
}
