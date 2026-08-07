/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.delivery.metadata;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.delivery.metadata.IPSMetadataProperty.VALUETYPE;
import com.percussion.delivery.metadata.extractor.data.PSMetadataEntry;
import com.percussion.delivery.metadata.extractor.data.PSMetadataProperty;
import com.percussion.delivery.metadata.rdbms.impl.PSDbMetadataEntry;
import com.percussion.delivery.metadata.rdbms.impl.PSDbMetadataProperty;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author erikserating
 */
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-beans.xml"})
public class PSMetadataIndexerServiceTest {
  @Autowired public IPSMetadataIndexerService service;

  /** Entries that will be created in performance tests. */
  private static final int ENTRIES_COUNT_FOR_PERFORMANCE_TESTING = 7;

  @BeforeEach
  public void setUp() throws Exception {
    service.deleteAllMetadataEntries();
  }

  private List<IPSMetadataEntry> createEntries() {
    return createEntries(ENTRIES_COUNT_FOR_PERFORMANCE_TESTING);
  }

  private List<IPSMetadataEntry> createEntries(int count) {
    List<IPSMetadataEntry> entries = new ArrayList<IPSMetadataEntry>();
    service.deleteAllMetadataEntries();
    for (int i = 0; i < count; i++) {
      String pagepath = "/testsite/folder1/child1/foo.html" + i;

      PSDbMetadataEntry entry =
          new PSDbMetadataEntry(
              "foo.html" + i, "/folder1/child1/", pagepath, "TestType", "testsite");
      entry.setLinktext("the linktext value");

      entry.clearProperties();
      entry.addProperty(new PSDbMetadataProperty("prop1", "foo1"));
      entry.addProperty(new PSDbMetadataProperty("prop2", 4));

      entries.add(entry);
    }

    return entries;
  }

  @Test
  public void testInsertMultiplePerformance() throws Exception {
    // Create entries
    List<IPSMetadataEntry> entries = createEntries();

    // Insert entries
    System.out.println("Saving...");

    Calendar before = Calendar.getInstance();
    service.save(entries);
    Calendar after = Calendar.getInstance();

    List<IPSMetadataEntry> allSavedEntries = service.getAllEntries();
    Map<String, List<IPSMetadataProperty>> props;
    assertEquals(ENTRIES_COUNT_FOR_PERFORMANCE_TESTING, allSavedEntries.size());

    for (IPSMetadataEntry e : allSavedEntries) {
      assertTrue(e.getPagepath().startsWith("/testsite/folder1/child1/foo.html"), "Pagepath");
      assertEquals("the linktext value", e.getLinktext(), "Linktext");

      props = toPropsMap(e.getProperties());
      assertEquals(2, props.size(), "Properties count");

      assertTrue(props.containsKey("prop1"), "prop1 exists");
      assertEquals(VALUETYPE.STRING, props.get("prop1").get(0).getValuetype(), "prop1 value type");
      assertEquals("foo1", props.get("prop1").get(0).getStringvalue(), "prop1 value");

      assertTrue(props.containsKey("prop2"), "prop2 exists");
      assertEquals(VALUETYPE.NUMBER, props.get("prop2").get(0).getValuetype(), "prop2 value type");
      assertEquals(Double.valueOf(4), props.get("prop2").get(0).getNumbervalue(), "prop2 value");
    }

    System.out.println();
    System.out.print(
        "Insertion took: "
            + ((after.getTimeInMillis() - before.getTimeInMillis()) / 1000)
            + " seconds");
    System.out.println();
  }

