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
package com.percussion.services.virtualsite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.sitemgr.data.PSSite;
import com.percussion.services.sitemgr.data.PSSiteProperty;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PSVirtualSiteHelperTest {

  @Test
  void repositoryWhenNoSourceKind() {
    PSSite site = mock(PSSite.class);
    when(site.getProperties()).thenReturn(Set.of());
    assertEquals(SourceKind.REPOSITORY, PSVirtualSiteHelper.sourceKind(site));
    assertFalse(PSVirtualSiteHelper.isVirtual(site));
  }

  @Test
  void virtualWhenGitFilesystem() {
    PSSite site = mock(PSSite.class);
    PSSiteProperty kind = new PSSiteProperty();
    kind.setName(PSVirtualSiteHelper.PROP_SOURCE_KIND);
    kind.setValue("git-filesystem");
    PSSiteProperty root = new PSSiteProperty();
    root.setName(PSVirtualSiteHelper.PROP_ROOT_PATH);
    root.setValue("C:/docs/product-docs");
    Set<PSSiteProperty> props = new HashSet<>();
    props.add(kind);
    props.add(root);
    when(site.getProperties()).thenReturn(props);
    when(site.getName()).thenReturn("Help");

    assertTrue(PSVirtualSiteHelper.isVirtual(site));
    assertEquals(
        VirtualSiteSourceType.GIT_FILESYSTEM,
        PSVirtualSiteHelper.virtualSourceType(site).orElseThrow());
    assertEquals("Help", PSVirtualSiteHelper.siteKey(site));
    assertTrue(PSVirtualSiteHelper.rootPath(site).isPresent());
  }
}
