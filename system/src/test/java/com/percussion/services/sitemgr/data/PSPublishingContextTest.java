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
package com.percussion.services.sitemgr.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import org.junit.jupiter.api.Test;

/** Unit test for the {@link PSPublishingContext} object (equals/clone + helper XML smoke). */
class PSPublishingContextTest {

  @Test
  void testEquals() throws Exception {
    PSPublishingContext context1 = createContext();
    PSPublishingContext context2 = new PSPublishingContext();
    assertFalse(context1.equals(context2));
    context2 = (PSPublishingContext) context1.clone();
    assertEquals(context1, context2);
    assertEquals(context1.hashCode(), context2.hashCode());

    context2.setDescription("This is a new description");
    assertFalse(context1.equals(context2));
  }

  @Test
  void testXml() throws Exception {
    PSPublishingContext context1 = createContext();
    PSPublishingContext context2 = new PSPublishingContext();
    assertFalse(context1.equals(context2));
    String str = context1.toXML();
    assertTrue(str.contains("publishing-context"), str);
    context2.fromXML(str);

    assertEquals(context1.getName(), context2.getName());
    assertEquals(context1.getDescription(), context2.getDescription());
    assertEquals(context1.getGUID().toString(), context2.getGUID().toString());
    assertEquals(
        context1.getDefaultSchemeId().toString(), context2.getDefaultSchemeId().toString());
  }

  private PSPublishingContext createContext() {
    PSPublishingContext context = new PSPublishingContext();
    context.setDefaultSchemeId(new PSGuid(PSTypeEnum.LOCATION_SCHEME, 314L));
    context.setDescription("This is a test description");
    context.setGUID(new PSGuid(PSTypeEnum.CONTEXT, 1L));
    context.setName("Publish");
    return context;
  }
}
