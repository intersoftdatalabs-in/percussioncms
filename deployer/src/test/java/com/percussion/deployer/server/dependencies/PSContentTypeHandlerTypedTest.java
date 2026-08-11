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
 * Signature smoke for typed ContentType dependency handler surfaces (issue #3047 / #2028 Xlint
 * residual after ContentRelation #3017). Runtime packaging paths require a live CMS; these tests
 * lock compile-time API shapes that cleared rawtypes diagnostics.
 */
public class PSContentTypeHandlerTypedTest {

  @Test
  public void contentTypeChildDependenciesAndFilesReturnTypedIterators() throws Exception {
    assertTypedIterator(
        PSContentTypeDependencyHandler.class.getMethod(
            "getChildDependencies", PSSecurityToken.class, PSDependency.class),
        PSDependency.class);
    assertTypedIterator(
        PSContentTypeDependencyHandler.class.getMethod(
            "getDependencies", PSSecurityToken.class),
        PSDependency.class);
    assertTypedIterator(
        PSContentTypeDependencyHandler.class.getMethod(
            "getDependencyFiles", PSSecurityToken.class, PSDependency.class),
        PSDependencyFile.class);
    assertTypedIterator(
        PSContentTypeDependencyHandler.class.getMethod("getChildTypes"), String.class);
    assertEquals("ContentType", PSContentTypeDependencyHandler.DEPENDENCY_TYPE);
  }

  @Test
  public void contentTypeChildTypesListIsStringParameterized() throws Exception {
    var field = PSContentTypeDependencyHandler.class.getDeclaredField("ms_childTypes");
    field.setAccessible(true);
    Type generic = field.getGenericType();
    assertTrue(generic instanceof ParameterizedType, "ms_childTypes should be parameterized");
    Type[] args = ((ParameterizedType) generic).getActualTypeArguments();
    assertEquals(1, args.length);
    assertEquals(String.class, args[0]);
    assertEquals(List.class, field.getType());
  }

  @Test
  public void contentTypeItemDefFilesFromArchiveReturnsTypedIterator() throws Exception {
    Method method =
        PSContentTypeDependencyHandler.class.getDeclaredMethod(
            "getItemDefFilesFromArchive",
            com.percussion.deployer.server.PSArchiveHandler.class,
            PSDependency.class);
    method.setAccessible(true);
    assertTypedIterator(method, PSDependencyFile.class);
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
