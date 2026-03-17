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
package com.percussion.pagemanagement.service;

import static com.percussion.pagemanagement.service.impl.PSResourceDefinitionUtils.sortByDependencies;
import static com.percussion.test.TestAssertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSFileResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinition;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDependency;
import com.percussion.pagemanagement.service.impl.PSResourceDefinitionUtils;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PSResourceDefinitionUtilsTest {
  private List<PSResourceDefinition> resources;
  private List<PSResourceDefinition> actual;
  private List<PSResourceDefinition> expected;
  private PSResourceDefinition a;
  private PSResourceDefinition b;
  private PSResourceDefinition c;
  private PSResourceDefinition d;
  private PSResourceDefinition e;
  private PSResourceDefinition f;

  @BeforeEach
  public void setUp() {
    a = createResource("a", (String[]) null);
    b = createResource("b", "a");
    c = createResource("c", "b", "a");
    d = createResource("d", "c", "f");
    e = createResource("e", "d", "b");
    f = createResource("f", "e");

    resources = new ArrayList<>(List.of(b, d, c, a, e));
    expected = List.of(a, b, c, d, e);
  }

  @Test
  public void testDepOrder() throws Exception {
    actual = sortByDependencies(resources);
    assertEquals("Expected to sort", expected, actual);
  }

  @Test
  public void testCycle() throws Exception {
    resources.add(f);
    assertThrows(
        PSResourceDefinitionUtils.PSResourceDefinitionDependencyCycleException.class,
        () -> sortByDependencies(resources));
  }

  public PSFileResource createResource(String id, String... depIds) {
    var r = new PSFileResource();
    if (depIds != null) r.setDependencies(createDeps(depIds));
    r.setUniqueId(id);
    return r;
  }

  public List<PSResourceDependency> createDeps(String... ids) {
    var deps = new ArrayList<PSResourceDependency>();
    for (var id : ids) {
      var d = new PSResourceDependency();
      d.setDependeeId(id);
      deps.add(d);
    }
    return deps;
  }
}
