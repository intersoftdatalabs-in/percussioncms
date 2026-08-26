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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** In-process Preview assemble must {@code normalize()} the work item (#3809). */
@Tag("UnitTest")
class PSRenderAssemblyBridgeNormalizeTest {

  @Test
  @DisplayName("normalizeForPreview calls IPSAssemblyItem.normalize")
  void callsNormalize() throws Exception {
    IPSAssemblyItem work = mock(IPSAssemblyItem.class);
    PSRenderAssemblyBridge.normalizeForPreview(work);
    verify(work).normalize();
  }

  @Test
  @DisplayName("null work item is rejected")
  void nullWork() {
    assertThrows(NullPointerException.class, () -> PSRenderAssemblyBridge.normalizeForPreview(null));
  }

  @Test
  @DisplayName("lazy site templates fall back to unmodifiable load then empty")
  void associatedTemplatesForPreviewLazy() throws Exception {
    IPSSiteManager siteManager = mock(IPSSiteManager.class);
    PSRenderAssemblyBridge bridge =
        new PSRenderAssemblyBridge(
            mock(IPSAssemblyService.class),
            mock(IPSContentDesignWs.class),
            mock(IPSContentWs.class),
            mock(IPSIdMapper.class),
            siteManager,
            mock(IPSCmsObjectMgr.class));
    IPSSite lazy = mock(IPSSite.class);
    IPSGuid guid = mock(IPSGuid.class);
    when(lazy.getGUID()).thenReturn(guid);
    when(lazy.getAssociatedTemplates())
        .thenThrow(new RuntimeException("Cannot lazily initialize collection (no session)"));
    IPSSite loaded = mock(IPSSite.class);
    when(loaded.getAssociatedTemplates()).thenReturn(Set.of());
    when(siteManager.loadUnmodifiableSite(guid)).thenReturn(loaded);
    assertTrue(bridge.associatedTemplatesForPreview(lazy).isEmpty());
    verify(siteManager).loadUnmodifiableSite(guid);
  }
}
