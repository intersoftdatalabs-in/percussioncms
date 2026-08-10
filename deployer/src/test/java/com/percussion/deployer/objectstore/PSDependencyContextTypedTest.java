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

package com.percussion.deployer.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed {@link PSDependencyContext} / {@link PSDependencyTreeContext}
 * (issue #2697 Xlint batch 3).
 */
public class PSDependencyContextTypedTest {

  private static PSDeployableElement newPkg(String id, String display) {
    return new PSDeployableElement(
        PSDependency.TYPE_SHARED, id, "Package", "Package", display, true, false, false);
  }

  private static PSDeployableObject newShared(String id, String display) {
    return new PSDeployableObject(
        PSDependency.TYPE_SHARED, id, "ObjType", "Obj Type", display, true, false, false);
  }

  private static PSDeployableObject newLocal(String id, String display) {
    return new PSDeployableObject(
        PSDependency.TYPE_LOCAL, id, "ObjType", "Obj Type", display, true, false, false);
  }

  @Test
  public void testAddContainsMultiAndSetIncluded() {
    PSDependencyTreeContext tree = new PSDependencyTreeContext();
    PSDeployableElement pkg = newPkg("pkg1", "pkg-display");
    PSDeployableObject shared1 = newShared("obj1", "shared-one");
    PSDeployableObject shared2 = newShared("obj1", "shared-two");

    tree.addPackage(pkg, false);
    PSDependencyContext ctx = tree.addDependency(shared1, pkg);
    assertNotNull(ctx);
    assertEquals(shared1.getKey(), ctx.getKey());
    assertTrue(ctx.containsDependency(shared1));
    assertFalse(ctx.isMulti());

    tree.addDependency(shared2, pkg);
    assertTrue(ctx.containsDependency(shared2));
    assertTrue(ctx.isMulti());

    assertTrue(ctx.canBeSelected());
    assertTrue(ctx.canBeIncluded());
    assertFalse(ctx.isIncluded());
    assertTrue(ctx.setIncluded(true));
    assertTrue(shared1.isIncluded());
    assertTrue(shared2.isIncluded());
    assertTrue(ctx.isIncluded());
  }

  @Test
  public void testRemoveDependencyClearsMapsAndRejectsUnknown() {
    PSDependencyTreeContext tree = new PSDependencyTreeContext();
    PSDeployableElement pkg = newPkg("pkgA", "pkg-a");
    PSDeployableObject dep = newShared("d1", "dep-one");

    tree.addPackage(pkg, false);
    PSDependencyContext ctx = tree.addDependency(dep, pkg);
    assertTrue(ctx.containsDependency(dep));

    ctx.removeDependency(dep, false);
    assertFalse(ctx.containsDependency(dep));
    assertNull(tree.getDependencyCtx(dep));

    PSDeployableObject orphan = newShared("d1", "orphan");
    assertThrows(IllegalArgumentException.class, () -> ctx.removeDependency(orphan, false));
  }

  @Test
  public void testAddChildDependenciesRecursesIntoTree() {
    PSDependencyTreeContext tree = new PSDependencyTreeContext();
    PSDeployableElement pkg = newPkg("pkgC", "pkg-c");
    PSDeployableObject parent = newShared("parent", "parent");
    PSDeployableObject child = newShared("child", "child");
    parent.setDependencies(Collections.singletonList(child).iterator());

    tree.addPackage(pkg, false);
    PSDependencyContext parentCtx = tree.addDependency(parent, pkg);
    parentCtx.addChildDependencies(parent);

    PSDependencyContext childCtx = tree.getDependencyCtx(child);
    assertNotNull(childCtx);
    assertTrue(childCtx.containsDependency(child));
    assertSame(childCtx, tree.getDependencyCtx(child.getKey()));
  }

  @Test
  public void testCheckRemoveLocalCollectsSharedPeersWhenLocalIncluded() {
    PSDependencyTreeContext tree = new PSDependencyTreeContext();
    PSDeployableElement pLoc = newPkg("pLoc", "p-loc");
    PSDeployableElement pShr = newPkg("pShr", "p-shr");
    // Package must be included so includesDependency treats nested TYPE_LOCAL as included
    pLoc.setIsIncluded(true);
    PSDeployableObject loc = newLocal("k1", "loc");
    PSDeployableObject shr = newShared("k1", "shr");
    shr.setIsIncluded(true);
    // Wire parent chain used by includesDependency(sameInstance=true)
    pLoc.setDependencies(Collections.singletonList(loc).iterator());

    tree.addPackage(pLoc, false);
    tree.addPackage(pShr, false);
    PSDependencyContext ctx = tree.addDependency(loc, pLoc);
    tree.addDependency(shr, pShr);

    // Refresh m_isIncluded via checkIncludedState on remove of the shared peer
    ctx.removeDependency(shr, false);
    assertTrue(ctx.isIncluded(), "local remaining in package should mark context included");
    tree.addDependency(shr, pShr);

    Map<String, List<PSDependency>> result = new HashMap<>();
    ctx.checkRemoveLocal(loc, pLoc, result);
    assertFalse(result.isEmpty(), "shared peers in other packages should be collected");
    assertTrue(result.containsKey(pShr.getKey()));
    assertTrue(result.get(pShr.getKey()).contains(shr));
  }

  @Test
  public void testCheckRemoveLocalNoOpWhenNotIncludedLocal() {
    PSDependencyTreeContext tree = new PSDependencyTreeContext();
    PSDeployableElement pkg = newPkg("pkg", "p");
    PSDeployableObject local = newLocal("id", "l");
    tree.addPackage(pkg, false);
    PSDependencyContext ctx = tree.addDependency(local, pkg);
    assertFalse(ctx.isIncluded());
    Map<String, List<PSDependency>> result = new HashMap<>();
    ctx.checkRemoveLocal(local, pkg, result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void testTreeGetPackageAndCtxLookup() {
    PSDependencyTreeContext tree = new PSDependencyTreeContext();
    PSDeployableElement pkg = newPkg("pkgX", "pkg-x");
    tree.addPackage(pkg, false);
    assertSame(pkg, tree.getPackage(pkg.getKey()));
    assertThrows(IllegalArgumentException.class, () -> tree.getPackage("missing"));
    assertThrows(IllegalArgumentException.class, () -> tree.getPackage(" "));
  }

  @Test
  public void testKeyMismatchRejectedOnAdd() {
    PSDependencyTreeContext tree = new PSDependencyTreeContext();
    PSDeployableElement pkg = newPkg("pkgY", "pkg-y");
    tree.addPackage(pkg, false);
    PSDeployableObject dep = newShared("d", "d");
    PSDependencyContext ctx = tree.addDependency(dep, pkg);
    PSDeployableObject otherKey = newShared("other", "o");
    assertThrows(IllegalArgumentException.class, () -> ctx.addDependency(otherKey, pkg));
  }
}
