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

package com.percussion.deployer.server.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.security.PSSecurityToken;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

/**
 * Signature smoke for typed ComponentSlot / ContentDef / File dependency handler surfaces (issue
 * #2894 Xlint residual after #2861 / PR #2893). Runtime paths require a live CMS; these tests lock
 * compile-time API shapes that cleared rawtypes diagnostics.
 */
public class PSComponentSlotContentDefHandlersTypedTest {

  @Test
  public void componentSlotDependenciesAndChildTypesReturnTypedIterators() throws Exception {
    assertTypedIterator(
        PSComponentSlotDependencyHandler.class.getMethod(
            "getChildDependencies", PSSecurityToken.class, PSDependency.class),
        PSDependency.class);
    assertTypedIterator(
        PSComponentSlotDependencyHandler.class.getMethod("getDependencies", PSSecurityToken.class),
        PSDependency.class);
    assertTypedIterator(
        PSComponentSlotDependencyHandler.class.getMethod(
            "getDependencyFiles", PSSecurityToken.class, PSDependency.class),
        PSDependencyFile.class);
    assertTypedIterator(
        PSComponentSlotDependencyHandler.class.getMethod("getChildTypes"), String.class);
    assertEquals("ComponentSlot", PSComponentSlotDependencyHandler.DEPENDENCY_TYPE);
  }

  @Test
  public void contentDefDependenciesAndChildTypesReturnTypedIterators() throws Exception {
    assertTypedIterator(
        PSContentDefDependencyHandler.class.getMethod(
            "getChildDependencies", PSSecurityToken.class, PSDependency.class),
        PSDependency.class);
    assertTypedIterator(
        PSContentDefDependencyHandler.class.getMethod("getDependencies", PSSecurityToken.class),
        PSDependency.class);
    assertTypedIterator(
        PSContentDefDependencyHandler.class.getMethod(
            "getDependencyFiles", PSSecurityToken.class, PSDependency.class),
        PSDependencyFile.class);
    assertTypedIterator(
        PSContentDefDependencyHandler.class.getMethod("getChildTypes"), String.class);
  }

  @Test
  public void fileHandlerDependenciesAndChildTypesReturnTypedIterators() throws Exception {
    assertTypedIterator(
        PSFileDependencyHandler.class.getMethod(
            "getChildDependencies", PSSecurityToken.class, PSDependency.class),
        PSDependency.class);
    assertTypedIterator(
        PSFileDependencyHandler.class.getMethod("getDependencies", PSSecurityToken.class),
        PSDependency.class);
    assertTypedIterator(
        PSFileDependencyHandler.class.getMethod(
            "getDependencyFiles", PSSecurityToken.class, PSDependency.class),
        PSDependencyFile.class);
    assertTypedIterator(PSFileDependencyHandler.class.getMethod("getChildTypes"), String.class);
  }

  @Test
  public void dataObjectHelpersReturnTypedIterators() throws Exception {
    Method getDepDataFiles =
        PSDataObjectDependencyHandler.class.getDeclaredMethod(
            "getDependecyDataFiles", PSArchiveHandler.class, PSDependency.class);
    getDepDataFiles.setAccessible(true);
    assertTypedIterator(getDepDataFiles, PSDependencyFile.class);

    Method getChildPairIds =
        PSDataObjectDependencyHandler.class.getDeclaredMethod(
            "getChildPairIdsFromTable", String.class, String.class, String.class, String.class);
    getChildPairIds.setAccessible(true);
    assertTypedIterator(getChildPairIds, String.class);
  }

  @Test
  public void configFileTypeConstantStable() {
    assertEquals("ConfigFile", PSConfigFileDependencyHandler.DEPENDENCY_TYPE);
  }

  private static void assertTypedIterator(Method method, Class<?> typeArg) {
    assertEquals(Iterator.class, method.getReturnType());
    Type generic = method.getGenericReturnType();
    assertTrue(generic instanceof ParameterizedType, method.getName() + " should be parameterized");
    Type[] args = ((ParameterizedType) generic).getActualTypeArguments();
    assertEquals(1, args.length);
    assertEquals(typeArg, args[0], method.getName() + " type arg");
  }
}
