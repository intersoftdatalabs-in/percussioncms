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
package com.percussion.share.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.assetmanagement.data.PSAssetList;
import com.percussion.assetmanagement.data.PSAssetSummaryList;
import com.percussion.itemmanagement.data.PSItemStateTransition;
import com.percussion.pagemanagement.data.PSWidgetContentTypeList;
import com.percussion.pagemanagement.data.PSWidgetSummaryList;
import com.percussion.sitemanage.data.PSPublishingActionList;
import com.percussion.sitemanage.data.PSSiteSection;
import com.percussion.user.data.PSUser;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.junit.jupiter.api.Test;

/**
 * Guards serialVersionUID + construction for JAXB/JSON list wrappers cleaned in issue #2032 batch
 * 1, residual serialVersionUID batch 2 (#2421), and serial-field concrete collection types (#2870).
 */
public class PSSerializableListWrappersTest {

  @Test
  public void listWrappersDefineSerialVersionUid() throws Exception {
    assertSerialVersionUid(PSItemPropertiesList.class);
    assertSerialVersionUid(PSLightWeightObjectList.class);
    assertSerialVersionUid(PSAssetList.class);
    assertSerialVersionUid(PSAssetSummaryList.class);
    assertSerialVersionUid(PSWidgetContentTypeList.class);
    assertSerialVersionUid(PSWidgetSummaryList.class);
    assertSerialVersionUid(PSPublishingActionList.class);
  }

  /**
   * Guards serialVersionUID for residual DTO / nested-exception types cleaned in issue #2421 batch
   * 2 (after #2032 batch 1).
   */
  @Test
  public void residualDtoAndExceptionTypesDefineSerialVersionUid() throws Exception {
    assertSerialVersionUid(PSEnumVals.EnumVal.class);
    assertSerialVersionUid(com.percussion.pathmanagement.data.PSGenerateSiteMapOptions.class);
    assertSerialVersionUid(com.percussion.foldermanagement.data.PSFolderItem.class);
    assertSerialVersionUid(com.percussion.foldermanagement.data.PSFolders.class);
    assertSerialVersionUid(
        com.percussion.foldermanagement.data.PSGetAssignedFoldersJobStatus.class);
    assertSerialVersionUid(com.percussion.user.data.PSCurrentUser.class);
    assertSerialVersionUid(com.percussion.assetmanagement.data.PSContentEditCriteria.class);
    assertSerialVersionUid(com.percussion.itemmanagement.data.PSItemDates.class);
    assertSerialVersionUid(com.percussion.itemmanagement.data.PSSoProMetadata.class);
    assertSerialVersionUid(com.percussion.itemmanagement.data.PSRevision.class);
    assertSerialVersionUid(com.percussion.itemmanagement.data.PSComment.class);
    assertSerialVersionUid(com.percussion.assetmanagement.data.PSUnusedAssetSummary.class);
    assertSerialVersionUid(com.percussion.category.data.PSCategory.class);
    assertSerialVersionUid(com.percussion.category.data.PSCategoryNode.class);
    assertSerialVersionUid(com.percussion.dashboardmanagement.data.PSDashboardConfiguration.class);
    assertSerialVersionUid(com.percussion.sitemanage.importer.data.PSImportLogEntry.class);
    assertSerialVersionUid(com.percussion.licensemanagement.data.PSLicenseStatus.class);
    assertSerialVersionUid(com.percussion.redirect.data.PSCreateRedirectRequest.class);
    assertSerialVersionUid(
        com.percussion.designmanagement.service.IPSFileSystemService.PSFolderOperationException
            .class);
    assertSerialVersionUid(
        com.percussion.designmanagement.service.IPSFileSystemService.PSFileOperationException
            .class);
    assertSerialVersionUid(
        com.percussion.designmanagement.service.IPSFileSystemService.PSFileAlreadyExistsException
            .class);
    assertSerialVersionUid(
        com.percussion.cloudservice.IPSCloudService.PSCloudServiceException.class);
    assertSerialVersionUid(
        com.percussion.pageoptimizer.IPSPageOptimizerService.PageOptimizerException.class);
  }

