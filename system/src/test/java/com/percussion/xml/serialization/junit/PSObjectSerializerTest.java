/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.xml.serialization.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.xml.serialization.PSObjectSerializer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * System-module coverage for {@link PSObjectSerializer} / {@link PSXmlSerializationHelper} after
 * the Jackson default cutover (issue #1893 / parent #1823 / epic #505).
 *
 * <p>Uses the Person fixture family in {@code modules/utils} ({@link Person}, {@link Address},
 * {@link Book}, {@link Name}). Behavioral round-trips are the gate; wire shape asserts document
 * approved Jackson deviations vs historical Betwixt.
 *
 * <p><strong>Approved XML deviations (no Betwixt graph identity):</strong>
 *
 * <ul>
 *   <li>Jackson does <em>not</em> emit Betwixt graph-identity {@code id="…"} attributes on complex
 *       elements (values live in child elements).
 *   <li>Unannotated collection items use the bean property name as the item element (e.g. nested
 *       {@code <books>} under a {@code <books>} wrapper) rather than Betwixt type-mapped names such
 *       as {@code <book>}. Domain slices (#1888+) add annotations where package wire must match
 *       legacy item tags.
 * </ul>
 *
 * @author RammohanVangapalli
 */
@TestMethodOrder(MethodName.class)
public class PSObjectSerializerTest {

  /**
   * Betwixt graph-identity attributes look like {@code id="0"} / {@code id="12"} on complex
   * elements. Jackson must not emit them (approved deviation #1887 / #1893).
   */
  private static final Pattern BETWIXT_GRAPH_ID_ATTR =
      Pattern.compile("\\sid\\s*=\\s*\"\\d+\"", Pattern.CASE_INSENSITIVE);

  public static class PersonList {
    private List<Person> mi_people = new ArrayList<>();

    public PersonList() {
      // required for serialization frameworks
    }

    public void addPerson(Person x) {
      mi_people.add(x);
    }

    public List<Person> getPersons() {
      return mi_people;
    }

    /**
     * Required for Jackson XML list binding (Betwixt historically used addPerson only). Without a
     * setter, write/read round-trips leave the list empty under the Jackson engine.
     *
     * @param people people to replace, may be {@code null} (treated as empty)
     */
    public void setPersons(List<Person> people) {
      this.mi_people = people == null ? new ArrayList<>() : new ArrayList<>(people);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      PersonList that = (PersonList) o;
      return mi_people.equals(that.mi_people);
    }

    @Override
    public int hashCode() {
      return Objects.hash(mi_people);
    }
  }

  /** Test object created in the ctor to be used by the tests, later. */
  private static Person person = null;

  /** Instance of the serializer to perform the testing of serialization and deserialization. */
  private static final PSObjectSerializer serializer = PSObjectSerializer.getInstance();

  /**
   * XML representation of {@link #person} produced by {@link #setUp()}. Pre-populated by
   * {@code @BeforeAll} so the round-trip test does not depend on test execution order or on {@link
   * #test01Serialization()} having populated this field. Reassigned by {@code
   * test01Serialization()} for that test's own assertion.
   */
  private static String serializedString;

  public PSObjectSerializerTest() {}

  /**
   * Initializes the {@link Person} under test and produces its canonical XML representation so
   * {@link #test02DeSerialization()} can deserialize from a known-good payload without depending on
   * test execution order.
   *
   * @throws Exception error
   */
  @BeforeAll
  public static void setUp() throws Exception {
    // Ensure default production engine (Jackson); residual Betwixt rollback is out of scope here.
    System.clearProperty(PSXmlSerializationHelper.ENGINE_PROPERTY);
    assertTrue(
        PSXmlSerializationHelper.isJacksonEngine(),
        "suite expects Jackson default after #1887 cutover");

    PSXmlSerializationHelper.addType("person", Person.class);
    PSXmlSerializationHelper.addType("address", Address.class);
    PSXmlSerializationHelper.addType("book", Book.class);
    PSXmlSerializationHelper.addType("name", Name.class);
    // Jackson root for PersonList uses mapped type name person-list (not legacy alias p-list).
    PSXmlSerializationHelper.addType("person-list", PersonList.class);
    PSXmlSerializationHelper.addType("p-list", PersonList.class);

    person = new Person("Rammohan", "Vangapalli");
    person.setAddress(new Address("10 Germano Way", "Andover", "MA", "01810"));
    person.addBook(new Book("Life without God1", "09052010"));
    person.addBook(new Book("Life without God2", "09052011"));
    person.addBook(new Book("Life without God3", "09052012"));
    person.addBook(new Book("Life without God4", "09052013"));
    serializedString = serializer.toXmlString(person);
  }

  /** Serialization test case. Verifies that the object created in the ctor serializes to XML. */
  @Test
  public void test01Serialization() throws Exception {
    String xml = serializer.toXmlString(person);
    assertTrue(xml.length() > 0);
    serializedString = xml;
  }

  /**
   * Test case de-serialization. Restores the object from the XML produced in {@link #setUp()} and
   * compares it with the one created directly.
   */
  @Test
  public void test02DeSerialization() throws Exception {
    Person personNew = (Person) serializer.fromXmlString(serializedString);
    assertEquals(person, personNew);
  }

  /**
   * Jackson wire shape for the Person fixture: modern root, nested beans, books collection, and no
   * Betwixt graph-identity {@code id} attributes.
   */
  @Test
  public void test01bJacksonWireShapeWithoutGraphIds() throws Exception {
    String xml = serializer.toXmlString(person);
    assertNotNull(xml);
    assertTrue(containsTag(xml, "person"), "root person: " + xml);
    assertTrue(containsTag(xml, "name"), xml);
    assertTrue(containsTag(xml, "address"), xml);
    assertTrue(containsTag(xml, "street"), xml);
    assertTrue(containsTag(xml, "books"), xml);
    assertTrue(xml.contains("Rammohan"), xml);
    assertTrue(xml.contains("Andover"), xml);
    assertTrue(xml.contains("Life without God1"), xml);
    assertFalse(
        BETWIXT_GRAPH_ID_ATTR.matcher(xml).find(),
        "Jackson must not emit Betwixt graph-identity id attributes: " + xml);
    assertFalse(xml.trim().startsWith("<null"), "modern write must not use legacy null root");
  }

  /**
   * Round-trip a collection root. Registers both the Jackson-mapped root {@code person-list} and
   * the historical Betwixt alias {@code p-list} for polymorphic lookup; write uses the mapped type
   * name.
   *
   * @throws Exception error
   */
  @Test
  public void test03Collections1() throws Exception {
    PersonList plist = new PersonList();

    Person a = new Person();
    Person b = new Person();
    Person c = new Person();

    a.setName(new Name("John", "Doe"));
    b.setName(new Name("Sally", "Fields"));
    c.setName(new Name("Jacob", "Marley"));

    plist.addPerson(a);
    plist.addPerson(b);
    plist.addPerson(c);

    String ser = serializer.toXmlString(plist);
    assertTrue(containsTag(ser, "person-list"), "Jackson root person-list: " + ser);
    assertFalse(
        BETWIXT_GRAPH_ID_ATTR.matcher(ser).find(),
        "no Betwixt graph id attrs on collection write: " + ser);

    PersonList deser = (PersonList) serializer.fromXmlString(ser);

    assertEquals(3, deser.getPersons().size(), "restored people: " + ser);
    assertTrue(
        Arrays.equals(plist.getPersons().toArray(), deser.getPersons().toArray()),
        "person list round-trip under Jackson");
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }
}
