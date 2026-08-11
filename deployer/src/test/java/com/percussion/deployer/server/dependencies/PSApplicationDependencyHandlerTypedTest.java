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
import com.percussion.security.PSSecurityToken;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

/**
 * Signature smoke for typed Application / AppObject dependency handler surfaces (issue #2847 Xlint
 * residual after batch 5). Runtime paths require a live CMS; these tests lock compile-time API
 * shapes that cleared rawtypes diagnostics.
 */
public class PSApplicationDependencyHandlerTypedTest {

  @Test
  public void getDependenciesAndChildTypesReturnTypedIterators() throws Exception {
    Method getDeps =
        PSApplicationDependencyHandler.class.getMethod("getDependencies", PSSecurityToken.class);
    assertEquals(Iterator.class, getDeps.getReturnType());
    Type generic = getDeps.getGenericReturnType();
    assertTrue(generic instanceof ParameterizedType);
    Type[] args = ((ParameterizedType) generic).getActualTypeArguments();
    assertEquals(1, args.length);
    assertEquals(PSDependency.class, args[0]);

    Method getChildTypes = PSApplicationDependencyHandler.class.getMethod("getChildTypes");
    assertEquals(Iterator.class, getChildTypes.getReturnType());
    Type childGeneric = getChildTypes.getGenericReturnType();
    assertTrue(childGeneric instanceof ParameterizedType);
    Type[] childArgs = ((ParameterizedType) childGeneric).getActualTypeArguments();
    assertEquals(1, childArgs.length);
    assertEquals(String.class, childArgs[0]);
  }

  @Test
  public void getAppFilesReturnsTypedFileIterator() throws Exception {
    Method getAppFiles =
        PSAppObjectDependencyHandler.class.getDeclaredMethod(
            "getAppFiles", PSSecurityToken.class, String.class);
    assertEquals(Iterator.class, getAppFiles.getReturnType());
    Type generic = getAppFiles.getGenericReturnType();
    assertTrue(generic instanceof ParameterizedType);
    Type[] args = ((ParameterizedType) generic).getActualTypeArguments();
    assertEquals(1, args.length);
    assertEquals(File.class, args[0]);
  }

  @Test
  public void dependencyTypeConstantStable() {
    assertEquals("Application", PSApplicationDependencyHandler.DEPENDENCY_TYPE);
  }
}
