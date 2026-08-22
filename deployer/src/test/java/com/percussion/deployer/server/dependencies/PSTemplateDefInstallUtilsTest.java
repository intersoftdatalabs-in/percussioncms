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
package com.percussion.deployer.server.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.deployer.objectstore.PSIdMap;
import com.percussion.deployer.objectstore.PSIdMapping;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Install-path ID reservation for TemplateDef (issue #3727). Package emit tests already assert
 * archive GUIDs {@code 0-4-602}..{@code 614}; this covers the deployer apply policy that previously
 * allocated sequential type-4 UUIDs ({@code 0-4-1001}..) on fresh H2.
 */
public class PSTemplateDefInstallUtilsTest {

  /** Same value as {@code PSTemplateDefDependencyHandler.DEPENDENCY_TYPE} — do not load that class (Spring locator). */
  private static final String TYPE = "TemplateDef";

  @Test
  public void freshAssignKeepsArchiveUuid() {
    PSIdMapping mapping = newNewMapping("602", "perc.page");
    assertTrue(PSTemplateDefInstallUtils.tryKeepSourceUuid(mapping, false));
    assertEquals("602", mapping.getTargetId());
    assertEquals("0-4-602", PSTemplateDefInstallUtils.assemblyGuidString(mapping.getTargetId()));
  }

  @Test
  public void existingCustomerRowIsNotRemapped() {
    PSIdMapping mapping = new PSIdMapping("602", "perc.page", TYPE);
    mapping.setIsNewObject(false);
    mapping.setTarget("1001", "perc.page");

    assertFalse(PSTemplateDefInstallUtils.tryKeepSourceUuid(mapping, false));
    assertEquals("1001", mapping.getTargetId());
    assertEquals(
        "0-4-1001", PSTemplateDefInstallUtils.assemblyGuidString(mapping.getTargetId()));
  }

  @Test
  public void takenSourceUuidFallsThroughToNextUuid() {
    PSIdMapping mapping = newNewMapping("602", "perc.page");
    assertFalse(PSTemplateDefInstallUtils.tryKeepSourceUuid(mapping, true));
    assertNull(mapping.getTargetId());
    assertFalse(PSTemplateDefInstallUtils.shouldKeepSourceUuid("602", true));
    assertNull(PSTemplateDefInstallUtils.resolveKeptSourceId("602", true));
  }

  @Test
  public void blankOrInvalidSourceDoesNotKeep() {
    assertFalse(PSTemplateDefInstallUtils.shouldKeepSourceUuid(null, false));
    assertFalse(PSTemplateDefInstallUtils.shouldKeepSourceUuid("  ", false));
    assertFalse(PSTemplateDefInstallUtils.shouldKeepSourceUuid("not-a-guid", false));
    assertThrows(
        IllegalArgumentException.class, () -> PSTemplateDefInstallUtils.assemblyGuidString(""));
  }

  @Test
  public void reservedTargetUuidIsDetectedAcrossStringForms() {
    PSIdMap idMap = new PSIdMap("src:repo");
    PSIdMapping other = newNewMapping("999", "other.template");
    other.setTarget("602", "other.template");
    idMap.addMapping(other);

    assertTrue(PSTemplateDefInstallUtils.isUuidReservedAsTarget(idMap, "602", TYPE));
    assertTrue(PSTemplateDefInstallUtils.isUuidReservedAsTarget(idMap, "0-4-602", TYPE));
    assertFalse(PSTemplateDefInstallUtils.isUuidReservedAsTarget(idMap, "604", TYPE));
  }

  @Test
  public void selfMappingIsNotTreatedAsReserved() {
    PSIdMap idMap = new PSIdMap("src:repo");
    PSIdMapping self = newNewMapping("602", "perc.page");
    self.setTarget("602", "perc.page");
    idMap.addMapping(self);

    assertFalse(PSTemplateDefInstallUtils.isUuidReservedAsTarget(idMap, "602", TYPE));
  }

  @Test
  public void sameTemplateUuidAcceptsEncodedLong() {
    String encoded = String.valueOf(new PSGuid(0, PSTypeEnum.TEMPLATE, 602).longValue());
    assertTrue(PSTemplateDefInstallUtils.sameTemplateUuid("602", encoded));
    assertTrue(PSTemplateDefInstallUtils.sameTemplateUuid("0-4-602", encoded));
    assertFalse(PSTemplateDefInstallUtils.sameTemplateUuid("602", "604"));
  }

  @Test
  public void baselineSystemTemplateGuidsAreStableOnFreshInstall() {
    Map<String, String> expected =
        Map.of(
            "perc.page", "0-4-602",
            "perc.pageDatabase", "0-4-604",
            "perc.pageDispatcher", "0-4-606",
            "perc.pageXml", "0-4-608",
            "perc.sys.resource", "0-4-610",
            "perc.widget", "0-4-612",
            "perc.widgetDispatcher", "0-4-614");
    assertEquals(expected, PSTemplateDefInstallUtils.BASELINE_SYSTEM_TEMPLATE_GUIDS);
    expected.forEach(
        (name, guid) -> {
          String uuid = guid.substring("0-4-".length());
          PSIdMapping mapping = newNewMapping(uuid, name);
          assertTrue(
              PSTemplateDefInstallUtils.tryKeepSourceUuid(mapping, false),
              "first assign must keep " + name);
          assertEquals(guid, PSTemplateDefInstallUtils.assemblyGuidString(mapping.getTargetId()));
        });
  }

  @Test
  public void tryKeepSourceUuidRejectsNullMapping() {
    assertThrows(
        NullPointerException.class, () -> PSTemplateDefInstallUtils.tryKeepSourceUuid(null, false));
  }

  private static PSIdMapping newNewMapping(String sourceId, String name) {
    PSIdMapping mapping = new PSIdMapping(sourceId, name, TYPE, true);
    mapping.setIsNewObject(true);
    return mapping;
  }
}
