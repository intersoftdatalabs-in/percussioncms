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
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.impl.PSItemWorkflowService;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.share.dao.IPSFolderHelper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;

/**
 * Belt-and-braces param {@code @Lazy} on {@link PSItemWorkflowService} → known-cycle peer edges
 * (#2423 residual #2515, peer of #2476 assetService→pageService).
 *
 * <p>Static inventory (#2463) ranks {@code PSItemWorkflowService} as the second-hottest sitemanage
 * hub. It construct-requires peers on the known {@code folderHelper → … → contentItemDao →
 * folderHelper} chain:
 *
 * <pre>
 * itemWorkflowService
 *   → assetDao                         (@Lazy param — #2515)
 *   → folderHelper                     (@Lazy param — #2515)
 *   → recycleService                   (@Lazy param — #2515)
 *   → widgetAssetRelationshipService   (@Lazy param — #2515)
 * </pre>
 *
 * <p>Class-level {@code @Lazy} on the hub does <em>not</em> break constructor dependency edges when
 * an eager consumer forces creation. Parameter {@code @Lazy} injects proxies (peer of {@link
 * PSContentItemDaoCycleLazyWiringTest}). Reverse edges from these peers back to {@code
 * IPSItemWorkflowService} remain banned by {@link PSItemWorkflowServiceHubReverseEdgeWiringTest}
 * (#2478).
 *
 * <p>Behavior review (#2515): ctor body only field-assigns the four peers; method calls happen only
 * on checkout/transition/shared-asset paths post-construction ({@code assetDao} is assigned but
 * unused in production methods today). Lazy proxies are therefore safe for the chosen edges.
 *
 * <p>Inventory: {@code
 * docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md}.
 */
@Tag("UnitTest")
public class PSItemWorkflowServiceCycleLazyWiringTest {

  @Test
  public void itemWorkflowConstructRequiresCyclePeers() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSItemWorkflowService.class);
    assertNotNull(
        findParamOfType(ctor, IPSAssetDao.class),
        "PSItemWorkflowService must still construct-require IPSAssetDao — inventory #2463 / #2515"
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
  public void assetDaoConstructorParameterIsLazy() throws NoSuchMethodException {
    assertParamLazy(
        IPSAssetDao.class,
        "IPSAssetDao constructor parameter on PSItemWorkflowService must be @Lazy"
            + " (belt-and-braces cycle-peer protection; see #2515 / #2463)."
            + " assetDao is only field-assigned in the ctor (unused post-construction today).");
  }

  @Test
  public void folderHelperConstructorParameterIsLazy() throws NoSuchMethodException {
    assertParamLazy(
        IPSFolderHelper.class,
        "IPSFolderHelper constructor parameter on PSItemWorkflowService must be @Lazy"
            + " (belt-and-braces cycle-peer protection; see #2515 / #2463)."
            + " folderHelper is only used post-construction (folder path / access / workflow id).");
  }

  @Test
  public void recycleServiceConstructorParameterIsLazy() throws NoSuchMethodException {
    assertParamLazy(
        IPSRecycleService.class,
        "IPSRecycleService constructor parameter on PSItemWorkflowService must be @Lazy"
            + " (belt-and-braces cycle-peer protection; see #2515 / #2463)."
            + " recycleService is only used post-construction (isInRecycler checks).");
  }

  @Test
  public void widgetAssetRelationshipServiceConstructorParameterIsLazy()
      throws NoSuchMethodException {
    assertParamLazy(
        IPSWidgetAssetRelationshipService.class,
        "IPSWidgetAssetRelationshipService constructor parameter on PSItemWorkflowService must be"
            + " @Lazy (belt-and-braces cycle-peer protection; see #2515 / #2463)."
            + " widgetAsset is only used post-construction (shared/linked/local asset relations).");
  }

  private static void assertParamLazy(Class<?> paramType, String message)
      throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSItemWorkflowService.class);
    Parameter param = findParamOfType(ctor, paramType);
    assertNotNull(
        param,
        "Expected a " + paramType.getSimpleName() + " constructor parameter on PSItemWorkflowService");
    assertTrue(param.isAnnotationPresent(Lazy.class), message);
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
}
