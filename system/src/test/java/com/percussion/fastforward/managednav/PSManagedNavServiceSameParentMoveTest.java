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
package com.percussion.fastforward.managednav;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Sibling Move up/down must compare parent by content id (not revisioned
 * toString) and must not checkout the parent navon (#3797).
 */
class PSManagedNavServiceSameParentMoveTest {

  @Mock private IPSContentWs contentWs;
  @Mock private IPSContentDesignWs contentDsWs;
  @Mock private IPSAssemblyService asmService;
  @Mock private IPSGuidManager guidMgr;
  @Mock private IPSCmsObjectMgr cmsMgr;

  private PSManagedNavService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = spy(new PSManagedNavService(contentWs, contentDsWs, asmService, guidMgr, cmsMgr));
  }

  @Test
  void sameNavonContentIdIgnoresRevision() {
    IPSGuid revMinusOne = new PSLegacyGuid(42, -1);
    IPSGuid revOne = new PSLegacyGuid(42, 1);
    IPSGuid other = new PSLegacyGuid(43, 1);
    assertTrue(PSManagedNavService.sameNavonContentId(revMinusOne, revOne));
    assertTrue(PSManagedNavService.sameNavonContentId(revOne, revOne));
    assertFalse(PSManagedNavService.sameNavonContentId(revOne, other));
    assertFalse(PSManagedNavService.sameNavonContentId(revOne, null));
    assertFalse(PSManagedNavService.sameNavonContentId(null, revOne));
  }

  @Test
  void moveNavonSameParentDoesNotPrepareForEdit() throws Exception {
    IPSGuid src = new PSLegacyGuid(11, 1);
    IPSGuid parentTreeId = new PSLegacyGuid(10, -1);
    IPSGuid parentHead = new PSLegacyGuid(10, 1);
    doNothing().when(service).rearrangeSameParentChild(any(), any(), anyInt());

    service.moveNavon(src, parentTreeId, parentHead, 1);

    verify(service).rearrangeSameParentChild(eq(src), eq(parentHead), eq(1));
    verify(contentWs, never()).prepareForEdit(anyList());
    verify(contentWs, never()).releaseFromEdit(anyList(), anyBoolean());
  }

  @Test
  void moveNavonDifferentParentDoesNotTakeSameParentShortcut() throws Exception {
    IPSGuid src = new PSLegacyGuid(11, 1);
    IPSGuid srcParent = new PSLegacyGuid(10, -1);
    IPSGuid target = new PSLegacyGuid(20, 1);
    doNothing().when(service).rearrangeSameParentChild(any(), any(), anyInt());

    try {
      service.moveNavon(src, srcParent, target, 0);
    } catch (RuntimeException ignored) {
      // prepareForEdit / loadComponentSummary not stubbed — reparent path
    }

    verify(service, never()).rearrangeSameParentChild(any(), any(), anyInt());
  }
}
