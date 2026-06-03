/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.rest.assets;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class AssetSerialDeserialTests {

  // QA update for task_1780430034917 GH#685 Flash widget removal: Flash subtype removed
  // from REST models; generic Asset and file assets cover former flash use case.

  private Asset getTestAsset() {
    var a = new Asset();
    a.setName("testName");
    a.setFolderPath("/Assets/uploads/testPath");
    a.setCreatedDate(new Date());
    a.setId("testId");
    a.setLastModifiedDate(new Date());
    a.setType("percTest");
    a.setRemove(false);
    a.setFields(new AssetFieldList());
    a.getFields().add(new AssetField("testProp", "testValue"));
    return a;
  }

  @Test
  public void testSerialize() throws JsonProcessingException {
    var mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
    mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
    var a = getTestAsset();
    assertTrue(mapper.canSerialize(Asset.class));
    var assetString = mapper.writeValueAsString(a);

    assertNotNull(assetString);

    var n = mapper.readValue(assetString, Asset.class);

    assertFalse(n.getFields().isEmpty());
    assertEquals(a, n);
  }
}
