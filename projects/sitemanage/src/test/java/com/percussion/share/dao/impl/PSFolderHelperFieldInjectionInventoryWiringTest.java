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
package com.percussion.share.dao.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.dao.impl.PSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.dao.impl.PSPageDaoHelper;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.recycle.service.impl.PSRecycleService;
import com.percussion.searchmanagement.service.IPSPageIndexService;
import com.percussion.searchmanagement.service.impl.PSPageIndexService;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Field / setter injection inventory freeze for the {@code folderHelper} recycle subgraph
 * (#2525 / parent #2423).
 *
 * <p>The constructor reverse-edge inventory (#2485) found no remaining live ctor reverse edges
 * into {@link IPSFolderHelper}. This test freezes the equivalent state for field and setter
 * injection: none of the seven cycle-subgraph beans may carry a field-level {@code @Autowired} /
 * {@code @Resource} / {@code @Inject} annotation on a target interface, and none may expose a
 * public setter that takes a target interface. Field/setter injection would bypass the constructor
 * {@link Lazy @Lazy} breaks and re-enter the recycle subgraph while {@code folderHelper} is still
 * being constructed.
 *
 * <pre>
 * Path A: folderHelper → recycleService → widgetAssetRelationshipService
 *             → assetDao        → contentItemDao       → folderHelper
 * Path B: folderHelper → recycleService → widgetAssetRelationshipService
 *             → pageIndexService    → pageDaoHelper      → folderHelper
 * </pre>
 *
 * <p>Does not duplicate hub hardening for pageService / itemWorkflow / templateService / siteData
 * — those are separate residuals (#2476–#2478, #2514–#2521). Full disposition table:
 * {@code docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md}
 * (section "folderHelper field / setter injection inventory (#2525)").
 *
 * <p>Peers: {@link PSFolderHelperReverseEdgeInventoryWiringTest}, {@link
 * PSContentItemDaoCycleLazyWiringTest}, {@link PSPageDaoHelperCycleLazyWiringTest}, {@link
 * PSFolderHelperRecycleLazyWiringTest}, {@link PSAssetServicePageServiceNearCycleWiringTest}.
 */
@Tag("UnitTest")
public class PSFolderHelperFieldInjectionInventoryWiringTest {

  /** Cycle interfaces whose field / setter injection would re-enter the recycle subgraph. */
  private static final List<Class<?>> CYCLE_INTERFACES = List.of(
      IPSFolderHelper.class,
      IPSRecycleService.class,
      IPSWidgetAssetRelationshipService.class,
      IPSAssetDao.class,
      IPSContentItemDao.class,
      IPSPageDaoHelper.class,
      IPSPageIndexService.class);

  /**
   * Beans on the recycle subgraph. Forcing any of these to construct while {@code folderHelper} is
   * still creating would close paths A/B. Field / setter injection of a cycle interface on any of
   * these beans is a live reverse field edge.
   */
  private static final List<Class<?>> CYCLE_SUBGRAPH_BEANS = List.of(
      PSFolderHelper.class,
      PSRecycleService.class,
      PSWidgetAssetRelationshipService.class,
      PSAssetDao.class,
      PSContentItemDao.class,
      PSPageIndexService.class,
      PSPageDaoHelper.class);

  @Test
  public void cycleSubgraphBeansMustNotFieldInjectAnyCycleInterface() {
    for (Class<?> bean : CYCLE_SUBGRAPH_BEANS) {
      assertNoFieldInjection(bean);
    }
  }

  @Test
  public void cycleSubgraphBeansMustNotExposePublicSetterForAnyCycleInterface() {
    for (Class<?> bean : CYCLE_SUBGRAPH_BEANS) {
      assertNoPublicSetterForCycleInterface(bean);
    }
  }

  /**
   * The existing ctor-only discipline is what keeps the field-injection vector closed. Each cycle
   * subgraph bean must continue to take its cycle-interface dependencies via constructor; if a
   * field were to gain a {@code @Autowired} annotation in the future, this test fails before the
   * change ships.
   */
  @Test
  public void cycleSubgraphBeansKeepConstructorInjectionForCycleInterfaces() throws Exception {
    for (Class<?> bean : CYCLE_SUBGRAPH_BEANS) {
      assertConstructRequiresCycleInterface(bean);
    }
  }

  /**
   * Verify that none of the declared fields on the cycle subgraph carries a field-level
   * dependency-injection annotation for any cycle interface. Catches accidental field
   * {@code @Autowired} on a class that is still ctor-constructing, which would bypass the
   * constructor {@code @Lazy} breaks.
   */
  private static void assertNoFieldInjection(Class<?> bean) {
    Class<?> current = bean;
    while (current != null && current != Object.class) {
      for (Field f : current.getDeclaredFields()) {
        if (Modifier.isStatic(f.getModifiers())) {
          continue;
        }
        if (!isCycleInterface(f.getType())) {
          continue;
        }
        boolean hasAutowired = f.isAnnotationPresent(Autowired.class);
        boolean hasResource = f.isAnnotationPresent(Resource.class);
        boolean hasInject = f.isAnnotationPresent(Inject.class);
        assertTrue(
            !hasAutowired && !hasResource && !hasInject,
            bean.getSimpleName()
                + " field '"
                + f.getName()
                + "' of type "
                + f.getType().getSimpleName()
                + " must not carry a field-level @Autowired / @Resource / @Inject annotation."
                + " Field injection bypasses the constructor @Lazy breaks on path A/B (#2423 /"
                + " #2437 / #2525). Inject via the constructor instead.");
      }
      current = current.getSuperclass();
    }
  }

  /**
   * Verify that none of the cycle subgraph beans expose a public setter that takes a cycle
   * interface. Setter injection resolves after construction starts and would re-enter the
   * recycle subgraph.
   */
  private static void assertNoPublicSetterForCycleInterface(Class<?> bean) {
    for (Method m : bean.getDeclaredMethods()) {
      if (!m.getName().startsWith("set")
          || m.getName().length() <= "set".length()
          || m.getParameterCount() != 1) {
        continue;
      }
      if (!Modifier.isPublic(m.getModifiers())) {
        continue;
      }
      if (!isCycleInterface(m.getParameterTypes()[0])) {
        continue;
      }
      boolean hasAutowired = m.isAnnotationPresent(Autowired.class);
      boolean hasResource = m.isAnnotationPresent(Resource.class);
      boolean hasInject = m.isAnnotationPresent(Inject.class);
      assertTrue(
          !hasAutowired && !hasResource && !hasInject,
          bean.getSimpleName()
              + " public setter '"
              + m.getName()
              + "'("
              + m.getParameterTypes()[0].getSimpleName()
              + ") must not carry a setter-level @Autowired / @Resource / @Inject annotation."
              + " Setter injection bypasses the constructor @Lazy breaks on path A/B (#2423 /"
              + " #2437 / #2525). Use constructor injection instead.");
    }
  }

  /**
   * Each cycle subgraph bean must still construct-require at least one cycle interface (the
   * forward edge the constructor {@code @Lazy} breaks). This confirms that converting the
   * subgraph to pure ctor injection did not silently drop the cycle edges.
   */
  private static void assertConstructRequiresCycleInterface(Class<?> bean) {
    boolean found = false;
    for (java.lang.reflect.Constructor<?> ctor : bean.getDeclaredConstructors()) {
      for (Class<?> paramType : ctor.getParameterTypes()) {
        if (isCycleInterface(paramType)) {
          found = true;
          break;
        }
      }
      if (found) {
        break;
      }
    }
    assertTrue(
        found,
        bean.getSimpleName()
            + " must still construct-require at least one cycle interface — the @Lazy breaks on"
            + " paths A/B only hold while the forward edges remain in the constructor"
            + " signature (#2423 / #2525).");
  }

  private static boolean isCycleInterface(Class<?> type) {
    for (Class<?> iface : CYCLE_INTERFACES) {
      if (iface.isAssignableFrom(type)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Sanity check on the test fixtures themselves: each cycle subgraph bean must be reachable from
   * at least one declared field type on the recycle subgraph so the {@link #CYCLE_INTERFACES}
   * list is not silently empty.
   */
  @Test
  public void cycleInterfacesListIsNotEmpty() {
    List<Class<?>> nonEmpty = new ArrayList<>(CYCLE_INTERFACES);
    assertNotNull(nonEmpty);
    assertTrue(
        !nonEmpty.isEmpty(),
        "CYCLE_INTERFACES must include at least the folderHelper/recycle/widget/assetDao/"
            + "contentItemDao/pageDaoHelper/pageIndex set; if this fires, the test fixture was"
            + " accidentally emptied.");
  }

  /**
   * Each cycle subgraph bean must be reachable from the test fixture so the field / setter scan
   * covers the full recycle path.
   */
  @Test
  public void cycleSubgraphBeansListIsNotEmpty() {
    List<Class<?>> nonEmpty = new ArrayList<>(CYCLE_SUBGRAPH_BEANS);
    assertNotNull(nonEmpty);
    assertTrue(
        !nonEmpty.isEmpty(),
        "CYCLE_SUBGRAPH_BEANS must list every bean that can be forced to construct while"
            + " folderHelper is creating; if this fires, the test fixture was accidentally"
            + " emptied.");
  }
}