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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.error.PSException;
import com.percussion.server.PSRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for typed {@link PSInlineLinkProcessor} relationship maps (issue #2453 server
 * handlers rawtypes residual batch 2f). Full processing requires a live CMS stack; this covers
 * null/empty map short-circuits on the fully-specified entry point that validates the contract
 * without reading the request security token.
 */
@Tag("UnitTest")
public class PSInlineLinkProcessorContractTest {

  @Test
  void nullArgumentsRejected() {
    PSRequest request = new PSRequest(null, null, null, null);
    PSLocator item = new PSLocator(1, 1);
    Map<Integer, PSRelationship> relationships = new HashMap<>();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSInlineLinkProcessor.processInlineLinkItem(
                null, item, relationships, -1, false, false));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSInlineLinkProcessor.processInlineLinkItem(
                request, null, relationships, -1, false, false));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSInlineLinkProcessor.processInlineLinkItem(
                request, item, null, -1, false, false));
  }

  @Test
  void emptyRelationshipMapIsNoOp() throws PSException {
    PSRequest request = new PSRequest(null, null, null, null);
    PSLocator item = new PSLocator(1, 1);

    assertDoesNotThrow(
        () ->
            PSInlineLinkProcessor.processInlineLinkItem(
                request, item, Collections.emptyMap(), -1, false, false));
  }
}
