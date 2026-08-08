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

import com.percussion.assetmanagement.dao.impl.PSAssetDao;
import com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService;
import com.percussion.linkmanagement.service.IPSManagedLinkService;
import com.percussion.pagemanagement.dao.impl.PSPageDao;
import com.percussion.pagemanagement.dao.impl.PSPageDaoHelper;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.recycle.service.impl.PSEmptyRecycleService;
import com.percussion.recycle.service.impl.PSRecycleService;
import com.percussion.searchmanagement.service.impl.PSPageIndexService;
import com.percussion.share.dao.IPSFolderHelper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;

/**
 * Inventory freeze for {@code folderHelper} reverse-edge constructor cycles (#2485 / parent #2423).
 *
 * <p>Live reverse edges into {@link IPSFolderHelper} (beans forced while {@code folderHelper} is
 * still constructing via the recycle subgraph) must carry parameter {@link Lazy @Lazy}. Intermediate
 * cycle-subgraph beans must <em>not</em> construct-require {@code folderHelper} (that would
 * short-circuit the known breaks). Downstream product consumers may inject {@code folderHelper}
 * without param {@code @Lazy}; class-level {@code @Lazy} alone is <strong>false safety</strong> for
 * constructor cycles.
 *
 * <pre>
 * Path A: folderHelper → recycle → widgetAsset → assetDao → contentItemDao → folderHelper
 * Path B: folderHelper → recycle → widgetAsset → pageIndex → pageDaoHelper → folderHelper
 * </pre>
 *
 * <p>Does not duplicate hub hardening for pageService / itemWorkflow / templateService / siteData —
 * those are separate residuals (#2476–#2478, #2514–#2521). Full disposition table:
 * {@code docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md}.
 *
 * <p>Peers: {@link PSContentItemDaoCycleLazyWiringTest}, {@link PSPageDaoHelperCycleLazyWiringTest},
 * {@link PSFolderHelperRecycleLazyWiringTest}, {@link PSAssetServicePageServiceNearCycleWiringTest}.
 */
@Tag("UnitTest")
public class PSFolderHelperReverseEdgeInventoryWiringTest {

  @Test
  public void contentItemDaoFolderHelperConstructorParameterIsLazy() throws NoSuchMethodException {
    assertFolderHelperParamIsLazy(
        PSContentItemDao.class,
        "PSContentItemDao → IPSFolderHelper is live reverse edge path A (#2435 / #2485)");
  }

  @Test
  public void pageDaoHelperFolderHelperConstructorParameterIsLazy() throws NoSuchMethodException {
    assertFolderHelperParamIsLazy(
        PSPageDaoHelper.class,
        "PSPageDaoHelper → IPSFolderHelper is live reverse edge path B (#2437 / #2485)");
  }

  @Test
  public void folderHelperRecycleServiceConstructorParameterIsLazy() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSFolderHelper.class);
    Parameter recycleParam = findParamOfType(ctor, IPSRecycleService.class);
    assertNotNull(
        recycleParam, "PSFolderHelper must still construct-require IPSRecycleService (path A/B)");
    assertTrue(
        recycleParam.isAnnotationPresent(Lazy.class),
        "IPSRecycleService constructor parameter on PSFolderHelper must be @Lazy (forward"
            + " deferral of recycle subgraph; class-level @Lazy alone is false safety — #2437)");
  }

  /**
   * Intermediate beans on the known recycle subgraph must not gain a constructor edge back to
   * {@code folderHelper}. Such an edge would reverse-bypass contentItemDao / pageDaoHelper param
   * {@code @Lazy}.
   */
  @Test
  public void cycleSubgraphIntermediatesMustNotConstructRequireFolderHelper()
      throws NoSuchMethodException {
    assertNoCtorParam(
        PSRecycleService.class,
        IPSFolderHelper.class,
        "recycleService → folderHelper would reverse folderHelper→recycleService");
    assertNoCtorParam(
        PSWidgetAssetRelationshipService.class,
        IPSFolderHelper.class,
        "widgetAsset → folderHelper would short-circuit paths A/B");
    assertNoCtorParam(
        PSAssetDao.class,
        IPSFolderHelper.class,
        "assetDao → folderHelper would reverse-bypass contentItemDao @Lazy");
    assertNoCtorParam(
        PSPageIndexService.class,
        IPSFolderHelper.class,
        "pageIndexService → folderHelper would reverse-bypass pageDaoHelper @Lazy");
    assertNoCtorParam(
        PSPageDao.class,
        IPSFolderHelper.class,
        "pageDao → folderHelper would form a parallel reverse edge via pageIndex path");
  }

  /**
   * {@code managedLinkService} is currently looked up from the application context (not
   * constructor-injected) on widgetAsset. A ctor inject would pull {@code pageService →
   * folderHelper} and create a third reverse path.
   */
  @Test
  public void widgetAssetMustNotConstructRequireManagedLinkService() throws NoSuchMethodException {
    assertNoCtorParam(
        PSWidgetAssetRelationshipService.class,
        IPSManagedLinkService.class,
        "widgetAsset ctor → managedLinkService would force pageService→folderHelper while"
            + " folderHelper may still be creating (path C risk; keep context lookup)");
  }

  /**
   * emptyRecycle is a consumer of folderHelper, not a reverse edge on folderHelper's construction
   * path. Freeze that it is not construct-required by recycleService (would close a new mid-cycle).
   */
  @Test
  public void recycleServiceMustNotConstructRequireEmptyRecycleService()
      throws NoSuchMethodException {
    assertNoCtorParam(
        PSRecycleService.class,
        com.percussion.recycle.service.IPSEmptyRecycleService.class,
        "recycleService → emptyRecycleService would pull pathService/folderHelper mid-cycle");
    // emptyRecycle may inject folderHelper without param @Lazy (consumer-only disposition #2485)
    Constructor<?> emptyCtor = singlePublicConstructor(PSEmptyRecycleService.class);
    Parameter folderParam = findParamOfType(emptyCtor, IPSFolderHelper.class);
    assertNotNull(
        folderParam,
        "PSEmptyRecycleService is inventoried as a consumer of IPSFolderHelper; if removed,"
            + " update inventory #2485 disposition");
  }

  private static void assertFolderHelperParamIsLazy(Class<?> bean, String why)
      throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(bean);
    Parameter folderHelperParam = findParamOfType(ctor, IPSFolderHelper.class);
    assertNotNull(
        folderHelperParam,
        bean.getSimpleName() + " must construct-require IPSFolderHelper — " + why);
    assertTrue(
        folderHelperParam.isAnnotationPresent(Lazy.class),
        bean.getSimpleName()
            + " IPSFolderHelper constructor parameter must be @Lazy: "
            + why
            + ". Class-level @Lazy alone does not break constructor edges.");
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
