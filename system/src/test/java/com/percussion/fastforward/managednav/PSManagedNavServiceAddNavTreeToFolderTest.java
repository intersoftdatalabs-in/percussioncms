/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.NavigationErrorCodes;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Create Site managed-nav seeds a NavTree without requiring a successful
 * workflow check-in (#3364). Folder-already-has-nav is a client error.
 */
class PSManagedNavServiceAddNavTreeToFolderTest {

  @Mock private IPSContentWs contentWs;
  @Mock private IPSContentDesignWs contentDsWs;
  @Mock private IPSAssemblyService asmService;
  @Mock private IPSGuidManager guidMgr;
  @Mock private IPSCmsObjectMgr cmsMgr;
  @Mock private PSCoreItem coreItem;

  private PSManagedNavService service;
  private IPSGuid created;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    service =
        Mockito.spy(
            new PSManagedNavService(contentWs, contentDsWs, asmService, guidMgr, cmsMgr));
    created = new PSLegacyGuid(7001, 1);
    doReturn(null).when(service).findNavSummary("//Sites/QaSite3364");
    doReturn(List.of("percNavTree")).when(service).getNavTreeContentTypeNames();
    when(contentWs.createItems("percNavTree", 1)).thenReturn(List.of(coreItem));
    when(coreItem.getFieldByName("sys_contentstartdate")).thenReturn(null);
  }

  @Test
  void savesWithoutForcedCheckinAndAttachesToFolder() throws Exception {
    when(contentWs.saveItems(any(), eq(false), eq(false))).thenReturn(List.of(created));

    IPSGuid result =
        service.addNavTreeToFolder("//Sites/QaSite3364", "QaSite3364-NavTree", "Home", 4);

    assertSame(created, result);
    verify(coreItem).setTextField("sys_title", "QaSite3364-NavTree");
    verify(coreItem).setTextField("displaytitle", "Home");
    verify(coreItem).setTextField("sys_workflowid", "4");
    verify(contentWs).saveItems(any(), eq(false), eq(false));
    verify(contentWs, never()).saveItems(any(), eq(false), eq(true));
    verify(contentWs).addFolderChildren(eq("//Sites/QaSite3364"), eq(List.of(created)));
    verify(contentWs, never()).checkinItems(any(), eq(null));
  }

  @Test
  void doesNotCheckinSoSurroundingSiteCreateTransactionStaysWritable() throws Exception {
    when(contentWs.saveItems(any(), eq(false), eq(false))).thenReturn(List.of(created));

    IPSGuid result =
        service.addNavTreeToFolder("//Sites/QaSite3364", "QaSite3364-NavTree", "Home", -1);

    assertSame(created, result);
    verify(contentWs).addFolderChildren(eq("//Sites/QaSite3364"), eq(List.of(created)));
    verify(contentWs, never()).checkinItems(any(), eq(null));
    verify(coreItem, never()).setTextField(eq("sys_workflowid"), any());
  }

  @Test
  void rejectsFolderThatAlreadyHasNavTree() {
    PSComponentSummary existing = Mockito.mock(PSComponentSummary.class);
    when(existing.getContentTypeId()).thenReturn(99L);
    doReturn(existing).when(service).findNavSummary("//Sites/QaSite3364");
    doReturn(List.of(301L)).when(service).getNavonContentTypeIds();

    PSNavException thrown =
        assertThrows(
            PSNavException.class,
            () ->
                service.addNavTreeToFolder(
                    "//Sites/QaSite3364", "QaSite3364-NavTree", "Home", -1));
    assertSame(
        NavigationErrorCodes.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVTREE,
        thrown.getTypedErrorCode());
    assertEquals(
        NavigationErrorCodes.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVTREE
            .numericCode(),
        thrown.getErrorCode());
    assertFalse(thrown.isAuditable());
  }

  @Test
  void rejectsFolderThatAlreadyHasNavon() {
    PSComponentSummary existing = Mockito.mock(PSComponentSummary.class);
    when(existing.getContentTypeId()).thenReturn(301L);
    doReturn(existing).when(service).findNavSummary("//Sites/QaSite3364");
    doReturn(List.of(301L)).when(service).getNavonContentTypeIds();

    PSNavException thrown =
        assertThrows(
            PSNavException.class,
            () ->
                service.addNavTreeToFolder(
                    "//Sites/QaSite3364", "QaSite3364-NavTree", "Home", -1));
    assertSame(
        NavigationErrorCodes.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVON,
        thrown.getTypedErrorCode());
    assertEquals(
        NavigationErrorCodes.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVON
            .numericCode(),
        thrown.getErrorCode());
    assertFalse(thrown.isAuditable());
  }

  @Test
  void wrapsSaveFailureAsNavException() throws Exception {
    when(contentWs.saveItems(any(), eq(false), eq(false)))
        .thenThrow(new PSErrorResultsException());

    PSNavException thrown =
        assertThrows(
            PSNavException.class,
            () ->
                service.addNavTreeToFolder(
                    "//Sites/QaSite3364", "QaSite3364-NavTree", "Home", -1));
    assertSame(
        NavigationErrorCodes.NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER,
        thrown.getTypedErrorCode());
    assertEquals(
        NavigationErrorCodes.NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER.numericCode(),
        thrown.getErrorCode());
    assertFalse(thrown.isAuditable());
    assertEquals(PSErrorResultsException.class, thrown.getCause().getClass());
    verify(contentWs, never()).addFolderChildren(anyString(), any());
  }

  @Test
  void rejectsMissingNavTreeContentType() {
    doReturn(Collections.emptyList()).when(service).getNavTreeContentTypeNames();

    PSNavException thrown =
        assertThrows(
            PSNavException.class,
            () ->
                service.addNavTreeToFolder(
                    "//Sites/QaSite3364", "QaSite3364-NavTree", "Home", -1));
    assertSame(
        NavigationErrorCodes.NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER,
        thrown.getTypedErrorCode());
    assertEquals(
        NavigationErrorCodes.NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER.numericCode(),
        thrown.getErrorCode());
    assertFalse(thrown.isAuditable());
  }
}
