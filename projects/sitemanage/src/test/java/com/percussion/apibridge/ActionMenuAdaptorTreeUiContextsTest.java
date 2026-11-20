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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSAction;
import com.percussion.cms.objectstore.PSMenuModeContextMapping;
import com.percussion.rest.actions.ActionMenu;
import com.percussion.rest.actions.ActionMenuList;
import com.percussion.rest.actions.ActionMenuModeUIContext;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.ui.IPSUiDesignWs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Bulk design overlay of per-mode uiContexts for the {@code /find} tree (#6824).
 *
 * <p>The {@code findActionMenusTree} path never carries mode mappings, so the server
 * {@code ?uiMode=} filter and the shell evaluator would filter on an always-empty field.
 * One unlocked {@code loadActions} call covers the whole tree; failures stay permissive.</p>
 */
@Tag("UnitTest")
class ActionMenuAdaptorTreeUiContextsTest {

  private IPSUiDesignWs designWs;
  private ActionMenuAdaptor adaptor;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    designWs = mock(IPSUiDesignWs.class);
    adaptor = new ActionMenuAdaptor(designWs, () -> true);
  }

  @AfterEach
  void tearDown() {
    ActionMenuAdaptor.clearRequestHibernateIndex();
    PSRequestInfo.resetRequestInfo();
  }

  private static PSAction domainAction(int id, String modeId, String modeName) {
    PSAction action = new PSAction("Menu" + id, "Menu" + id);
    action.setLocator(PSAction.createKey(String.valueOf(id)));
    PSMenuModeContextMapping mapping =
        new PSMenuModeContextMapping(modeId, "9", String.valueOf(id));
    mapping.setModeName(modeName);
    mapping.setContextName("Folder");
    action.getModeUIContexts().add(mapping);
    return action;
  }

  private static ActionMenu dtoMenu(int id, String name) {
    ActionMenu menu = new ActionMenu();
    menu.setId(id);
    menu.setName(name);
    return menu;
  }

  @Test
  void overlayPopulatesRootsAndDescendantsFromOneLoad() throws Exception {
    ActionMenu child = dtoMenu(16, "Create_New_Item");
    ActionMenu root = dtoMenu(15, "Create_New_Folder");
    ActionMenuList children = new ActionMenuList();
    children.add(child);
    root.setChildren(children);
    ActionMenu other = dtoMenu(7, "Edit");
    List<ActionMenu> tree = new ArrayList<>(List.of(root, other));

    when(designWs.loadActions(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(
            List.of(
                domainAction(15, "3", "CXNAV"),
                domainAction(16, "3", "CXNAV"),
                domainAction(7, "4", "CXMAIN")));

    adaptor.overlayTreeUiContexts(tree);

    verify(designWs, times(1))
        .loadActions(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin"));
    assertEquals("CXNAV", root.getUiContexts()[0].getModeName());
    assertEquals("3", root.getUiContexts()[0].getModeId());
    assertEquals("CXNAV", child.getUiContexts()[0].getModeName());
    assertEquals("CXMAIN", other.getUiContexts()[0].getModeName());
  }

  @Test
  void overlayLeavesMenusWithoutLoadedRowUntouched() throws Exception {
    ActionMenu known = dtoMenu(15, "Create_New_Folder");
    ActionMenu unknown = dtoMenu(999, "Custom");
    List<ActionMenu> tree = new ArrayList<>(List.of(known, unknown));

    when(designWs.loadActions(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(domainAction(15, "3", "CXNAV")));

    adaptor.overlayTreeUiContexts(tree);

    assertEquals("CXNAV", known.getUiContexts()[0].getModeName());
    assertNull(unknown.getUiContexts());
  }

  @Test
  void overlayStaysPermissiveWhenLoadFails() throws Exception {
    ActionMenu menu = dtoMenu(15, "Create_New_Folder");
    List<ActionMenu> tree = new ArrayList<>(List.of(menu));

    when(designWs.loadActions(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenThrow(new RuntimeException("design unavailable"));

    adaptor.overlayTreeUiContexts(tree);

    assertNull(menu.getUiContexts());
  }

  @Test
  void overlaySkipsTreesWithoutPersistedIds() throws Exception {
    ActionMenu menu = new ActionMenu();
    menu.setName("Unpersisted");
    List<ActionMenu> tree = new ArrayList<>(List.of(menu));

    adaptor.overlayTreeUiContexts(tree);
    adaptor.overlayTreeUiContexts(new ArrayList<>());
    adaptor.overlayTreeUiContexts(null);

    verify(designWs, never())
        .loadActions(anyList(), anyBoolean(), anyBoolean(), any(), any());
  }

  /**
   * H2-level latency evidence (#6853): a real-sized catalog (40 roots x 10 children plus a
   * duplicate id per root to exercise {@code LinkedHashSet} dedup) with a simulated 150ms
   * design load. The cold call pays the load once; the warm call is served from the TTL
   * cache with no second service call. Absolute numbers are mock-backed (they prove call
   * elimination, not DB latency); MySQL/MSSQL measurement stays a human-env residual.
   */
  @Test
  void repeatOverlayWithinTtlSkipsDesignLoad_andLogsLatency() throws Exception {
    int roots = 40;
    int perRoot = 10;
    List<ActionMenu> tree = new ArrayList<>(roots);
    List<PSAction> domains = new ArrayList<>(roots * (perRoot + 1));
    int id = 1000;
    for (int r = 0; r < roots; r++) {
      ActionMenu root = dtoMenu(id, "Root" + r);
      domains.add(domainAction(id, "3", "CXNAV"));
      id++;
      ActionMenuList children = new ActionMenuList();
      for (int c = 0; c < perRoot; c++) {
        ActionMenu child = dtoMenu(id, "Root" + r + "_Child" + c);
        children.add(child);
        domains.add(domainAction(id, "3", "CXNAV"));
        id++;
      }
      ActionMenu dup = dtoMenu(id - 1, "Root" + r + "_ChildDup");
      children.add(dup);
      root.setChildren(children);
      tree.add(root);
    }
    int nodes = roots * (perRoot + 2);

    when(designWs.loadActions(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenAnswer(
            inv -> {
              Thread.sleep(150);
              return domains;
            });

    long coldStart = System.nanoTime();
    adaptor.overlayTreeUiContexts(tree);
    long coldMillis = (System.nanoTime() - coldStart) / 1_000_000;

    long warmStart = System.nanoTime();
    adaptor.overlayTreeUiContexts(tree);
    long warmMillis = (System.nanoTime() - warmStart) / 1_000_000;

    System.out.println(
        "[uiContexts-cache] nodes="
            + nodes
            + " coldMs="
            + coldMillis
            + " warmMs="
            + warmMillis);

    verify(designWs, times(1))
        .loadActions(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin"));
    assertEquals(nodes, countOverlaid(tree));
  }

  @Test
  void expiredCacheReloadsFromDesign() throws Exception {
    ActionMenuAdaptor shortTtl = new ActionMenuAdaptor(designWs, () -> true, null, 0L);
    ActionMenu menu = dtoMenu(15, "Create_New_Folder");
    List<ActionMenu> tree = new ArrayList<>(List.of(menu));

    when(designWs.loadActions(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(domainAction(15, "3", "CXNAV")));

    shortTtl.overlayTreeUiContexts(tree);
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() == start
        && System.currentTimeMillis() - start < 5000) {
      Thread.yield();
    }
    shortTtl.overlayTreeUiContexts(tree);

    verify(designWs, times(2))
        .loadActions(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin"));
    assertEquals("CXNAV", menu.getUiContexts()[0].getModeName());
  }

  @Test
  void saveThroughAdaptorInvalidatesUiContextsCache() throws Exception {
    ActionMenu menu = dtoMenu(42, "MyMenu");
    List<ActionMenu> tree = new ArrayList<>(List.of(menu));
    when(designWs.loadActions(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(domainAction(42, "3", "CXNAV")));

    adaptor.overlayTreeUiContexts(tree);
    adaptor.overlayTreeUiContexts(tree);
    verify(designWs, times(1))
        .loadActions(anyList(), eq(false), eq(false), any(), any());

    PSAction existing = new PSAction("MyMenu", "MyMenu");
    existing.setGUID(new PSGuid(PSTypeEnum.ACTION, 42));
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(existing.getGUID());
    when(sum.getName()).thenReturn("MyMenu");
    when(designWs.findActions(
            nullable(String.class), nullable(String.class), nullable(List.class)))
        .thenReturn(List.of(sum));
    PSAction locked = new PSAction("MyMenu", "MyMenu");
    locked.setGUID(new PSGuid(PSTypeEnum.ACTION, 42));
    when(designWs.loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));
    when(designWs.objectIdToPath(any())).thenReturn("//ContentExplorer/Menus/User/MyMenu");

    ActionMenu body = new ActionMenu();
    body.setLabel("Updated");
    adaptor.saveActionMenu("MyMenu", body);

    when(designWs.loadActions(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(domainAction(42, "4", "CXMAIN")));
    adaptor.overlayTreeUiContexts(tree);
    // 3 unlocked loads total: prime overlay + save path catalog load + post-invalidation
    // reload. The reload proves the save cleared the cache (it picks up CXMAIN).
    verify(designWs, times(3))
        .loadActions(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin"));
    assertEquals("CXMAIN", menu.getUiContexts()[0].getModeName());
  }

  private static int countOverlaid(List<ActionMenu> menus) {
    if (menus == null) {
      return 0;
    }
    int count = 0;
    for (ActionMenu menu : menus) {
      if (menu == null) {
        continue;
      }
      ActionMenuModeUIContext[] uiContexts = menu.getUiContexts();
      if (uiContexts != null && uiContexts.length > 0) {
        count++;
      }
      count += countOverlaid(menu.getChildren());
    }
    return count;
  }
}
