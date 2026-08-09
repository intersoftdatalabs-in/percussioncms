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
package com.percussion.services.assembly.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.assembly.data.PSTemplateBinding;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for package-install binding id / version handling before Hibernate merge (#2540).
 */
class PSAssemblyServiceOptimisticLockVersionTest {

  @Test
  void ensureOptimisticLockVersions_leavesVersionsNull_forMergeInsert() {
    PSAssemblyTemplate template = new PSAssemblyTemplate();
    template.setGUID(new PSGuid(PSTypeEnum.TEMPLATE, 1003L));
    template.setName("perc.pageDatabase");
    // Package archives set assigned ids without versions — null version => merge insert.
    assertNull(template.getVersion());

    PSTemplateBinding binding =
        new PSTemplateBinding(1, "sys_title", "$sys.item.getProperty(\"rx:sys_title\")");
    binding.setId(7023114029744795792L);
    assertNull(binding.getVersion());

    List<PSTemplateBinding> bindings = new ArrayList<>();
    bindings.add(binding);
    template.setBindings(bindings);

    PSAssemblyService.ensureOptimisticLockVersions(template);

    assertNull(template.getVersion());
    assertNull(binding.getVersion());
    assertEquals(7023114029744795792L, binding.getBindingId());
  }

  @Test
  void ensureOptimisticLockVersions_preservesExistingVersionsAndIds() {
    PSAssemblyTemplate template = new PSAssemblyTemplate();
    template.setGUID(new PSGuid(PSTypeEnum.TEMPLATE, 1003L));
    template.setVersion(3);

    PSTemplateBinding binding = new PSTemplateBinding(1, "sys_title", "x");
    binding.setId(1L);
    binding.setVersion(5);
    template.setBindings(List.of(binding));

    PSAssemblyService.ensureOptimisticLockVersions(template);

    assertEquals(3, template.getVersion());
    assertEquals(5, binding.getVersion());
    assertEquals(1L, binding.getBindingId());
  }

  @Test
  void ensureOptimisticLockVersions_nullTemplateIsNoOp() {
    PSAssemblyService.ensureOptimisticLockVersions(null);
  }
}
