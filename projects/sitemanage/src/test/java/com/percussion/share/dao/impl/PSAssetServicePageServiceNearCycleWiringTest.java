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

import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.impl.PSAssetService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.impl.PSPageService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;

/**
 * Protection for the next-hottest sitemanage injection edge after the folderHelper cycle (#2423).
 *
 * <p>Static inventory (#2463) found only one closed constructor cycle in sitemanage — the known
 * {@code folderHelper → … → contentItemDao → folderHelper} chain broken by {@code @Lazy} on {@link
 * PSContentItemDao}. The next hottest <em>near-cycle</em> is a one-way hub edge:
 *
 * <pre>
 * assetService  →  pageService   (constructor, param @Lazy — #2476)
 * pageService   ↛  assetService  (must remain true)
 * </pre>
 *
 * <p>{@link PSAssetService} is a high fan-in product service that construct-requires {@link
 * IPSPageService}. Class-level {@code @Lazy} on both beans does <em>not</em> break constructor
 * dependency edges when an eager consumer forces creation; parameter {@code @Lazy} injects a proxy
 * (peer of {@link PSContentItemDaoCycleLazyWiringTest}). {@link PSPageService} already
 * construct-requires cycle peers ({@code contentItemDao}, {@code folderHelper}, {@code
 * recycleService}, {@code widgetAssetRelationshipService}). Adding {@link IPSAssetService} to
 * {@code PSPageService}'s constructor without a matching lazy break would still be a wiring hazard;
 * the reverse edge remains banned here and the forward edge carries param {@code @Lazy}.
 *
 * <p>Peers: {@link PSContentItemDaoCycleLazyWiringTest}, {@code FolderHelperCycleContextTest}
 * (#2436). Full inventory: {@code
 * docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md}.
 */
@Tag("UnitTest")
public class PSAssetServicePageServiceNearCycleWiringTest {

  @Test
  public void assetServiceConstructorTakesPageService() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSAssetService.class);
    Parameter pageParam = findParamOfType(ctor, IPSPageService.class);
    assertNotNull(
        pageParam,
        "PSAssetService must still construct-require IPSPageService — inventory #2463 assumed this"
            + " one-way hub edge; if the edge was removed, update the inventory and this test");
  }

  /**
   * Belt-and-braces: param {@code @Lazy} on the forward edge so Spring injects a pageService proxy
   * (#2476). Class-level {@code @Lazy} alone is insufficient when both beans are forced during
   * startup (see inventory note on class vs param {@code @Lazy}).
   */
  @Test
  public void assetServicePageServiceConstructorParameterIsLazy() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSAssetService.class);
    Parameter pageParam = findParamOfType(ctor, IPSPageService.class);
    assertNotNull(pageParam, "Expected an IPSPageService constructor parameter on PSAssetService");
    assertTrue(
        pageParam.isAnnotationPresent(Lazy.class),
        "IPSPageService constructor parameter on PSAssetService must be @Lazy (belt-and-braces"
            + " near-cycle protection for assetService→pageService; see #2476 / #2463)."
            + " pageService is only used post-construction so the proxy is safe.");
  }

  @Test
  public void pageServiceConstructorMustNotTakeAssetService() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSPageService.class);
    Parameter assetParam = findParamOfType(ctor, IPSAssetService.class);
    assertTrue(
        assetParam == null,
        "PSPageService must not construct-require IPSAssetService: that would close a"
            + " pageService↔assetService cycle (next hottest edge after folderHelper; see #2463)."
            + " Prefer method-level lookup, setter injection, or @Lazy on the injection point.");
  }

  /**
   * Intermediate known-cycle edges must stay one-way so a reverse ctor dep cannot bypass the
   * contentItemDao {@code @Lazy} break.
   */
  @Test
  public void knownCycleIntermediateBeansHaveNoReverseConstructorEdges()
      throws NoSuchMethodException {
    // assetDao must not construct-require cycle peers other than contentItemDao (forward)
    assertNoCtorParam(
        com.percussion.assetmanagement.dao.impl.PSAssetDao.class,
        com.percussion.share.dao.IPSFolderHelper.class,
        "PSAssetDao → folderHelper would reverse-bypass contentItemDao @Lazy");
    assertNoCtorParam(
        com.percussion.assetmanagement.dao.impl.PSAssetDao.class,
        com.percussion.recycle.service.IPSRecycleService.class,
        "PSAssetDao → recycleService would form a new mid-cycle");
    assertNoCtorParam(
        com.percussion.assetmanagement.dao.impl.PSAssetDao.class,
        com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService.class,
        "PSAssetDao → widgetAsset would reverse the widgetAsset→assetDao edge");

    // widgetAsset must not construct-require recycle / folderHelper / contentItemDao
    assertNoCtorParam(
        com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService.class,
        com.percussion.recycle.service.IPSRecycleService.class,
        "widgetAsset → recycleService would reverse recycle→widgetAsset");
    assertNoCtorParam(
        com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService.class,
        com.percussion.share.dao.IPSFolderHelper.class,
        "widgetAsset → folderHelper would short-circuit the known cycle");
    assertNoCtorParam(
        com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService.class,
        com.percussion.share.dao.IPSContentItemDao.class,
        "widgetAsset → contentItemDao would skip assetDao and re-cycle via folderHelper");

    // recycleService must not construct-require folderHelper / contentItemDao / assetDao
    assertNoCtorParam(
        com.percussion.recycle.service.impl.PSRecycleService.class,
        com.percussion.share.dao.IPSFolderHelper.class,
        "recycleService → folderHelper would reverse folderHelper→recycleService");
    assertNoCtorParam(
        com.percussion.recycle.service.impl.PSRecycleService.class,
        com.percussion.share.dao.IPSContentItemDao.class,
        "recycleService → contentItemDao would form a parallel cycle path");
    assertNoCtorParam(
        com.percussion.recycle.service.impl.PSRecycleService.class,
        com.percussion.assetmanagement.dao.IPSAssetDao.class,
        "recycleService → assetDao would skip widgetAsset");
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

  private static void assertNoCtorParam(Class<?> bean, Class<?> forbidden, String why)
      throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(bean);
    Parameter p = findParamOfType(ctor, forbidden);
    assertTrue(
        p == null,
        bean.getSimpleName()
            + " must not construct-require "
            + forbidden.getSimpleName()
            + ": "
            + why);
  }
}