  @Test
  public void testUpdateMultiplePerformance() throws Exception {
    // Create entries
    List<IPSMetadataEntry> entries = createEntries();

    // Insert entries
    System.out.println("Saving new entries");
    service.save(entries);

    Map<String, List<IPSMetadataProperty>> props;

    // Create the same entries (with the same pagepath), modify any property
    // and save them
    entries = createEntries();

    for (IPSMetadataEntry e : entries) {
      e.setLinktext("New value for linktext");

      e.addProperty(new PSDbMetadataProperty("prop3", Date.valueOf("2011-02-28")));
    }

    System.out.println("Updating entries");

    Calendar before = Calendar.getInstance();
    service.save(entries);
    Calendar after = Calendar.getInstance();

    List<IPSMetadataEntry> allSavedEntries = service.getAllEntries();
    assertEquals(ENTRIES_COUNT_FOR_PERFORMANCE_TESTING, allSavedEntries.size());

    for (IPSMetadataEntry e : allSavedEntries) {
      assertTrue(e.getPagepath().startsWith("/testsite/folder1/child1/foo.html"), "Pagepath");
      assertEquals("New value for linktext", e.getLinktext(), "Linktext");

      props = toPropsMap(e.getProperties());
      assertEquals(3, props.size(), "Properties count");

      assertTrue(props.containsKey("prop1"), "prop1 exists");
      assertEquals(VALUETYPE.STRING, props.get("prop1").get(0).getValuetype(), "prop1 value type");
      assertEquals("foo1", props.get("prop1").get(0).getStringvalue(), "prop1 value");

      assertTrue(props.containsKey("prop2"), "prop2 exists");
      assertEquals(VALUETYPE.NUMBER, props.get("prop2").get(0).getValuetype(), "prop2 value type");
      assertEquals(Double.valueOf(4), props.get("prop2").get(0).getNumbervalue(), "prop2 value");

      assertTrue(props.containsKey("prop3"), "prop3 exists");
      assertEquals(VALUETYPE.DATE, props.get("prop3").get(0).getValuetype(), "prop3 value type");
      assertEquals(Date.valueOf("2011-02-28"), props.get("prop3").get(0).getDatevalue(), "prop3 value");
    }

    System.out.println();
    System.out.print(
        "Update took: "
            + ((after.getTimeInMillis() - before.getTimeInMillis()) / 1000)
            + " seconds");
    System.out.println();
  }

  @Test
  public void testDeleteMultiplePerformance() throws Exception {
    // Create entries
    List<IPSMetadataEntry> entries = createEntries();

    // Insert entries
    service.save(entries);

    List<IPSMetadataEntry> allSavedEntries = service.getAllEntries();

    Collection<String> entriesToDelete = new ArrayList<String>();
    for (IPSMetadataEntry e : allSavedEntries) entriesToDelete.add(e.getPagepath());

    assertEquals(ENTRIES_COUNT_FOR_PERFORMANCE_TESTING, entriesToDelete.size());

    Calendar before = Calendar.getInstance();
    service.delete(entriesToDelete);
    Calendar after = Calendar.getInstance();

    assertEquals(0, service.getAllEntries().size());

    System.out.println();
    System.out.print(
        "Deletion took: "
            + ((after.getTimeInMillis() - before.getTimeInMillis()) / 1000)
            + " seconds");
    System.out.println();
  }

  @Test
  public void testUpdateMultiple_RemoveAllProperties() throws Exception {
    int entriesCount = 10;

    // Create entries
    List<IPSMetadataEntry> entries = createEntries(entriesCount);
    service.save(entries);
    entries = service.getAllEntries();

    Map<String, List<IPSMetadataProperty>> props;

    // Create the same entries (with the same pagepath), modify any property and save them
    entries = createEntries(entriesCount);

    for (IPSMetadataEntry e : entries) {
      e.setLinktext("New value for linktext");

      e.clearProperties();
    }

    service.save(entries);

    // Assert
    List<IPSMetadataEntry> allSavedEntries = service.getAllEntries();
    assertEquals(entriesCount, allSavedEntries.size());

    for (IPSMetadataEntry e : allSavedEntries) {
      assertTrue(e.getPagepath().startsWith("/testsite/folder1/child1/foo.html"), "Pagepath");
      assertEquals("New value for linktext", e.getLinktext(), "Linktext");

      props = toPropsMap(e.getProperties());
      assertEquals(0, props.size(), "Properties count");
    }
  }

