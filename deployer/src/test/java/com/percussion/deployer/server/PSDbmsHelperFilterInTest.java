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

package com.percussion.deployer.server;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.tablefactory.PSJdbcSelectFilter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed {@link PSDbmsHelper#getFilterInFromIds(java.util.Iterator, String)}
 * (issue #2417).
 */
public class PSDbmsHelperFilterInTest {

  @Test
  public void testGetFilterInFromIdsBuildsInClause() {
    List<String> ids = new ArrayList<>();
    ids.add("10");
    ids.add("20");
    ids.add("30");

    PSJdbcSelectFilter filter =
        PSDbmsHelper.getInstance().getFilterInFromIds(ids.iterator(), "ROLEID");

    // toString is the WHERE clause fragment (no WHERE keyword)
    String clause = filter.toString();
    assertTrue(clause.startsWith("ROLEID"), clause);
    assertTrue(clause.contains("IN"), clause);
    assertTrue(clause.contains("10"), clause);
    assertTrue(clause.contains("20"), clause);
    assertTrue(clause.contains("30"), clause);
    assertTrue(clause.contains("(10,20,30)"), clause);
  }

  @Test
  public void testGetFilterInFromIdsRejectsEmpty() {
    List<String> empty = new ArrayList<>();
    assertThrows(
        IllegalArgumentException.class,
        () -> PSDbmsHelper.getInstance().getFilterInFromIds(empty.iterator(), "COL"));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSDbmsHelper.getInstance().getFilterInFromIds(null, "COL"));
  }
}
