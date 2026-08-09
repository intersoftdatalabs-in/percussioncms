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
package com.percussion.relationship.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.data.PSExecutionData;
import com.percussion.design.objectstore.PSConditionalEffect;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.extension.PSExtensionRef;
import com.percussion.relationship.IPSExecutionContext;
import com.percussion.relationship.PSExecutionContext;
import com.percussion.relationship.PSTestResult;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed relationship-effect result collections (issue #2453 cms.objectstore
 * server handlers rawtypes residual batch 2f).
 */
@Tag("UnitTest")
public class PSRelationshipEffectTestResultGenericsTest {

  @Test
  void typedGetResultsReturnsAddedPairsInOrder() {
    PSRelationship relationship = sampleRelationship(42);
    PSRelationshipEffectTestResult result = new PSRelationshipEffectTestResult(relationship);
    assertSame(relationship, result.getRelationship());

    PSConditionalEffect effect1 = sampleEffect("effect1");
    PSConditionalEffect effect2 = sampleEffect("effect2");
    PSTestResult test1 = new PSTestResult();
    test1.setRecurseDependents(true);
    test1.setActivationEndPoint(true);
    PSTestResult test2 = new PSTestResult();
    test2.setRecurseDependents(false);
    test2.setActivationEndPoint(false);

    result.add(effect1, test1);
    result.add(effect2, test2);

    Iterator<PSEffectTestResultPair> pairs = result.getResults();
    assertTrue(pairs.hasNext());
    PSEffectTestResultPair first = pairs.next();
    assertSame(effect1, first.getEffect());
    assertSame(test1, first.getResult());
    assertTrue(first.getResult().getRecurseDependents());
    assertTrue(first.getResult().isActivationEndPointOwner());

    assertTrue(pairs.hasNext());
    PSEffectTestResultPair second = pairs.next();
    assertSame(effect2, second.getEffect());
    assertSame(test2, second.getResult());
    assertFalse(second.getResult().getRecurseDependents());
    assertFalse(second.getResult().isActivationEndPointOwner());

    assertFalse(pairs.hasNext());
  }

  @Test
  void executionContextReturnsTypedProcessedRelationshipsFromMap() {
    PSRelationship relationship = sampleRelationship(7);
    PSRelationshipEffectTestResult effectResult = new PSRelationshipEffectTestResult(relationship);
    effectResult.add(sampleEffect("processed"), new PSTestResult());

    Map<Integer, PSRelationshipEffectTestResult> processed = new HashMap<>();
    processed.put(relationship.getId(), effectResult);

    PSExecutionData data = new PSExecutionData(null, null, null);
    PSExecutionContext ctx =
        new PSExecutionContext(IPSExecutionContext.RS_PRE_WORKFLOW, data, processed);

    Set<PSRelationship> found = ctx.getProcessedRelationships();
    assertEquals(1, found.size());
    assertTrue(found.contains(relationship));
  }

  @Test
  void executionContextReturnsEmptyWhenProcessedMapIsNull() {
    PSExecutionData data = new PSExecutionData(null, null, null);
    PSExecutionContext ctx =
        new PSExecutionContext(IPSExecutionContext.RS_PRE_WORKFLOW, data, null);
    assertTrue(ctx.getProcessedRelationships().isEmpty());
  }

  private static PSRelationship sampleRelationship(int id) {
    PSRelationshipConfig config =
        new PSRelationshipConfig(
            "ActiveAssembly",
            PSRelationshipConfig.RS_TYPE_SYSTEM,
            PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY);
    return new PSRelationship(id, new PSLocator(100, 1), new PSLocator(200, 1), config);
  }

  private static PSConditionalEffect sampleEffect(String name) {
    PSExtensionRef ref = new PSExtensionRef("Java", "global/percussion/relationship/", name);
    return new PSConditionalEffect(new PSExtensionCall(ref, null));
  }
}