  @Test
  public void itemPropertiesListPreservesOrderAndContents() {
    var a = new PSItemProperties();
    a.setId("1");
    var b = new PSItemProperties();
    b.setId("2");
    var list = new PSItemPropertiesList(List.of(a, b));
    assertEquals(2, list.size());
    assertEquals("1", list.get(0).getId());
    assertEquals("2", list.get(1).getId());
  }

  /**
   * Guards issue #2870 serial-field cleanup: representative DTO collection fields use concrete
   * serializable types (ArrayList/HashMap/HashSet/Vector), not bare List/Map/Set interfaces.
   */
  @Test
  public void serialFieldCollectionTypesAreConcreteAndSerializable() throws Exception {
    assertCollectionFieldConcrete(PSUser.class, "roles", ArrayList.class);
    assertCollectionFieldConcrete(PSItemStateTransition.class, "transitionTriggers", ArrayList.class);
    assertCollectionFieldConcrete(PSSiteSection.class, "childIds", ArrayList.class);
    assertCollectionFieldConcrete(PSItemProperties.class, "tags", ArrayList.class);
    assertCollectionFieldConcrete(
        com.percussion.pathmanagement.data.PSPathItem.class, "displayProperties", HashMap.class);
    assertCollectionFieldConcrete(
        com.percussion.dashboardmanagement.data.PSGadget.class, "settings", HashMap.class);
    assertCollectionFieldConcrete(
        com.percussion.comments.data.PSComment.class, "commentTags", HashSet.class);
    assertCollectionFieldConcrete(
        com.percussion.pagemanagement.assembler.impl.PSProxyAssemblyTemplate.class,
        "bindings",
        Vector.class);
  }

  /**
   * Setters accepting List/Map must defensive-copy into the concrete field so callers cannot share
   * a non-serializable collection implementation with the DTO.
   */
  @Test
  public void serialFieldSettersDefensivelyCopyCollections() {
    var user = new PSUser();
    List<String> linked = new LinkedList<>(Arrays.asList("Editor", "Admin"));
    user.setRoles(linked);
    assertEquals(List.of("Editor", "Admin"), user.getRoles());
    assertTrue(user.getRoles() instanceof ArrayList, "roles field/runtime should be ArrayList");
    linked.add("Extra");
    assertEquals(
        2, user.getRoles().size(), "mutating input list must not change DTO roles after set");

    var transition = new PSItemStateTransition();
    List<String> triggers = new LinkedList<>(List.of("approve", "reject"));
    transition.setTransitionTriggers(triggers);
    assertEquals(List.of("approve", "reject"), transition.getTransitionTriggers());
    triggers.clear();
    assertEquals(2, transition.getTransitionTriggers().size());

    var section = new PSSiteSection();
    section.setChildIds(new LinkedList<>(List.of("a", "b")));
    assertEquals(List.of("a", "b"), section.getChildIds());
    section.setChildIds(null);
    assertNotNull(section.getChildIds());
    assertTrue(section.getChildIds().isEmpty());
  }

  private static void assertSerialVersionUid(Class<?> type) throws Exception {
    assertTrue(Serializable.class.isAssignableFrom(type), type.getName() + " should be Serializable");
    Field f = type.getDeclaredField("serialVersionUID");
    f.setAccessible(true);
    assertNotNull(f.get(null));
    assertEquals(long.class, f.getType());
  }

  private static void assertCollectionFieldConcrete(
      Class<?> type, String fieldName, Class<?> expectedConcrete) throws Exception {
    Field f = type.getDeclaredField(fieldName);
    Class<?> fieldType = f.getType();
    assertTrue(
        expectedConcrete.equals(fieldType),
        type.getName()
            + "#"
            + fieldName
            + " expected field type "
            + expectedConcrete.getName()
            + " but was "
            + fieldType.getName());
    assertTrue(
        Serializable.class.isAssignableFrom(fieldType),
        type.getName() + "#" + fieldName + " field type must be Serializable");
    assertTrue(
        !fieldType.isInterface(),
        type.getName() + "#" + fieldName + " must not be a bare collection interface");
    // keep modifiers readable for future audits
    assertTrue(Modifier.isPrivate(f.getModifiers()) || Modifier.isProtected(f.getModifiers()));
  }
}
