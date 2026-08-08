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
package com.percussion.share.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.assetmanagement.data.PSAssetList;
import com.percussion.assetmanagement.data.PSAssetSummaryList;
import com.percussion.pagemanagement.data.PSWidgetContentTypeList;
import com.percussion.pagemanagement.data.PSWidgetSummaryList;
import com.percussion.sitemanage.data.PSPublishingActionList;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards serialVersionUID + construction for JAXB/JSON list wrappers cleaned in issue #2032 batch
 * 1.
 */
public class PSSerializableListWrappersTest {

  @Test
  public void listWrappersDefineSerialVersionUid() throws Exception {
    assertSerialVersionUid(PSItemPropertiesList.class);
    assertSerialVersionUid(PSLightWeightObjectList.class);
    assertSerialVersionUid(PSAssetList.class);
    assertSerialVersionUid(PSAssetSummaryList.class);
    assertSerialVersionUid(PSWidgetContentTypeList.class);
    assertSerialVersionUid(PSWidgetSummaryList.class);
    assertSerialVersionUid(PSPublishingActionList.class);
  }

  @Test
  public void itemPropertiesListPreservesOrderAndContents() {
    var a = new PSItemProperties();
    a.setId("1");
    var b = new PSItemProperties();
    b.setId("2");
    var list = new PSItemPropertiesList(List.of(a, b));
    assertEquals(2, list.size());
    assertEquals("1", list.get(0).getId());
    assertEquals("2", list.get(1).getId());
  }

  private static void assertSerialVersionUid(Class<?> type) throws Exception {
    assertTrue(Serializable.class.isAssignableFrom(type), type.getName() + " should be Serializable");
    Field f = type.getDeclaredField("serialVersionUID");
    f.setAccessible(true);
    assertNotNull(f.get(null));
    assertEquals(long.class, f.getType());
  }
}
