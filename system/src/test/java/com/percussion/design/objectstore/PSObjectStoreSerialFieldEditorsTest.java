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

import com.percussion.util.PSCollection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Java serialization surface checks for design.objectstore serial-field cleanup on hottest content
 * editor types (#2405) and editor mapper companions (#2452 / parent #2022). Field declared types use
 * concrete {@link Serializable} collections/maps or Serializable {@link PSCollection} hierarchy
 * (utils #2450); companion holders implement {@link Serializable}.
 */
public class PSObjectStoreSerialFieldEditorsTest {

  @Test
  public void testReplacementValueInterfaceIsSerializable() {
    assertTrue(
        Serializable.class.isAssignableFrom(IPSReplacementValue.class),
        "IPSReplacementValue must extend Serializable so IPSBackEndMapping/locator fields clear serial-field");
    assertTrue(Serializable.class.isAssignableFrom(IPSBackEndMapping.class));
  }

  @Test
  public void testViewAndViewSetJavaSerialization() throws Exception {
    PSView view = new PSView("sys_All", Collections.singletonList("sys_title").iterator());
    PSViewSet viewSet = new PSViewSet();
    viewSet.addView(view);

    byte[] bytes;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(view);
      oos.writeObject(viewSet);
      bytes = bos.toByteArray();
    }

    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      PSView serView = (PSView) ois.readObject();
      PSViewSet serSet = (PSViewSet) ois.readObject();

      assertEquals(view, serView);
      assertEquals("sys_All", serView.getName());
      assertEquals(view, serSet.getView("sys_All"));
      assertEquals(view, serSet.getView("SYS_ALL"));
    }
  }

  @Test
  public void testControlDependencyMapEmptyRoundTrip() throws Exception {
    PSControlDependencyMap map = new PSControlDependencyMap();

    byte[] bytes;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(map);
      bytes = bos.toByteArray();
    }

    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      PSControlDependencyMap ser = (PSControlDependencyMap) ois.readObject();
      assertNotNull(ser);
      assertNotNull(ser.getInputDataExtensions());
      assertTrue(ser.getInputDataExtensions().isEmpty());
    }
  }

  @Test
  public void testConcreteFieldTypesOnHottestEditors() throws Exception {
    // Reflect declared field types so regressions to Map/List/Collection interfaces fail the suite.
    assertEquals(
        ArrayList.class,
        PSContentEditor.class.getDeclaredField("m_customActionGroups").getType());
    assertEquals(HashMap.class, PSField.class.getDeclaredField("m_occurrenceSettings").getType());
    assertEquals(HashMap.class, PSField.class.getDeclaredField("m_properties").getType());
    assertEquals(TreeMap.class, PSFieldSet.class.getDeclaredField("m_fields").getType());
    assertEquals(ArrayList.class, PSControlMeta.class.getDeclaredField("m_params").getType());
    assertEquals(ArrayList.class, PSControlMeta.class.getDeclaredField("m_dependencies").getType());
    assertEquals(ArrayList.class, PSControlMeta.class.getDeclaredField("m_files").getType());
    assertEquals(ArrayList.class, PSControlParameter.class.getDeclaredField("m_choiceList").getType());
    assertEquals(ArrayList.class, PSView.class.getDeclaredField("m_fields").getType());
    assertEquals(HashMap.class, PSViewSet.class.getDeclaredField("m_views").getType());
    assertEquals(HashMap.class, PSViewSet.class.getDeclaredField("m_conditionalViews").getType());
    assertEquals(
        HashMap.class,
        PSControlDependencyMap.class.getDeclaredField("m_controlDependencies").getType());
  }

  /**
   * Mapper companions (#2452): declared field types must remain Serializable concrete types after
   * PSCollection/PSConcurrentList became Serializable (#2450).
   */
  @Test
  public void testMapperCompanionFieldTypesAreSerializable() throws Exception {
    assertEquals(
        PSCollection.class, PSUIDefinition.class.getDeclaredField("m_defaultUI").getType());
    assertEquals(
        PSDisplayMapper.class, PSUIDefinition.class.getDeclaredField("m_displayMapper").getType());
    assertEquals(
        PSCollection.class, PSCustomActionGroup.class.getDeclaredField("m_removeActions").getType());
    assertEquals(
        PSActionLinkList.class,
        PSCustomActionGroup.class.getDeclaredField("m_actionLinkList").getType());
    assertEquals(
        PSLocation.class, PSCustomActionGroup.class.getDeclaredField("m_location").getType());
    assertEquals(
        PSFormAction.class, PSCustomActionGroup.class.getDeclaredField("m_formAction").getType());
    assertEquals(
        PSDisplayMapper.class,
        PSDisplayMapping.class.getDeclaredField("m_displayMapper").getType());
    assertEquals(PSUISet.class, PSDisplayMapping.class.getDeclaredField("m_uiSet").getType());
    assertEquals(
        ArrayList.class, PSLocation.class.getDeclaredField("m_fieldRefs").getType());

    assertEquals(
        PSCollection.class, PSContentEditor.class.getDeclaredField("m_sectionLinkList").getType());
    assertEquals(
        PSValidationRules.class,
        PSContentEditor.class.getDeclaredField("m_validationRules").getType());
    assertEquals(
        PSInputTranslations.class,
        PSContentEditor.class.getDeclaredField("m_inputTranslations").getType());
    assertEquals(
        PSOutputTranslations.class,
        PSContentEditor.class.getDeclaredField("m_outputTranslations").getType());

    // Direct field types must be Serializable for -Xlint:serial serial-field.
    assertTrue(Serializable.class.isAssignableFrom(PSCollection.class));
    assertTrue(Serializable.class.isAssignableFrom(PSDisplayMapper.class));
    assertTrue(Serializable.class.isAssignableFrom(PSActionLinkList.class));
    assertTrue(Serializable.class.isAssignableFrom(PSLocation.class));
    assertTrue(Serializable.class.isAssignableFrom(PSFormAction.class));
    assertTrue(Serializable.class.isAssignableFrom(PSUISet.class));
    assertTrue(Serializable.class.isAssignableFrom(PSValidationRules.class));
    assertTrue(Serializable.class.isAssignableFrom(PSInputTranslations.class));
    assertTrue(Serializable.class.isAssignableFrom(PSOutputTranslations.class));
    assertTrue(Serializable.class.isAssignableFrom(ArrayList.class));
  }

  @Test
  public void testUiDefinitionJavaSerializationRoundTrip() throws Exception {
    PSDisplayMapper mapper = new PSDisplayMapper("main");
    PSUISet uiSet = new PSUISet();
    mapper.add(new PSDisplayMapping("sys_title", uiSet));
    PSCollection defaultUI = new PSCollection(PSUISet.class);
    defaultUI.add(new PSUISet());
    PSUIDefinition def = new PSUIDefinition(mapper, defaultUI);

    PSUIDefinition ser = roundTrip(def);
    assertEquals(def, ser);
    assertEquals("main", ser.getDisplayMapper().getFieldSetRef());
    assertNotNull(ser.getDefaultUI());
    assertTrue(ser.getDefaultUI().hasNext());
  }

  @Test
  public void testDisplayMappingJavaSerializationRoundTrip() throws Exception {
    PSUISet uiSet = new PSUISet();
    PSDisplayMapping mapping = new PSDisplayMapping("body", uiSet);
    mapping.setDisplayMapper(new PSDisplayMapper("childSet"));

    PSDisplayMapping ser = roundTrip(mapping);
    assertEquals(mapping, ser);
    assertEquals("body", ser.getFieldRef());
    assertNotNull(ser.getDisplayMapper());
    assertEquals("childSet", ser.getDisplayMapper().getFieldSetRef());
  }

  @Test
  public void testCustomActionGroupJavaSerializationRoundTrip() throws Exception {
    PSParam param = new PSParam("pssessionid", new PSUserContext("/User/SessionId"));
    PSCollection parameters = new PSCollection(param.getClass());
    parameters.add(param);

    PSActionLink actionLink = new PSActionLink(new PSDisplayText("Go"));
    actionLink.setParameters(parameters);
    PSActionLinkList actions = new PSActionLinkList(actionLink);

    PSLocation location = new PSLocation(PSLocation.PAGE_SUMMARY_VIEW, PSLocation.TYPE_ROW);
    location.setFieldRefs(Collections.singletonList("testField").iterator());

    PSCustomActionGroup group = new PSCustomActionGroup(location, actions);
    PSCollection removeActions = new PSCollection(String.class);
    removeActions.add("remove1");
    removeActions.add("remove2");
    group.setRemoveActions(removeActions);

    PSCustomActionGroup ser = roundTrip(group);
    assertEquals(group, ser);

    Iterator removeIt = ser.getRemoveActions();
    assertTrue(removeIt.hasNext());
    assertEquals("remove1", removeIt.next());
    assertEquals("remove2", removeIt.next());
    assertFalse(removeIt.hasNext());

    assertTrue(ser.getActionLinkList().hasNext());
    assertEquals(PSLocation.PAGE_SUMMARY_VIEW, ser.getLocation().getPage());
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
