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

package com.percussion.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSAttribute;
import com.percussion.design.objectstore.PSAttributeList;
import com.percussion.design.objectstore.PSSubject;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Behavioral tests for typed backend cataloger attribute/subject parsing (issue #2386 residual
 * security cataloger rawtypes batch after #2299 / PR #2387).
 *
 * <p>Also covers regression: empty Attribute shells from outer-join subject catalogs must not fail
 * findUsers (PSRoleMgr log spam: Attribute @name null).
 */
@Tag("UnitTest")
public class PSBackendCatalogerAttributesTest {

  @Test
  void getAttributesSkipsNamelessAttributeNodes() throws Exception {
    String xml =
        """
        <Subject name="Admin" type="1">
          <Attribute context="global"/>
          <Attribute name="sys_defaultCommunity">
            <Value>Default</Value>
          </Attribute>
        </Subject>
        """;
    Document doc =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));
    Element subject = doc.getDocumentElement();

    PSAttributeList attrs = PSBackendCataloger.getAttributes(subject);

    assertEquals(1, attrs.size());
    assertFalse(attrs.getAttribute("sys_defaultCommunity") == null);
  }

  @Test
  void getAttributesEmptyWhenOnlyNamelessNodes() throws Exception {
    String xml =
        """
        <Subject name="Admin" type="1">
          <Attribute context="global"/>
        </Subject>
        """;
    Document doc =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));

    PSAttributeList attrs = PSBackendCataloger.getAttributes(doc.getDocumentElement());
    assertEquals(0, attrs.size());
  }

  @Test
  void getValuesReturnsTypedStringList() throws Exception {
    String xml =
        """
        <Attribute name="sys_defaultCommunity">
          <Value>Default</Value>
          <Value>Engineering</Value>
        </Attribute>
        """;
    Document doc =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));

    List<String> values = PSBackendCataloger.getValues(doc.getDocumentElement());
    assertNotNull(values);
    assertEquals(List.of("Default", "Engineering"), values);
  }

  @Test
  void getValuesReturnsNullWhenNoValueNodes() throws Exception {
    String xml = """
        <Attribute name="sys_defaultCommunity"/>
        """;
    Document doc =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));

    assertNull(PSBackendCataloger.getValues(doc.getDocumentElement()));
  }

  @Test
  void processCatalogedSubjectsMergesDuplicateSubjectsAndAttributes() throws Exception {
    String xml =
        """
        <Subjects>
          <Subject name="Admin" type="1">
            <Attribute name="sys_defaultCommunity">
              <Value>Default</Value>
            </Attribute>
          </Subject>
          <Subject name="Admin" type="1">
            <Attribute name="sys_email">
              <Value>admin@example.com</Value>
            </Attribute>
          </Subject>
          <Subject name="Editor" type="1">
            <Attribute name="sys_defaultCommunity">
              <Value>Engineering</Value>
            </Attribute>
          </Subject>
        </Subjects>
        """;
    Document doc =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));

    Set<PSSubject> subjects =
        PSBackendCataloger.processCatalogedSubjects(doc.getDocumentElement(), false);

    assertEquals(2, subjects.size());

    PSSubject admin = findByName(subjects, "Admin");
    assertNotNull(admin);
    assertEquals(PSSubject.SUBJECT_TYPE_USER, admin.getType());
    PSAttribute community = admin.getAttributes().getAttribute("sys_defaultCommunity");
    assertNotNull(community);
    assertEquals("Default", community.getValues().get(0));
    PSAttribute email = admin.getAttributes().getAttribute("sys_email");
    assertNotNull(email);
    assertEquals("admin@example.com", email.getValues().get(0));

    PSSubject editor = findByName(subjects, "Editor");
    assertNotNull(editor);
    assertEquals(
        "Engineering",
        editor.getAttributes().getAttribute("sys_defaultCommunity").getValues().get(0));
  }

  @Test
  void processCatalogedSubjectsCanIncludeEmptyAttributeSubjects() throws Exception {
    String xml =
        """
        <Subjects>
          <Subject name="EmptyUser" type="1">
            <Attribute context="global"/>
          </Subject>
        </Subjects>
        """;
    Document doc =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));

    Set<PSSubject> withEmpty =
        PSBackendCataloger.processCatalogedSubjects(doc.getDocumentElement(), true);
    assertEquals(1, withEmpty.size());
    assertEquals(0, withEmpty.iterator().next().getAttributes().size());

    Set<PSSubject> withoutEmpty =
        PSBackendCataloger.processCatalogedSubjects(doc.getDocumentElement(), false);
    assertTrue(withoutEmpty.isEmpty());
  }

  @Test
  void processSubjectAttributesRejectsWrongRootElement() throws Exception {
    String xml = """
        <Roles/>
        """;
    Document doc =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));

    assertThrows(
        PSSecurityException.class,
        () -> PSBackendCataloger.processSubjectAttributes(doc, true));
  }

  @Test
  void processSubjectAttributesEmptyDocumentReturnsEmptyList() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    List<PSSubject> subjects = PSBackendCataloger.processSubjectAttributes(doc, true);
    assertNotNull(subjects);
    assertTrue(subjects.isEmpty());
  }

  private static PSSubject findByName(Set<PSSubject> subjects, String name) {
    for (PSSubject subject : subjects) {
      if (name.equals(subject.getName())) {
        return subject;
      }
    }
    return null;
  }
}
