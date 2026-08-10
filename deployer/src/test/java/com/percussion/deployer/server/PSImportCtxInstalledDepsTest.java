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

package com.percussion.deployer.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.percussion.deployer.objectstore.PSAppPolicySettings;
import com.percussion.deployer.objectstore.PSDbmsInfo;
import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDeployableElement;
import com.percussion.deployer.objectstore.PSDeployableObject;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed package-dependency tracking on {@link PSImportCtx} after real
 * generics cleanup of {@code m_installedPkgDeps} (issue #2417).
 */
public class PSImportCtxInstalledDepsTest {

  @Test
  public void testInstalledDependencyTracking() {
    PSImportCtx ctx =
        new PSImportCtx(
            "tester",
            new PSDbmsInfo("drv", "server", "db", "origin", "uid", "pwd", false),
            null,
            mock(PSIdMapManager.class),
            mock(PSLogHandler.class),
            new PSAppPolicySettings());

    PSDeployableElement pkg =
        new PSDeployableElement(
            PSDependency.TYPE_SHARED,
            "pkg1",
            "PkgType",
            "Package",
            "MyPackage",
            true,
            false,
            false);
    PSDeployableObject dep =
        new PSDeployableObject(
            PSDependency.TYPE_LOCAL,
            "dep1",
            "ObjType",
            "Object",
            "MyObject",
            true,
            false,
            true);

    assertFalse(ctx.isDependencyInstalled(dep));
    assertFalse(ctx.isDependencyInstalled(dep, pkg));

    ctx.addInstalledDependency(dep, pkg);

    assertTrue(ctx.isDependencyInstalled(dep));
    assertTrue(ctx.isDependencyInstalled(dep, pkg));

    // same package again is illegal
    assertThrows(IllegalStateException.class, () -> ctx.addInstalledDependency(dep, pkg));

    // different package is allowed for same dep key
    PSDeployableElement pkg2 =
        new PSDeployableElement(
            PSDependency.TYPE_SHARED,
            "pkg2",
            "PkgType",
            "Package",
            "OtherPackage",
            true,
            false,
            false);
    ctx.addInstalledDependency(dep, pkg2);
    assertTrue(ctx.isDependencyInstalled(dep, pkg2));
  }
}
