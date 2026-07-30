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
package com.percussion.xml.serialization.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.xml.serialization.PSObjectSerializer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * @author RammohanVangapalli
 */
@TestMethodOrder(MethodName.class)
public class PSObjectSerializerTest {

  public static class PersonList {
    private final List<Person> mi_people = new ArrayList<>();

    public PersonList() {
      //
    }

    public void addPerson(Person x) {
      mi_people.add(x);
    }

    public List<Person> getPersons() {
      return mi_people;
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
    PSXmlSerializationHelper.addType("person", Person.class);
    PSXmlSerializationHelper.addType("address", Address.class);
    PSXmlSerializationHelper.addType("book", Book.class);
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
   * Test case serializes a collection and restores it, then compares for equality
   *
   * @throws Exception error
   */
  @Test
  @Disabled
  public void test03Collections1() throws Exception {
    PSXmlSerializationHelper.addType("p-list", PersonList.class);
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

    PersonList deser = (PersonList) serializer.fromXmlString(ser);

    assertTrue(Arrays.equals(plist.getPersons().toArray(), deser.getPersons().toArray()));
  }
}
