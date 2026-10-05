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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSItemField;
import com.percussion.cms.objectstore.PSTextValue;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import com.percussion.recent.service.rest.IPSRecentService;
import com.percussion.redirect.service.IPSRedirectService;
import com.percussion.rest.errors.BackendException;
import com.percussion.rest.errors.FolderNotFoundException;
import com.percussion.rest.errors.NotAuthorizedException;
import com.percussion.rest.folders.ItemProperties;
import com.percussion.services.content.data.PSItemStatus;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSiteSectionService;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class FolderAdaptorSaveItemPropertiesTest {

  @Mock private IPSPathService pathService;
  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSSiteSectionService sectionService;
  @Mock private IPSManagedNavService navSrv;
  @Mock private IPSIdMapper idMapper;
  @Mock private IPSPageService pageService;
  @Mock private IPSTemplateService templateService;
  @Mock private IPSPageDaoHelper pageDaoHelper;
  @Mock private IPSPageDao pageDao;
  @Mock private IPSContentWs contentService;
  @Mock private IPSRenderAssemblyBridge asmBridge;
  @Mock private IPSUserService userService;
  @Mock private IPSRedirectService redirectService;
  @Mock private IPSSiteDataService siteDataService;
  @Mock private IPSRecentService recentService;

  private FolderAdaptor adaptor;
  private final URI base = URI.create("http://localhost/rest");

  @BeforeEach
  void setUp() throws Exception {
    adaptor =
        new FolderAdaptor(
            pathService,
            folderHelper,
            sectionService,
            navSrv,
            idMapper,
            pageService,
            templateService,
            pageDaoHelper,
            pageDao,
            contentService,
            asmBridge,
            userService,
            redirectService,
            siteDataService,
            recentService);
    PSCurrentUser admin = new PSCurrentUser();
    admin.setName("admin1");
    when(userService.getCurrentUser()).thenReturn(admin);
    when(userService.isAdminUser("admin1")).thenReturn(true);
  }

  @Test
  void saveItemPropertiesThrowsNotAuthorizedWhenUserIsNotAdmin() {
    when(userService.isAdminUser("admin1")).thenReturn(false);
    assertThrows(
        NotAuthorizedException.class,
        () -> adaptor.saveItemProperties(base, "/Assets/src/item", "n", "t"));
  }

  @Test
  void saveItemPropertiesMapsMissingSourceToFolderNotFound() throws Exception {
    when(folderHelper.findItem("//Folders/$System$/Assets/missing/item"))
        .thenThrow(new PSPathNotFoundServiceException("missing"));
    assertThrows(
        FolderNotFoundException.class,
        () -> adaptor.saveItemProperties(base, "/Assets/missing/item", "n", "t"));
  }

  @Test
  void saveItemPropertiesRejectsBlankNameAs400() {
    WebApplicationException thrown =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.saveItemProperties(base, "/Assets/src/item", "  ", "t"));
    assertEquals(
        jakarta.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode(),
        thrown.getResponse().getStatus());
  }

  @Test
  void saveItemPropertiesPersistsNameAndDisplayTitle() throws Exception {
    PSDataItemSummary source = new PSDataItemSummary();
    source.setId("1-101-7");
    source.setType("percSimpleTextAsset");
    source.setName("old");
    when(folderHelper.findItem("//Folders/$System$/Assets/src/item")).thenReturn(source);
    PSLegacyGuid guid = new PSLegacyGuid(101, 1);
    when(idMapper.getGuid("1-101-7")).thenReturn(guid);
    PSItemStatus status = mock(PSItemStatus.class);
    when(contentService.prepareForEdit(guid)).thenReturn(status);
    PSCoreItem core = mock(PSCoreItem.class);
    when(contentService.loadItems(anyList(), eq(false), eq(false), eq(false), eq(false)))
        .thenReturn(Collections.singletonList(core));
    when(contentService.getIdByPath("//Folders/$System$/Assets/src")).thenReturn(guid);
    when(contentService.saveItems(anyList(), eq(false), eq(false), eq(guid)))
        .thenReturn(List.of(guid));

    ItemProperties saved =
        adaptor.saveItemProperties(base, "/Assets/src/item", "qa-name", "qa-title");

    assertEquals("qa-name", saved.getName());
    assertEquals("qa-title", saved.getDisplayTitle());
    verify(core).setTextField("sys_title", "qa-name");
    verify(core).setTextField("displaytitle", "qa-title");
    verify(contentService).releaseFromEdit(status, false);
  }

  @Test
  void saveItemPropertiesResolvesItemInsideRequiresNew() throws Exception {
    PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    TransactionStatus txStatus = mock(TransactionStatus.class);
    AtomicBoolean inNewTx = new AtomicBoolean(false);
    when(tm.getTransaction(any(TransactionDefinition.class)))
        .thenAnswer(
            invocation -> {
              TransactionDefinition def = invocation.getArgument(0);
              assertEquals(
                  TransactionDefinition.PROPAGATION_REQUIRES_NEW, def.getPropagationBehavior());
              inNewTx.set(true);
              return txStatus;
            });
    Field txField = FolderAdaptor.class.getDeclaredField("transactionManager");
    txField.setAccessible(true);
    txField.set(adaptor, tm);

    PSDataItemSummary source = new PSDataItemSummary();
    source.setId("1-101-7");
    source.setType("percSimpleTextAsset");
    source.setName("old");
    when(folderHelper.findItem("//Folders/$System$/Assets/src/item"))
        .thenAnswer(
            invocation -> {
              assertTrue(inNewTx.get(), "findItem must run inside REQUIRES_NEW");
              return source;
            });
    PSLegacyGuid guid = new PSLegacyGuid(101, 1);
    when(idMapper.getGuid("1-101-7")).thenReturn(guid);
    PSItemStatus status = mock(PSItemStatus.class);
    when(contentService.prepareForEdit(guid)).thenReturn(status);
    PSCoreItem core = mock(PSCoreItem.class);
    when(contentService.loadItems(anyList(), eq(false), eq(false), eq(false), eq(false)))
        .thenReturn(Collections.singletonList(core));
    when(contentService.getIdByPath("//Folders/$System$/Assets/src")).thenReturn(guid);
    when(contentService.saveItems(anyList(), eq(false), eq(false), eq(guid)))
        .thenReturn(List.of(guid));

    adaptor.saveItemProperties(base, "/Assets/src/item", "qa-name", "qa-title");

    verify(tm, atLeastOnce()).commit(txStatus);
    verify(core).setTextField("displaytitle", "qa-title");
  }

  @Test
  void saveItemPropertiesDoesNotSucceedWhenFieldWriteRollsBack() throws Exception {
    // Commits: 1 resolve, 2 preview load, 3 field save. The save commit is the one that must fail.
    transactionManagerRollingBackOnCommit(3);
    stubSavableItem();

    BackendException thrown =
        assertThrows(
            BackendException.class,
            () -> adaptor.saveItemProperties(base, "/Assets/src/item", "qa-name", "qa-title"));

    assertInstanceOf(UnexpectedRollbackException.class, thrown.getCause());
    verify(contentService).saveItems(anyList(), eq(false), eq(false), any());
    verify(contentService).releaseFromEdit(any(PSItemStatus.class), eq(false));
  }

  private PlatformTransactionManager transactionManagerRollingBackOnCommit(int failingCommit)
      throws Exception {
    PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    TransactionStatus txStatus = mock(TransactionStatus.class);
    AtomicInteger commits = new AtomicInteger();
    when(tm.getTransaction(any(TransactionDefinition.class)))
        .thenAnswer(
            invocation -> {
              TransactionDefinition def = invocation.getArgument(0);
              assertEquals(
                  TransactionDefinition.PROPAGATION_REQUIRES_NEW, def.getPropagationBehavior());
              return txStatus;
            });
    doAnswer(
            invocation -> {
              if (commits.incrementAndGet() == failingCommit) {
                throw new UnexpectedRollbackException("rollback-only");
              }
              return null;
            })
        .when(tm)
        .commit(any(TransactionStatus.class));
    Field txField = FolderAdaptor.class.getDeclaredField("transactionManager");
    txField.setAccessible(true);
    txField.set(adaptor, tm);
    return tm;
  }

  private void stubSavableItem() throws Exception {
    PSDataItemSummary source = new PSDataItemSummary();
    source.setId("1-101-7");
    source.setType("percSimpleTextAsset");
    source.setName("old");
    when(folderHelper.findItem("//Folders/$System$/Assets/src/item")).thenReturn(source);
    PSLegacyGuid guid = new PSLegacyGuid(101, 1);
    when(idMapper.getGuid("1-101-7")).thenReturn(guid);
    PSItemStatus status = mock(PSItemStatus.class);
    when(contentService.prepareForEdit(guid)).thenReturn(status);
    PSCoreItem core = mock(PSCoreItem.class);
    when(contentService.loadItems(anyList(), eq(false), eq(false), eq(false), eq(false)))
        .thenReturn(Collections.singletonList(core));
    when(contentService.getIdByPath("//Folders/$System$/Assets/src")).thenReturn(guid);
    when(contentService.saveItems(anyList(), eq(false), eq(false), eq(guid)))
        .thenReturn(List.of(guid));
  }

  @Test
  void getItemPropertiesReturnsNameFromSummary() throws Exception {
    PSDataItemSummary source = new PSDataItemSummary();
    source.setId("1-101-7");
    source.setType("percSimpleTextAsset");
    source.setName("listed");
    when(folderHelper.findItem("//Folders/$System$/Assets/src/item")).thenReturn(source);
    PSLegacyGuid guid = new PSLegacyGuid(101, 1);
    when(idMapper.getGuid("1-101-7")).thenReturn(guid);
    PSCoreItem core = mock(PSCoreItem.class);
    when(core.getFieldByName("displaytitle")).thenReturn(null);
    when(contentService.loadItems(anyList(), eq(false), eq(false), eq(false), eq(false)))
        .thenReturn(Collections.singletonList(core));

    ItemProperties loaded = adaptor.getItemProperties(base, "/Assets/src/item");
    assertEquals("listed", loaded.getName());
  }

  @Test
  void getItemPropertiesReadsDisplayTitleTextNotObjectIdentity() throws Exception {
    PSDataItemSummary source = new PSDataItemSummary();
    source.setId("1-101-7");
    source.setType("percFileAsset");
    source.setName("listed");
    when(folderHelper.findItem("//Folders/$System$/Assets/src/item")).thenReturn(source);
    PSLegacyGuid guid = new PSLegacyGuid(101, 1);
    when(idMapper.getGuid("1-101-7")).thenReturn(guid);
    PSCoreItem core = mock(PSCoreItem.class);
    PSItemField field = mock(PSItemField.class);
    PSTextValue stored = new PSTextValue("Home banner");
    when(field.getValue()).thenReturn(stored);
    when(core.getFieldByName("displaytitle")).thenReturn(field);
    when(contentService.loadItems(anyList(), eq(false), eq(false), eq(false), eq(false)))
        .thenReturn(Collections.singletonList(core));

    ItemProperties loaded = adaptor.getItemProperties(base, "/Assets/src/item");

    assertEquals("listed", loaded.getName());
    assertEquals("Home banner", loaded.getDisplayTitle());
  }
}
