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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.security.data.PSCommunity;
import com.percussion.tablefactory.PSJdbcDbmsDef;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed install/upgrade helpers after install-package rawtypes cleanup (#2933
 * / #2877 / #2942 upgrade-plugin slices).
 */
@Tag("UnitTest")
@DisplayName("install package generics")
class PSInstallPackageTypedTest {

  @AfterEach
  void clearPluginResponses() {
    RxUpgrade.ms_pluginResponses = new ArrayList<>();
    // Defense-in-depth: drop test probe if a prior test aborted mid-mutation.
    Set<String> apps = PSPreUpgradePluginDeprecatedSysApps.getDeprecatedSysApps();
    if (apps != null) {
      apps.remove("sys_testOnly.xml");
    }
  }

  @Test
  @DisplayName("RxUpgrade stores typed PSPluginResponse list")
  void pluginResponsesAreTyped() {
    PSPluginResponse r1 = new PSPluginResponse(PSPluginResponse.SUCCESS, "ok");
    PSPluginResponse r2 = new PSPluginResponse(PSPluginResponse.WARNING, "warn");
    RxUpgrade.addResponse(r1);
    RxUpgrade.addResponse(r2);

    ArrayList<PSPluginResponse> responses = RxUpgrade.getResponses();
    assertEquals(2, responses.size());
    assertSame(r1, responses.get(0));
    assertSame(r2, responses.get(1));
    assertEquals(PSPluginResponse.SUCCESS, responses.get(0).getType());
    assertEquals("warn", responses.get(1).getMessage());
  }

  @Test
  @DisplayName("PSNameSpacesUtil accepts typed Set and avoids name clashes")
  void nameSpacesUtilTypedSet() {
    Set<String> names = new HashSet<>();
    names.add("a_b");
    names.add("a_b1");
    assertEquals("a_b2", PSNameSpacesUtil.removeWhitespacesFromName("a b", names));
    assertEquals("plain", PSNameSpacesUtil.removeWhitespacesFromName("plain", names));
  }

  @Test
  @DisplayName("deprecated sys apps set is typed String set")
  void deprecatedSysAppsTyped() {
    Set<String> apps = PSPreUpgradePluginDeprecatedSysApps.getDeprecatedSysApps();
    assertNotNull(apps);
    String sample = PSPreUpgradePluginDeprecatedSysApps.SYS_APPS[0];
    assertTrue(sample.endsWith(".xml"));
  }

  @Test
  @DisplayName("ModifyColumnBase constructNewValue disambiguates with typed set")
  void constructNewValueTyped() {
    PSUpgradePluginModifyColumnBase plugin =
        new PSUpgradePluginModifyColumnBase() {
          @Override
          protected boolean modifyColumnValues(
              Connection conn,
              PSJdbcDbmsDef dbmsDef,
              String table,
              String column,
              String idcolumn) {
            return false;
          }
        };

    Set<String> existing = new HashSet<>();
    existing.add("Article");
    existing.add("Article1");
    // same package: protected constructNewValue
    assertEquals("Article2", plugin.constructNewValue("Article", existing));
    assertEquals("Fresh", plugin.constructNewValue("Fresh", existing));
  }

  @Test
  @DisplayName("getDeprecatedSysApps returns non-null typed set")
  void deprecatedSysAppsNonNull() {
    Set<String> apps = PSPreUpgradePluginDeprecatedSysApps.getDeprecatedSysApps();
    assertNotNull(apps);
    // empty until process() scans ObjectStore; still a live mutable String set
    final String probe = "sys_testOnly.xml";
    try {
      apps.add(probe);
      assertTrue(
          PSPreUpgradePluginDeprecatedSysApps.getDeprecatedSysApps().contains(probe));
    } finally {
      apps.remove(probe);
    }
  }

  @Test
  @DisplayName("empty plugin response list after reassignment")
  void emptyResponsesAfterClear() {
    RxUpgrade.addResponse(new PSPluginResponse(PSPluginResponse.SUCCESS, "x"));
    assertFalse(RxUpgrade.getResponses().isEmpty());
    RxUpgrade.ms_pluginResponses = new ArrayList<>();
    assertTrue(RxUpgrade.getResponses().isEmpty());
    Iterator<PSPluginResponse> it = RxUpgrade.getResponses().iterator();
    assertFalse(it.hasNext());
  }

  @Test
  @DisplayName("community visibility findCommunitiesToFixNames is typed")
  void convertCommunityVisibilityFindsNamesWithSpaces() {
    List<PSCommunity> communities = new ArrayList<>();
    communities.add(communityWithName("NoSpace"));
    communities.add(communityWithName("Has Space"));
    communities.add(communityWithName("Also_Ok"));

    List<PSCommunity> toFix =
        PSUpgradePluginConvertCommunityVisibility.findCommunitiesToFixNames(communities);
    assertEquals(1, toFix.size());
    assertEquals("Has Space", toFix.get(0).getName());
  }

  @Test
  @DisplayName("community visibility getCommunityNames returns typed String set")
  void convertCommunityVisibilityCommunityNames() {
    List<PSCommunity> communities = new ArrayList<>();
    communities.add(communityWithName("Alpha"));
    communities.add(communityWithName("Beta"));

    Set<String> names =
        PSUpgradePluginConvertCommunityVisibility.getCommunityNames(communities);
    assertEquals(2, names.size());
    assertTrue(names.contains("Alpha"));
    assertTrue(names.contains("Beta"));
  }

  /** Avoids PSCommunity(String,String) which needs a live guid manager Spring bean. */
  private static PSCommunity communityWithName(String name) {
    PSCommunity c = new PSCommunity();
    c.setName(name);
    return c;
  }

  @Test
  @DisplayName("slot name modifyName collapses spaces for upgrade maps")
  void slotNameModifyNameTyped() {
    assertEquals("My_Slot", InstallUtil.modifyName("My Slot"));
    assertEquals("Already_Ok", InstallUtil.modifyName("Already_Ok"));
    assertEquals(null, InstallUtil.modifyName(null));
  }

  @Test
  @DisplayName("convertSlotName rejects null application")
  void convertSlotNameNullApp() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSUpgradeDbAndHtmlAndXslFilesForSlotNames.convertSlotName(null));
  }
}
