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
package com.percussion.cms.objectstore.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSRelationshipConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for activation-endpoint filtering used by {@link
 * PSRelationshipEffectProcessor} (issue #2453). Pure decision table — no live relationship/effect
 * stack required.
 */
@Tag("UnitTest")
public class PSRelationshipEffectProcessorActivationTest {

  @Test
  void activationEndpointDecisionTable() {
    assertTrue(
        PSRelationshipEffectProcessor.isProcessThisEffect(
            true, PSRelationshipConfig.ACTIVATION_ENDPOINT_EITHER));
    assertTrue(
        PSRelationshipEffectProcessor.isProcessThisEffect(
            false, PSRelationshipConfig.ACTIVATION_ENDPOINT_EITHER));

    assertTrue(
        PSRelationshipEffectProcessor.isProcessThisEffect(
            true, PSRelationshipConfig.ACTIVATION_ENDPOINT_OWNER));
    assertFalse(
        PSRelationshipEffectProcessor.isProcessThisEffect(
            false, PSRelationshipConfig.ACTIVATION_ENDPOINT_OWNER));

    assertFalse(
        PSRelationshipEffectProcessor.isProcessThisEffect(
            true, PSRelationshipConfig.ACTIVATION_ENDPOINT_DEPENDENT));
    assertTrue(
        PSRelationshipEffectProcessor.isProcessThisEffect(
            false, PSRelationshipConfig.ACTIVATION_ENDPOINT_DEPENDENT));
  }
}
