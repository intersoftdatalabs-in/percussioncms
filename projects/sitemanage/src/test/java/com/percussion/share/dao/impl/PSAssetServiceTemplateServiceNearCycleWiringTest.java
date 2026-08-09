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
 * Protection for the {@code assetService ↔ templateService} near-cycle edge (#2423 residual
 * #2521).
 *
 * <p>Static inventory (#2463) ranks {@link PSTemplateService} as a high fan-in hub (ctor out ~6 /
 * in ~20) and {@link PSAssetService} as a consumer hub that construct-requires {@link
 * IPSTemplateService}:
 *
 * <pre>
 * assetService  →  templateService   (constructor, one-way — frozen here)
 * templateService  ↛  assetService   (must remain true)
 * </pre>
 *
 * <p>{@link PSAssetServicePageServiceNearCycleWiringTest} covers the hotter {@code
 * assetService→pageService} edge (with param {@code @Lazy}). Template-service reverse-edge bans
 * against cycle peers appear under #2477; this test freezes the <em>assetService↔templateService
 * pair specifically</em>, including field-injection reverse edges.
 *
 * <p><strong>Param {@code @Lazy} disposition (forward edge):</strong> {@code PSAssetService} only
 * calls {@code templateService} post-construction ({@code load}/{@code find}); a ctor-body {@code
 * notNull} check is proxy-safe. Param {@code @Lazy} would therefore be <em>safe if added</em>, but
 * is <strong>not required</strong> while the reverse edge remains banned and no closed cycle
 * exists through this pair. Class-level {@code @Lazy} on {@code PSAssetService} is lazy-init only
 * and is not treated as a cycle breaker. Prefer documenting intentional param {@code @Lazy} in the
 * inventory if product risk later warrants it (#2476 pattern).
 *
 * <p>Peers: {@link PSAssetServicePageServiceNearCycleWiringTest}, {@link
 * PSItemWorkflowServiceHubReverseEdgeWiringTest}, {@link PSContentItemDaoCycleLazyWiringTest}.
 * Inventory: {@code
 * docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md}.
 */
@Tag("UnitTest")
public class PSAssetServiceTemplateServiceNearCycleWiringTest {

  @Test
  public void assetServiceConstructorTakesTemplateService() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSAssetService.class);
    Parameter templateParam = findParamOfType(ctor, IPSTemplateService.class);
    assertNotNull(
        templateParam,
        "PSAssetService must still construct-require IPSTemplateService — inventory #2463 / #2521"
            + " one-way hub edge; if the edge was removed, update the inventory and this test");
    // Param @Lazy on this edge is optional / not required (see class Javadoc + inventory #2521).
    // Do not assert presence or absence of @Lazy here — reverse-edge ban is the hard gate.
  }

  @Test
  public void templateServiceConstructorMustNotTakeAssetService() throws NoSuchMethodException {
    Constructor<?> ctor = singlePublicConstructor(PSTemplateService.class);
    Parameter assetParam = findParamOfType(ctor, IPSAssetService.class);
    if (assetParam == null) {
      return; // desired: no reverse ctor edge
    }
    assertTrue(
        assetParam.isAnnotationPresent(Lazy.class),
        "PSTemplateService must not construct-require IPSAssetService without @Lazy: that would"
            + " close an assetService↔templateService cycle independent of folderHelper (see"
            + " #2521 / #2463). Prefer method-level lookup, setter injection, or @Lazy on the"
            + " injection point; document intentional @Lazy reverse edges in the inventory.");
  }

  /**
   * Eager {@code @Autowired} field inject of {@link IPSAssetService} on {@link PSTemplateService}
   * is the same class of reverse edge as a constructor param (class-level {@code @Lazy} on either
   * bean does not break an eager field edge from a bean already under construction).
   */
  @Test
  public void templateServiceMustNotEagerFieldInjectAssetService() {
    List<String> violations = new ArrayList<>();
    for (Field field : PSTemplateService.class.getDeclaredFields()) {
      if (!IPSAssetService.class.isAssignableFrom(field.getType())) {
        continue;
      }
      if (field.isAnnotationPresent(Lazy.class)) {
        continue; // documented intentional reverse edge
      }
      boolean autowired = field.isAnnotationPresent(Autowired.class);
      violations.add(
          "PSTemplateService."
              + field.getName()
              + (autowired ? " (@Autowired)" : " (field type)")
              + " — reverse field edge without @Lazy");
    }
    assertTrue(
        violations.isEmpty(),
        "PSTemplateService must not eagerly field-inject IPSAssetService (use @Lazy or remove the"
            + " edge). Violations: "
            + String.join("; ", violations));
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