  @Test
  public void testUpdateMultiple_ChangeExistingProperty() throws Exception {
    int entriesCount = 10;

    // Create entries
    List<IPSMetadataEntry> entries = createEntries(entriesCount);
    service.save(entries);

    Map<String, List<IPSMetadataProperty>> props;

    // Create the same entries (with the same pagepath), modify any property and save them
    entries = createEntries(entriesCount);

    for (IPSMetadataEntry e : entries) {
      e.setLinktext("New value for linktext");

      for (IPSMetadataProperty prop : e.getProperties()) {
        if (prop.getName().equals("prop1")) prop.setStringvalue(prop.getStringvalue() + "changed");
        else prop.setNumbervalue(10.0);
      }
    }

    service.save(entries);

    // Assert
    List<IPSMetadataEntry> allSavedEntries = service.getAllEntries();
    assertEquals(entriesCount, allSavedEntries.size(), "Entries count");

    for (IPSMetadataEntry e : allSavedEntries) {
      assertTrue(e.getPagepath().startsWith("/testsite/folder1/child1/foo.html"), "Pagepath");
      assertEquals("New value for linktext", e.getLinktext(), "Linktext");

      props = toPropsMap(e.getProperties());
      assertEquals(2, props.size(), "Properties count");

      assertTrue(props.containsKey("prop1"), "prop1 exists");
      assertEquals(VALUETYPE.STRING, props.get("prop1").get(0).getValuetype(), "prop1 value type");
      assertEquals("foo1changed", props.get("prop1").get(0).getStringvalue(), "prop1 value");

      assertTrue(props.containsKey("prop2"), "prop2 exists");
      assertEquals(VALUETYPE.NUMBER, props.get("prop2").get(0).getValuetype(), "prop2 value type");
      assertEquals(Double.valueOf(10), props.get("prop2").get(0).getNumbervalue(), "prop2 value");
    }
  }

  @Test
  public void testUpdateWith_RemoveExistingProperty() throws Exception {
    int entriesCount = 1;

    // Create entries
    List<IPSMetadataEntry> entries = createEntries(entriesCount);
    service.save(entries);
    List<IPSMetadataEntry> allSavedEntries = service.getAllEntries();
    assertEquals(entriesCount, allSavedEntries.size(), "Entries count");

    Map<String, List<IPSMetadataProperty>> props;

    for (IPSMetadataEntry e : allSavedEntries) {
      e.setLinktext("New value for linktext");

      Set<IPSMetadataProperty> newProps = new HashSet<>();
      for (IPSMetadataProperty prop : e.getProperties()) {
        // Don't add prop1 again, just add prop2 with changed value
        if (prop.getName().equals("prop1")) {
        } else {
          prop.setNumbervalue(10.0);
          newProps.add(prop);
        }
      }
      // Setting new list of properties
      e.setProperties(newProps);
    }

    service.save(entries);

    // Assert

    assertEquals(entriesCount, allSavedEntries.size(), "Entries count");

    for (IPSMetadataEntry e : allSavedEntries) {

      assertTrue(e.getPagepath().startsWith("/testsite/folder1/child1/foo.html"), "Pagepath");
      assertEquals("New value for linktext", e.getLinktext(), "Linktext");

      // Removed property should not be returned
      Set<IPSMetadataProperty> propsN = e.getProperties();
      assertEquals(1, propsN.size(), "Properties count");
    }
  }

  @Test
  public void testSave_MultipleValueProperties() throws Exception {
    // Insert entry
    String pagepath = "/testsite/folder1/child1/foo.html";
    IPSMetadataEntry entry =
        new PSDbMetadataEntry("foo.html", "/folder1/child1", pagepath, "TestType", "testsite");
    PSDbMetadataProperty prop1 = new PSDbMetadataProperty("prop1", "foo1");
    entry.addProperty(prop1);
    PSDbMetadataProperty prop2 = new PSDbMetadataProperty("prop2", 4);
    entry.addProperty(prop2);
    // prop1 == prop3
    PSDbMetadataProperty prop3 = new PSDbMetadataProperty("prop3", "foo2");
    entry.addProperty(prop3);

    service.save(entry);

    // find entry
    entry = service.findEntry(pagepath);
    assertNotNull(entry);
    assertNotNull(entry.getProperties());
    assertEquals(3, entry.getProperties().size());

    Map<String, List<IPSMetadataProperty>> propsMap = toPropsMap(entry.getProperties());
    assertEquals(1, propsMap.get("prop1").size(), "prop1 count");

    List<String> expectedProp1Values = new ArrayList<String>();
    expectedProp1Values.add("foo1");
    expectedProp1Values.add("foo2");

    for (IPSMetadataProperty pro : propsMap.get("prop1")) {
      assertTrue(expectedProp1Values.contains(pro.getStringvalue()), "expected prop1 value");
      expectedProp1Values.remove(pro.getStringvalue());
    }

    assertEquals(
        Double.valueOf(4), propsMap.get("prop2").get(0).getNumbervalue(), "prop2 expected value");

    // Update
    entry.setName("TestEntry1_Modified");
    entry.clearProperties();
    entry.addProperty(prop1);
    entry.addProperty(new PSDbMetadataProperty("prop2", 66));
    entry.addProperty(new PSDbMetadataProperty("prop3", 77));

    service.save(entry);

    IPSMetadataEntry updatedEntry = service.findEntry(pagepath);
    assertNotNull(updatedEntry);
    assertNotNull(updatedEntry.getProperties());
    assertEquals(3, updatedEntry.getProperties().size());

    propsMap = toPropsMap(updatedEntry.getProperties());
    assertEquals("TestEntry1_Modified", updatedEntry.getName());
    assertEquals(Double.valueOf(66), propsMap.get("prop2").get(0).getNumbervalue());
    assertEquals("foo1", propsMap.get("prop1").get(0).getStringvalue());
    assertEquals(Double.valueOf(77), propsMap.get("prop3").get(0).getNumbervalue());

    // Delete
    service.delete(pagepath);
    assertNull(service.findEntry(pagepath));
  }

