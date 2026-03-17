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
package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Authentication object store class testing, including constructors, <code>PSComponent</code>
 * functionality, accessors and XML functionality.
 */
public class PSAuthenticationTest {

  @TempDir public Path temporaryFolder;

  private String rxdeploydir;

  @BeforeEach
  public void setup() {
    rxdeploydir = System.getProperty("rxdeploydir");
    System.setProperty("rxdeploydir", temporaryFolder.toAbsolutePath().toString());
  }

  @AfterEach
  public void teardown() {
    // Reset the deploy dir property if it was set prior to test
    if (rxdeploydir != null) System.setProperty("rxdeploydir", rxdeploydir);
  }

  public PSAuthenticationTest() {}

  /**
   * Test component constructors and accessors.
   *
   * @throws Exception if any exceptions occur or assertions fail.
   */
  @Test
  public void testComponent() throws Exception {
    PSAuthentication auth1 =
        new PSAuthentication(
            "auth1", PSAuthentication.SCHEME_SIMPLE, "user", "userAttr", "pw", "filter");

    PSAuthentication auth2 =
        new PSAuthentication(
            "auth2", PSAuthentication.SCHEME_SIMPLE, "user", "userAttr", "pw", "filter");

    PSAuthentication auth2_1 =
        new PSAuthentication(
            "auth2", PSAuthentication.SCHEME_NONE, "user", "userAttr", "pw", "filter");

    PSAuthentication auth2_2 =
        new PSAuthentication(
            "auth2", PSAuthentication.SCHEME_SIMPLE, "user1", "userAttr", "pw", "filter");

    PSAuthentication auth2_3 =
        new PSAuthentication(
            "auth2", PSAuthentication.SCHEME_SIMPLE, "user", "userAttr1", "pw", "filter");

    PSAuthentication auth2_4 =
        new PSAuthentication(
            "auth2", PSAuthentication.SCHEME_SIMPLE, "user", "userAttr", "pw1", "filter");

    PSAuthentication auth2_5 =
        new PSAuthentication(
            "auth2", PSAuthentication.SCHEME_SIMPLE, "user", "userAttr", "pw", "filter1");

    // testing compare/equals
    assertTrue(PSComponent.compare(auth1, auth1));
    assertFalse(PSComponent.compare(auth1, auth2));
    assertFalse(PSComponent.compare(auth2, auth2_1));
    assertFalse(PSComponent.compare(auth2, auth2_2));
    assertFalse(PSComponent.compare(auth2, auth2_3));
    assertTrue(PSComponent.compare(auth2, auth2_4));
    assertFalse(PSComponent.compare(auth2, auth2_5));

    // testing clone/copyFrom
    PSAuthentication clone = (PSAuthentication) auth1.clone();
    assertEquals(auth1, clone);
    auth2.copyFrom(auth1);
    assertEquals(auth1, auth2);

    // test append base dn
    clone.setAppendBaseDn(true);
    assertNotEquals(auth1, clone);

    // testing name accessors
    assertThrows(IllegalArgumentException.class, () -> auth1.setName(null));
    assertThrows(IllegalArgumentException.class, () -> auth1.setName(" "));

    // testing scheme accessors
    assertThrows(IllegalArgumentException.class, () -> auth1.setScheme(null));
    assertThrows(IllegalArgumentException.class, () -> auth1.setScheme(" "));
    assertThrows(IllegalArgumentException.class, () -> auth1.setScheme("not suported"));

    // testing user accessors
    assertDoesNotThrow(() -> auth1.setUser(null));
    assertDoesNotThrow(() -> auth1.setUser(" "));

    // testing user attribute accessors
    assertDoesNotThrow(() -> auth1.setUserAttr(null));
    assertDoesNotThrow(() -> auth1.setUserAttr(" "));

    // testing password accessors
    assertDoesNotThrow(() -> auth1.setPassword(null));
    assertEquals("", auth1.getPassword());

    assertDoesNotThrow(() -> auth1.setPassword(" "));

    // testing filter extension accessors
    assertDoesNotThrow(() -> auth1.setFilterExtension(null));
    assertEquals("", auth1.getFilterExtension());

    assertDoesNotThrow(() -> auth1.setFilterExtension(" "));
  }

  /**
   * Test to and from xml methods of this os object.
   *
   * @throws Exception If any exceptions occur or assertions fail.
   */
  @Test
  public void testXml() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();

    // test with optional elements defined
    PSAuthentication auth1 =
        new PSAuthentication(
            "auth1", PSAuthentication.SCHEME_SIMPLE, "user", "userAttr", "pw", "filter");
    System.out.println("authentication 1:\n" + PSXmlDocumentBuilder.toString(auth1.toXml(doc)));

    Element auth1Elem = auth1.toXml(doc);

    PSAuthentication auth2 = new PSAuthentication(auth1Elem, null, null, true);
    System.out.println("authentication 2:\n" + PSXmlDocumentBuilder.toString(auth2.toXml(doc)));
    assertTrue(auth1.equals(auth2));

    // test unencrypted password
    NodeList passwords = auth1Elem.getElementsByTagName("Password");
    Element oldPassword = (Element) passwords.item(0);
    Element newPassword = doc.createElement("Password");
    newPassword.setAttribute("attributeName", "pwAttr");
    newPassword.setAttribute("encrypted", "no");
    newPassword.appendChild(doc.createTextNode("pw"));
    auth1Elem.getFirstChild().replaceChild(newPassword, oldPassword);
    PSAuthentication auth2unencrypted = new PSAuthentication(auth1Elem, null, null, true);
    System.out.println(
        "authentication 2 unencrypted:\n"
            + PSXmlDocumentBuilder.toString(auth2unencrypted.toXml(doc)));
    assertTrue(auth1.equals(auth2unencrypted));

    // test with optional elements undefined
    PSAuthentication auth3 =
        new PSAuthentication("auth3", PSAuthentication.SCHEME_SIMPLE, null, null, null, null);
    System.out.println("authentication 3:\n" + PSXmlDocumentBuilder.toString(auth3.toXml(doc)));
    PSAuthentication auth4 = new PSAuthentication(auth3.toXml(doc), null, null, true);
    System.out.println("authentication 4:\n" + PSXmlDocumentBuilder.toString(auth4.toXml(doc)));
    assertTrue(auth3.equals(auth4));
  }
}
