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
package com.percussion.pagemanagement.assembler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.pagemanagement.assembler.PSRegionResult;
import com.percussion.pagemanagement.assembler.impl.PSConcurrentRegionsAssembler.FutureList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** Ensures typed {@link FutureList} is a true {@link List} and resolves the future on access. */
public class PSConcurrentRegionsAssemblerFutureListTest {

  @Test
  void futureListIsAssignableToTypedListAndResolves() {
    List<String> expected = List.of("a", "b");
    FutureList<String> futureList =
        new FutureList<>(CompletableFuture.completedFuture(expected));

    // Compile-time: FutureList<String> is a List<String> (no unchecked put into Map).
    List<String> asList = futureList;
    assertEquals(2, asList.size());
    assertEquals("a", asList.get(0));
    assertEquals("b", asList.get(1));
    assertTrue(asList instanceof List);
  }

  @Test
  void futureListWorksWithRegionResultTypeParameter() {
    List<PSRegionResult> empty = List.of();
    List<PSRegionResult> asList =
        new FutureList<>(CompletableFuture.completedFuture(empty));
    assertEquals(0, asList.size());
  }
}
