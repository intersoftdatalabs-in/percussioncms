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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.server.dependencies.PSContentEditorObjectDependencyHandler;
import com.percussion.design.objectstore.PSContainerLocator;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

/**
 * Signature smoke for typed leaf/job/helper surfaces (issue #2915 Xlint residual after #2894 /
 * #2861). Runtime paths often need a live CMS; these tests lock compile-time API shapes that
 * cleared rawtypes / unchecked diagnostics.
 */
public class PSDeployJobLeafTypedTest {

  @Test
  public void deployJobInitDepCountAcceptsTypedDependencyIterator() throws Exception {
    Method oneArg = PSDeployJob.class.getDeclaredMethod("initDepCount", Iterator.class);
    assertTypedDependencyIteratorParam(oneArg.getGenericParameterTypes()[0]);

    Method twoArg =
        PSDeployJob.class.getDeclaredMethod("initDepCount", Iterator.class, boolean.class);
    assertTypedDependencyIteratorParam(twoArg.getGenericParameterTypes()[0]);
  }

  @Test
  public void contentEditorObjectLocatorTablesReturnsTypedStringIterator() throws Exception {
    Method m =
        PSContentEditorObjectDependencyHandler.class.getMethod(
            "getLocatorTables", PSContainerLocator.class);
    assertTypedIterator(m.getGenericReturnType(), String.class);
  }

  @Test
  public void pkgElementDeclaresSerialVersionUid() throws Exception {
    Class<?> pkgElement = Class.forName("com.percussion.services.pkginfo.data.PSPkgElement");
    assertTrue(Serializable.class.isAssignableFrom(pkgElement));
    Field field = pkgElement.getDeclaredField("serialVersionUID");
    assertEquals(long.class, field.getType());
    field.setAccessible(true);
    assertEquals(1L, field.getLong(null));
  }

  private static void assertTypedDependencyIteratorParam(Type paramType) {
    assertTrue(paramType instanceof ParameterizedType, "expected ParameterizedType, got " + paramType);
    ParameterizedType p = (ParameterizedType) paramType;
    assertEquals(Iterator.class, p.getRawType());
    Type arg = p.getActualTypeArguments()[0];
    if (arg instanceof WildcardType) {
      Type[] upper = ((WildcardType) arg).getUpperBounds();
      assertEquals(1, upper.length);
      assertEquals(PSDependency.class, upper[0]);
    } else {
      assertEquals(PSDependency.class, arg);
    }
  }

  private static void assertTypedIterator(Type genericReturn, Class<?> elementType) {
    assertTrue(
        genericReturn instanceof ParameterizedType,
        "expected ParameterizedType, got " + genericReturn);
    ParameterizedType p = (ParameterizedType) genericReturn;
    assertEquals(Iterator.class, p.getRawType());
    assertEquals(elementType, p.getActualTypeArguments()[0]);
  }
}
