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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
 * Landing attach on H2 sample percNavon must not fail closed on prepareForEdit NPE
 * (#3676 / parent #3092).
 */
class PSManagedNavServiceLandingAttachTest {

  @Mock private IPSContentWs contentWs;
  @Mock private IPSContentDesignWs contentDsWs;
  @Mock private IPSAssemblyService asmService;
  @Mock private IPSGuidManager guidMgr;
  @Mock private IPSCmsObjectMgr cmsMgr;

  private PSManagedNavService service;
  private IPSGuid navonId;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new PSManagedNavService(contentWs, contentDsWs, asmService, guidMgr, cmsMgr);
    navonId = new PSLegacyGuid(9001, 1);
  }

  @Test
  void skipsPrepareForEditOnSampleWorkflowNpe() {
    when(contentWs.prepareForEdit(navonId)).thenThrow(new NullPointerException());

    service.prepareForEditIgnoringSampleWorkflow(navonId);

    verify(contentWs).prepareForEdit(navonId);
  }

  @Test
  void skipsPrepareForEditWhenContentStateFieldMissing() {
    when(contentWs.prepareForEdit(navonId))
        .thenThrow(new IllegalStateException("Field sys_contentstateid not found"));

    service.prepareForEditIgnoringSampleWorkflow(navonId);

    verify(contentWs).prepareForEdit(navonId);
  }

  @Test
  void rethrowsNonSamplePrepareForEditFailure() {
    doThrow(new IllegalStateException("duplicate slot")).when(contentWs).prepareForEdit(navonId);

    assertThrows(
        IllegalStateException.class, () -> service.prepareForEditIgnoringSampleWorkflow(navonId));
  }
}
