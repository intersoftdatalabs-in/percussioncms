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
package com.percussion.services.catalog.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.security.PSPermissions;
import com.percussion.services.security.data.PSUserAccessLevel;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Object summary serialization coverage under Jackson-backed {@link PSXmlSerializationHelper}
 * (issue #1903 residual of #1893 / parent #1823 / epic #505).
 *
 * <p>Write-side shape and full equality round-trips are enabled. Nested {@link PSUserAccessLevel}
 * is suppressed on write (historical Betwixt {@code @IPSXmlSerialization}); permissions wire as
 * {@code permission-value}. {@link PSUserAccessLevel} also has a Jackson no-arg constructor for
 * empty nested graphs elsewhere.
 *
 * <p><strong>Approved deviations:</strong> no Betwixt graph-identity {@code id="…"} attributes on
 * write.
 *
 * @author dougrand
 */
public class PSObjectSummaryTest {
  private static final SecureRandom ms_rand = new SecureRandom();

  private static final Pattern BETWIXT_GRAPH_ID_ATTR =
      Pattern.compile("\\sid\\s*=\\s*\"\\d+\"", Pattern.CASE_INSENSITIVE);

  public PSObjectSummaryTest() {}

  @BeforeAll
  static void ensureJacksonDefault() {
    System.clearProperty(PSXmlSerializationHelper.ENGINE_PROPERTY);
    assertTrue(PSXmlSerializationHelper.isJacksonEngine());
    PSXmlSerializationHelper.addType(PSObjectSummary.class);
  }

  /**
   * Write-side shape for a minimal summary: modern root, name/label/guid present, no nested
   * permissions graph, no graph ids.
   *
   * @throws Exception on write failure
   */
  @Test
  public void testWriteShapeMinimalSummary() throws Exception {
    PSObjectSummary nsum =
        new PSObjectSummary(
            new PSGuid(PSTypeEnum.ACL, ms_rand.nextInt(1000)),
            "Test object summary",
            "Test object summary label",
            null);
    String ser = PSXmlSerializationHelper.writeToXml(nsum);

    assertTrue(containsTag(ser, "object-summary"), ser);
    assertTrue(containsTag(ser, "name"), ser);
    assertTrue(ser.contains("Test object summary"), ser);
    assertTrue(ser.contains("Test object summary label"), ser);
    assertTrue(containsTag(ser, "guid"), ser);
    assertFalse(containsTag(ser, "permissions"), "nested permissions suppressed: " + ser);
    assertFalse(BETWIXT_GRAPH_ID_ATTR.matcher(ser).find(), "no graph id attrs: " + ser);
    assertFalse(ser.trim().startsWith("<null"), ser);
  }

  /**
   * Full equality round-trip for an incomplete summary (default empty permissions).
   *
   * @throws Exception on write/read failure
   */
  @Test
  public void testSerialization() throws Exception {
    PSObjectSummary nsum =
        new PSObjectSummary(
            new PSGuid(PSTypeEnum.ACL, ms_rand.nextInt(1000)),
            "Test object summary",
            "Test object summary label",
            null);
    String ser = PSXmlSerializationHelper.writeToXml(nsum);

    PSObjectSummary restore = (PSObjectSummary) PSXmlSerializationHelper.readFromXML(ser);

    org.junit.jupiter.api.Assertions.assertEquals(nsum, restore);
  }

  /**
   * Fully populated summary including lock + permissions (permission-value string path).
   *
   * @throws Exception on write/read failure
   */
  @Test
  public void testCompleteSerialization() throws Exception {
    PSObjectSummary nsum =
        new PSObjectSummary(
            new PSGuid(PSTypeEnum.ACL, ms_rand.nextInt(1000)),
            "Test object summary",
            "Test object summary label",
            null);
    nsum.setLockedInfo("session_1", "orange_julius", 123456789);
    Collection<PSPermissions> permissions = new ArrayList<>();

    permissions.add(PSPermissions.RUNTIME_VISIBLE);
    permissions.add(PSPermissions.OWNER);

    nsum.setPermissions(new PSUserAccessLevel(permissions));

    String ser = PSXmlSerializationHelper.writeToXml(nsum);
    assertTrue(containsTag(ser, "permission-value"), ser);
    assertTrue(ser.contains("RUNTIME_VISIBLE"), ser);
    assertTrue(ser.contains("OWNER"), ser);
    assertFalse(containsTag(ser, "permissions"), "nested permissions suppressed: " + ser);
    assertTrue(containsTag(ser, "locked"), ser);

    PSObjectSummary restore = (PSObjectSummary) PSXmlSerializationHelper.readFromXML(ser);

    org.junit.jupiter.api.Assertions.assertEquals(nsum, restore, "Expected to be equal");
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }
}
