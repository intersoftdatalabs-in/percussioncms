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
package com.percussion.webui.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Behavioral tests for leftover CE bookmark → React editor host (#3473). */
public class PSEditorHostRedirectTest {

  @Test
  public void parseContentIdAcceptsNumericAndGuidTail() {
    assertEquals("551", PSEditorHostRedirect.parseContentId("551"));
    assertEquals("551", PSEditorHostRedirect.parseContentId("16777215-101-551"));
    assertEquals("708", PSEditorHostRedirect.parseContentId("1-101-708"));
    assertNull(PSEditorHostRedirect.parseContentId(null));
    assertNull(PSEditorHostRedirect.parseContentId(""));
    assertNull(PSEditorHostRedirect.parseContentId("0"));
    assertNull(PSEditorHostRedirect.parseContentId("not-an-id"));
  }

  @Test
  public void normalizeModeMapsReadonlyToView() {
    assertEquals("view", PSEditorHostRedirect.normalizeMode("readonly"));
    assertEquals("view", PSEditorHostRedirect.normalizeMode("VIEW"));
    assertEquals("promote", PSEditorHostRedirect.normalizeMode("promote"));
    assertEquals("edit", PSEditorHostRedirect.normalizeMode(null));
    assertEquals("edit", PSEditorHostRedirect.normalizeMode("edit"));
  }

  @Test
  public void spaRedirectDoesNotUseLeftoverViewEditor() {
    String loc = PSEditorHostRedirect.buildSpaRedirect("", "16777215-101-551", "readonly");
    assertEquals("/cm/app/spa.jsp?entry=editor&contentId=551&mode=view", loc);
    assertFalse(loc.contains("view=editor"));
    assertFalse(loc.contains("editAsset.jsp"));
    String empty = PSEditorHostRedirect.buildSpaRedirect(null, null, null);
    assertEquals("/cm/app/spa.jsp?entry=editor&mode=edit", empty);
    assertTrue(
        PSEditorHostRedirect.buildSpaRedirect("/Rhythmyx", "42", "edit")
            .startsWith("/Rhythmyx/cm/app/spa.jsp?entry=editor"));
  }

  @Test
  public void retiredEditAssetJspMatchesAppAndPagesTrees() {
    assertTrue(PSEditorHostRedirect.isRetiredEditAssetJsp("/cm/app/editasset.jsp"));
    assertTrue(PSEditorHostRedirect.isRetiredEditAssetJsp("/cm/pages/app/editasset.jsp"));
    assertTrue(PSEditorHostRedirect.isRetiredEditAssetJsp("/rhythmyx/cm/app/editasset.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredEditAssetJsp("/cm/app/webmgt.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredEditAssetJsp(null));
  }
}
