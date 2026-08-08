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
package com.percussion.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed {@link PSExecutionBlock} step list. */
@Tag("UnitTest")
class PSExecutionBlockTypedTest {

  @Test
  void unconditionalBlockRunsAllStepsInOrder() throws Exception {
    AtomicInteger counter = new AtomicInteger();
    AtomicReference<PSExecutionData> seen = new AtomicReference<>();
    // Non-null fixture: IPSExecutionStep.execute contract requires non-null data.
    PSExecutionData data = new PSExecutionData(null, null, null);
    PSExecutionBlock block = new PSExecutionBlock(null);
    block.add(
        execData -> {
          assertNotNull(execData);
          seen.set(execData);
          assertEquals(0, counter.getAndIncrement());
        });
    block.add(
        execData -> {
          assertSame(data, execData);
          assertEquals(1, counter.getAndIncrement());
        });

    block.execute(data);
    assertEquals(2, counter.get());
    assertSame(data, seen.get());
  }
}
