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

package com.percussion.deployer.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSParam;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed {@link PSDeployComponentUtils#parseParams} and {@link
 * PSDeployComponentUtils#convertToParams} (issue #2825 Xlint batch 5).
 */
public class PSDeployComponentUtilsTypedTest {

  @Test
  public void parseParamsReturnsTypedMapWithQueryValues() {
    StringBuilder base = new StringBuilder();
    Map<String, Object> params =
        PSDeployComponentUtils.parseParams("/Rhythmyx/app/page?sys_contentid=301&sys_folderid=2", base);

    assertEquals("/Rhythmyx/app/page", base.toString());
    assertEquals("301", params.get("sys_contentid"));
    assertEquals("2", params.get("sys_folderid"));
  }

  @Test
  public void parseParamsEmptyQueryReturnsEmptyMap() {
    Map<String, Object> params = PSDeployComponentUtils.parseParams("no-query", null);
    assertNotNull(params);
    assertTrue(params.isEmpty());
  }

  @Test
  public void parseParamsRejectsNullUrl() {
    assertThrows(IllegalArgumentException.class, () -> PSDeployComponentUtils.parseParams(null, null));
  }

  @Test
  public void convertToParamsSingleValue() {
    Map.Entry<String, Object> entry = Map.entry("sys_contentid", "301");
    List<PSParam> params = PSDeployComponentUtils.convertToParams(entry);
    assertEquals(1, params.size());
    assertEquals("sys_contentid", params.get(0).getName());
    assertEquals("301", params.get(0).getValue().getValueText());
  }

  @Test
  public void convertToParamsRepeatedValues() {
    List<String> values = new ArrayList<>();
    values.add("a");
    values.add("b");
    Map.Entry<String, Object> entry = Map.entry("sys_id", values);

    List<PSParam> params = PSDeployComponentUtils.convertToParams(entry);
    assertEquals(2, params.size());
    assertEquals("sys_id[0]", params.get(0).getName());
    assertEquals("a", params.get(0).getValue().getValueText());
    assertEquals("sys_id[1]", params.get(1).getName());
    assertEquals("b", params.get(1).getValue().getValueText());
  }

  @Test
  public void convertToEntriesFlattensRepeatedParams() {
    Map<String, Object> map =
        PSDeployComponentUtils.parseParams("x?one=1&two=2", new StringBuilder());
    Iterator<Map.Entry<String, Object>> it = PSDeployComponentUtils.convertToEntries(map);
    int count = 0;
    while (it.hasNext()) {
      Map.Entry<String, Object> e = it.next();
      assertNotNull(e.getKey());
      assertNotNull(e.getValue());
      count++;
    }
    assertEquals(2, count);
  }
}
