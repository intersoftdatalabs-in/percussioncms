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
package com.percussion.pagemanagement.assembler.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSControlRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * {@code isHtmlField} must accept the renamed RTE control
 * {@code sys_default_richtext_editor} (#6834, owner decision 2026-11-24)
 * while still accepting the legacy {@code sys_tinymce} alias for stored
 * content types not yet migrated by the installer upgrade task (#7236).
 */
@Tag("UnitTest")
class PSAssemblyItemBridgeHtmlFieldTest {

  private PSAssemblyItemBridge bridgeFor(String controlName) throws Exception {
    PSItemDefManager mgr = mock(PSItemDefManager.class);
    when(mgr.contentTypeNameToId("percPage")).thenReturn(1L);
    PSContentEditor editor = mock(PSContentEditor.class);
    PSControlRef ref = mock(PSControlRef.class);
    when(ref.getName()).thenReturn(controlName);
    when(editor.getFieldControl("body")).thenReturn(ref);
    when(mgr.getContentEditorDef(1L)).thenReturn(editor);
    return new PSAssemblyItemBridge(
        null, null, null, null, null, null, null, null, null, null, null, null, mgr, null);
  }

  @Test
  @DisplayName("renamed sys_default_richtext_editor field is an HTML field")
  void renamedControlIsHtmlField() throws Exception {
    assertTrue(bridgeFor("sys_default_richtext_editor").isHtmlField("percPage", "body"));
  }

  @Test
  @DisplayName("legacy sys_tinymce alias is still an HTML field")
  void legacyAliasIsHtmlField() throws Exception {
    assertTrue(bridgeFor("sys_tinymce").isHtmlField("percPage", "body"));
  }

  @Test
  @DisplayName("plain text control is not an HTML field")
  void plainControlIsNotHtmlField() throws Exception {
    assertFalse(bridgeFor("sys_EditBox").isHtmlField("percPage", "body"));
  }
}
