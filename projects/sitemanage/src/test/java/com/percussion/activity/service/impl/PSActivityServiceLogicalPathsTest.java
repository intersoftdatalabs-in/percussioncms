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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.dao.IPSResourceDefinitionGroupDao;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.system.IPSSystemService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CMS-logical path routing for activity nodes (issue #6325).
 *
 * <p>The string joins in {@code createActivityNodesByPaths}/{@code createActivityChildNodes}
 * ({@code "/.system"}, {@code SITES_FINDER_ROOT + "/"}, {@code "/" + path}, {@code path + "/"},
 * {@code '/' + path}) concatenate CMS content-repository paths ({@code //Sites/...}), never OS
 * filesystem paths — so they are intentionally not {@code Path.resolve}. These tests pin the
 * logical-path routing each suppressed sink implements.
 */
@ExtendWith(MockitoExtension.class)
class PSActivityServiceLogicalPathsTest {

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
  private IPSSite site;

  @BeforeEach
  void setUp() {
    activityService =
        new PSActivityService(
            siteMgr, resDao, itemDefMgr, pub, contentMgr, sysSrv, contentWs, itemWfSrvc, guidMgr,
            idMapper);
    site = mock(IPSSite.class);
    // Lenient: not every routing test reaches the site node mapping or the child lookup.
    lenient().when(site.getName()).thenReturn("Demo");
    when(site.getFolderRoot()).thenReturn("//Sites/Demo");
    when(siteMgr.findAllSites()).thenReturn(List.of(site));
    when(contentWs.getIdByPath(anyString())).thenReturn(mock(IPSGuid.class));
    lenient()
        .when(contentWs.findFolderChildren(anyString(), anyBoolean()))
        .thenReturn(Collections.emptyList());
  }

  @Test
  void sitesRoot_listsOneNodePerSite() {
    var nodes = activityService.createActivityNodesByPaths("/Sites/", true);

    assertNotNull(nodes);
    assertEquals(1, nodes.size());
    assertEquals("Demo", nodes.get(0).getName());
    assertEquals("//Sites/Demo", nodes.get(0).getPath());
  }

  @Test
  void exactFolderRoot_returnsSiteNode() {
    var nodes = activityService.createActivityNodesByPaths("//Sites/Demo", true);

    assertNotNull(nodes);
    assertEquals(1, nodes.size());
    assertEquals("//Sites/Demo", nodes.get(0).getPath());
  }

  @Test
  void singleSlashForm_matchesSingleSlashFolderRoot() {
    when(site.getFolderRoot()).thenReturn("/Sites/Demo");

    var nodes = activityService.createActivityNodesByPaths("Sites/Demo", true);

    assertNotNull(nodes);
    assertEquals(1, nodes.size());
    assertEquals("/Sites/Demo", nodes.get(0).getPath());
    // The logical child lookup normalizes to the "//"-rooted repository path.
    verify(contentWs).findFolderChildren("//Sites/Demo", false);
  }

  @Test
  void childPath_queriesRepositoryPathUnchanged() {
    var nodes = activityService.createActivityNodesByPaths("//Sites/Demo/About", false);

    assertNotNull(nodes);
    assertTrue(nodes.isEmpty());
    verify(contentWs).findFolderChildren("//Sites/Demo/About", false);
  }

  @Test
  void systemMarker_queriedAsRepositoryPath() {
    activityService.createActivityNodesByPaths("/Sites/", true);

    verify(contentWs).getIdByPath(argThat(p -> p != null && p.endsWith("/.system")));
  }

  @Test
  void unknownPathWithSites_returnsEmptyList() {
    var nodes = activityService.createActivityNodesByPaths("/Sites/Elsewhere", true);

    assertNotNull(nodes);
    assertTrue(nodes.isEmpty());
  }
}
