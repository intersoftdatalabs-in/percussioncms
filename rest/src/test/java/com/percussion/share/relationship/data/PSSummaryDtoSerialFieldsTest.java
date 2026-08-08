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

package com.percussion.share.relationship.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.share.relationship.data.PSLocalDependencySummary.PSLocalDependencyLink;
import com.percussion.share.relationship.data.PSRelationshipSummary.PSRelationshipTypeBucket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for summary DTO list fields after the serial real fix ({@link ArrayList} storage
 * with defensive copies).
 */
class PSSummaryDtoSerialFieldsTest {

  @Test
  void taxonomySummaryCopiesNodesAndExposesArrayListStorage() {
    List<String> input = new ArrayList<>(List.of("/Sites/a", "/Sites/b"));
    PSTaxonomySummary summary = new PSTaxonomySummary(2L, input);

    assertEquals(2L, summary.getCount());
    assertEquals(input, summary.getNodes());
    assertInstanceOf(ArrayList.class, summary.getNodes());
    assertNotSame(input, summary.getNodes());

    input.add("/Sites/c");
    assertEquals(2, summary.getNodes().size());

    summary.setNodes(null);
    assertTrue(summary.getNodes().isEmpty());
  }

  @Test
  void relationshipSummaryCopiesByTypeBuckets() {
    List<PSRelationshipTypeBucket> input =
        new ArrayList<>(List.of(new PSRelationshipTypeBucket("translation", 3L)));
    PSRelationshipSummary summary = new PSRelationshipSummary(3L, input);

    assertEquals(3L, summary.getCount());
    assertEquals(1, summary.getByType().size());
    assertInstanceOf(ArrayList.class, summary.getByType());
    assertNotSame(input, summary.getByType());

    summary.setByType(null);
    assertTrue(summary.getByType().isEmpty());
  }

  @Test
  void localDependencySummaryCopiesLinks() {
    List<PSLocalDependencyLink> input =
        new ArrayList<>(List.of(new PSLocalDependencyLink("local", "0-1-2")));
    PSLocalDependencySummary summary = new PSLocalDependencySummary(1L, input);

    assertEquals(1L, summary.getCount());
    assertEquals(1, summary.getLinks().size());
    assertEquals("local", summary.getLinks().get(0).getType());
    assertInstanceOf(ArrayList.class, summary.getLinks());
    assertNotSame(input, summary.getLinks());

    summary.setLinks(null);
    assertTrue(summary.getLinks().isEmpty());
  }
}
