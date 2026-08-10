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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSTextLiteral;
import java.sql.Types;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@link PSStatementBlock} / {@link PSStatementGroup} extractor lists
 * after rawtypes cleanup.
 */
@Tag("UnitTest")
class PSStatementBlockTypedTest {

  @Test
  void staticTextBlockHasEmptyExtractorsAndLobColumns() {
    PSStatementBlock block = new PSStatementBlock(true);
    block.addText("SELECT * FROM RXSITES");

    List<IPSDataExtractor> extractors = block.getReplacementValueExtractors();
    List<PSStatementColumn> lobCols = block.getLobStatementColumns();

    assertTrue(extractors.isEmpty());
    assertTrue(lobCols.isEmpty());
    assertTrue(block.isStaticBlock());
  }

  @Test
  void statementGroupMergesEmptyExtractorsFromBothSides() {
    PSStatementBlock left = new PSStatementBlock(true);
    left.addText("WHERE ");
    PSStatementBlock right = new PSStatementBlock(true);
    right.addText("1=1");

    PSStatementGroup group = new PSStatementGroup("WHERE", left, null, right);
    List<IPSDataExtractor> extractors = group.getReplacementValueExtractors();
    List<PSStatementColumn> lobCols = group.getLobStatementColumns();

    assertEquals(0, extractors.size());
    assertEquals(0, lobCols.size());
  }

  @Test
  void populatedReplacementFieldsYieldTypedExtractorsAndLobColumns() {
    PSStatementBlock block = new PSStatementBlock(false);
    block.addText("UPDATE t SET c=");
    block.addReplacementField(new PSTextLiteral("plain"), Types.VARCHAR, null);
    block.addReplacementField(new PSTextLiteral("blob-payload"), Types.BLOB, null);
    block.addReplacementField(new PSTextLiteral("clob-payload"), Types.CLOB, null);

    List<IPSDataExtractor> extractors = block.getReplacementValueExtractors();
    List<PSStatementColumn> lobCols = block.getLobStatementColumns();

    assertEquals(3, extractors.size());
    for (IPSDataExtractor extractor : extractors) {
      assertNotNull(extractor);
    }
    assertEquals(2, lobCols.size());
    assertFalse(block.isStaticBlock());
  }
}
