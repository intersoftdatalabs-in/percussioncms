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
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.dao.impl.PSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.impl.PSItemWorkflowService;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.recycle.service.impl.PSRecycleService;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Protection for the {@link PSItemWorkflowService} high fan-in hub (#2423 residual #2478).
 *
 * <p>Static inventory (#2463) ranked {@code PSItemWorkflowService} as the second-hottest sitemanage
 * hub (ctor out ~7 / in ~23). It construct-requires known-cycle peers:
 *
 * <pre>
 * itemWorkflowService
 *   → assetDao
 *   → folderHelper
 *   → recycleService
 *   → widgetAssetRelationshipService
 * </pre>
 *
 * <p>Those peers sit on the known {@code folderHelper → … → contentItemDao → folderHelper} chain
 * (broken by {@code @Lazy} on {@link PSContentItemDao}). A reverse constructor (or eager field)
 * edge from any of those peers back to {@link IPSItemWorkflowService} would form a new creation
 * cycle independent of the contentItemDao {@code @Lazy} break:
 *
 * <pre>
 * folderHelper → recycle → widgetAsset → assetDao → contentItemDao → folderHelper
 *       ↑________________________________ itemWorkflow ______________________/
 * </pre>
 *
 * <p>This test freezes the one-way hub edges and forbids reverse ctor / non-{@code @Lazy} field
 * inject of {@code IPSItemWorkflowService} on the cycle peers. Intentional reverse edges must use
 * parameter/field {@link Lazy @Lazy} and be documented in the inventory note.
 *
 * <p>Peers: {@link PSAssetServicePageServiceNearCycleWiringTest}, {@link
 * PSContentItemDaoCycleLazyWiringTest}, {@code FolderHelperCycleContextTest} (#2436). Inventory:
 * {@code docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md}.
 */
@Tag("UnitTest")
public class PSItemWorkflowServiceHubReverseEdgeWiringTest {

  /** Cycle peers that itemWorkflow construct-requires (and must not reverse-require it). */
  private static final Class<?>[] CYCLE_PEERS = {
    PSAssetDao.class,
    PSContentItemDao.class,
    PSWidgetAssetRelationshipService.class,
    PSRecycleService.class,
    PSFolderHelper.class
  };

  @Test
  public void itemWorkflowServiceConstructRequiresCyclePeers() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSItemWorkflowService.class);

    assertNotNull(
        findParamOfType(ctor, IPSAssetDao.class),
        "PSItemWorkflowService must still construct-require IPSAssetDao — inventory #2463 / #2478"
            + " hub model; if removed, update inventory and this test");
    assertNotNull(
        findParamOfType(ctor, IPSFolderHelper.class),
        "PSItemWorkflowService must still construct-require IPSFolderHelper");
    assertNotNull(
        findParamOfType(ctor, IPSRecycleService.class),
        "PSItemWorkflowService must still construct-require IPSRecycleService");
    assertNotNull(
        findParamOfType(ctor, IPSWidgetAssetRelationshipService.class),
        "PSItemWorkflowService must still construct-require IPSWidgetAssetRelationshipService");
  }

  @Test
  public void cyclePeersMustNotConstructRequireItemWorkflowService() throws NoSuchMethodException {
    for (Class<?> peer : CYCLE_PEERS) {
      Constructor<?> ctor = singlePublicConstructor(peer);
      Parameter itemWf = findParamOfType(ctor, IPSItemWorkflowService.class);
      if (itemWf == null) {
        continue; // desired: no reverse ctor edge
      }
      // Intentional exception path: only allowed with parameter @Lazy
      assertTrue(
          itemWf.isAnnotationPresent(Lazy.class),
          peer.getSimpleName()
              + " construct-requires IPSItemWorkflowService without @Lazy — that closes a"
              + " reverse cycle with the itemWorkflow hub (see #2478). Prefer method-level lookup,"
              + " setter injection, or @Lazy on the injection point; document intentional @Lazy"
              + " reverse edges in sitemanage-injection-cycle-inventory.md.");
    }
  }

  /**
   * Eager {@code @Autowired} field inject of {@link IPSItemWorkflowService} on a cycle peer is the
   * same class of reverse edge as a constructor param (class-level {@code @Lazy} on the peer does
   * not break an eager field edge from a bean already under construction).
   *
   * <p>Also flags unannotated fields of that type (legacy XML/setter surfaces) unless marked
   * {@code @Lazy}.
   */
  @Test
  public void cyclePeersMustNotEagerFieldInjectItemWorkflowService() {
    List<String> violations = new ArrayList<>();
    for (Class<?> peer : CYCLE_PEERS) {
      for (Field field : peer.getDeclaredFields()) {
        if (!IPSItemWorkflowService.class.isAssignableFrom(field.getType())) {
          continue;
        }
        if (field.isAnnotationPresent(Lazy.class)) {
          continue; // documented intentional reverse edge
        }
        boolean autowired = field.isAnnotationPresent(Autowired.class);
        violations.add(
            peer.getSimpleName()
                + "."
                + field.getName()
                + (autowired ? " (@Autowired)" : " (field type)")
                + " — reverse field edge without @Lazy");
      }
    }
    assertTrue(
        violations.isEmpty(),
        "Cycle peers must not eagerly field-inject IPSItemWorkflowService (use @Lazy or remove"
            + " the edge). Violations: "
            + String.join("; ", violations));
  }

  @Test
  public void contentItemDaoStillHasNoItemWorkflowConstructorEdge() throws NoSuchMethodException {
    // Explicit named assertion for the known-cycle break peer (contentItemDao is the @Lazy site)
    assertNoEagerCtorParam(
        PSContentItemDao.class,
        IPSItemWorkflowService.class,
        "contentItemDao → itemWorkflow would couple the cycle-break peer to the hub");
  }

  private static Constructor<?> singlePublicConstructor(Class<?> type)
      throws NoSuchMethodException {
    Constructor<?>[] ctors = type.getConstructors();
    if (ctors.length != 1) {
      fail(
          type.getSimpleName()
              + " expected exactly one public constructor for wiring inspection, found "
              + ctors.length);
    }
    return ctors[0];
  }

  private static Parameter findParamOfType(Constructor<?> ctor, Class<?> paramType) {
    for (Parameter p : ctor.getParameters()) {
      if (paramType.isAssignableFrom(p.getType())) {
        return p;
      }
    }
    return null;
  }

  private static void assertNoEagerCtorParam(Class<?> bean, Class<?> forbidden, String why)
      throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(bean);
    Parameter p = findParamOfType(ctor, forbidden);
    if (p == null) {
      return;
    }
    assertTrue(
        p.isAnnotationPresent(Lazy.class),
        bean.getSimpleName()
            + " must not construct-require "
            + forbidden.getSimpleName()
            + " without @Lazy: "
            + why);
  }
}
