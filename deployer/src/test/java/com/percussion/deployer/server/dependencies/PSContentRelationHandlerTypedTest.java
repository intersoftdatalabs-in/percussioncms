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
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Signature smoke for typed ContentRelation dependency handler surfaces (issue #3017 / #2028 Xlint
 * residual after batch 10 #3016). Runtime packaging paths require a live CMS; these tests lock
 * compile-time API shapes that cleared rawtypes diagnostics.
 */
public class PSContentRelationHandlerTypedTest {

  @Test
  public void contentRelationChildDependenciesAndFilesReturnTypedIterators() throws Exception {
    assertTypedIterator(
        PSContentRelationDependencyHandler.class.getMethod(
            "getChildDependencies", PSSecurityToken.class, PSDependency.class),
        PSDependency.class);
    assertTypedIterator(
        PSContentRelationDependencyHandler.class.getMethod(
            "getDependencies", PSSecurityToken.class),
        PSDependency.class);
    assertTypedIterator(
        PSContentRelationDependencyHandler.class.getMethod(
            "getDependencyFiles", PSSecurityToken.class, PSDependency.class),
        PSDependencyFile.class);
    assertTypedIterator(
        PSContentRelationDependencyHandler.class.getMethod("getChildTypes"), String.class);
    assertEquals("ContentRelation", PSContentRelationDependencyHandler.DEPENDENCY_TYPE);
  }

  @Test
  public void contentRelationChildTypesListIsStringParameterized() throws Exception {
    var field = PSContentRelationDependencyHandler.class.getDeclaredField("ms_childTypes");
    field.setAccessible(true);
    Type generic = field.getGenericType();
    assertTrue(generic instanceof ParameterizedType, "ms_childTypes should be parameterized");
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
