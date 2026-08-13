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
package com.percussion.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSAttribute;
import com.percussion.design.objectstore.PSMacroDefinition;
import com.percussion.design.objectstore.PSMacroDefinitionSet;
import com.percussion.design.objectstore.PSSearchConfig;
import com.percussion.search.PSSearchEngine;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Behavioral tests for typed PSServer init/lock/search leftovers after #3212 (#3270 residual of
 * #2022).
 */
@Tag("UnitTest")
@DisplayName("PSServer init/lock/search typed leftovers")
class PSServerInitLockSearchTypedTest {

  @Test
  @DisplayName("firstNonEmptyAttributeValue returns first non-empty matching value")
  void firstNonEmptyAttributeValueFindsFirst() {
    PSAttribute skip = new PSAttribute("other");
    skip.setValues(List.of("ignored"));
    PSAttribute empty = new PSAttribute("sys_defaultCommunity");
    empty.setValues(List.of(""));
    PSAttribute hit = new PSAttribute("sys_defaultCommunity");
    hit.setValues(List.of("7", "9"));

    List<PSAttribute> attrs = new ArrayList<>();
    attrs.add(skip);
    attrs.add(empty);
    attrs.add(hit);

    assertEquals("7", PSServer.firstNonEmptyAttributeValue(attrs, "sys_defaultCommunity"));
  }

  @Test
  @DisplayName("firstNonEmptyAttributeValue returns null when missing or empty")
  void firstNonEmptyAttributeValueMissing() {
    assertNull(PSServer.firstNonEmptyAttributeValue(null, "sys_defaultCommunity"));
    assertNull(PSServer.firstNonEmptyAttributeValue(List.of(), "sys_defaultCommunity"));

    PSAttribute empty = new PSAttribute("sys_defaultCommunity");
    empty.setValues(List.of(""));
    assertNull(PSServer.firstNonEmptyAttributeValue(List.of(empty), "sys_defaultCommunity"));
  }

  @Test
  @DisplayName("mergeUserMacros adds unique user macros and overlays system names")
  void mergeUserMacrosAddsAndOverlays() {
    PSMacroDefinitionSet system = new PSMacroDefinitionSet();
    system.add(new PSMacroDefinition("sysA", "sys.A"));
    system.add(new PSMacroDefinition("sysB", "sys.B"));

    PSMacroDefinitionSet user = new PSMacroDefinitionSet();
    user.add(new PSMacroDefinition("sysA", "user.A"));
    user.add(new PSMacroDefinition("userC", "user.C"));

    PSMacroDefinitionSet target = new PSMacroDefinitionSet();
    PSServer.mergeUserMacros(target, system, user);

    assertEquals("user.A", ((PSMacroDefinition) target.get(0)).getClassName());
    assertEquals("sysB", ((PSMacroDefinition) target.get(1)).getName());
    assertEquals("userC", ((PSMacroDefinition) target.get(2)).getName());
  }

  @Test
  @DisplayName("buildSearchEngineProperties copies typed custom props")
  void buildSearchEnginePropertiesCopiesCustom() {
    PSSearchConfig cfg = new PSSearchConfig();
    cfg.addCustomProp("index_on_startup", "yes");
    cfg.addCustomProp("indexRootDir", "/tmp/idx");

    Properties props = PSServer.buildSearchEngineProperties(cfg);
    assertEquals(
        "com.percussion.search.lucene.PSSearchEngineImpl",
        props.getProperty(PSSearchEngine.PROP_CLASSNAME));
    assertEquals("yes", props.getProperty("index_on_startup"));
    assertEquals("/tmp/idx", props.getProperty("indexRootDir"));
  }

  @Test
  @DisplayName("getCustomProps returns an independent typed map")
  void customPropsAreTypedClone() {
    PSSearchConfig cfg = new PSSearchConfig();
    cfg.addCustomProp("k", "v");
    Map<String, String> copy = cfg.getCustomProps();
    copy.put("k", "changed");
    assertEquals("v", cfg.getCustomProp("k"));
  }

  @Test
  @DisplayName("lock result maps conflicting resources to lockers")
  void lockResultMapsConflicts() {
    PSServerLock held =
        new PSServerLock(3, "publisher-job", new int[] {PSServerLockManager.RESOURCE_PUBLISHER});
    PSServerLock requested =
        new PSServerLock(-1, "other", new int[] {PSServerLockManager.RESOURCE_PUBLISHER});

    Map<Integer, PSServerLock> lockerMap =
        PSServerLockResult.lockerMapFromConflicts(List.of(held));
    assertSame(held, lockerMap.get(PSServerLockManager.RESOURCE_PUBLISHER));

    PSServerLockResult failed = new PSServerLockResult(requested, List.of(held));
    assertFalse(failed.wasLockAcquired());
    assertEquals(1, failed.getLockedResources().length);
    assertEquals(PSServerLockManager.RESOURCE_PUBLISHER, failed.getLockedResources()[0]);
    assertEquals("publisher-job", failed.getResourceLocker(PSServerLockManager.RESOURCE_PUBLISHER));
    Iterator<PSServerLock> conflicts = failed.getConflicts();
    assertTrue(conflicts.hasNext());
    assertEquals(3, conflicts.next().getLockId());
    assertFalse(conflicts.hasNext());
  }

  @Test
  @DisplayName("successful lock result has empty conflict map")
  void successfulLockHasNoConflicts() {
    PSServerLock lock =
        new PSServerLock(1, "me", new int[] {PSServerLockManager.RESOURCE_PUBLISHER});
    PSServerLockResult ok = new PSServerLockResult(lock);
    assertTrue(ok.wasLockAcquired());
    assertEquals(0, ok.getLockedResources().length);
    assertFalse(ok.getConflicts().hasNext());
    assertNull(ok.getResourceLocker(PSServerLockManager.RESOURCE_PUBLISHER));
  }

  @Test
  @DisplayName("public leftover APIs advertise generic signatures")
  void publicApiGenericSignatures() throws Exception {
    Method extra =
        PSServer.class.getMethod(
            "getInternalRequest",
            String.class,
            PSRequest.class,
            Map.class,
            boolean.class,
            Document.class);
    assertTrue(extra.getGenericParameterTypes()[2].getTypeName().contains("String"));

    Map<String, String> stringParams = Map.of("a", "1");
    Map<String, Object> copied = PSServer.copyRequestExtraParams(stringParams);
    assertEquals("1", copied.get("a"));
    copied.put("b", Integer.valueOf(2));
    assertEquals(2, copied.get("b"));
    assertFalse(stringParams.containsKey("b"));

    Method custom = PSSearchConfig.class.getMethod("getCustomProps");
    assertTrue(custom.getGenericReturnType().getTypeName().contains("String"));

    Method locks = PSServerLockManager.class.getMethod("getAllLocks");
    assertTrue(locks.getGenericReturnType().getTypeName().contains("PSServerLock"));

    Method conflicts = PSServerLockResult.class.getMethod("getConflicts");
    assertTrue(conflicts.getGenericReturnType().getTypeName().contains("PSServerLock"));
  }
}
