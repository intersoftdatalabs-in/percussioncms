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
package com.percussion.activity.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.dao.IPSResourceDefinitionGroupDao;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.system.IPSSystemService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Empty-cell behavior for Home gadgets activity: a fresh H2 cell has no sites, so unknown paths
 * must yield empty node lists (HTTP 200 downstream), not a RuntimeException (HTTP 500).
 */
@ExtendWith(MockitoExtension.class)
class PSActivityServiceEmptyCellTest {

  @Mock private IPSSiteManager siteMgr;
  @Mock private IPSResourceDefinitionGroupDao resDao;
  @Mock private PSItemDefManager itemDefMgr;
  @Mock private IPSPublisherService pub;
  @Mock private IPSContentMgr contentMgr;
  @Mock private IPSSystemService sysSrv;
  @Mock private IPSContentWs contentWs;
  @Mock private IPSItemWorkflowService itemWfSrvc;
  @Mock private IPSGuidManager guidMgr;
  @Mock private IPSIdMapper idMapper;

  private PSActivityService activityService;

  @BeforeEach
  void setUp() {
    activityService =
        new PSActivityService(
            siteMgr, resDao, itemDefMgr, pub, contentMgr, sysSrv, contentWs, itemWfSrvc, guidMgr,
            idMapper);
  }

  @Test
  void createActivityNodesByPaths_unknownPath_returnsEmptyList() {
    when(siteMgr.findAllSites()).thenReturn(Collections.emptyList());

    var nodes = activityService.createActivityNodesByPaths("/Sites/Corporate_Investments", true);

    assertNotNull(nodes);
    assertTrue(nodes.isEmpty());
  }

  @Test
  void createActivityNodesByPaths_sitesRootNoSites_returnsEmptyList() {
    when(siteMgr.findAllSites()).thenReturn(Collections.emptyList());

    var nodes = activityService.createActivityNodesByPaths("/Sites", true);

    assertNotNull(nodes);
    assertTrue(nodes.isEmpty());
  }
}
