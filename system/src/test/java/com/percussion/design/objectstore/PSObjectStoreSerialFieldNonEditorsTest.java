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
package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.debug.PSTraceFlag;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Java serialization surface checks for design.objectstore serial-field cleanup on non-editor high
 * count holders (#2451 / parent #2022). Prefer concrete {@link Serializable} collections/maps or
 * product-safe {@code transient}; companion interfaces extend {@link Serializable}.
 */
public class PSObjectStoreSerialFieldNonEditorsTest {

  @Test
  public void testDocumentMappingAndResultsInterfacesAreSerializable() {
    assertTrue(
        Serializable.class.isAssignableFrom(IPSDocumentMapping.class),
        "IPSDocumentMapping must extend Serializable for PSDataMapping.m_docMapping");
    assertTrue(
        Serializable.class.isAssignableFrom(IPSResults.class),
        "IPSResults must extend Serializable for PSDataSet.m_results");
  }

  @Test
  public void testConcreteFieldTypesOnNonEditorHolders() throws Exception {
    assertEquals(HashMap.class, PSSearchConfig.class.getDeclaredField("m_properties").getType());
    assertEquals(HashMap.class, PSSearchConfig.class.getDeclaredField("m_analyzers").getType());
    assertEquals(
        HashMap.class, PSSearchConfig.class.getDeclaredField("m_textConverters").getType());

    assertEquals(HashSet.class, PSResultPage.class.getDeclaredField("m_extensions").getType());

    assertEquals(
        ArrayList.class,
        PSJndiGroupProviderInstance.class.getDeclaredField("m_objectClasses").getType());
    assertEquals(
        HashMap.class,
        PSJndiGroupProviderInstance.class.getDeclaredField("m_objectClassMap").getType());
    assertEquals(
        ArrayList.class,
        PSJndiGroupProviderInstance.class.getDeclaredField("m_groupNodes").getType());

    assertEquals(ArrayList.class, PSRevisionHistory.class.getDeclaredField("m_revs").getType());
    assertEquals(
        HashMap.class, PSDirectorySet.class.getDeclaredField("m_requiredAttributeNames").getType());
    assertEquals(
        ArrayList.class, PSDirectory.class.getDeclaredField("m_groupProviderNames").getType());
    assertEquals(
        ArrayList.class, PSResourceCacheSettings.class.getDeclaredField("m_extraKeys").getType());
    assertEquals(
        ArrayList.class,
        PSResourceCacheSettings.class.getDeclaredField("m_dependencies").getType());
    assertEquals(
        ArrayList.class, PSConditionalEffect.class.getDeclaredField("m_exeContexts").getType());
    assertEquals(ArrayList.class, PSWorkflowInfo.class.getDeclaredField("m_values").getType());
    assertEquals(ArrayList.class, PSFunctionCall.class.getDeclaredField("m_params").getType());
    assertEquals(ArrayList.class, PSExtensionCall.class.getDeclaredField("m_params").getType());
    assertEquals(ArrayList.class, PSExtensionCall.class.getDeclaredField("m_applyTo").getType());
    assertEquals(
        ArrayList.class, PSRelationship.class.getDeclaredField("m_userProperties").getType());
    assertEquals(ArrayList.class, PSDisplayError.class.getDeclaredField("m_details").getType());
    assertEquals(ArrayList.class, PSDetails.class.getDeclaredField("m_fieldErrors").getType());
  }

  @Test
  public void testTransientRuntimeCaches() throws Exception {
    assertTrue(
        java.lang.reflect.Modifier.isTransient(
            PSDataMapping.class.getDeclaredField("m_textFormatter").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isTransient(
            PSConfig.class.getDeclaredField("m_configObj").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isTransient(
            PSFile.class.getDeclaredField("m_content").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isTransient(
            PSFunctionCall.class.getDeclaredField("m_dbFuncDef").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isTransient(
            PSWorkflowInfo.class.getDeclaredField("m_valueAccessor").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isTransient(
            PSUserConfiguration.class.getDeclaredField("m_propertyTree").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isTransient(
            PSSystemValidationException.class
                .getDeclaredField("m_sourceDocument")
                .getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isTransient(
            PSMinorValidationException.class
                .getDeclaredField("m_sourceComponent")
                .getModifiers()));
  }

  @Test
  public void testSearchConfigJavaSerializationRoundTrip() throws Exception {
    PSSearchConfig cfg = new PSSearchConfig();
    cfg.addCustomProp("indexRootDir", "/tmp/index");
    cfg.setFtsEnabled(true);
    cfg.setAdminMaster(true);

    PSSearchConfig ser = roundTrip(cfg);
    assertEquals(cfg, ser);
    assertEquals("/tmp/index", ser.getCustomProp("indexRootDir"));
    assertTrue(ser.isFtsEnabled());
    assertTrue(ser.isAdminMaster());
    assertEquals(HashMap.class, ser.getCustomProps().getClass());
  }

  @Test
  public void testResultPageExtensionsRoundTrip() throws Exception {
    PSResultPage page = new PSResultPage((java.net.URL) null);
    page.setExtensions(new HashSet<>(Arrays.asList("html", "xml")));

    PSResultPage ser = roundTrip(page);
    assertEquals(page, ser);
    assertTrue(ser.getExtensions().contains("html"));
    assertTrue(ser.getExtensions().contains("xml"));
    assertEquals(2, ser.getExtensions().size());
  }

  @Test
  public void testWorkflowInfoAndTraceFlagSerializableCompanions() throws Exception {
    assertTrue(Serializable.class.isAssignableFrom(PSTraceFlag.class));
    assertTrue(Serializable.class.isAssignableFrom(PSDisplayError.class));
    assertTrue(Serializable.class.isAssignableFrom(PSDetails.class));
    assertTrue(Serializable.class.isAssignableFrom(PSFieldError.class));

    PSWorkflowInfo info =
        new PSWorkflowInfo(PSWorkflowInfo.TYPE_INCLUSIONARY, Arrays.asList(1, 2, 3));
    PSWorkflowInfo ser = roundTrip(info);
    List values = ser.getWorkflowIds();
    assertEquals(3, values.size());
    assertTrue(values.contains(1));
    assertEquals(PSWorkflowInfo.TYPE_INCLUSIONARY, ser.getType());
  }

  @Test
  public void testResourceCacheSettingsAndDirectorySetFieldTypes() throws Exception {
    PSResourceCacheSettings settings = new PSResourceCacheSettings();
    settings.setIsCachingEnabled(true);
    PSResourceCacheSettings ser = roundTrip(settings);
    assertEquals(settings, ser);
    assertTrue(ser.isCachingEnabled());

    assertTrue(
        Serializable.class.isAssignableFrom(
            PSDirectorySet.class.getDeclaredField("m_requiredAttributeNames").getType()));
    assertTrue(
        Serializable.class.isAssignableFrom(
            PSProperty.class.getDeclaredField("m_value").getType()));
  }

  @Test
  public void testConditionalEffectExecutionContextsFieldType() throws Exception {
    // Field declared type is the serial-field surface (setter always copies into ArrayList).
    assertEquals(
        ArrayList.class, PSConditionalEffect.class.getDeclaredField("m_exeContexts").getType());
  }

  @SuppressWarnings("unchecked")
  private static <T> T roundTrip(T value) throws Exception {
    byte[] bytes;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(value);
      bytes = bos.toByteArray();
    }
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return (T) ois.readObject();
    }
  }
}
