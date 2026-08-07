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
package com.percussion.category.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.category.data.PSCategory;
import com.percussion.category.data.PSCategoryNode;
import com.percussion.category.marshaller.PSCategoryMarshaller;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for category → DTS rename payload building (GH-957).
 *
 * <p>Publish Categories only rewrites {@code perc:category} metadata paths for nodes that carry
 * {@code previousCategoryName}. These tests lock that selection logic and the sibling-rename
 * isolation fix.
 */
class PSCategoryServiceUtilPublishTest {

  @Test
  void renamedTopLevelCategoryProducesPreviousAndNewPath() throws Exception {
    PSCategoryNode renamed = node("n1", "News2", "News");
    String json = marshalTree(List.of(renamed));

    String payload = PSCategoryServiceUtil.getCategoriesForPublish(json);
    assertNotNull(payload);
    assertFalse("[]".equals(payload), "expected at least one rename pair");

    JSONArray arr = new JSONArray(payload);
    assertEquals(1, arr.length());
    JSONObject pair = arr.getJSONObject(0);
    assertEquals("/Categories/News", pair.getString("previousCategoryName"));
    assertEquals("/Categories/News2", pair.getString("title"));
  }

  @Test
  void unmodifiedSiblingsAreNotPublishedWhenNeighborIsRenamed() throws Exception {
    // Sibling bleed regression: a rename on A must not invent renames for B/C.
    PSCategoryNode a = node("a", "AlphaNew", "Alpha");
    PSCategoryNode b = node("b", "Beta", null);
    PSCategoryNode c = node("c", "Gamma", null);

    JSONArray arr =
        PSCategoryServiceUtil.findModifiedCategories(
            List.of(a, b, c), "/Categories", null, false);

    assertEquals(1, arr.length(), "only the renamed sibling should produce a pair");
    assertEquals("/Categories/Alpha", arr.getJSONObject(0).getString("previousCategoryName"));
    assertEquals("/Categories/AlphaNew", arr.getJSONObject(0).getString("title"));
  }

  @Test
  void childPathsUpdateWhenParentIsRenamed() throws Exception {
    PSCategoryNode child = node("child", "Leaf", null);
    PSCategoryNode parent = node("parent", "ParentNew", "ParentOld");
    parent.setChildNodes(List.of(child));

    JSONArray arr =
        PSCategoryServiceUtil.findModifiedCategories(
            List.of(parent), "/Categories", null, false);

    assertEquals(2, arr.length());
    assertEquals("/Categories/ParentOld", arr.getJSONObject(0).getString("previousCategoryName"));
    assertEquals("/Categories/ParentNew", arr.getJSONObject(0).getString("title"));
    assertEquals(
        "/Categories/ParentOld/Leaf", arr.getJSONObject(1).getString("previousCategoryName"));
    assertEquals("/Categories/ParentNew/Leaf", arr.getJSONObject(1).getString("title"));
  }

  @Test
  void emptyTreeYieldsEmptyArrayPayload() throws Exception {
    String json = marshalTree(List.of());
    String payload = PSCategoryServiceUtil.getCategoriesForPublish(json);
    // No top-level nodes → method returns null (caller treats as nothing to publish)
    // or empty array if title present with empty children. Accept either empty signal.
    assertTrue(
        payload == null || "[]".equals(payload),
        "expected no renames for empty tree, got: " + payload);
  }

  @Test
  void onlyTitleChangeWithoutPreviousNameIsNotPublished() throws Exception {
    PSCategoryNode plain = node("p", "JustATitle", null);
    JSONArray arr =
        PSCategoryServiceUtil.findModifiedCategories(
            List.of(plain), "/Categories", null, false);
    assertEquals(0, arr.length());
  }

  private static PSCategoryNode node(String id, String title, String previousName) {
    var n = new PSCategoryNode();
    n.setId(id);
    n.setTitle(title);
    n.setPreviousCategoryName(previousName);
    n.setChildNodes(new ArrayList<>());
    return n;
  }

  private static String marshalTree(List<PSCategoryNode> top) {
    var cat = new PSCategory();
    cat.setTitle("Categories");
    cat.setTopLevelNodes(new ArrayList<>(top));
    return PSCategoryMarshaller.marshalToJson(cat);
  }
}
