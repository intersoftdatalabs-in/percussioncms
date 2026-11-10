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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.pubserver.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.pubserver.data.PSPublishServerProperty;
import com.percussion.rx.publisher.IPSRxPublisherService;
import com.percussion.security.PSEncryptor;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.pubserver.IPSDatabasePubServerFilesService;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.pubserver.IPSPubServerService.PSPubServerServiceException;
import com.percussion.sitemanage.dao.IPSSitePublishDao;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSitePublishStatusService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.io.PathUtils;
import com.percussion.utils.service.IPSUtilityService;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Partial-PUT dispatch on {@link PSPubServerService#updatePubServer} after the complexity
 * extraction (issue #5778). The grouped dispatch helpers must route exactly as the previous
 * inline chain: folder-only bodies store only the folder, missing servers still fail before any
 * write, and blank ids are still rejected before dispatch.
 */
class PSPubServerServicePartialDispatchTest {

  @Test
  void folderOnlyPutStoresFolderAndSaves() throws Exception {
    IPSPubServerDao pubServerDao = mock(IPSPubServerDao.class);
    IPSSiteManager siteMgr = mock(IPSSiteManager.class);
    IPSSiteDataService siteDataService = mock(IPSSiteDataService.class);
    IPSGuidManager guidMgr = mock(IPSGuidManager.class);

    IPSGuid siteGuid = mock(IPSGuid.class);
    IPSGuid serverGuid = mock(IPSGuid.class);
    when(guidMgr.makeGuid("101", PSTypeEnum.SITE)).thenReturn(siteGuid);
    when(guidMgr.makeGuid("9", PSTypeEnum.PUBLISHING_SERVER)).thenReturn(serverGuid);

    PSPubServer stored = localServer(9L, "local-files", "/sites/old");
    when(pubServerDao.loadPubServerModifiable(serverGuid)).thenReturn(stored);

    IPSSite site = mock(IPSSite.class);
    when(site.getGUID()).thenReturn(siteGuid);
    when(site.getName()).thenReturn("Corporate_Investments");
    when(site.isPageBased()).thenReturn(false);
    when(site.getDefaultPubServer()).thenReturn(3L);
    when(siteMgr.findSite(siteGuid)).thenReturn(site);
    when(siteDataService.getBasePublishingRoot(anyString(), anyString()))
        .thenAnswer(inv -> inv.getArgument(0));

    PSPubServerService service =
        spyService(
            pubServerDao,
            siteMgr,
            siteDataService,
            guidMgr,
            mock(IPSContentChangeService.class));

    PSPublishServerInfo body = new PSPublishServerInfo();
    body.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "  /sites/new  ")));

    PSPublishServerInfo result = service.updatePubServer("101", "9", body);

    ArgumentCaptor<PSPubServer> saved = ArgumentCaptor.forClass(PSPubServer.class);
    verify(pubServerDao).savePubServer(saved.capture());
    assertEquals("/sites/new", saved.getValue().getPropertyValue(
        IPSPubServerDao.PUBLISH_FOLDER_PROPERTY).orElse(""));
    assertEquals("local-files", saved.getValue().getName());
    assertEquals(
        "Local",
        saved.getValue().getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse(""));
    assertEquals("local-files", result.getServerName());
  }

  @Test
  void missingServerStillThrowsBeforeWrite() throws Exception {
    IPSPubServerDao pubServerDao = mock(IPSPubServerDao.class);
    IPSSiteManager siteMgr = mock(IPSSiteManager.class);
    IPSGuidManager guidMgr = mock(IPSGuidManager.class);

    IPSGuid siteGuid = mock(IPSGuid.class);
    IPSGuid serverGuid = mock(IPSGuid.class);
    when(guidMgr.makeGuid("101", PSTypeEnum.SITE)).thenReturn(siteGuid);
    when(guidMgr.makeGuid("9", PSTypeEnum.PUBLISHING_SERVER)).thenReturn(serverGuid);

    IPSSite site = mock(IPSSite.class);
    when(site.getGUID()).thenReturn(siteGuid);
    when(siteMgr.findSite(siteGuid)).thenReturn(site);
    when(pubServerDao.loadPubServerModifiable(serverGuid))
        .thenThrow(new RuntimeException("gone"));

    PSPubServerService service =
        spyService(
            pubServerDao,
            siteMgr,
            mock(IPSSiteDataService.class),
            guidMgr,
            mock(IPSContentChangeService.class));

    PSPublishServerInfo body = new PSPublishServerInfo();
    body.setProperties(List.of(property(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/sites/new")));

    assertThrows(
        PSPubServerServiceException.class, () -> service.updatePubServer("101", "9", body));
    verify(pubServerDao, never()).savePubServer(any());
  }

  @Test
  void blankIdsRejectedBeforeDispatch() throws Exception {
    IPSPubServerDao pubServerDao = mock(IPSPubServerDao.class);
    IPSSiteManager siteMgr = mock(IPSSiteManager.class);
    IPSGuidManager guidMgr = mock(IPSGuidManager.class);

    PSPubServerService service =
        spyService(
            pubServerDao,
            siteMgr,
            mock(IPSSiteDataService.class),
            guidMgr,
            mock(IPSContentChangeService.class));

    PSPublishServerInfo body = new PSPublishServerInfo();
    body.setProperties(List.of(property(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/sites/new")));

    assertThrows(
        IllegalArgumentException.class, () -> service.updatePubServer("  ", "9", body));
    assertThrows(
        IllegalArgumentException.class, () -> service.updatePubServer("101", "  ", body));
    verifyNoInteractions(pubServerDao, siteMgr);
  }

  private static PSPubServer localServer(long id, String name, String folder) {
    PSPubServer server = new PSPubServer();
    server.setServerId(id);
    server.setName(name);
    server.setPublishType("filesystem");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, folder);
    return server;
  }

  private static PSPublishServerProperty property(String key, String value) {
    PSPublishServerProperty property = new PSPublishServerProperty();
    property.setKey(key);
    property.setValue(value);
    return property;
  }

  private static PSPubServerService spyService(
      IPSPubServerDao pubServerDao,
      IPSSiteManager siteMgr,
      IPSSiteDataService siteDataService,
      IPSGuidManager guidMgr,
      IPSContentChangeService contentChangeService)
      throws Exception {
    File rxDir = Path.of("tmp", "perc-partial-dispatch-rx").toFile();
    try (MockedStatic<PathUtils> paths = mockStatic(PathUtils.class);
        MockedStatic<PSEncryptor> encryptor = mockStatic(PSEncryptor.class)) {
      paths.when(() -> PathUtils.getRxDir(null)).thenReturn(rxDir);
      PSEncryptor instance = mock(PSEncryptor.class);
      encryptor.when(() -> PSEncryptor.getInstance(eq("AES"), anyString())).thenReturn(instance);
      return spy(
          new PSPubServerService(
              pubServerDao,
              siteMgr,
              mock(IPSDatabasePubServerFilesService.class),
              siteDataService,
              mock(IPSSitePublishStatusService.class),
              guidMgr,
              mock(IPSRxPublisherService.class),
              mock(IPSSitePublishDao.class),
              mock(IPSPublisherService.class),
              contentChangeService,
              mock(IPSUtilityService.class)));
    }
  }
}
