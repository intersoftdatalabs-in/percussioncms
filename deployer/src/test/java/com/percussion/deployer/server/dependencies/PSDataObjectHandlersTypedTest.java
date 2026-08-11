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
import com.percussion.tablefactory.PSJdbcTableData;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Iterator;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Signature smoke for typed DataObject / CmsObject / AuthType / Community / Component dependency
 * handler surfaces (issue #2861 Xlint residual after #2847). Runtime paths require a live CMS;
 * these tests lock compile-time API shapes that cleared rawtypes diagnostics.
 */
public class PSDataObjectHandlersTypedTest {

  @Test
  public void dataObjectHelperMethodsReturnTypedIterators() throws Exception {
    Method getAppNames =
        PSDataObjectDependencyHandler.class.getDeclaredMethod(
            "getAppNamesFromTableData", PSJdbcTableData.class, String.class, String.class);
    assertTypedIterator(getAppNames.getGenericReturnType(), String.class);

    Method getDepDataFiles =
        PSDataObjectDependencyHandler.class.getDeclaredMethod(
            "getDependecyDataFiles", PSArchiveHandler.class, PSDependency.class);
    assertTypedIterator(getDepDataFiles.getGenericReturnType(), PSDependencyFile.class);

    Method getChildPairIds =
        PSDataObjectDependencyHandler.class.getDeclaredMethod(
            "getChildPairIdsFromTable", String.class, String.class, String.class, String.class);
    assertTypedIterator(getChildPairIds.getGenericReturnType(), String.class);
  }

  @Test
  public void cmsObjectArchiveAndProcessorSurfacesAreTyped() throws Exception {
    Method getFiles =
        PSCmsObjectDependencyHandler.class.getDeclaredMethod(
            "getDependencyFilesFromArchive", PSArchiveHandler.class, PSDependency.class);
    assertTypedIterator(getFiles.getGenericReturnType(), PSDependencyFile.class);

    Method relProc =
        PSCmsObjectDependencyHandler.class.getDeclaredMethod(
            "getRelationshipProcessor", PSSecurityToken.class, Map.class);
    Type[] params = relProc.getGenericParameterTypes();
    assertEquals(2, params.length);
    assertTrue(params[1] instanceof ParameterizedType);
    ParameterizedType mapType = (ParameterizedType) params[1];
    assertEquals(Map.class, mapType.getRawType());
    Type[] mapArgs = mapType.getActualTypeArguments();
    assertEquals(String.class, mapArgs[0]);
    assertTrue(
        mapArgs[1] instanceof WildcardType,
        "second map type arg should be wildcard (?), was " + mapArgs[1]);
  }

  @Test
  public void authTypeChildTypesAndDependencyFilesAreTyped() throws Exception {
    Method getChildTypes = PSAuthTypeDependencyHandler.class.getMethod("getChildTypes");
    assertTypedIterator(getChildTypes.getGenericReturnType(), String.class);

    Method getDepFiles =
        PSAuthTypeDependencyHandler.class.getMethod(
            "getDependencyFiles", PSSecurityToken.class, PSDependency.class);
    assertTypedIterator(getDepFiles.getGenericReturnType(), PSDependencyFile.class);

    assertEquals("AuthType", PSAuthTypeDependencyHandler.DEPENDENCY_TYPE);
  }

  @Test
  public void communityAndComponentHandlerSurfacesAreTyped() throws Exception {
    Method communityChildTypes = PSCommunityDependencyHandler.class.getMethod("getChildTypes");
    assertTypedIterator(communityChildTypes.getGenericReturnType(), String.class);

    Method ceChildTypes = PSCEDependencyHandler.class.getMethod("getChildTypes");
    assertTypedIterator(ceChildTypes.getGenericReturnType(), String.class);

    Method compDeps =
        PSComponentDefDependencyHandler.class.getMethod("getDependencies", PSSecurityToken.class);
    assertTypedIterator(compDeps.getGenericReturnType(), PSDependency.class);

    Method compFiles =
        PSComponentDefDependencyHandler.class.getMethod(
            "getDependencyFiles", PSSecurityToken.class, PSDependency.class);
    assertTypedIterator(compFiles.getGenericReturnType(), PSDependencyFile.class);

    Method aclFiles =
        PSAclDefDependencyHandler.class.getDeclaredMethod(
            "getAclDependencyFilesFromArchive", PSArchiveHandler.class, PSDependency.class);
    assertTypedIterator(aclFiles.getGenericReturnType(), PSDependencyFile.class);
  }

  private static void assertTypedIterator(Type genericReturn, Class<?> elementType) {
    assertTrue(genericReturn instanceof ParameterizedType, "expected ParameterizedType, got " + genericReturn);
    ParameterizedType pt = (ParameterizedType) genericReturn;
    assertEquals(Iterator.class, pt.getRawType());
    Type[] args = pt.getActualTypeArguments();
    assertEquals(1, args.length);
    assertEquals(elementType, args[0]);
  }
}
