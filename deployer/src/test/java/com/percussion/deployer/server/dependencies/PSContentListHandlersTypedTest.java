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
import com.percussion.security.PSSecurityToken;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

/**
 * Signature smoke for typed ContentAssembler / Content / ContentListDef dependency handler surfaces
 * (issue #2028 Xlint residual batch 10 after #2915 / PR #2981). Runtime paths require a live CMS;
 * these tests lock compile-time API shapes that cleared rawtypes diagnostics.
 */
public class PSContentListHandlersTypedTest {

  @Test
  public void contentAssemblerChildTypesReturnTypedIterator() throws Exception {
    assertTypedIterator(
        PSContentAssemblerDependencyHandler.class.getMethod("getChildTypes"), String.class);
    assertTypedIterator(
        PSContentAssemblerDependencyHandler.class.getMethod(
            "getChildDependencies", PSSecurityToken.class, PSDependency.class),
        PSDependency.class);
    assertEquals("ContentAssembler", PSContentAssemblerDependencyHandler.DEPENDENCY_TYPE);
  }

  @Test
  public void contentHandlerChildTypesAndListAreTyped() throws Exception {
    assertTypedIterator(
        PSContentDependencyHandler.class.getMethod("getChildTypes"), String.class);
    assertTypedIterator(
        PSContentDependencyHandler.class.getMethod(
            "getChildDependencies", PSSecurityToken.class, PSDependency.class),
        PSDependency.class);
    assertEquals("Content", PSContentDependencyHandler.DEPENDENCY_TYPE);
  }

  @Test
  public void contentListDefDependenciesAndChildTypesReturnTypedIterators() throws Exception {
    assertTypedIterator(
        PSContentListDefDependencyHandler.class.getMethod(
            "getChildDependencies", PSSecurityToken.class, PSDependency.class),
        PSDependency.class);
    assertTypedIterator(
        PSContentListDefDependencyHandler.class.getMethod(
            "getDependencies", PSSecurityToken.class),
        PSDependency.class);
    assertTypedIterator(
        PSContentListDefDependencyHandler.class.getMethod(
            "getDependencyFiles", PSSecurityToken.class, PSDependency.class),
        PSDependencyFile.class);
    assertTypedIterator(
        PSContentListDefDependencyHandler.class.getMethod("getChildTypes"), String.class);
    assertTypedIterator(
        PSContentListDefDependencyHandler.class.getMethod(
            "getContentListNames", String.class),
        String.class);
    assertEquals("ContentListDef", PSContentListDefDependencyHandler.DEPENDENCY_TYPE);
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
