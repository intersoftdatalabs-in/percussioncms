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

package com.percussion.deployer.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.deployer.objectstore.PSApplicationIDTypes;
import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDeployableElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Signature-level coverage for parameterized {@link PSDeploymentManager} public APIs introduced in
 * issue #2764 Xlint batch 4 (no live server connection required).
 */
public class PSDeploymentManagerSignatureTypedTest {

  @Test
  public void getDeployableElementsReturnsTypedIterator() throws Exception {
    Method m = PSDeploymentManager.class.getMethod("getDeployableElements", String.class);
    assertEqualsIteratorOf(m.getGenericReturnType(), PSDeployableElement.class);
  }

  @Test
  public void getDependenciesReturnsTypedIterator() throws Exception {
    Method m = PSDeploymentManager.class.getMethod("getDependencies", String.class, String.class);
    assertEqualsIteratorOf(m.getGenericReturnType(), PSDependency.class);
  }

  @Test
  public void getParentTypesReturnsStringMap() throws Exception {
    Method m = PSDeploymentManager.class.getMethod("getParentTypes");
    Type ret = m.getGenericReturnType();
    assertTrue(ret instanceof ParameterizedType);
    ParameterizedType pt = (ParameterizedType) ret;
    assertTrue(pt.getRawType() == Map.class);
    Type[] args = pt.getActualTypeArguments();
    assertTrue(args[0] == String.class);
    assertTrue(args[1] == String.class);
  }

  @Test
  public void getIdTypesReturnsTypedIterator() throws Exception {
    Method m = PSDeploymentManager.class.getMethod("getIdTypes", Iterator.class);
    assertEqualsIteratorOf(m.getGenericReturnType(), PSApplicationIDTypes.class);
  }

  @Test
  public void constructorRejectsNullConnection() {
    assertThrows(IllegalArgumentException.class, () -> new PSDeploymentManager(null));
  }

  @Test
  public void classIsPresentOnClasspath() {
    assertNotNull(PSDeploymentManager.class.getName());
  }

  private static void assertEqualsIteratorOf(Type ret, Class<?> elementType) {
    assertTrue(ret instanceof ParameterizedType, "expected parameterized return, got " + ret);
    ParameterizedType pt = (ParameterizedType) ret;
    assertTrue(pt.getRawType() == Iterator.class);
    Type arg = pt.getActualTypeArguments()[0];
    assertTrue(arg == elementType, "expected " + elementType + " but was " + arg);
  }
}
