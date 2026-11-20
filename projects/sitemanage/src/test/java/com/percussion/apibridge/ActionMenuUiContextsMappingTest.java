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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSDbComponentCollection;
import com.percussion.cms.objectstore.PSMenuModeContextMapping;
import com.percussion.rest.actions.ActionMenuModeUIContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for the uiContexts DTO mapping (#6619, parent #4530).
 *
 * <p>The SPA shell UI-mode claim ({@code GET /actions/find?uiMode=} and
 * the WebUI evaluator) matches on mode <em>names</em>, so this mapping
 * must preserve {@code modeId}, {@code modeName}, {@code contextId},
 * and {@code contextName} from every {@code PSMenuModeContextMapping}.
 * A mapping that dropped names would silently turn every scoped menu
 * into an unresolvable (permissive) entry.</p>
 */
@Tag("UnitTest")
public class ActionMenuUiContextsMappingTest {

  private static PSDbComponentCollection mappings(PSMenuModeContextMapping... entries) {
    PSDbComponentCollection collection =
        new PSDbComponentCollection(PSMenuModeContextMapping.class);
    for (PSMenuModeContextMapping entry : entries) {
      collection.add(entry);
    }
    return collection;
  }

  private static PSMenuModeContextMapping mapping(
      String modeId, String modeName, String contextId, String contextName) {
    PSMenuModeContextMapping mapping =
        new PSMenuModeContextMapping(modeId, contextId, "42");
    mapping.setModeName(modeName);
    mapping.setContextName(contextName);
    return mapping;
  }

  @Test
  void toDtoUiContextsPreservesShellClaimFields() {
    ActionMenuModeUIContext[] dtos =
        ActionMenuAdaptor.toDtoUiContexts(
            mappings(
                mapping("4", "CXMAIN", "1", "Folder"),
                mapping("7", "Publishing", "2", "Site")));

    assertEquals(2, dtos.length);
    assertEquals("4", dtos[0].getModeId());
    assertEquals("CXMAIN", dtos[0].getModeName());
    assertEquals("1", dtos[0].getContextId());
    assertEquals("Folder", dtos[0].getContextName());
    assertEquals("7", dtos[1].getModeId());
    assertEquals("Publishing", dtos[1].getModeName());
  }

  @Test
  void toDtoUiContextsShellClaimRoundTripsThroughMatcher() {
    ActionMenuModeUIContext[] dtos =
        ActionMenuAdaptor.toDtoUiContexts(
            mappings(mapping("4", "CXMAIN", "1", "Folder")));

    assertTrue(
        ActionMenuModeUIContext.isVisibleInUiMode(
            dtos, ActionMenuModeUIContext.SPA_SHELL_MODE_NAME));
  }

  @Test
  void toDtoUiContextsEmptyStaysEmpty() {
    assertEquals(
        0, ActionMenuAdaptor.toDtoUiContexts(mappings()).length);
    assertEquals(
        0, ActionMenuAdaptor.toDtoUiContexts(null).length);
  }

  @Test
  void toDtoUiContextsPreservesCxnavMapping() {
    // Installer seed audit (#6824): the Create_New cascade and
    // Search_Within_Folder live solely in CXNAV (MODEID 3, navigation
    // pane). The mapping must preserve the nav-pane claim or it would
    // degrade to an unresolvable (permissive) entry.
    ActionMenuModeUIContext[] dtos =
        ActionMenuAdaptor.toDtoUiContexts(
            mappings(mapping("3", "CXNAV", "9", "Folder")));

    assertEquals(1, dtos.length);
    assertEquals("3", dtos[0].getModeId());
    assertEquals("CXNAV", dtos[0].getModeName());
    assertEquals("9", dtos[0].getContextId());
    assertEquals("Folder", dtos[0].getContextName());
  }

  @Test
  void cxnavEntryHiddenUnderCxmainClaimButVisibleUnderCxnavToken() {
    // The SPA shell claims CXMAIN only (#6824 why-not): a CXNAV-scoped
    // menu hides under the shell claim and survives a CXNAV token, so
    // both halves of the contract stay locked against regressions.
    ActionMenuModeUIContext[] dtos =
        ActionMenuAdaptor.toDtoUiContexts(
            mappings(mapping("3", "CXNAV", "9", "Folder")));

    assertFalse(
        ActionMenuModeUIContext.isVisibleInUiMode(
            dtos, ActionMenuModeUIContext.SPA_SHELL_MODE_NAME));
    assertTrue(
        ActionMenuModeUIContext.isVisibleInUiMode(dtos, "CXNAV"));
    assertTrue(
        ActionMenuModeUIContext.isVisibleInUiMode(dtos, "3"));
  }
}
