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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.communities.Community;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Issue #5177 — rename one community (400 / 403 / 409 / success). */
@Tag("UnitTest")
class CommunityAdaptorRenameTest {

  private IPSSecurityDesignWs securityDesignWs;
  private CommunityAdaptor adaptor;
  private PSCommunity stored;

  @BeforeEach
  void setUp() throws Exception {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    securityDesignWs = mock(IPSSecurityDesignWs.class);
    adaptor = new CommunityAdaptor();
    adaptor.adminChecker = () -> true;
    Field field = CommunityAdaptor.class.getDeclaredField("securityDesignWs");
    field.setAccessible(true);
    field.set(adaptor, securityDesignWs);
    stored = new PSCommunity();
    stored.setId(10L);
    stored.setName("Default");
    stored.setDescription("keep me");
    IPSCatalogSummary defaultSummary = summary("Default", 10L);
    when(securityDesignWs.findCommunities(any())).thenReturn(List.of());
    when(securityDesignWs.loadCommunities(anyList(), anyBoolean(), anyBoolean(), any(), any()))
        .thenReturn(List.of(stored));
    when(securityDesignWs.findCommunities(eq("Default"))).thenReturn(List.of(defaultSummary));
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void blankAndOverlongNamesDoNotSave() {
    IllegalArgumentException blank =
        assertThrows(IllegalArgumentException.class, () -> adaptor.renameCommunity("Default", "  "));
    assertTrue(blank.getMessage().toLowerCase().contains("empty"));
    IllegalArgumentException overlong =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.renameCommunity("Default", "E".repeat(51)));
    assertTrue(overlong.getMessage().contains("50"));
    verify(securityDesignWs, never())
        .saveCommunities(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void nonAdminIs403AndDoesNotSave() {
    adaptor.adminChecker = () -> false;
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.renameCommunity("Default", "Enterprise"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(securityDesignWs, never())
        .saveCommunities(anyList(), anyBoolean(), any(), any());
    assertEquals("Default", stored.getName());
  }

  @Test
  void missingSessionIs403() {
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, " ");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.renameCommunity("Default", "Enterprise"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(securityDesignWs, never())
        .saveCommunities(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void unknownCommunityReturnsNull() {
    assertNull(adaptor.renameCommunity("missing", "Enterprise"));
    verify(securityDesignWs, never())
        .saveCommunities(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void sameNameDoesNotSave() {
    Community out = adaptor.renameCommunity("Default", " Default ");
    assertEquals("Default", out.getName());
    assertEquals("keep me", out.getDescription());
    verify(securityDesignWs, never())
        .saveCommunities(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void duplicateNameIs409AndKeepsOldName() {
    IPSCatalogSummary enterprise = summary("Enterprise", 22L);
    when(securityDesignWs.findCommunities(eq("Enterprise"))).thenReturn(List.of(enterprise));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.renameCommunity("Default", "Enterprise"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("already exists"));
    verify(securityDesignWs, never())
        .saveCommunities(anyList(), anyBoolean(), any(), any());
    assertEquals("Default", stored.getName());
  }

  @Test
  void lockConflictIs409AndKeepsOldName() throws Exception {
    when(securityDesignWs.loadCommunities(anyList(), eq(true), anyBoolean(), any(), any()))
        .thenThrow(new PSErrorResultsException());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.renameCommunity("Default", "Enterprise"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(securityDesignWs, never())
        .saveCommunities(anyList(), anyBoolean(), any(), any());
    assertEquals("Default", stored.getName());
  }

  @Test
  void saveAlreadyExistsIs409() throws Exception {
    PSErrorsException failure = new PSErrorsException();
    failure.addError(stored.getGUID(), "The name 'Enterprise' already exists.");
    doThrow(failure)
        .when(securityDesignWs)
        .saveCommunities(anyList(), eq(true), eq("test-session"), eq("Admin"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.renameCommunity("Default", "Enterprise"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void renamePersistsNewNameAndKeepsDescription() throws Exception {
    Community out = adaptor.renameCommunity("Default", " Enterprise ");
    assertEquals("Enterprise", out.getName());
    assertEquals("keep me", out.getDescription());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSCommunity>> saved = ArgumentCaptor.forClass(List.class);
    verify(securityDesignWs)
        .saveCommunities(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals("Enterprise", saved.getValue().get(0).getName());
    assertEquals("keep me", saved.getValue().get(0).getDescription());
    assertNull(saved.getValue().get(0).getVersion());
    assertEquals("Default", stored.getName());
  }

  @Test
  void caseOnlyChangeStillSaves() throws Exception {
    IPSCatalogSummary same = summary("Default", 10L);
    when(securityDesignWs.findCommunities(eq("default"))).thenReturn(List.of(same));
    adaptor.renameCommunity("Default", "default");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSCommunity>> saved = ArgumentCaptor.forClass(List.class);
    verify(securityDesignWs)
        .saveCommunities(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals("default", saved.getValue().get(0).getName());
    assertEquals("keep me", saved.getValue().get(0).getDescription());
    assertNull(saved.getValue().get(0).getVersion());
  }

  private static IPSCatalogSummary summary(String name, long id) {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    IPSGuid guid = mock(IPSGuid.class);
    when(guid.longValue()).thenReturn(id);
    when(guid.getHostId()).thenReturn(0L);
    when(guid.getUUID()).thenReturn((int) id);
    when(guid.getType()).thenReturn(PSTypeEnum.COMMUNITY_DEF.getOrdinal());
    when(guid.toString()).thenReturn("0-13-" + id);
    when(guid.toStringUntyped()).thenReturn("0-" + id);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn(name);
    when(sum.getDescription()).thenReturn(name + " desc");
    when(sum.getLabel()).thenReturn(name);
    return sum;
  }
}
