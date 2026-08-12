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
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Signature smoke for typed FilterDef / Custom / Context* dependency handler surfaces (issue
 * #3180 / #2028 Xlint residual after SharedGroup+WorkflowDef #3179). Runtime packaging paths
 * require a live CMS; these tests lock compile-time API shapes that cleared rawtypes diagnostics.
 */
public class PSFilterDefCustomContextHandlersTypedTest {

  @Test
  public void filterDefChildDependenciesAndFilesReturnTypedIterators() throws Exception {
    assertTypedIterator(
        PSFilterDefDependencyHandler.class.getMethod(
            "getChildDependencies", PSSecurityToken.class, PSDependency.class),
        PSDependency.class);
    assertTypedIterator(
        PSFilterDefDependencyHandler.class.getMethod("getDependencies", PSSecurityToken.class),
        PSDependency.class);
    assertTypedIterator(
        PSFilterDefDependencyHandler.class.getMethod(
            "getDependencyFiles", PSSecurityToken.class, PSDependency.class),
        PSDependencyFile.class);
    assertTypedIterator(
        PSFilterDefDependencyHandler.class.getMethod("getChildTypes"), String.class);
    assertEquals("FilterDef", PSFilterDefDependencyHandler.DEPENDENCY_TYPE);
  }

  @Test
  public void filterDefFilesFromArchiveReturnsTypedIterator() throws Exception {
    Method method =
        PSFilterDefDependencyHandler.class.getDeclaredMethod(
            "getFilterDependecyFilesFromArchive", PSArchiveHandler.class, PSDependency.class);
    method.setAccessible(true);
    assertTypedIterator(method, PSDependencyFile.class);
  }

  @Test
  public void customChildDependenciesAndDependenciesReturnTypedIterators() throws Exception {
    assertTypedIterator(
        PSCustomDependencyHandler.class.getMethod(
            "getChildDependencies", PSSecurityToken.class, PSDependency.class),
        PSDependency.class);
    assertTypedIterator(
        PSCustomDependencyHandler.class.getMethod("getDependencies", PSSecurityToken.class),
        PSDependency.class);
    assertTypedIterator(PSCustomDependencyHandler.class.getMethod("getChildTypes"), String.class);
    assertEquals("Custom", PSCustomDependencyHandler.DEPENDENCY_TYPE);
  }

  @Test
  public void contextDefAndElementHandlersReturnTypedIterators() throws Exception {
    assertTypedIterator(
        PSContextDefDependencyHandler.class.getMethod(
            "getChildDependencies", PSSecurityToken.class, PSDependency.class),
        PSDependency.class);
    assertTypedIterator(
        PSContextDefDependencyHandler.class.getMethod("getDependencies", PSSecurityToken.class),
        PSDependency.class);
    assertTypedIterator(
        PSContextDefDependencyHandler.class.getMethod(
            "getDependencyFiles", PSSecurityToken.class, PSDependency.class),
        PSDependencyFile.class);
    assertTypedIterator(
        PSContextDefDependencyHandler.class.getMethod("getChildTypes"), String.class);
    assertTypedIterator(PSContextDependencyHandler.class.getMethod("getChildTypes"), String.class);
    assertTypedIterator(PSFilterDependencyHandler.class.getMethod("getChildTypes"), String.class);
    assertEquals("ContextDef", PSContextDefDependencyHandler.DEPENDENCY_TYPE);
    assertEquals("Context", PSContextDependencyHandler.DEPENDENCY_TYPE);
    assertEquals("Filter", PSFilterDependencyHandler.DEPENDENCY_TYPE);
  }

  @Test
  public void filterDefCustomContextChildTypesListsAreStringParameterized() throws Exception {
    assertStringListField(PSFilterDefDependencyHandler.class, "ms_childTypes");
    assertStringListField(PSCustomDependencyHandler.class, "ms_childTypes");
    assertStringListField(PSContextDefDependencyHandler.class, "ms_childTypes");
    assertStringListField(PSContextDependencyHandler.class, "ms_childTypes");
    assertStringListField(PSFilterDependencyHandler.class, "ms_childTypes");
  }

  private static void assertStringListField(Class<?> clazz, String fieldName) throws Exception {
    var field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    Type generic = field.getGenericType();
    assertTrue(generic instanceof ParameterizedType, fieldName + " should be parameterized");
    Type[] args = ((ParameterizedType) generic).getActualTypeArguments();
    assertEquals(1, args.length);
    assertEquals(String.class, args[0]);
    assertEquals(List.class, field.getType());
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
