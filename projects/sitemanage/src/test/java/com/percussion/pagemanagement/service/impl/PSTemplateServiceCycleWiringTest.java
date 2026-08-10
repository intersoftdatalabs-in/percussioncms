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

import com.percussion.assetmanagement.service.IPSAssetService;
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
 * Cycle-risk protection for {@link PSTemplateService} (#2423 residual #2477).
 *
 * <p>Per the static inventory in {@code
 * docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md},
 * {@code PSTemplateService} is a high fan-in hub that construct-requires {@code
 * IPSWidgetAssetRelationshipService} and the page / template DAOs. The known {@code folderHelper}
 * cycle is broken by {@code @Lazy}; {@code PSTemplateService} itself has no closed cycle today but
 * sits close enough to several cycle peers that a future reverse ctor dep on it would close one.
 * Belt-and-braces: class-level {@code @Lazy} on {@link PSTemplateService} defers the bean until
 * first use (cheaper during startup and stops an eager consumer from forcing the cycle path), and
 * the assertions below forbid the reverse edges that would close new cycles via the template
 * service.
 *
 * <p>Peers: {@code PSAssetServicePageServiceNearCycleWiringTest} ({@code PSAssetService} ↔ {@code
 * PSPageService}), {@code PSContentItemDaoCycleLazyWiringTest} (folderHelper cycle break).
 */
@Tag("UnitTest")
public class PSTemplateServiceCycleWiringTest {

  @Test
  public void templateServiceIsClassLevelLazy() {
    assertTrue(
        PSTemplateService.class.isAnnotationPresent(Lazy.class),
        "PSTemplateService must carry class-level @Lazy to harden against cycle formation"
            + " (#2423 residual #2477). Class @Lazy defers the bean until first use, breaking any"
            + " eager consumer path that could close a cycle through templateService.");
  }

  @Test
  public void templateServiceMustNotConstructRequireCyclePeers() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSTemplateService.class);
    assertNoCtorParam(
        ctor,
        IPSFolderHelper.class,
        "templateService → folderHelper would close a cycle if a"
            + " peer later gains a reverse ctor edge into templateService");
    assertNoCtorParam(
        ctor,
        IPSContentItemDao.class,
        "templateService → contentItemDao would skip the"
            + " contentItemDao cycle break and re-cycle via folderHelper");
    assertNoCtorParam(
        ctor,
        IPSRecycleService.class,
        "templateService → recycleService would form a new" + " mid-cycle branch");
    assertNoCtorParam(
        ctor,
        IPSAssetService.class,
        "templateService → assetService would skip the pageService" + " near-cycle guard");
    assertNoCtorParam(
        ctor,
        IPSPageService.class,
        "templateService → pageService would form a parallel cycle"
            + " path with pageService already requiring recycleService / folderHelper");
    // Note: PSTemplateService does construct-require IPSWidgetAssetRelationshipService today
    // (inventory #2463). That edge is mitigated by class-level @Lazy on PSTemplateService above;
    // forbidding it here would require removing a real product dependency, which is out of scope
    // for the cycle-risk hardening slice (#2477). Belt-and-braces coverage: the @Lazy assertion
    // plus the reverse-edge ban in cyclePeersMustNotConstructRequireTemplateService below.
  }

  /**
   * Known cycle peers must not construct-require {@link PSTemplateService} either. Belt-and-braces
   * against a future reverse edge into the template hub.
   */
  @Test
  public void cyclePeersMustNotConstructRequireTemplateService() throws NoSuchMethodException {
    assertNoCtorParam(
        singlePublicConstructor(com.percussion.share.dao.impl.PSFolderHelper.class),
        PSTemplateService.class,
        "folderHelper → templateService would add a reverse edge into the high fan-in hub");
    assertNoCtorParam(
        singlePublicConstructor(com.percussion.recycle.service.impl.PSRecycleService.class),
        PSTemplateService.class,
        "recycleService → templateService would close a new cycle branch");
    assertNoCtorParam(
        singlePublicConstructor(com.percussion.pagemanagement.service.impl.PSPageService.class),
        PSTemplateService.class,
        "pageService → templateService would close a pageService↔templateService cycle branch");
    assertNoCtorParam(
        singlePublicConstructor(com.percussion.assetmanagement.service.impl.PSAssetService.class),
        PSTemplateService.class,
        "assetService → templateService would form a parallel cycle path");
  }

  @Test
  public void templateServiceConstructorExists() throws NoSuchMethodException {
    // Sanity guard: if PSTemplateService ever loses its single public constructor, the assertions
    // above stop being meaningful. The reflection lookups in this class all assume one ctor.
    Constructor<?> ctor = singlePublicConstructor(PSTemplateService.class);
    assertNotNull(
        ctor,
        "PSTemplateService must declare exactly one public constructor for Spring"
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

  private static void assertNoCtorParam(Constructor<?> ctor, Class<?> forbidden, String why) {
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
}
