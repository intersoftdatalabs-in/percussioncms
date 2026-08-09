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

import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;

/**
 * Reverse-edge cycle protection for {@link PSPageService} (#2423 residual #2514).
 *
 * <p>Per the static inventory in {@code
 * docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md},
 * {@code PSPageService} is the rank-1 sitemanage fan-in hub (out ~11 / in ~28) and
 * construct-requires the cycle peers {@code contentItemDao}, {@code folderHelper},
 * {@code recycleService}, {@code widgetAssetRelationshipService}, and {@code
 * itemWorkflow}. The forward edges are intentional and broken on the {@code contentItemDao}
 * side via class {@code @Lazy} there. This test forbids the reverse edges: cycle peers and
 * other high-risk partners must not construct-require {@link IPSPageService}, since adding
 * such a reverse edge would close a new cycle path even with class {@code @Lazy} on
 * {@code PSPageService} (peer-cycle reasoning — see {@code
 * PSAssetServicePageServiceNearCycleWiringTest}).</p>
 *
 * <p>Peers: {@code PSAssetServicePageServiceNearCycleWiringTest} ({@code PSAssetService}
 * ↔ {@code PSPageService}), {@code PSContentItemDaoCycleLazyWiringTest} (folderHelper cycle
 * break), {@code PSTemplateServiceCycleWiringTest} (#2477).</p>
 */
@Tag("UnitTest")
public class PSPageServiceCycleWiringTest {

  @Test
  public void pageServiceIsClassLevelLazy() {
    assertTrue(
        PSPageService.class.isAnnotationPresent(Lazy.class),
        "PSPageService must carry class-level @Lazy to harden against cycle formation"
            + " (#2423 residual #2514 / #2463 inventory). Class @Lazy defers the bean until"
            + " first use, breaking any eager consumer path that could close a cycle through"
            + " pageService.");
  }

  /**
   * The reverse edges are the dangerous ones: cycle peers must not construct-require
   * IPSPageService. This locks down the four cycle peers the #2463 inventory named.
   *
   * <p>Note: {@code PSAssetService} already takes {@link IPSPageService} as a forward
   * ctor edge (param {@code @Lazy}, peer #2476 / #2463). That edge is intentional and
   * mitigated by the param-level {@code @Lazy} on {@code PSAssetService}, so it is
   * <em>not</em> banned here. The companion reverse-edge check lives in
   * {@code PSAssetServicePageServiceNearCycleWiringTest}: that test forbids
   * {@code PSPageService → IPSAssetService} (the other direction of the near-cycle).</p>
   */
  @Test
  public void cyclePeersMustNotConstructRequirePageService() throws NoSuchMethodException {
    assertNoCtorParam(
        singlePublicConstructor(
            com.percussion.share.dao.impl.PSFolderHelper.class),
        IPSPageService.class,
        "folderHelper → pageService would close a folderHelper↔pageService cycle");
    assertNoCtorParam(
        singlePublicConstructor(
            com.percussion.share.dao.impl.PSContentItemDao.class),
        IPSPageService.class,
        "contentItemDao → pageService would reverse the existing pageService→contentItemDao"
            + " forward edge and form a new cycle path");
    assertNoCtorParam(
        singlePublicConstructor(
            com.percussion.recycle.service.impl.PSRecycleService.class),
        IPSPageService.class,
        "recycleService → pageService would close a pageService↔recycleService cycle branch");
    assertNoCtorParam(
        singlePublicConstructor(
            com.percussion.itemmanagement.service.impl.PSItemWorkflowService.class),
        IPSPageService.class,
        "itemWorkflow → pageService would close a pageService↔itemWorkflow cycle branch");
  }

  /**
   * Belt-and-braces: PSPageService must keep construct-requiring its cycle peers (forward
   * edges). The cycle is broken by contentItemDao's class @Lazy, not by removing the
   * forward edges. If anyone removes a forward edge they should also remove the
   * corresponding reverse-edge ban here AND update the inventory.
   */
  @Test
  public void pageServiceConstructorKeepsCyclePeerForwardEdges() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSPageService.class);
    assertHasCtorParam(ctor, IPSFolderHelper.class, "PSPageService must still construct-require"
        + " folderHelper (cycle inventory #2463); the cycle is broken on contentItemDao, not"
        + " here");
    assertHasCtorParam(ctor, IPSContentItemDao.class, "PSPageService must still construct-require"
        + " contentItemDao (cycle inventory #2463); removing the edge would invalidate the"
        + " contentItemDao @Lazy break");
    assertHasCtorParam(ctor, IPSRecycleService.class, "PSPageService must still construct-require"
        + " recycleService (cycle inventory #2463); reverse-edge into recycle is forbidden"
        + " above");
  }

  @Test
  public void pageServiceConstructorExists() throws NoSuchMethodException {
    // Sanity guard: PSPageService must declare exactly one public constructor for Spring
    // wiring inspection; this test enforces the assumption shared by all assertions above.
    Constructor<?> ctor = singlePublicConstructor(PSPageService.class);
    assertNotNull(ctor, "PSPageService must declare exactly one public constructor for Spring"
        + " wiring inspection; this test enforces the assumption");
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

  private static void assertNoCtorParam(
      Constructor<?> ctor, Class<?> forbidden, String why) {
    for (Parameter p : ctor.getParameters()) {
      if (forbidden.isAssignableFrom(p.getType())) {
        fail(
            ctor.getDeclaringClass().getSimpleName()
                + " must not construct-require "
                + forbidden.getSimpleName()
                + ": "
                + why);
      }
    }
  }

  private static void assertHasCtorParam(
      Constructor<?> ctor, Class<?> expected, String why) {
    for (Parameter p : ctor.getParameters()) {
      if (expected.isAssignableFrom(p.getType())) {
        return;
      }
    }
    fail(
        ctor.getDeclaringClass().getSimpleName()
            + " must still construct-require "
            + expected.getSimpleName()
            + ": "
            + why);
  }
}
