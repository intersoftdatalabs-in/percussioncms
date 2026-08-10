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
package com.percussion.recycle.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.pathmanagement.service.impl.PSPathService;
import com.percussion.share.dao.IPSFolderHelper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;

/**
 * Belt-and-braces protection for the {@code emptyRecycle / pathService} neighborhood of the {@code
 * folderHelper–recycle} subgraph (#2526, residual of #2485 / parent #2423).
 *
 * <p>{@link PSEmptyRecycleService} is a consumer of {@link IPSFolderHelper} (not on {@code
 * folderHelper}'s construction path). The ctor body only field-assigns {@code folderHelper} — the
 * field is only used post-construction in {@code purgeLeaf(PSPathItem, PSEmptyRecycleResult)}.
 * Adding parameter {@link Lazy @Lazy} on the {@code IPSFolderHelper} ctor param therefore injects a
 * Spring proxy that is safe under the known usage and protects against a future eager consumer
 * forcing {@code folderHelper} creation while {@code emptyRecycle} is still under construction
 * (e.g. via a new reverse edge from {@code recycleService} or via a path/recycle cross-wire).
 *
 * <pre>
 * PSEmptyRecycleService (consumer)
 *   → IPSPathService  (class @Lazy, param not @Lazy — ctor body does not call pathService)
 *   → IPSUserService
 *   → IPSFolderHelper (param @Lazy as of #2526; was not @Lazy before)
 * </pre>
 *
 * <p>This test freezes:
 *
 * <ul>
 *   <li>{@code emptyRecycle} still construct-requires {@code folderHelper} (consumer edge)
 *   <li>that {@code folderHelper} ctor param is {@code @Lazy} (the #2526 fix)
 *   <li>{@code recycleService} must not construct-require {@code emptyRecycle} or {@code
 *       pathService} without {@code @Lazy} (would close a new mid-cycle)
 *   <li>{@code pathService} must not construct-require {@code emptyRecycle} (reverse-edge ban)
 * </ul>
 *
 * <p>Peers: {@link com.percussion.share.dao.impl.PSFolderHelperReverseEdgeInventoryWiringTest}
 * (#2485), {@link com.percussion.share.dao.impl.PSContentItemDaoCycleLazyWiringTest} (#2435),
 * {@link com.percussion.share.dao.impl.PSAssetServicePageServiceNearCycleWiringTest} (#2476). Full
 * inventory: {@code
 * docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md}.
 */
@Tag("UnitTest")
public class PSEmptyRecycleServiceCycleLazyWiringTest {

  @Test
  public void emptyRecycleServiceConstructorTakesFolderHelper() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSEmptyRecycleService.class);
    Parameter folderParam = findParamOfType(ctor, IPSFolderHelper.class);
    assertNotNull(
        folderParam,
        "PSEmptyRecycleService is inventoried as a consumer of IPSFolderHelper (#2485); if the"
            + " edge was removed, update the inventory note and this test");
  }

  /**
   * Belt-and-braces param {@code @Lazy} on the {@code folderHelper} ctor param (#2526). The ctor
   * body does not call {@code folderHelper}, and {@code folderHelper} is only used in {@code
   * purgeLeaf} (post-construction), so injecting a Spring proxy is safe.
   */
  @Test
  public void emptyRecycleServiceFolderHelperConstructorParameterIsLazy()
      throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSEmptyRecycleService.class);
    Parameter folderParam = findParamOfType(ctor, IPSFolderHelper.class);
    assertNotNull(
        folderParam, "Expected an IPSFolderHelper constructor parameter on PSEmptyRecycleService");
    assertTrue(
        folderParam.isAnnotationPresent(Lazy.class),
        "IPSFolderHelper constructor parameter on PSEmptyRecycleService must be @Lazy"
            + " (belt-and-braces near-cycle protection for the emptyRecycle/pathService"
            + " neighborhood; see #2526 / #2485). folderHelper is only used post-construction"
            + " (purgeLeaf) so the proxy is safe; class-level @Lazy alone does not break"
            + " constructor edges when an eager consumer forces construction.");
  }

  /**
   * {@code recycleService} must not gain a constructor edge to {@code emptyRecycle} (would pull
   * {@code pathService} + {@code folderHelper} while {@code folderHelper} is still creating on the
   * known cycle path A/B). A future reverse edge must use parameter {@code @Lazy} and be documented
   * in the inventory.
   */
  @Test
  public void recycleServiceMustNotConstructRequireEmptyRecycleService()
      throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSRecycleService.class,
        com.percussion.recycle.service.IPSEmptyRecycleService.class,
        "recycleService → emptyRecycle would pull pathService/folderHelper mid-cycle (#2526)");
  }

  /**
   * {@code recycleService} must not gain a constructor edge to {@code pathService} (would close a
   * path/recycle cross-wire cycle via {@code pathService} → {@code folderHelper} and {@code
   * recycleService} → {@code folderHelper} once either is added).
   */
  @Test
  public void recycleServiceMustNotConstructRequirePathService() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSRecycleService.class,
        IPSPathService.class,
        "recycleService → pathService would form a path/recycle cross-wire cycle (#2526)");
  }

  /**
   * {@code pathService} must not gain a constructor edge back to {@code emptyRecycle} (would
   * re-enter {@code folderHelper} via {@code emptyRecycle}'s ctor param, even with param
   * {@code @Lazy}, and would couple two near-cycle hubs).
   */
  @Test
  public void pathServiceMustNotConstructRequireEmptyRecycleService() throws NoSuchMethodException {
    assertNoEagerCtorParam(
        PSPathService.class,
        com.percussion.recycle.service.IPSEmptyRecycleService.class,
        "pathService → emptyRecycle would re-enter folderHelper and couple hubs (#2526)");
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
      return; // desired: no reverse ctor edge
    }
    assertTrue(
        p.isAnnotationPresent(Lazy.class),
        bean.getSimpleName()
            + " must not construct-require "
            + forbidden.getSimpleName()
            + " without @Lazy: "
            + why
            + ". Prefer method-level lookup, setter injection, or @Lazy on the injection point"
            + " and document intentional @Lazy reverse edges in"
            + " sitemanage-injection-cycle-inventory.md.");
  }
}
