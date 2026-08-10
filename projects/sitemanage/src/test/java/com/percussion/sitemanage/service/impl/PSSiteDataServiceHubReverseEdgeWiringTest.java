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
package com.percussion.sitemanage.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.dao.impl.PSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.assetmanagement.service.impl.PSAssetService;
import com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.impl.PSItemWorkflowService;
import com.percussion.pagemanagement.dao.impl.PSPageDaoHelper;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.impl.PSPageService;
import com.percussion.pagemanagement.service.impl.PSTemplateService;
import com.percussion.pathmanagement.service.impl.PSPathService;
import com.percussion.recycle.service.impl.PSRecycleService;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.impl.PSContentItemDao;
import com.percussion.share.dao.impl.PSFolderHelper;
import com.percussion.sitemanage.service.IPSSiteDataService;
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
 * Protection for the {@link PSSiteDataService} wide sitemanage hub (#2423 residual #2516).
 *
 * <p>Static inventory (#2463) ranked {@code PSSiteDataService} as the sixth-hottest sitemanage hub
 * (ctor out ~15 / in ~12). It is class-{@code @Lazy} and construct-requires known-cycle peers /
 * page-item hubs:
 *
 * <pre>
 * siteDataService
 *   → folderHelper
 *   → widgetAssetRelationshipService
 *   → assetDao
 *   → itemWorkflowService
 *   (+ field: pageService, pathService)
 * </pre>
 *
 * <p>Those peers sit on or next to the known {@code folderHelper → … → contentItemDao →
 * folderHelper} chain (broken by {@code @Lazy} on {@link PSContentItemDao}). A reverse constructor
 * (or eager field) edge from any of those peers / page-item hubs back to {@link IPSSiteDataService}
 * would form a new creation cycle independent of the contentItemDao {@code @Lazy} break:
 *
 * <pre>
 * folderHelper → recycle → widgetAsset → assetDao → contentItemDao → folderHelper
 *       ↑______________________ siteDataService _______________________________/
 * </pre>
 *
 * <p>This test freezes the one-way hub edges and forbids reverse ctor / non-{@code @Lazy} field
 * inject of {@code IPSSiteDataService} on the cycle peers and rank-1/2 page/item hubs. Intentional
 * reverse edges must use parameter/field {@link Lazy @Lazy} and be documented in the inventory
 * note.
 *
 * <p>Scan snapshot (2026-08-08 / #2516): none of the listed peers currently inject {@code
 * IPSSiteDataService} (ctor or field). Downstream consumers (path items, REST adaptors, publish
 * handlers) remain allowed — they are not cycle fuel.
 *
 * <p>Peers: {@link com.percussion.share.dao.impl.PSItemWorkflowServiceHubReverseEdgeWiringTest},
 * {@code PSPageServiceCycleWiringTest}, {@code PSContentItemDaoCycleLazyWiringTest}. Inventory:
 * {@code
 * docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md}.
 */
@Tag("UnitTest")
public class PSSiteDataServiceHubReverseEdgeWiringTest {

  /**
   * Cycle peers that siteData construct-requires (or that sit on the folderHelper creation path)
   * plus rank-1/2 page/item hubs that must not reverse-require siteData without {@code @Lazy}.
   */
  private static final Class<?>[] CYCLE_PEERS_AND_PAGE_ITEM_HUBS = {
    PSFolderHelper.class,
    PSContentItemDao.class,
    PSWidgetAssetRelationshipService.class,
    PSRecycleService.class,
    PSAssetDao.class,
    PSPageDaoHelper.class,
    PSPageService.class,
    PSItemWorkflowService.class,
    PSAssetService.class,
    PSTemplateService.class
  };

  @Test
  public void siteDataServiceIsClassLevelLazy() {
    assertTrue(
        PSSiteDataService.class.isAnnotationPresent(Lazy.class),
        "PSSiteDataService must carry class-level @Lazy to harden against cycle formation"
            + " (#2423 residual #2516 / #2463 inventory). Class @Lazy defers the bean until first"
            + " use; it is not a constructor-cycle breaker once creation starts.");
  }

  @Test
  public void siteDataServiceConstructRequiresCyclePeers() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSSiteDataService.class);

    assertNotNull(
        findParamOfType(ctor, IPSFolderHelper.class),
        "PSSiteDataService must still construct-require IPSFolderHelper — inventory #2463 / #2516"
            + " hub model; if removed, update inventory and this test");
    assertNotNull(
        findParamOfType(ctor, IPSWidgetAssetRelationshipService.class),
        "PSSiteDataService must still construct-require IPSWidgetAssetRelationshipService");
    assertNotNull(
        findParamOfType(ctor, IPSAssetDao.class),
        "PSSiteDataService must still construct-require IPSAssetDao");
    assertNotNull(
        findParamOfType(ctor, IPSItemWorkflowService.class),
        "PSSiteDataService must still construct-require IPSItemWorkflowService");
  }

  /**
   * Documented forward field edges (not reverse-cycle fuel): siteData → pageService / pathService.
   * These are intentional consumer edges from the hub; the reverse (page/path → siteData) is banned
   * via the peer scan for {@link PSPageService} (path service is not a cycle peer).
   */
  @Test
  public void siteDataServiceFieldInjectsPageAndPathServices() {
    assertTrue(
        hasFieldOfType(PSSiteDataService.class, IPSPageService.class)
            || hasFieldOfType(PSSiteDataService.class, PSPageService.class),
        "PSSiteDataService should still field-inject pageService (inventory #2463 / #2516); if"
            + " moved to ctor without @Lazy on a reverse partner, re-scan");
    assertTrue(
        hasFieldOfType(PSSiteDataService.class, PSPathService.class),
        "PSSiteDataService should still field-inject pathService (inventory #2463 / #2516)");
  }

  @Test
  public void cyclePeersAndPageItemHubsMustNotConstructRequireSiteDataService() {
    for (Class<?> peer : CYCLE_PEERS_AND_PAGE_ITEM_HUBS) {
      // Scan all declared constructors so a non-public @Autowired wiring ctor is not missed.
      for (Constructor<?> ctor : peer.getDeclaredConstructors()) {
        Parameter siteData = findParamOfType(ctor, IPSSiteDataService.class);
        if (siteData == null) {
          continue; // desired: no reverse ctor edge on this ctor
        }
        // Intentional exception path: only allowed with parameter @Lazy
        assertTrue(
            siteData.isAnnotationPresent(Lazy.class),
            peer.getSimpleName()
                + " construct-requires IPSSiteDataService without @Lazy — that closes a reverse"
                + " cycle with the siteDataService hub (see #2516). Prefer method-level lookup,"
                + " setter injection, or @Lazy on the injection point; document intentional @Lazy"
                + " reverse edges in sitemanage-injection-cycle-inventory.md.");
      }
    }
  }

  /**
   * Eager {@code @Autowired} field inject of {@link IPSSiteDataService} on a cycle peer / page-item
   * hub is the same class of reverse edge as a constructor param (class-level {@code @Lazy} on the
   * peer does not break an eager field edge from a bean already under construction).
   *
   * <p>Also flags unannotated fields of that type (legacy XML/setter surfaces) unless marked
   * {@code @Lazy}.
   *
   * <p><strong>Intentional exceptions:</strong> none among {@link #CYCLE_PEERS_AND_PAGE_ITEM_HUBS}
   * as of #2516. If a peer later needs a reverse edge, add field/param {@code @Lazy} and document
   * it in the inventory (same pattern as {@code PSAssetService → IPSPageService} for #2476).
   */
  @Test
  public void cyclePeersAndPageItemHubsMustNotEagerFieldInjectSiteDataService() {
    List<String> violations = new ArrayList<>();
    for (Class<?> peer : CYCLE_PEERS_AND_PAGE_ITEM_HUBS) {
      // Walk hierarchy so inherited reverse edges are not silently skipped (#2516 review).
      for (Field field : declaredFieldsIncludingInherited(peer)) {
        if (!IPSSiteDataService.class.isAssignableFrom(field.getType())
            && !PSSiteDataService.class.isAssignableFrom(field.getType())) {
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
        "Cycle peers / page-item hubs must not eagerly field-inject IPSSiteDataService (use @Lazy"
            + " or remove the edge). Violations: "
            + String.join("; ", violations));
  }

  @Test
  public void contentItemDaoStillHasNoSiteDataConstructorEdge() throws NoSuchMethodException {
    // Explicit named assertion for the known-cycle break peer (contentItemDao is the @Lazy site)
    assertNoEagerCtorParam(
        PSContentItemDao.class,
        IPSSiteDataService.class,
        "contentItemDao → siteDataService would couple the cycle-break peer to the hub");
  }

  @Test
  public void pageServiceStillHasNoSiteDataConstructorEdge() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSPageService.class,
        IPSSiteDataService.class,
        "pageService → siteDataService would reverse siteData→page field edge and form a hub↔hub"
            + " cycle");
  }

  @Test
  public void itemWorkflowStillHasNoSiteDataConstructorEdge() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSItemWorkflowService.class,
        IPSSiteDataService.class,
        "itemWorkflow → siteDataService would reverse siteData→itemWorkflow forward edge");
  }

  /**
   * Prefer a single {@code @Autowired} declared constructor (Spring wiring ctor), then fall back to
   * the single public constructor. Avoids missing a non-public wiring ctor when a public no-arg
   * also exists (#2516 review).
   */
  private static Constructor<?> singlePublicConstructor(Class<?> type)
      throws NoSuchMethodException {
    List<Constructor<?>> autowired = new ArrayList<>();
    for (Constructor<?> ctor : type.getDeclaredConstructors()) {
      if (ctor.isAnnotationPresent(Autowired.class)) {
        autowired.add(ctor);
      }
    }
    if (autowired.size() == 1) {
      return autowired.get(0);
    }
    if (autowired.size() > 1) {
      fail(
          type.getSimpleName()
              + " expected at most one @Autowired constructor for wiring inspection, found "
              + autowired.size());
    }
    Constructor<?>[] publicCtors = type.getConstructors();
    if (publicCtors.length == 1) {
      return publicCtors[0];
    }
    Constructor<?>[] declared = type.getDeclaredConstructors();
    if (declared.length == 1) {
      return declared[0];
    }
    fail(
        type.getSimpleName()
            + " expected exactly one public (or single declared / @Autowired) constructor for"
            + " wiring inspection, found public="
            + publicCtors.length
            + " declared="
            + declared.length);
    return null; // unreachable
  }

  private static Parameter findParamOfType(Constructor<?> ctor, Class<?> paramType) {
    for (Parameter p : ctor.getParameters()) {
      if (paramType.isAssignableFrom(p.getType())) {
        return p;
      }
    }
    return null;
  }

  /** Declared fields on {@code bean} and all superclasses (stops at {@link Object}). */
  private static List<Field> declaredFieldsIncludingInherited(Class<?> bean) {
    List<Field> fields = new ArrayList<>();
    Class<?> current = bean;
    while (current != null && current != Object.class) {
      for (Field field : current.getDeclaredFields()) {
        fields.add(field);
      }
      current = current.getSuperclass();
    }
    return fields;
  }

  private static boolean hasFieldOfType(Class<?> bean, Class<?> fieldType) {
    for (Field field : declaredFieldsIncludingInherited(bean)) {
      if (fieldType.isAssignableFrom(field.getType())) {
        return true;
      }
    }
    return false;
  }

  private static void assertNoEagerCtorParam(Class<?> bean, Class<?> forbidden, String why) {
    for (Constructor<?> ctor : bean.getDeclaredConstructors()) {
      Parameter p = findParamOfType(ctor, forbidden);
      if (p == null) {
        continue;
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
}
