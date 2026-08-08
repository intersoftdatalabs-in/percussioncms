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

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.dao.IPSTemplateDao;
import com.percussion.pagemanagement.dao.impl.PSPageDao;
import com.percussion.pagemanagement.dao.impl.PSPageDaoHelper;
import com.percussion.pagemanagement.dao.impl.PSTemplateDao;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pagemanagement.service.impl.PSTemplateService;
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
 * Protection for the {@link PSTemplateService} high fan-in hub (#2423 residual #2477).
 *
 * <p>Static inventory (#2463) ranked {@code PSTemplateService} as the third-hottest sitemanage hub
 * (ctor out ~6 / in ~20). Unlike {@code pageService} / {@code assetService}, it historically lacked
 * class-level {@code @Lazy}. It construct-requires peers that sit next to the known folderHelper
 * cycle path:
 *
 * <pre>
 * templateService  →  widgetAssetRelationshipService
 * templateService  →  pageDao / pageDaoHelper / templateDao
 * </pre>
 *
 * <p>No closed cycle exists today, but a reverse constructor (or eager field) edge from any of those
 * peers back to {@link IPSTemplateService} would form a new {@code BeanCurrentlyInCreationException}
 * path independent of the contentItemDao {@code @Lazy} break.
 *
 * <p><strong>Disposition (#2477):</strong> class-level {@code @Lazy} on {@link PSTemplateService}
 * (consistent with page/asset hubs) <em>plus</em> reverse-edge bans. Class {@code @Lazy} is lazy
 * init, not a constructor-edge cycle breaker when an eager consumer forces full construction —
 * reverse-edge freezes are the real protection for peers that inject the hub.
 *
 * <p>Peers: {@link PSAssetServicePageServiceNearCycleWiringTest}, {@code
 * PSItemWorkflowServiceHubReverseEdgeWiringTest} (#2478), {@link
 * PSContentItemDaoCycleLazyWiringTest}. Inventory: {@code
 * docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md}.
 */
@Tag("UnitTest")
public class PSTemplateServiceHubNearCycleWiringTest {

  /** Peers that templateService construct-requires and must not reverse-require it. */
  private static final Class<?>[] HUB_PEERS = {
    PSWidgetAssetRelationshipService.class,
    PSPageDao.class,
    PSPageDaoHelper.class,
    PSTemplateDao.class
  };

  @Test
  public void templateServiceIsClassLazy() {
    assertTrue(
        PSTemplateService.class.isAnnotationPresent(Lazy.class),
        "PSTemplateService must carry class-level @Lazy (hub alignment with pageService/assetService;"
            + " disposition #2477). Class @Lazy is not a cycle breaker alone — reverse-edge bans"
            + " below remain required.");
  }

  @Test
  public void templateServiceConstructRequiresWidgetAssetAndPageDaoPeers()
      throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSTemplateService.class);

    assertNotNull(
        findParamOfType(ctor, IPSWidgetAssetRelationshipService.class),
        "PSTemplateService must still construct-require IPSWidgetAssetRelationshipService —"
            + " inventory #2463 / #2477 hub model; if removed, update inventory and this test");
    assertNotNull(
        findParamOfType(ctor, IPSPageDao.class),
        "PSTemplateService must still construct-require IPSPageDao");
    assertNotNull(
        findParamOfType(ctor, IPSPageDaoHelper.class),
        "PSTemplateService must still construct-require IPSPageDaoHelper");
    assertNotNull(
        findParamOfType(ctor, IPSTemplateDao.class),
        "PSTemplateService must still construct-require IPSTemplateDao");
  }

  @Test
  public void hubPeersMustNotConstructRequireTemplateService() throws NoSuchMethodException {
    for (Class<?> peer : HUB_PEERS) {
      Constructor<?> ctor = singlePublicConstructor(peer);
      Parameter template = findParamOfType(ctor, IPSTemplateService.class);
      if (template == null) {
        continue; // desired: no reverse ctor edge
      }
      assertTrue(
          template.isAnnotationPresent(Lazy.class),
          peer.getSimpleName()
              + " construct-requires IPSTemplateService without @Lazy — that closes a"
              + " reverse cycle with the templateService hub (see #2477). Prefer method-level"
              + " lookup, setter injection, or @Lazy on the injection point; document intentional"
              + " @Lazy reverse edges in sitemanage-injection-cycle-inventory.md.");
    }
  }

  /**
   * Eager {@code @Autowired} field inject of {@link IPSTemplateService} on a hub peer is the same
   * class of reverse edge as a constructor param (class-level {@code @Lazy} on either bean does not
   * break an eager field edge from a bean already under construction).
   */
  @Test
  public void hubPeersMustNotEagerFieldInjectTemplateService() {
    List<String> violations = new ArrayList<>();
    for (Class<?> peer : HUB_PEERS) {
      for (Field field : peer.getDeclaredFields()) {
        if (!IPSTemplateService.class.isAssignableFrom(field.getType())) {
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
        "Hub peers must not eagerly field-inject IPSTemplateService (use @Lazy or remove the"
            + " edge). Violations: "
            + String.join("; ", violations));
  }

  /** Named assertion for the primary near-cycle pair called out in #2477 acceptance. */
  @Test
  public void widgetAssetMustNotConstructRequireTemplateService() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSWidgetAssetRelationshipService.class,
        IPSTemplateService.class,
        "widgetAsset → templateService would reverse templateService→widgetAsset (#2477)");
  }

  /** Named assertion for the primary near-cycle pair called out in #2477 acceptance. */
  @Test
  public void pageDaoMustNotConstructRequireTemplateService() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSPageDao.class,
        IPSTemplateService.class,
        "pageDao → templateService would reverse templateService→pageDao (#2477)");
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
