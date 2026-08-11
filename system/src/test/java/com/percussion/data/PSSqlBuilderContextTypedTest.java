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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@link PSSqlBuilderContext} block lists after rawtypes cleanup.
 */
@Tag("UnitTest")
class PSSqlBuilderContextTypedTest {

  @Test
  void initialContextHasSingleStaticStatementBlock() {
    PSSqlBuilderContext ctx = new PSSqlBuilderContext();
    ctx.addText("SELECT 1", true);

    IPSStatementBlock[] blocks = ctx.getBlocks();
    assertNotNull(blocks);
    assertEquals(1, blocks.length);
    assertTrue(blocks[0] instanceof PSStatementBlock);
    assertTrue(blocks[0].isStaticBlock());
  }

  @Test
  void newBlockAddsTypedStatementBlockToContext() {
    PSSqlBuilderContext ctx = new PSSqlBuilderContext();
    ctx.addText("UPDATE t SET a=1", true);
    ctx.newBlock(true);
    ctx.addText(" WHERE b=2", true);

    IPSStatementBlock[] blocks = ctx.getBlocks();
    assertEquals(2, blocks.length);
    for (IPSStatementBlock block : blocks) {
      assertNotNull(block);
      assertTrue(block instanceof PSStatementBlock || block instanceof PSFunctionBlock);
    }
  }
}
