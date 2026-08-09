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
import com.percussion.pagemanagement.dao.impl.PSPageDaoHelper;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pagemanagement.service.impl.PSPageService;
import com.percussion.pagemanagement.service.impl.PSTemplateService;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.recycle.service.impl.PSRecycleService;
import com.percussion.searchmanagement.service.IPSPageIndexService;
import com.percussion.searchmanagement.service.impl.PSPageIndexService;
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
 * Protection for the {@link PSWidgetAssetRelationshipService} rank-4 hub on the known folderHelper
 * cycle path (#2423 residual #2519).
 *
 * <p>Static inventory (#2463) ranks {@code PSWidgetAssetRelationshipService} as the fourth-hottest
 * sitemanage hub (ctor out ~4 / in ~18). It sits <em>on</em> the known creation chain:
 *
 * <pre>
 * folderHelper → recycleService → widgetAssetRelationshipService → assetDao → contentItemDao
 *       ↑_______________________________ @Lazy break ________________________________/
 *
 * folderHelper → recycleService → widgetAssetRelationshipService → pageIndexService
 *       → pageDaoHelper → folderHelper   (@Lazy on pageDaoHelper)
 * </pre>
 *
 * <p><strong>Disposition (#2519):</strong> class-level {@code @Lazy} on {@link
 * PSWidgetAssetRelationshipService} (hub alignment with page/template/itemWorkflow) <em>plus</em>
 * reverse-edge freezes. Class {@code @Lazy} is lazy init only — not a constructor-edge cycle
 * breaker when an eager consumer forces full construction.
 *
 * <p>Forward fan-in into this hub from {@code recycleService} / {@code pageService} / {@code
 * templateService} is intentional product wiring and is frozen positive below. The dangerous edges
 * are reverse of known one-ways (assetDao → widgetAsset; widgetAsset → recycle / page / template)
 * and eager reverse field inject of the hub on cycle-path peers.
 *
 * <p>Peers: {@link PSItemWorkflowServiceHubReverseEdgeWiringTest} (#2478), {@code
 * PSTemplateServiceCycleWiringTest} (#2477), {@link PSAssetServicePageServiceNearCycleWiringTest}.
 * Inventory: {@code
 * docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md}.
 *
 * <p>Out of scope: managedLink path-C ctor inject (#2527).
 */
@Tag("UnitTest")
public class PSWidgetAssetRelationshipServiceHubReverseEdgeWiringTest {

  /**
   * Cycle-path peers that must not reverse-construct-require {@link
   * IPSWidgetAssetRelationshipService} without param {@code @Lazy}. These are dependents on the
   * folderHelper creation subgraph (or the known-cycle break peers) — <em>not</em> intentional
   * product consumers (recycle / page / template / assetService), which inject the hub forward.
   */
  private static final Class<?>[] CYCLE_PATH_REVERSE_PEERS = {
    PSAssetDao.class,
    PSContentItemDao.class,
    PSFolderHelper.class,
    PSPageIndexService.class,
    PSPageDaoHelper.class
  };

  @Test
  public void widgetAssetRelationshipServiceIsClassLevelLazy() {
    assertTrue(
        PSWidgetAssetRelationshipService.class.isAnnotationPresent(Lazy.class),
        "PSWidgetAssetRelationshipService must carry class-level @Lazy (hub alignment with"
            + " pageService/templateService/itemWorkflow; disposition #2519). Class @Lazy is not a"
            + " cycle breaker alone — reverse-edge bans below remain required.");
  }

  @Test
  public void widgetAssetConstructRequiresAssetDaoAndPageIndex() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSWidgetAssetRelationshipService.class);

    assertNotNull(
        findParamOfType(ctor, IPSAssetDao.class),
        "PSWidgetAssetRelationshipService must still construct-require IPSAssetDao — inventory"
            + " #2463 / #2519 hub model on path A; if removed, update inventory and this test");
    assertNotNull(
        findParamOfType(ctor, IPSPageIndexService.class),
        "PSWidgetAssetRelationshipService must still construct-require IPSPageIndexService —"
            + " inventory #2463 path B; if removed, update inventory and this test");
  }

  /**
   * Intentional forward fan-in: product hubs on/near the cycle path construct-require widgetAsset.
   * Freezing these positive edges documents that recycle/page/template → widgetAsset is <em>not</em>
   * a reverse edge (see inventory #2463 / #2519).
   */
  @Test
  public void intentionalConsumersStillConstructRequireWidgetAsset() throws NoSuchMethodException {
    assertNotNull(
        findParamOfType(
            singlePublicConstructor(PSRecycleService.class), IPSWidgetAssetRelationshipService.class),
        "PSRecycleService must still construct-require IPSWidgetAssetRelationshipService"
            + " (known-cycle path A/B forward edge)");
    assertNotNull(
        findParamOfType(
            singlePublicConstructor(PSPageService.class), IPSWidgetAssetRelationshipService.class),
        "PSPageService must still construct-require IPSWidgetAssetRelationshipService"
            + " (rank-1 hub fan-in; inventory #2463)");
    assertNotNull(
        findParamOfType(
            singlePublicConstructor(PSTemplateService.class),
            IPSWidgetAssetRelationshipService.class),
        "PSTemplateService must still construct-require IPSWidgetAssetRelationshipService"
            + " (rank-3 hub fan-in; #2477)");
  }

  @Test
  public void cyclePathPeersMustNotConstructRequireWidgetAssetWithoutLazy()
      throws NoSuchMethodException {
    for (Class<?> peer : CYCLE_PATH_REVERSE_PEERS) {
      Constructor<?> ctor = singlePublicConstructor(peer);
      Parameter widgetAsset = findParamOfType(ctor, IPSWidgetAssetRelationshipService.class);
      if (widgetAsset == null) {
        continue; // desired: no reverse ctor edge
      }
      assertTrue(
          widgetAsset.isAnnotationPresent(Lazy.class),
          peer.getSimpleName()
              + " construct-requires IPSWidgetAssetRelationshipService without @Lazy — that"
              + " closes a reverse cycle with the widgetAsset hub on the folderHelper path (see"
              + " #2519). Prefer method-level lookup, setter injection, or @Lazy on the injection"
              + " point; document intentional @Lazy reverse edges in"
              + " sitemanage-injection-cycle-inventory.md.");
    }
  }

  /**
   * Eager {@code @Autowired} field inject of the hub on a cycle-path peer is the same class of
   * reverse edge as a constructor param (class-level {@code @Lazy} on either bean does not break an
   * eager field edge from a bean already under construction).
   */
  @Test
  public void cyclePathPeersMustNotEagerFieldInjectWidgetAsset() {
    List<String> violations = new ArrayList<>();
    for (Class<?> peer : CYCLE_PATH_REVERSE_PEERS) {
      for (Class<?> type = peer; type != null && type != Object.class; type = type.getSuperclass()) {
        for (Field field : type.getDeclaredFields()) {
          if (!IPSWidgetAssetRelationshipService.class.isAssignableFrom(field.getType())
              && !PSWidgetAssetRelationshipService.class.isAssignableFrom(field.getType())) {
            continue;
          }
          if (field.isAnnotationPresent(Lazy.class)) {
            continue; // documented intentional reverse edge
          }
          boolean autowired = field.isAnnotationPresent(Autowired.class);
          String owner =
              type.equals(peer)
                  ? peer.getSimpleName()
                  : peer.getSimpleName() + ":" + type.getSimpleName();
          violations.add(
              owner
                  + "."
                  + field.getName()
                  + (autowired ? " (@Autowired)" : " (field type)")
                  + " — reverse field edge without @Lazy");
        }
      }
    }
    assertTrue(
        violations.isEmpty(),
        "Cycle-path peers must not eagerly field-inject IPSWidgetAssetRelationshipService (use"
            + " @Lazy or remove the edge). Violations: "
            + String.join("; ", violations));
  }

  /** Named assertion: reverse of widgetAsset → assetDao would skip the contentItemDao @Lazy break. */
  @Test
  public void assetDaoMustNotConstructRequireWidgetAsset() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSAssetDao.class,
        IPSWidgetAssetRelationshipService.class,
        "assetDao → widgetAsset would reverse widgetAsset→assetDao and skip the contentItemDao"
            + " @Lazy break (#2519 / inventory path A)");
  }

  /** Named assertion: reverse of recycle → widgetAsset closes the mid-chain. */
  @Test
  public void widgetAssetMustNotConstructRequireRecycleService() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSWidgetAssetRelationshipService.class,
        IPSRecycleService.class,
        "widgetAsset → recycleService would reverse recycle→widgetAsset and close a mid-chain"
            + " cycle (#2519)");
  }

  /** Named assertion: reverse of pageService → widgetAsset closes a hub↔hub cycle branch. */
  @Test
  public void widgetAssetMustNotConstructRequirePageService() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSWidgetAssetRelationshipService.class,
        IPSPageService.class,
        "widgetAsset → pageService would reverse pageService→widgetAsset (#2519 / #2514)");
  }

  /** Named assertion: reverse of templateService → widgetAsset closes a hub↔hub cycle branch. */
  @Test
  public void widgetAssetMustNotConstructRequireTemplateService() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSWidgetAssetRelationshipService.class,
        IPSTemplateService.class,
        "widgetAsset → templateService would reverse templateService→widgetAsset (#2519 /"
            + " #2477)");
  }

  @Test
  public void pageIndexServiceMustNotConstructRequireWidgetAsset() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSPageIndexService.class,
        IPSWidgetAssetRelationshipService.class,
        "pageIndexService → widgetAsset would reverse widgetAsset→pageIndex (path B)");
  }

  @Test
  public void contentItemDaoStillHasNoWidgetAssetConstructorEdge() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSContentItemDao.class,
        IPSWidgetAssetRelationshipService.class,
        "contentItemDao → widgetAsset would couple the cycle-break peer to the hub");
  }

  @Test
  public void folderHelperStillHasNoWidgetAssetConstructorEdge() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSFolderHelper.class,
        IPSWidgetAssetRelationshipService.class,
        "folderHelper → widgetAsset would short-circuit recycle on the known cycle path");
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
