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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.assetmanagement.dao.impl.PSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.assetmanagement.service.impl.PSAssetService;
import com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.impl.PSItemWorkflowService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.recycle.service.impl.PSRecycleService;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.impl.PSContentItemDao;
import com.percussion.share.dao.impl.PSFolderHelper;
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
 * Reverse-edge cycle protection for the {@link PSPageService} high fan-in hub (#2423 residual
 * #2514).
 *
 * <p>Per the static inventory in {@code
 * docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md},
 * {@code PSPageService} is the rank-1 sitemanage fan-in hub (out ~11 / in ~28) and
 * construct-requires the cycle peers:
 *
 * <pre>
 * pageService
 *   → contentItemDao
 *   → folderHelper
 *   → recycleService
 *   → widgetAssetRelationshipService
 *   → itemWorkflow
 * </pre>
 *
 * <p>The forward edges are intentional and broken on the {@code contentItemDao} side via param
 * {@code @Lazy} there (path A). This test freezes the reverse direction: cycle peers and related
 * high-risk hubs must not construct-require or eagerly field-inject {@link IPSPageService} without
 * parameter/field {@link Lazy @Lazy}, since adding such a reverse edge would close a new cycle path
 * independent of the contentItemDao break.
 *
 * <p><strong>Intentional {@code @Lazy} exception (documented, not banned):</strong> {@link
 * PSAssetService} construct-requires {@code IPSPageService} with param {@code @Lazy} (#2476). That
 * is a product consumer edge, not a cycle-peer reverse. It is asserted as present + {@code @Lazy}
 * below; the companion reverse ban ({@code PSPageService ↛ IPSAssetService}) lives in {@code
 * PSAssetServicePageServiceNearCycleWiringTest}.
 *
 * <p>Peers: {@code PSItemWorkflowServiceHubReverseEdgeWiringTest} (#2478 pattern this class
 * mirrors), {@code PSAssetServicePageServiceNearCycleWiringTest}, {@code
 * PSContentItemDaoCycleLazyWiringTest}, {@code PSTemplateServiceCycleWiringTest} (#2477).
 */
@Tag("UnitTest")
public class PSPageServiceCycleWiringTest {

  /**
   * Cycle peers and high-risk intermediates that pageService construct-requires (or that sit on the
   * known folderHelper→…→contentItemDao chain). None of these may reverse-require {@link
   * IPSPageService} without {@code @Lazy}.
   */
  private static final Class<?>[] CYCLE_PEERS = {
    PSFolderHelper.class,
    PSContentItemDao.class,
    PSRecycleService.class,
    PSWidgetAssetRelationshipService.class,
    PSItemWorkflowService.class,
    PSAssetDao.class
  };

  @Test
  public void pageServiceIsClassLevelLazy() {
    assertTrue(
        PSPageService.class.isAnnotationPresent(Lazy.class),
        "PSPageService must carry class-level @Lazy to harden against cycle formation"
            + " (#2423 residual #2514 / #2463 inventory). Class @Lazy defers the bean until"
            + " first use, breaking any eager consumer path that could close a cycle through"
            + " pageService. Class @Lazy is lazy-init only — not a substitute for reverse-edge"
            + " bans or param @Lazy on reverse partners.");
  }

  @Test
  public void pageServiceConstructRequiresCyclePeers() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSPageService.class);

    assertNotNull(
        findParamOfType(ctor, IPSFolderHelper.class),
        "PSPageService must still construct-require IPSFolderHelper — inventory #2463 / #2514 hub"
            + " model; the cycle is broken on contentItemDao, not by removing this forward edge");
    assertNotNull(
        findParamOfType(ctor, IPSContentItemDao.class),
        "PSPageService must still construct-require IPSContentItemDao; removing the edge would"
            + " invalidate the contentItemDao @Lazy break relative to this hub");
    assertNotNull(
        findParamOfType(ctor, IPSRecycleService.class),
        "PSPageService must still construct-require IPSRecycleService; reverse-edge into recycle is"
            + " forbidden below");
    assertNotNull(
        findParamOfType(ctor, IPSWidgetAssetRelationshipService.class),
        "PSPageService must still construct-require IPSWidgetAssetRelationshipService (inventory"
            + " #2463 cycle peer)");
    assertNotNull(
        findParamOfType(ctor, IPSItemWorkflowService.class),
        "PSPageService must still construct-require IPSItemWorkflowService (inventory #2463 cycle"
            + " peer); reverse itemWorkflow→pageService is banned below");
  }

  /**
   * Reverse edges are the dangerous ones: cycle peers must not construct-require {@link
   * IPSPageService} without param {@code @Lazy}. Intentional reverse edges (none among cycle peers
   * today) must use parameter {@code @Lazy} and be documented in the inventory note.
   */
  @Test
  public void cyclePeersMustNotConstructRequirePageService() throws NoSuchMethodException {
    for (Class<?> peer : CYCLE_PEERS) {
      Constructor<?> ctor = singlePublicConstructor(peer);
      Parameter page = findParamOfType(ctor, IPSPageService.class);
      if (page == null) {
        continue; // desired: no reverse ctor edge
      }
      // Intentional exception path: only allowed with parameter @Lazy
      assertTrue(
          page.isAnnotationPresent(Lazy.class),
          peer.getSimpleName()
              + " construct-requires IPSPageService without @Lazy — that closes a reverse cycle"
              + " with the pageService hub (see #2514). Prefer method-level lookup, setter"
              + " injection, or @Lazy on the injection point; document intentional @Lazy reverse"
              + " edges in sitemanage-injection-cycle-inventory.md.");
    }
  }

  /**
   * Eager {@code @Autowired} field inject of {@link IPSPageService} on a cycle peer is the same
   * class of reverse edge as a constructor param (class-level {@code @Lazy} on the peer does not
   * break an eager field edge from a bean already under construction).
   *
   * <p>Also flags unannotated fields of that type (legacy XML/setter surfaces) unless marked
   * {@code @Lazy}.
   */
  @Test
  public void cyclePeersMustNotEagerFieldInjectPageService() {
    List<String> violations = new ArrayList<>();
    for (Class<?> peer : CYCLE_PEERS) {
      for (Field field : peer.getDeclaredFields()) {
        if (!IPSPageService.class.isAssignableFrom(field.getType())) {
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
        "Cycle peers must not eagerly field-inject IPSPageService (use @Lazy or remove the edge)."
            + " Violations: "
            + String.join("; ", violations));
  }

  /**
   * Documented intentional reverse partner: {@link PSAssetService} → {@link IPSPageService} with
   * param {@code @Lazy} (#2476). Not a cycle peer of the folderHelper chain, but the next-hottest
   * near-cycle consumer of pageService. Must remain {@code @Lazy}; bare reverse would re-open
   * assetService↔pageService risk independent of folderHelper.
   */
  @Test
  public void assetServiceIntentionalPageServiceEdgeIsLazy() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSAssetService.class);
    Parameter pageParam = findParamOfType(ctor, IPSPageService.class);
    assertNotNull(
        pageParam,
        "PSAssetService must still construct-require IPSPageService — inventory #2463 / #2476"
            + " intentional reverse partner of the pageService hub; if removed, update inventory"
            + " and PSAssetServicePageServiceNearCycleWiringTest");
    assertTrue(
        pageParam.isAnnotationPresent(Lazy.class),
        "IPSPageService constructor parameter on PSAssetService must remain @Lazy (intentional"
            + " reverse partner of the pageService hub; see #2476 / #2514). Do not drop @Lazy"
            + " without a new cycle break documented in the inventory.");
  }

  @Test
  public void contentItemDaoStillHasNoPageServiceConstructorEdge() throws NoSuchMethodException {
    // Explicit named assertion for the known-cycle break peer (contentItemDao is the @Lazy site)
    assertNoEagerCtorParam(
        PSContentItemDao.class,
        IPSPageService.class,
        "contentItemDao → pageService would couple the cycle-break peer to the rank-1 hub");
  }

  @Test
  public void pageServiceConstructorExists() throws NoSuchMethodException {
    // Sanity guard: PSPageService must declare exactly one public constructor for Spring
    // wiring inspection; this test enforces the assumption shared by all assertions above.
    Constructor<?> ctor = singlePublicConstructor(PSPageService.class);
    assertNotNull(
        ctor,
        "PSPageService must declare exactly one public constructor for Spring wiring inspection;"
            + " this test enforces the assumption");
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
