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

import com.percussion.rest.Guid;
import com.percussion.rest.itemfilter.ItemFilter;
import com.percussion.rest.itemfilter.ItemFilterRuleDefinition;
import com.percussion.rest.itemfilter.ItemFilterRuleDefinitionParam;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.IPSItemFilterRuleDef;
import com.percussion.services.filter.data.PSItemFilter;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * AS-07 POST create / PUT update / DELETE persist via {@code createItemFilters}/{@code
 * saveItemFilters}/{@code deleteItemFilters}. Admin only; unique name; in-use delete 409.
 */
@Tag("UnitTest")
class ItemFilterAdaptorWriteTest {

  private IPSFilterService filterService;
  private IPSSystemDesignWs designWs;
  private ItemFilterAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    filterService = mock(IPSFilterService.class);
    designWs = mock(IPSSystemDesignWs.class);
    adaptor = new ItemFilterAdaptor(filterService, designWs, () -> true);
    guid = new PSGuid(PSTypeEnum.ITEM_FILTER, 42L);
    when(designWs.findItemFilters(any())).thenReturn(Collections.emptyList());
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void create_usesCreateThenSaveAndReleasesLock() throws Exception {
    PSItemFilter filter = stubFilter("preview", "created via REST");
    when(designWs.createItemFilters(eq(List.of("preview")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(filter));
    when(filterService.loadFilter(eq(guid))).thenReturn(filter);

    ItemFilter body = new ItemFilter();
    body.setName("preview");
    body.setDescription("created via REST");

    ItemFilter out = adaptor.updateOrCreateItemFilter(body);

    assertEquals("preview", out.getName());
    assertEquals("created via REST", out.getDescription());
    verify(designWs).createItemFilters(eq(List.of("preview")), eq("test-session"), eq("Admin"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSItemFilter>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveItemFilters(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    verify(filter).setDescription("created via REST");
  }

  @Test
  void create_appliesRulesAndParent() throws Exception {
    PSItemFilter filter = stubFilter("child", "child");
    PSItemFilter parent = stubFilter("parent", "parent filter");
    IPSGuid parentGuid = new PSGuid(PSTypeEnum.ITEM_FILTER, 7L);
    when(parent.getGUID()).thenReturn(parentGuid);
    when(designWs.createItemFilters(eq(List.of("child")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(filter));
    when(filterService.findFilterByNameSafe(eq("parent"))).thenReturn(Optional.of(parent));
    IPSItemFilterRuleDef ruleDef = mock(IPSItemFilterRuleDef.class);
    when(ruleDef.getRuleName()).thenReturn("sys_filterByPublishDate");
    when(ruleDef.getGUID()).thenReturn(new PSGuid(PSTypeEnum.ITEM_FILTER, 8L));
    when(ruleDef.getParams()).thenReturn(Map.of("maxAge", "30"));
    when(filterService.createRuleDef(eq("sys_filterByPublishDate"), any())).thenReturn(ruleDef);
    when(filterService.loadFilter(eq(guid))).thenReturn(filter);
    when(filter.getParentFilter()).thenReturn(parent);
    when(filter.getRuleDefs()).thenReturn(Set.of(ruleDef));

    ItemFilter body = new ItemFilter();
    body.setName("child");
    ItemFilter parentDto = new ItemFilter();
    parentDto.setName("parent");
    body.setParentFilter(parentDto);
    ItemFilterRuleDefinition rule = new ItemFilterRuleDefinition();
    rule.setName("sys_filterByPublishDate");
    ItemFilterRuleDefinitionParam param = new ItemFilterRuleDefinitionParam();
    param.setName("maxAge");
    param.setValue("30");
    rule.setParams(List.of(param));
    body.setRules(Set.of(rule));

    ItemFilter out = adaptor.updateOrCreateItemFilter(body);

    assertEquals("parent", out.getParentFilter().getName());
    assertEquals(1, out.getRules().size());
    verify(filter).setParentFilter(parent);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Set<IPSItemFilterRuleDef>> rules = ArgumentCaptor.forClass(Set.class);
    verify(filter).setRuleDefs(rules.capture());
    assertEquals(1, rules.getValue().size());
  }

  @Test
  void create_duplicateName_is409BeforeCreate() throws Exception {
    IPSCatalogSummary existing = mock(IPSCatalogSummary.class);
    when(existing.getName()).thenReturn("preview");
    when(designWs.findItemFilters(eq("preview"))).thenReturn(List.of(existing));

    ItemFilter body = new ItemFilter();
    body.setName("preview");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateOrCreateItemFilter(body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("already exists"));
    verify(designWs, never()).createItemFilters(anyList(), any(), any());
    verify(designWs, never()).saveItemFilters(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_persistTimeDuplicate_is409() throws Exception {
    when(designWs.createItemFilters(eq(List.of("preview")), eq("test-session"), eq("Admin")))
        .thenThrow(
            new IllegalArgumentException("The name 'preview' for type 'ITEM_FILTER' already exists."));
    ItemFilter body = new ItemFilter();
    body.setName("preview");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateOrCreateItemFilter(body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveItemFilters(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_blankName_throwsBeforeDesignWs() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.updateOrCreateItemFilter(null));
    ItemFilter blank = new ItemFilter();
    blank.setName("  ");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.updateOrCreateItemFilter(blank));
    assertTrue(ex.getMessage().contains("name is required"));
    verify(designWs, never()).createItemFilters(anyList(), any(), any());
  }

  @Test
  void create_nameWithSpaces_throwsBeforeDesignWs() {
    ItemFilter body = new ItemFilter();
    body.setName("has space");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.updateOrCreateItemFilter(body));
    assertEquals("name cannot contain whitespace", ex.getMessage());
    verify(designWs, never()).createItemFilters(anyList(), any(), any());
  }

  @Test
  void create_nonAdmin_is403() {
    adaptor = new ItemFilterAdaptor(filterService, designWs, () -> false);
    ItemFilter body = new ItemFilter();
    body.setName("preview");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateOrCreateItemFilter(body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).createItemFilters(anyList(), any(), any());
  }

  @Test
  void create_missingSession_is403() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    ItemFilter body = new ItemFilter();
    body.setName("preview");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateOrCreateItemFilter(body));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("session"), ex.getMessage());
  }

  @Test
  void create_thenGetByName_returnsFilter() throws Exception {
    PSItemFilter filter = stubFilter("preview", "created via REST");
    when(designWs.createItemFilters(eq(List.of("preview")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(filter));
    when(filterService.loadFilter(eq(guid))).thenReturn(filter);
    when(filterService.findAllFilters()).thenReturn(List.of(filter));

    ItemFilter body = new ItemFilter();
    body.setName("preview");
    body.setDescription("created via REST");

    adaptor.updateOrCreateItemFilter(body);
    ItemFilter got = adaptor.findItemFilter("preview");

    assertEquals("preview", got.getName());
    assertEquals("created via REST", got.getDescription());
  }

  @Test
  void update_loadsWithLockAndSavesRulesAndParent() throws Exception {
    PSItemFilter filter = stubFilter("preview", "old");
    when(designWs.loadItemFilters(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(filter));
    when(filterService.loadFilter(eq(guid))).thenReturn(filter);
    PSItemFilter parent = stubFilter("parent", "p");
    IPSGuid parentGuid = new PSGuid(PSTypeEnum.ITEM_FILTER, 7L);
    when(parent.getGUID()).thenReturn(parentGuid);
    when(filterService.findFilterByNameSafe(eq("parent"))).thenReturn(Optional.of(parent));
    IPSItemFilterRuleDef ruleDef = mock(IPSItemFilterRuleDef.class);
    when(ruleDef.getRuleName()).thenReturn("sys_filterByPublishDate");
    when(ruleDef.getGUID()).thenReturn(new PSGuid(PSTypeEnum.ITEM_FILTER, 8L));
    when(ruleDef.getParams()).thenReturn(Map.of());
    when(filterService.createRuleDef(eq("sys_filterByPublishDate"), any())).thenReturn(ruleDef);

    ItemFilter body = new ItemFilter();
    body.setFilterId(restGuid(guid));
    body.setDescription("updated");
    ItemFilter parentDto = new ItemFilter();
    parentDto.setName("parent");
    body.setParentFilter(parentDto);
    ItemFilterRuleDefinition rule = new ItemFilterRuleDefinition();
    rule.setName("sys_filterByPublishDate");
    body.setRules(Set.of(rule));

    ItemFilter out = adaptor.updateOrCreateItemFilter(body);

    assertEquals("preview", out.getName());
    verify(filter).setDescription("updated");
    verify(filter).setParentFilter(parent);
    verify(designWs).saveItemFilters(anyList(), eq(true), eq("test-session"), eq("Admin"));
    verify(designWs, never()).createItemFilters(anyList(), any(), any());
  }

  @Test
  void update_unknown_returnsNull() throws Exception {
    when(designWs.loadItemFilters(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of());
    ItemFilter body = new ItemFilter();
    body.setFilterId(restGuid(guid));
    body.setName("preview");
    assertNull(adaptor.updateOrCreateItemFilter(body));
    verify(designWs, never()).saveItemFilters(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_nonAdmin_is403() {
    adaptor = new ItemFilterAdaptor(filterService, designWs, () -> false);
    ItemFilter body = new ItemFilter();
    body.setFilterId(restGuid(guid));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateOrCreateItemFilter(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void delete_thenGetByName_isNotFound() throws Exception {
    PSItemFilter filter = stubFilter("preview", "d");
    when(filterService.loadFilter(eq(guid))).thenReturn(filter);
    when(designWs.loadItemFilters(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(filter));

    adaptor.deleteItemFilter(restGuid(guid));
    when(filterService.findAllFilters()).thenReturn(List.of());
    assertNull(adaptor.findItemFilter("preview"));
    verify(designWs).deleteItemFilters(eq(List.of(guid)), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void delete_inUse_is409() throws Exception {
    PSItemFilter filter = stubFilter("preview", "d");
    when(filterService.loadFilter(eq(guid))).thenReturn(filter);
    when(designWs.loadItemFilters(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(filter));
    PSErrorsException errors = new PSErrorsException();
    errors.addError(guid, new PSErrorException("Object has dependents"));
    doThrow(errors)
        .when(designWs)
        .deleteItemFilters(anyList(), eq(false), any(), any());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteItemFilter(restGuid(guid)));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("content list"), ex.getMessage());
  }

  @Test
  void delete_unknown_throwsNotFound() throws Exception {
    when(filterService.loadFilter(eq(guid))).thenThrow(new PSNotFoundException("gone"));
    assertThrows(PSNotFoundException.class, () -> adaptor.deleteItemFilter(restGuid(guid)));
    verify(designWs, never()).deleteItemFilters(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_lockConflict_is409() throws Exception {
    PSItemFilter filter = stubFilter("preview", "d");
    when(filterService.loadFilter(eq(guid))).thenReturn(filter);
    when(designWs.loadItemFilters(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenThrow(new PSErrorResultsException());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteItemFilter(restGuid(guid)));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).deleteItemFilters(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_nonAdmin_is403() throws Exception {
    adaptor = new ItemFilterAdaptor(filterService, designWs, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteItemFilter(restGuid(guid)));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).deleteItemFilters(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void toIpsGuid_parsesStringValue() {
    Guid g = restGuid(guid);
    IPSGuid parsed = ItemFilterAdaptor.toIpsGuid(g);
    assertEquals(guid.toString(), parsed.toString());
    assertNull(ItemFilterAdaptor.toIpsGuid(null));
  }

  private PSItemFilter stubFilter(String name, String description) {
    PSItemFilter filter = mock(PSItemFilter.class);
    when(filter.getName()).thenReturn(name);
    when(filter.getDescription()).thenReturn(description);
    when(filter.getGUID()).thenReturn(guid);
    when(filter.getLegacyAuthtypeId()).thenReturn(null);
    when(filter.getParentFilter()).thenReturn(null);
    when(filter.getRuleDefs()).thenReturn(new HashSet<>());
    return filter;
  }

  private static Guid restGuid(IPSGuid g) {
    Guid out = new Guid();
    out.setStringValue(g.toString());
    return out;
  }
}
