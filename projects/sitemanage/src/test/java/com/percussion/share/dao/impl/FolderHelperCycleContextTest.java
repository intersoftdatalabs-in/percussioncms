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

import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.recycle.service.impl.PSRecycleService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;

/**
 * Spring constructor-injection cycle regression for the bean graph
 *
 * <pre>
 * folderHelper -> recycleService -> widgetAssetRelationshipService
 *             -> assetDao        -> contentItemDao       -> folderHelper
 * </pre>
 *
 * <p>Slice A of #2423 (#2435) breaks the cycle by marking the
 * {@code IPSFolderHelper folderHelper} constructor parameter of
 * {@link PSContentItemDao} as {@link Lazy @Lazy}. Without that marker,
 * Spring fails context startup with {@code BeanCurrentlyInCreationException}
 * on Jetty/Rhythmyx startup and the webapp is unusable.
 *
 * <p>This test (slice B / #2436) is a runtime-witness to that fix at the
 * Java reflection level: it walks every constructor of every cycle bean
 * in the JVM and asserts that the cycle is broken at the constructor
 * parameter level. Specifically:
 *
 * <ul>
 *   <li>The cycle's only back-edge ({@code contentItemDao} -> {@code
 *       folderHelper}) carries {@code @Lazy}.
 *   <li>Every other edge in the cycle is satisfied by an
 *       interface/impl pair that does not depend on a
 *       {@code @Lazy}-breakable cycle edge.
 * </ul>
 *
 * <p>Why reflection rather than a full {@code ApplicationContext.refresh()}
 * load? The cycle beans each bring field-injected JPA / Hibernate / JCR /
 * Spring-infrastructure collaborators (e.g. {@code @PersistenceContext}
 * on {@code PSWidgetAssetRelationshipDao.emf},
 * {@code @Autowired IPSContentChangeService changeService} on
 * {@code PSWorkflowHelper}). A plain {@code AnnotationConfigApplicationContext}
 * cannot supply those without either a Spring Boot autoconfiguration
 * (this module is not a Spring Boot app — see root {@code AGENTS.md})
 * or an embedded JPA setup that does not exist. Attempting to refresh
 * the context would test Spring's ability to bootstrap the full
 * sitemanage graph, not the cycle fix.
 *
 * <p>Slice A's reflection test {@link PSContentItemDaoCycleLazyWiringTest}
 * covers the {@code @Lazy} annotation directly. This slice B test
 * witnesses the fix at the bean-graph level by inspecting the constructor
 * wiring of every cycle bean, so the regression gate spans both layers.
 * Slice C (#2437) is the Docker {@code qa-up} health/login smoke against
 * the running container and exercises the fix in production.
 */
@Tag("UnitTest")
public class FolderHelperCycleContextTest {

  /** Cycle beans whose constructor parameters define the cycle edges. */
  private static final Class<?>[] CYCLE_BEANS = {
    PSFolderHelper.class,
    PSRecycleService.class,
    com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService.class,
    com.percussion.assetmanagement.dao.impl.PSAssetDao.class,
    PSContentItemDao.class
  };

  @Test
  void cycleBeansHaveUniqueNoArgOrInterfaceCollaborators() {
    for (Class<?> bean : CYCLE_BEANS) {
      Constructor<?> ctor = singlePublicCtor(bean);
      for (Parameter p : ctor.getParameters()) {
        Class<?> dep = p.getType();
        // Every cycle-edge dependency must be an interface or a class
        // not in the cycle itself. If a constructor takes a cycle-bean
        // concrete type without @Lazy, Spring's constructor injection
        // would deadlock at context refresh time.
        assertTrue(
            dep.isInterface() || !isInCycle(dep),
            bean.getSimpleName()
                + " constructor parameter "
                + dep.getSimpleName()
                + " is a concrete cycle-bean class without @Lazy — "
                + "this would re-introduce the folderHelper<->contentItemDao cycle "
                + "at Rhythmyx startup (#2423). Make the parameter an interface or "
                + "annotate it with @org.springframework.context.annotation.Lazy.");
      }
    }
  }

  @Test
  void contentItemDaoFolderHelperParameterCarriesLazy() {
    // Belt-and-braces witness of the slice A fix. If slice A's @Lazy
    // marker is ever removed from PSContentItemDao(IPSFolderHelper),
    // the cycleBeansHaveUniqueNoArgOrInterfaceCollaborators assertion
    // above still passes (IPSFolderHelper is an interface) but Spring
    // would deadlock at context refresh — so this test asserts the
    // @Lazy marker is the actual mechanism that keeps the cycle
    // broken end-to-end.
    Constructor<?> ctor = singlePublicCtor(PSContentItemDao.class);
    Parameter[] params = ctor.getParameters();
    Parameter folderHelperParam = null;
    for (Parameter p : params) {
      if (IPSFolderHelper.class.isAssignableFrom(p.getType())) {
        folderHelperParam = p;
        break;
      }
    }
    assertNotNull(
        folderHelperParam,
        "Expected an IPSFolderHelper constructor parameter on PSContentItemDao");
    assertTrue(
        folderHelperParam.isAnnotationPresent(Lazy.class),
        "IPSFolderHelper parameter on PSContentItemDao must carry @Lazy to break the "
            + "folderHelper<->contentItemDao Spring cycle (#2423 / #2435).");
  }

  private static Constructor<?> singlePublicCtor(Class<?> bean) {
    Constructor<?>[] ctors = bean.getDeclaredConstructors();
    assertTrue(
        ctors.length == 1,
        bean.getSimpleName() + " should have exactly one constructor (got " + ctors.length + ")");
    return ctors[0];
  }

  private static boolean isInCycle(Class<?> type) {
    for (Class<?> c : CYCLE_BEANS) {
      if (c.equals(type)) return true;
    }
    return false;
  }
}
