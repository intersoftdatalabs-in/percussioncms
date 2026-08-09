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
package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.dao.IPSTemplateDao;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;

/**
 * Belt-and-braces param {@code @Lazy} on {@link PSTemplateService} → forward ctor edges to
 * known-cycle peers (#2423 residual #2520, peer of #2515 itemWorkflow + #2476 assetService→page).
 *
 * <p>Static inventory (#2463 / #2485) ranks {@code PSTemplateService} as a top-3 sitemanage hub.
 * It construct-requires peers on or next to the known cycle subgraph:
 *
 * <pre>
 * templateService
 *   → templateDao                       (@Lazy param — #2520)
 *   → widgetAssetRelationshipService    (@Lazy param — #2520)
 *   → pageDao                           (@Lazy param — #2520)
 *   → pageDaoHelper                     (@Lazy param — #2520)
 * </pre>
 *
 * <p>Class-level {@code @Lazy} on the hub (#2477) does <em>not</em> break constructor dependency
 * edges when an eager consumer forces creation. Parameter {@code @Lazy} injects proxies (peer of
 * {@link PSItemWorkflowServiceCycleLazyWiringTest} / {@link
 * PSAssetServicePageServiceNearCycleWiringTest}). Reverse edges from these peers back to {@code
 * IPSTemplateService} remain banned by {@link PSTemplateServiceCycleWiringTest} (#2477).
 *
 * <p>Behavior review (#2520): ctor body only field-assigns the four peers listed above; method
 * calls happen only on save / load / template workflow paths post-construction. Lazy proxies are
 * therefore safe for the chosen edges. {@code IPSWidgetService} is intentionally NOT annotated
 * here — the ctor body uses it to construct {@code RegionWidgetValidator}.
 *
 * <p>Inventory: {@code
 * docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md}.
 */
@Tag("UnitTest")
public class PSTemplateServiceParamLazyWiringTest {

  @Test
  public void templateServiceConstructRequiresForwardEdges() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSTemplateService.class);
    assertNotNull(
        findParamOfType(ctor, IPSTemplateDao.class),
        "PSTemplateService must still construct-require IPSTemplateDao — inventory #2463 / #2520"
            + " hub model; if removed, update inventory and this test");
    assertNotNull(
        findParamOfType(ctor, IPSWidgetAssetRelationshipService.class),
        "PSTemplateService must still construct-require IPSWidgetAssetRelationshipService");
    assertNotNull(
        findParamOfType(ctor, IPSPageDao.class),
        "PSTemplateService must still construct-require IPSPageDao");
    assertNotNull(
        findParamOfType(ctor, IPSPageDaoHelper.class),
        "PSTemplateService must still construct-require IPSPageDaoHelper");
  }

  @Test
  public void templateDaoConstructorParameterIsLazy() throws NoSuchMethodException {
    assertParamLazy(
        IPSTemplateDao.class,
        "IPSTemplateDao constructor parameter on PSTemplateService must be @Lazy"
            + " (belt-and-braces cycle-peer protection; see #2520 / #2463)."
            + " templateDao is only field-assigned in the ctor (used on save / load / find paths).");
  }

  @Test
  public void widgetAssetRelationshipServiceConstructorParameterIsLazy()
      throws NoSuchMethodException {
    assertParamLazy(
        IPSWidgetAssetRelationshipService.class,
        "IPSWidgetAssetRelationshipService constructor parameter on PSTemplateService must be @Lazy"
            + " (belt-and-braces cycle-peer protection; see #2520 / #2463)."
            + " widgetAsset is only used post-construction (shared/linked/local asset relations on"
            + " save / import / create).");
  }

  @Test
  public void pageDaoConstructorParameterIsLazy() throws NoSuchMethodException {
    assertParamLazy(
        IPSPageDao.class,
        "IPSPageDao constructor parameter on PSTemplateService must be @Lazy"
            + " (belt-and-braces cycle-peer protection; see #2520 / #2463)."
            + " pageDao is only used post-construction (isValidPageId on save).");
  }

  @Test
  public void pageDaoHelperConstructorParameterIsLazy() throws NoSuchMethodException {
    assertParamLazy(
        IPSPageDaoHelper.class,
        "IPSPageDaoHelper constructor parameter on PSTemplateService must be @Lazy"
            + " (belt-and-braces cycle-peer protection; see #2520 / #2463)."
            + " pageDaoHelper is only used post-construction (pageIdsByTemplate / replaceTemplate"
            + " on delete).");
  }

  private static void assertParamLazy(Class<?> paramType, String message)
      throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSTemplateService.class);
    Parameter param = findParamOfType(ctor, paramType);
    assertNotNull(
        param,
        "Expected a " + paramType.getSimpleName() + " constructor parameter on PSTemplateService");
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
