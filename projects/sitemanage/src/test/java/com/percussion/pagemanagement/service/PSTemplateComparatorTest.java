/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
// REFACTORED: CP-JAVA11

package com.percussion.pagemanagement.service;

import static com.percussion.test.TestAssertions.*;
import static java.util.Arrays.asList;

import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.service.impl.PSTemplateService.PSTemplateSorter;
import java.util.List;
import org.junit.jupiter.api.Test;

class PSTemplateComparatorTest {

  private final PSTemplateSummary a = create("a", "");
  private final PSTemplateSummary A = create("perc.base.A", "Z");
  private final PSTemplateSummary b = create("perc.base.b", "b");
  private final PSTemplateSummary B = create("b", "B");

  {
    A.setReadOnly(true);
    b.setReadOnly(true);
  }

  private final List<PSTemplateSummary> sums = asList(b, A, B, a);
  private final PSTemplateSorter comparator = new PSTemplateSorter();

  private PSTemplateSummary create(String name, String label) {
    var s = new PSTemplateSummary();
    s.setName(name);
    s.setLabel(label);
    return s;
  }

  @Test
  void testCaseInsensitive() {
    var expected = asList(a, A, b, B);
    var actual = comparator.sort(sums);
    assertEquals(expected, actual);
  }
}