  @Test
  public void testSave_BigTextValue() throws Exception {
    // Insert entry
    String pagepath = "/testsite/folder1/child1/foo.html";
    IPSMetadataEntry entry =
        new PSDbMetadataEntry("foo.html", "/folder1/child1", pagepath, "TestType", "testsite");
    entry.setLinktext("link text 1");

    // Very big string
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < 10000; i++) sb.append("a");

    String stringValue = new String(sb.toString());

    PSDbMetadataProperty prop1 = new PSDbMetadataProperty("prop1", VALUETYPE.TEXT, sb.toString());
    entry.addProperty(prop1);

    service.save(entry);

    IPSMetadataEntry dbEntry = service.findEntry(pagepath);
    assertNotNull(dbEntry);
    assertEquals("foo.html", dbEntry.getName(), "dbEntry - name");
    assertEquals("/folder1/child1", dbEntry.getFolder(), "dbEntry - folder");
    assertEquals("TestType", dbEntry.getType(), "dbEntry - type");
    assertEquals("testsite", dbEntry.getSite(), "dbEntry - type");
    assertEquals("link text 1", dbEntry.getLinktext(), "dbEntry - linktext");

    assertNotNull(dbEntry.getProperties(), "dbEntry - properties");
    Map<String, List<IPSMetadataProperty>> props = toPropsMap(dbEntry.getProperties());
    assertEquals(1, dbEntry.getProperties().size(), "dbEntry - properties size");

