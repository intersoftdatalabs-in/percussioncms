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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSAction;
import com.percussion.cms.objectstore.PSMenuModeContextMapping;
import com.percussion.rest.actions.ActionMenu;
import com.percussion.rest.actions.ActionMenuList;
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
}
