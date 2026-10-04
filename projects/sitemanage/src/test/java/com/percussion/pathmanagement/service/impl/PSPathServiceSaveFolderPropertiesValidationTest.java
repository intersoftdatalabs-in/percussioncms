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
package com.percussion.pathmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.pathmanagement.data.PSFolderProperties;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.ui.service.IPSUiService;
import com.percussion.ui.service.impl.PSCm1ListViewHelper;
import com.percussion.user.service.IPSUserService;
import com.percussion.webservices.publishing.IPSPublishingWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit coverage for {@link PSPathService#saveFolderProperties} null/blank id gates (#2749).
 *
 * <p>Prevents reintroduction of Apache Validate.notNull NPEs when the Jackson UNWRAP_ROOT_VALUE
 * body is missing or {@code id} is blank.
 */
@ExtendWith(MockitoExtension.class)
class PSPathServiceSaveFolderPropertiesValidationTest {

  @Mock IPSFolderHelper folderHelper;
  @Mock IPSIdMapper idMapper;
  @Mock IPSPublishingWs publishingWs;
  @Mock IPSUiService uiService;
  @Mock IPSUserService userService;
  @Mock PSCm1ListViewHelper listViewHelper;
  @Mock IPSRecycleService recycleService;

  PSPathService service;

  @BeforeEach
  void setUp() {
    service =
        new PSPathService(
            folderHelper,
            publishingWs,
            idMapper,
            uiService,
            userService,
            listViewHelper,
            recycleService);
  }

  @Test
  void nullBody_throwsValidationNotNpeOnGetGuid() throws Exception {
    Exception ex = assertThrows(Exception.class, () -> service.saveFolderProperties(null));
    assertTrue(
        ex instanceof PSValidationException
            || (ex.getMessage() != null && !ex.getMessage().isBlank()),
        "expected validation failure, got " + ex.getClass().getName() + ": " + ex.getMessage());
    verify(idMapper, never()).getGuid(org.mockito.ArgumentMatchers.anyString());
    verify(folderHelper, never()).saveFolderProperties(any(PSFolderProperties.class));
  }

  @Test
  void blankId_throwsValidationNotNpeOnGetGuid() throws Exception {
    PSFolderProperties props = new PSFolderProperties();
    props.setName("Design");
    props.setId("  ");
    Exception ex = assertThrows(Exception.class, () -> service.saveFolderProperties(props));
    assertTrue(
        ex instanceof PSValidationException
            || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("id")),
        "expected id validation, got " + ex);
    verify(idMapper, never()).getGuid(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void validId_delegatesToFolderHelper() throws Exception {
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    PSFolderPermission perm = new PSFolderPermission();
    perm.setAccessLevel(PSFolderPermission.Access.ADMIN);
    props.setPermission(perm);

    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(props);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
  }

  @Test
  void missingFolder_mapsToHttp404NotSuccess() throws Exception {
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-999999");
    props.setName("Gone");
    when(folderHelper.findFolderProperties(anyString()))
        .thenThrow(new RuntimeException("Cannot find folder with id"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveFolderProperties(props));
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
    verify(folderHelper, never()).saveFolderProperties(any(PSFolderProperties.class));
  }

  @Test
  void nonAdmin_mapsToHttp403NotSuccess() throws Exception {
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(props);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveFolderProperties(props));
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
    verify(folderHelper, never()).saveFolderProperties(any(PSFolderProperties.class));
  }

  @Test
  void unknownWorkflow_mapsToHttp400AndDoesNotSave() throws Exception {
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    props.setWorkflowId(99);
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(props);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(folderHelper.isAssignableFolderWorkflow(99)).thenReturn(false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveFolderProperties(props));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    verify(folderHelper, never()).saveFolderProperties(any(PSFolderProperties.class));
    verify(publishingWs, never()).getItemSites(any());
  }

  @Test
  void catalogWorkflow_stillDelegatesToFolderHelper() throws Exception {
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    props.setWorkflowId(5);
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(props);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(folderHelper.isAssignableFolderWorkflow(5)).thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
  }

  @Test
  void unknownCommunity_mapsToHttp400AndDoesNotSave() throws Exception {
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    props.setCommunityId(99);
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(props);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(folderHelper.isAssignableFolderCommunity(99)).thenReturn(false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveFolderProperties(props));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    verify(folderHelper, never()).saveFolderProperties(any(PSFolderProperties.class));
    verify(publishingWs, never()).getItemSites(any());
  }

  @Test
  void catalogCommunity_stillDelegatesToFolderHelper() throws Exception {
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    props.setCommunityId(12);
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(props);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(folderHelper.isAssignableFolderCommunity(12)).thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
  }

  @Test
  void nonPositiveCommunityId_doesNotRequireCatalog() throws Exception {
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    props.setCommunityId(0);
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(props);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
    verify(folderHelper, never()).isAssignableFolderCommunity(anyInt());
  }

  @Test
  void unknownLocale_mapsToHttp400AndDoesNotSave() throws Exception {
    PSFolderProperties stored = new PSFolderProperties();
    stored.setId("16777215-101-703");
    stored.setName("Design");
    stored.setLocale("en-us");
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    props.setLocale("zz-zz");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(folderHelper.isAssignableFolderLocale("zz-zz")).thenReturn(false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveFolderProperties(props));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    verify(folderHelper, never()).saveFolderProperties(any(PSFolderProperties.class));
    verify(publishingWs, never()).getItemSites(any());
  }

  @Test
  void catalogLocale_stillDelegatesToFolderHelper() throws Exception {
    PSFolderProperties stored = new PSFolderProperties();
    stored.setId("16777215-101-703");
    stored.setName("Design");
    stored.setLocale("en-us");
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    props.setLocale("fr-fr");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(folderHelper.isAssignableFolderLocale("fr-fr")).thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
  }

  @Test
  void unchangedLocale_doesNotRequireCatalog() throws Exception {
    PSFolderProperties stored = new PSFolderProperties();
    stored.setId("16777215-101-703");
    stored.setName("Design");
    stored.setLocale("en-us");
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    props.setLocale("EN-US");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
    verify(folderHelper, never()).isAssignableFolderLocale(anyString());
  }

  @Test
  void blankLocale_doesNotRequireCatalog() throws Exception {
    PSFolderProperties stored = new PSFolderProperties();
    stored.setId("16777215-101-703");
    stored.setName("Design");
    stored.setLocale("fr-fr");
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    props.setLocale("  ");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
    verify(folderHelper, never()).isAssignableFolderLocale(anyString());
  }

  @Test
  void unknownDisplayFormatId_mapsToHttp400AndDoesNotSave() throws Exception {
    PSFolderProperties stored = storedFolder();
    stored.setDisplayFormatId("3");
    PSFolderProperties props = storedFolder();
    props.setDisplayFormatId("99");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(folderHelper.isAssignableFolderDisplayFormat("99")).thenReturn(false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveFolderProperties(props));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    verify(folderHelper, never()).saveFolderProperties(any(PSFolderProperties.class));
    verify(publishingWs, never()).getItemSites(any());
  }

  @Test
  void nameOnlyDisplayFormat_isHttp400AndDoesNotSave() throws Exception {
    PSFolderProperties stored = storedFolder();
    stored.setDisplayFormatId("3");
    stored.setDisplayFormatName("Default");
    PSFolderProperties props = storedFolder();
    props.setDisplayFormatName("Simple");
    props.setDisplayFormatId("Simple");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveFolderProperties(props));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    verify(folderHelper, never()).saveFolderProperties(any(PSFolderProperties.class));
    verify(folderHelper, never()).isAssignableFolderDisplayFormat(anyString());
    verify(publishingWs, never()).getItemSites(any());
  }

  @Test
  void catalogDisplayFormat_stillDelegatesToFolderHelper() throws Exception {
    PSFolderProperties stored = storedFolder();
    stored.setDisplayFormatId("3");
    PSFolderProperties props = storedFolder();
    props.setDisplayFormatId("12");
    props.setDisplayFormatName("Simple");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(folderHelper.isAssignableFolderDisplayFormat("12")).thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
  }

  @Test
  void unchangedDisplayFormatId_doesNotRequireCatalog() throws Exception {
    PSFolderProperties stored = storedFolder();
    stored.setDisplayFormatId("12");
    PSFolderProperties props = storedFolder();
    props.setDisplayFormatId("012");
    props.setDisplayFormatName("Whatever");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
    verify(folderHelper, never()).isAssignableFolderDisplayFormat(anyString());
  }

  @Test
  void omittedDisplayFormatId_doesNotRequireCatalog() throws Exception {
    PSFolderProperties stored = storedFolder();
    stored.setDisplayFormatId("3");
    stored.setDisplayFormatName("Default");
    PSFolderProperties props = storedFolder();
    props.setDisplayFormatName("Simple");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
    verify(folderHelper, never()).isAssignableFolderDisplayFormat(anyString());
  }

  @Test
  void unknownAllowedSite_mapsToHttp400AndDoesNotSave() throws Exception {
    PSFolderProperties stored = storedFolder();
    stored.setAllowedSites("301");
    PSFolderProperties props = storedFolder();
    props.setAllowedSites("301,999");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(folderHelper.isAssignableFolderAllowedSites("301,999")).thenReturn(false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveFolderProperties(props));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    verify(folderHelper, never()).saveFolderProperties(any(PSFolderProperties.class));
    verify(publishingWs, never()).getItemSites(any());
  }

  @Test
  void siteNameIsNotAnAllowedSiteId() throws Exception {
    PSFolderProperties stored = storedFolder();
    stored.setAllowedSites("301");
    PSFolderProperties props = storedFolder();
    props.setAllowedSites("Enterprise");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveFolderProperties(props));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    verify(folderHelper, never()).saveFolderProperties(any(PSFolderProperties.class));
    verify(folderHelper, never()).isAssignableFolderAllowedSites(anyString());
    verify(publishingWs, never()).getItemSites(any());
  }

  @Test
  void changedAllowedSites_areCanonicalizedBeforeSave() throws Exception {
    PSFolderProperties stored = storedFolder();
    stored.setAllowedSites("301");
    PSFolderProperties props = storedFolder();
    props.setAllowedSites(" 302, 301 ");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(folderHelper.isAssignableFolderAllowedSites("301,302")).thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    assertEquals("301,302", props.getAllowedSites());
    verify(folderHelper).saveFolderProperties(props);
  }

  @Test
  void emptyAllowedSites_clearsWithoutCatalogCheck() throws Exception {
    PSFolderProperties stored = storedFolder();
    stored.setAllowedSites("301,302");
    PSFolderProperties props = storedFolder();
    props.setAllowedSites("  ");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    assertEquals("", props.getAllowedSites());
    verify(folderHelper).saveFolderProperties(props);
    verify(folderHelper, never()).isAssignableFolderAllowedSites(anyString());
  }

  @Test
  void unchangedAllowedSites_doNotRequireCatalog() throws Exception {
    PSFolderProperties stored = storedFolder();
    stored.setAllowedSites("302,301");
    PSFolderProperties props = storedFolder();
    props.setAllowedSites("301,0302");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    assertEquals("301,0302", props.getAllowedSites());
    verify(folderHelper).saveFolderProperties(props);
    verify(folderHelper, never()).isAssignableFolderAllowedSites(anyString());
  }

  @Test
  void omittedAllowedSites_doNotRequireCatalog() throws Exception {
    PSFolderProperties stored = storedFolder();
    stored.setAllowedSites("301");
    PSFolderProperties props = storedFolder();
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
    verify(folderHelper, never()).isAssignableFolderAllowedSites(anyString());
  }

  @Test
  void unchangedMalformedAllowedSites_stillSaves() throws Exception {
    PSFolderProperties stored = storedFolder();
    stored.setAllowedSites("legacy");
    PSFolderProperties props = storedFolder();
    props.setAllowedSites(" legacy ");
    when(folderHelper.findFolderProperties("16777215-101-703")).thenReturn(stored);
    when(folderHelper.hasFolderPermission(
            eq("16777215-101-703"), eq(PSFolderPermission.Access.ADMIN)))
        .thenReturn(true);
    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
    verify(folderHelper, never()).isAssignableFolderAllowedSites(anyString());
  }

  private static PSFolderProperties storedFolder() {
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    return props;
  }
}