    assertTrue(props.containsKey("prop1"), "prop1 exists");
    assertEquals(VALUETYPE.TEXT, props.get("prop1").get(0).getValuetype(), "prop1 value type");
    assertEquals(sb.toString(), props.get("prop1").get(0).getStringvalue(), "prop1 value");
  }

  @Test
  public void testSaveDeleteSingle() throws Exception {
    // Insert entry
    String pagepath = "/testsite/folder1/child1/foo.html";
    IPSMetadataEntry entry =
        new PSDbMetadataEntry("foo.html", "/folder1/child1", pagepath, "TestType", "testsite");
    PSDbMetadataProperty prop1 = new PSDbMetadataProperty("prop1", "foo1");
    entry.addProperty(prop1);
    PSDbMetadataProperty prop2 = new PSDbMetadataProperty("prop2", 4);
    entry.addProperty(prop2);

    service.save(entry);

    // find entry
    entry = service.findEntry(pagepath);
    assertNotNull(entry);
    assertNotNull(entry.getProperties());
    assertEquals(2, entry.getProperties().size());

    // Update
    entry.setName("TestEntry1_Modified");
    entry.clearProperties();
    entry.addProperty(prop1);
    entry.addProperty(new PSDbMetadataProperty("prop2", 66));
    entry.addProperty(new PSDbMetadataProperty("prop3", 77));

    service.save(entry);

    // Delete
    IPSMetadataEntry updatedEntry = service.findEntry(pagepath);
    assertNotNull(updatedEntry);
    assertNotNull(updatedEntry.getProperties());
    assertEquals(3, updatedEntry.getProperties().size());

    Map<String, List<IPSMetadataProperty>> propsMap = toPropsMap(updatedEntry.getProperties());
    assertEquals("TestEntry1_Modified", updatedEntry.getName(), "Entry name");
    assertEquals(Double.valueOf(66), propsMap.get("prop2").get(0).getNumbervalue(), "prop2 value");
    assertEquals("foo1", propsMap.get("prop1").get(0).getStringvalue(), "prop1 value");
    assertEquals(Double.valueOf(77), propsMap.get("prop3").get(0).getNumbervalue(), "prop3 value");

    // Delete
    service.delete(pagepath);
    assertNull(service.findEntry(pagepath)); // Verify entry no longer
    // exists
  }

  @Test
  public void testSaveDeleteMultiple_WithDatabaseEntities() throws Exception {
    Collection<IPSMetadataEntry> entries = new ArrayList<IPSMetadataEntry>();
    String pagepath = "/testsite/folder1/child1/foo.html";
    PSDbMetadataEntry entry =
        new PSDbMetadataEntry("foo.html", "/folder1/child1", pagepath, "TestType", "testsite");
    PSDbMetadataProperty prop1 = new PSDbMetadataProperty("prop1", "foo1");
    entry.addProperty(prop1);
    PSDbMetadataProperty prop2 = new PSDbMetadataProperty("prop2", 4);
    entry.addProperty(prop2);
    entries.add(entry);

    String pagepath2 = "/testsite/folder1/child1/foo2.html";
    PSDbMetadataEntry entry2 =
        new PSDbMetadataEntry("foo2.html", "/folder1/child1", pagepath2, "TestType", "testsite");
    PSDbMetadataProperty prop3 = new PSDbMetadataProperty("prop3", "foo12");
    entry.addProperty(prop3);
    PSDbMetadataProperty prop4 = new PSDbMetadataProperty("prop4", 4);
    entry.addProperty(prop4);
    entries.add(entry2);

    // Insert entries
    service.save(entries);

    assertNotNull(service.findEntry(pagepath));
    assertNotNull(service.findEntry(pagepath2));

    // Delete
    Collection<String> deleteList = new ArrayList<String>();
    deleteList.add(pagepath);
    deleteList.add(pagepath2);
    service.delete(deleteList);
    assertNull(service.findEntry(pagepath));
    assertNull(service.findEntry(pagepath2));
  }

  @Test
  public void testSaveDeleteMultiple_PSMetadataEntries() throws Exception {
    Collection<IPSMetadataEntry> entries = new ArrayList<IPSMetadataEntry>();

    String pagepath = "/testsite/folder1/child1/foo.html";
    PSMetadataEntry entry =
        new PSMetadataEntry("foo.html", "/folder1/child1", pagepath, "TestType", "testsite1");
    entry.setLinktext("link text 1");
    PSMetadataProperty prop1 = new PSMetadataProperty("prop1", "foo1");
    entry.getProperties().add(prop1);
    PSMetadataProperty prop2 = new PSMetadataProperty("prop2", 4);
    entry.getProperties().add(prop2);
    entries.add(entry);

    String pagepath2 = "/testsite/folder1/child1/foo2.html";
    PSMetadataEntry entry2 =
        new PSMetadataEntry("foo2.html", "/folder1/child1", pagepath2, "TestType", "testsite2");
    entry2.setLinktext("link text 2");
    PSMetadataProperty prop3 = new PSMetadataProperty("prop3", "foo12");
    entry2.getProperties().add(prop3);
    PSMetadataProperty prop4 = new PSMetadataProperty("prop4", 5);
    entry2.getProperties().add(prop4);
    entries.add(entry2);

    // Insert entries
    service.save(entries);

    // Check entry1
    PSDbMetadataEntry dbEntry = (PSDbMetadataEntry) service.findEntry(pagepath);
    assertNotNull(dbEntry);
    assertEquals("foo.html", dbEntry.getName(), "dbEntry - name");
    assertEquals("/folder1/child1", dbEntry.getFolder(), "dbEntry - folder");
    assertEquals("TestType", dbEntry.getType(), "dbEntry - type");
    assertEquals("testsite1", dbEntry.getSite(), "dbEntry - type");
    assertEquals("link text 1", dbEntry.getLinktext(), "dbEntry - linktext");

    assertNotNull(dbEntry.getProperties(), "dbEntry - properties");
    Map<String, List<IPSMetadataProperty>> props = toPropsMap(dbEntry.getProperties());
    assertEquals(2, dbEntry.getProperties().size(), "dbEntry - properties size");

    assertTrue(props.containsKey("prop1"), "prop1 exists");
    assertEquals(VALUETYPE.STRING, props.get("prop1").get(0).getValuetype(), "prop1 value type");
    assertEquals("foo1", props.get("prop1").get(0).getStringvalue(), "prop1 value");

    assertTrue(props.containsKey("prop2"), "prop2 exists");
    assertEquals(VALUETYPE.NUMBER, props.get("prop2").get(0).getValuetype(), "prop2 value type");
    assertEquals(4.0, props.get("prop2").get(0).getNumbervalue(), "prop2 value");

    // Check entry2
    dbEntry = (PSDbMetadataEntry) service.findEntry(pagepath2);
    assertNotNull(dbEntry);
    assertEquals("foo2.html", dbEntry.getName(), "dbEntry - name");
    assertEquals("/folder1/child1", dbEntry.getFolder(), "dbEntry - folder");
    assertEquals("TestType", dbEntry.getType(), "dbEntry - type");
    assertEquals("testsite2", dbEntry.getSite(), "dbEntry - type");
    assertEquals("link text 2", dbEntry.getLinktext(), "dbEntry - linktext");

    assertNotNull(dbEntry.getProperties(), "dbEntry - properties");
    props = toPropsMap(dbEntry.getProperties());
    assertEquals(2, dbEntry.getProperties().size(), "dbEntry - properties size");

    assertTrue(props.containsKey("prop3"), "prop3 exists");
    assertEquals(VALUETYPE.STRING, props.get("prop3").get(0).getValuetype(), "prop3 value type");
    assertEquals("foo12", props.get("prop3").get(0).getStringvalue(), "prop3 value");

    assertTrue(props.containsKey("prop4"), "prop4 exists");
    assertEquals(VALUETYPE.NUMBER, props.get("prop4").get(0).getValuetype(), "prop4 value type");
    assertEquals(5.0, props.get("prop4").get(0).getNumbervalue(), "prop4 value");

    // Delete
    Collection<String> deleteList = new ArrayList<String>();
    deleteList.add(pagepath);
    deleteList.add(pagepath2);
    service.delete(deleteList);
    assertNull(service.findEntry(pagepath));
    assertNull(service.findEntry(pagepath2));
  }

  @Test
  public void testDeleteEntriesWithProperties_ShouldDeletePropertiesAsWell() throws Exception {
    Collection<IPSMetadataEntry> entries = new ArrayList<IPSMetadataEntry>();

    // entry
    String pagepath = "/testsite/folder1/child1/foo.html";
    IPSMetadataEntry entry = new PSMetadataEntry();
    entry.setName("foo.html");
    entry.setFolder("/folder1/child1");
    entry.setPagepath(pagepath);
    entry.setType("TestType");
    entry.setSite("testsite1");
    entry.setLinktext("link text 1");

    IPSMetadataProperty prop1 = new PSMetadataProperty("prop1", VALUETYPE.STRING, "foo1");
    entry.getProperties().add(prop1);

    PSMetadataProperty prop2 = new PSMetadataProperty("prop2", 4.0);
    entry.getProperties().add(prop2);

    entries.add(entry);

    // entry2
    String pagepath2 = "/testsite/folder1/child1/foo2.html";

    PSMetadataEntry entry2 = new PSMetadataEntry();
    entry2.setName("foo2.html");
    entry2.setFolder("/folder1/child1");
    entry2.setPagepath(pagepath2);
    entry2.setType("TestType");
    entry2.setSite("testsite2");
    entry2.setLinktext("link text 2");

    PSMetadataProperty prop3 = new PSMetadataProperty("prop3", VALUETYPE.STRING, "foo12");
    entry2.getProperties().add(prop3);

    PSMetadataProperty prop4 = new PSMetadataProperty("prop4", 5.0);
    entry2.getProperties().add(prop4);

    entries.add(entry2);

    // Insert entries
    service.save(entries);

    // Delete entry1 and make sure that the only properties left are
    // the ones for the second entry

    List<IPSMetadataProperty> allProperties = getAllProperties();

    assertNotNull(allProperties, "properties not null");
    assertEquals(4, allProperties.size(), "count of properties before deleting entry");

    service.delete(entry.getPagepath());

    allProperties = getAllProperties();

    assertNotNull(allProperties, "properties not null");
    assertEquals(2, allProperties.size(), "count of properties before deleting entry");

    for (IPSMetadataProperty prop : allProperties)
      assertEquals(
          ((PSDbMetadataProperty) prop).getMetadataEntry().getPagepath(),
          entry2.getPagepath(),
          "entry of property");
  }

  private List<IPSMetadataProperty> getAllProperties() {
    List<IPSMetadataProperty> allProperties = new ArrayList<>();
    List<IPSMetadataEntry> allEntries = service.getAllEntries();

    for (IPSMetadataEntry en : allEntries) allProperties.addAll(en.getProperties());

    return allProperties;
  }

  @Test
  public void testGetAllIndexedDirectories() throws Exception {
    Collection<IPSMetadataEntry> entries = new ArrayList<IPSMetadataEntry>();
    String pagepath = "/testsite/folder1/child1/foo.html";
    IPSMetadataEntry entry =
        new PSDbMetadataEntry("foo.html", "/folder1/child1", pagepath, "TestType", "testsite");
    PSDbMetadataProperty prop1 = new PSDbMetadataProperty("prop1", "foo1");
    PSDbMetadataProperty prop2 = new PSDbMetadataProperty("prop2", 4);
    Set<IPSMetadataProperty> props = new HashSet<IPSMetadataProperty>();
    props.add(prop1);
    props.add(prop2);
    entry.setProperties(props);
    entries.add(entry);

    String pagepath2 = "/testsite2/folder2/child2/foo2.html";
    PSDbMetadataEntry entry2 =
        new PSDbMetadataEntry("foo2.html", "/folder2/child2", pagepath2, "TestType", "testsite2");
    PSDbMetadataProperty prop3 = new PSDbMetadataProperty("prop3", "foo12");
    PSDbMetadataProperty prop4 = new PSDbMetadataProperty("prop4", 4);
    Set<IPSMetadataProperty> props2 = new HashSet<IPSMetadataProperty>();
    props2.add(prop3);
    props2.add(prop4);
    entry.setProperties(props2);
    entries.add(entry2);

    String pagepath3 = "/testsite2/folder2/foo2.html";
    PSDbMetadataEntry entry3 =
        new PSDbMetadataEntry("foo2.html", "/folder2", pagepath3, "TestType", "testsite2");
    PSDbMetadataProperty prop5 = new PSDbMetadataProperty("prop3", "foo12");
    PSDbMetadataProperty prop6 = new PSDbMetadataProperty("prop4", 4);
    Set<IPSMetadataProperty> props3 = new HashSet<IPSMetadataProperty>();
    props2.add(prop5);
    props2.add(prop6);
    entry.setProperties(props3);
    entries.add(entry3);

    String pagepath4 = "/testsite2/folder2/foo3.html";
    PSDbMetadataEntry entry4 =
        new PSDbMetadataEntry("foo2.html", "/folder2", pagepath4, "TestType", "testsite2");
    PSDbMetadataProperty prop7 = new PSDbMetadataProperty("prop3", "foo12");
    PSDbMetadataProperty prop8 = new PSDbMetadataProperty("prop4", 4);
    Set<IPSMetadataProperty> props4 = new HashSet<IPSMetadataProperty>();
    props2.add(prop7);
    props2.add(prop8);
    entry.setProperties(props4);
    entries.add(entry4);

    // Insert entries
    service.save(entries);
    assertNotNull(service.findEntry(pagepath));
    assertNotNull(service.findEntry(pagepath2));

    // Get all indexed directories
    Set<String> indexedDirectories = service.getAllIndexedDirectories();
    assertEquals(3, indexedDirectories.size(), "Directories count");
    assertTrue(indexedDirectories.contains("/testsite/folder1/child1"), "Directory name - 1");
    assertTrue(indexedDirectories.contains("/testsite2/folder2/child2"), "Directory name - 2");
    assertTrue(indexedDirectories.contains("/testsite2/folder2"), "Directory name - 3");
  }

  private Map<String, List<IPSMetadataProperty>> toPropsMap(Set<IPSMetadataProperty> props) {
    Map<String, List<IPSMetadataProperty>> results =
        new HashMap<String, List<IPSMetadataProperty>>();
    List<IPSMetadataProperty> list;

    for (IPSMetadataProperty p : props) {
      if (results.containsKey(p.getName())) list = results.get(p.getName());
      else {
        list = new ArrayList<IPSMetadataProperty>();
        results.put(p.getName(), list);
      }

      list.add(p);
    }

    return results;
  }
}
