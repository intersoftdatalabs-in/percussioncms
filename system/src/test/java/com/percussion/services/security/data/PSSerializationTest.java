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
package com.percussion.services.security.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.i18n.PSLocale;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import java.util.Collection;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Behavioral XML helper serialization for selected security / locale objects under the
 * Jackson-backed {@link PSXmlSerializationHelper} (issue #1893 / parent #1823 / epic #505).
 *
 * <p>Converted from a JUnit 3-style class (methods were not discovered by JUnit Platform) to JUnit
 * 5. Domain-specific goldens and package fixtures for {@link PSCommunity} / ACL / login live in
 * issue #1889; this suite keeps lightweight round-trip coverage only.
 *
 * <p><strong>Approved deviations:</strong> no Betwixt graph-identity {@code id="…"} attributes;
 * unannotated beans may emit derived catalog alias fields ({@code *-optional}, {@code
 * display-string}, …) until domain annotation slices suppress them.
 *
 * @author dougrand
 */
public class PSSerializationTest {

  private static final Pattern BETWIXT_GRAPH_ID_ATTR =
      Pattern.compile("\\sid\\s*=\\s*\"\\d+\"", Pattern.CASE_INSENSITIVE);

  @BeforeAll
  static void registerTypes() {
    // Jackson is the sole XML engine after #2062 (Betwixt purge).
    PSXmlSerializationHelper.addType(PSCommunity.class);
    PSXmlSerializationHelper.addType(PSLocale.class);
    PSXmlSerializationHelper.addType(PSGuid.class);
  }

  @Test
  public void testGuidSer() throws Exception {
    PSGuid g = new PSGuid(PSTypeEnum.ACL, 101101);

    String ser = PSXmlSerializationHelper.writeToXml(g);
    assertTrue(containsTag(ser, "guid"), ser);
    assertFalse(BETWIXT_GRAPH_ID_ATTR.matcher(ser).find(), ser);

    PSGuid res = (PSGuid) PSXmlSerializationHelper.readFromXML(ser);

    assertEquals(g, res);
    assertEquals(g.toString(), res.toString());
  }

  @Test
  public void testCommunitySerialization() throws Exception {
    PSCommunity community = new PSCommunity();

    community.setDescription("Test community");
    community.setGUID(new PSGuid(PSTypeEnum.COMMUNITY_DEF, 100101));
    community.setName("Test_1");
    community.addRoleAssociation(new PSGuid(PSTypeEnum.ROLE, 10));
    community.addRoleAssociation(new PSGuid(PSTypeEnum.ROLE, 11));

    String ser = PSXmlSerializationHelper.writeToXml(community);
    assertTrue(containsTag(ser, "community"), ser);
    assertTrue(containsTag(ser, "name"), ser);
    assertTrue(ser.contains("Test_1"), ser);
    assertFalse(BETWIXT_GRAPH_ID_ATTR.matcher(ser).find(), "no graph id attrs: " + ser);
    assertFalse(ser.trim().startsWith("<null"), ser);

    PSCommunity restore = (PSCommunity) PSXmlSerializationHelper.readFromXML(ser);
    assertEquals(community, restore);
    assertEquals("Test_1", restore.getName());
    assertEquals("Test community", restore.getDescription());
    assertEquals(community.getGUID().toString(), restore.getGUID().toString());

    Collection<Long> roles = restore.getRoles();
    assertEquals(2, roles.size(), "role associations restored: " + ser);
    assertTrue(roles.contains(10L), roles.toString());
    assertTrue(roles.contains(11L), roles.toString());
  }

  @Test
  public void testLocaleSerialization() throws Exception {
    PSLocale locale = new PSLocale();

    locale.setLocaleId(111);
    locale.setDescription("A locale");
    locale.setDisplayName("en_GB");
    locale.setLanguageString("en_UK_1");
    locale.setStatus(5);

    String ser = PSXmlSerializationHelper.writeToXml(locale);
    assertTrue(containsTag(ser, "locale"), ser);
    assertFalse(BETWIXT_GRAPH_ID_ATTR.matcher(ser).find(), ser);

    PSLocale restore = (PSLocale) PSXmlSerializationHelper.readFromXML(ser);

    assertEquals(locale, restore);
    assertEquals(111, restore.getLocaleId());
    assertEquals("A locale", restore.getDescription());
    assertEquals("en_GB", restore.getDisplayName());
    assertEquals("en_UK_1", restore.getLanguageString());
    assertEquals(5, restore.getStatus());
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }
}
