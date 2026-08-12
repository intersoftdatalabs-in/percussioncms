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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.DesignErrorCodes;
import com.percussion.design.objectstore.server.PSValidatorAdapter;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Issue #3142: {@link PSAclEntry} production call sites must use typed {@link DesignErrorCodes}
 * (not bare {@code IPSObjectStoreErrors} ints) for ACL level / type validation leftovers.
 */
public class PSAclEntryTypedErrorCodeTest {

  @Test
  public void fromXmlMissingAccessLevelUsesTypedDesignCode() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, PSAclEntry.ms_NodeType);
    root.setAttribute("id", "1");
    root.setAttribute("type", "user");
    PSXmlDocumentBuilder.addElement(doc, root, "name", "admin1");
    // no serverAccessLevel / applicationAccessLevel child

    PSAclEntry entry = new PSAclEntry();
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> entry.fromXml(root, null, null));
    assertEquals(DesignErrorCodes.ACL_ENTRY_LEVEL_NOT_FOUND.numericCode(), ex.getErrorCode());
    assertSame(DesignErrorCodes.ACL_ENTRY_LEVEL_NOT_FOUND, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void validateServerAclWithAppFlagsUsesSecurityLevelDesignCode() throws Exception {
    PSAclEntry entry = new PSAclEntry("admin1", PSAclEntry.ACE_TYPE_USER);
    // Corrupt state: server ACL flag with application access bits
    setPrivate(entry, "m_isServerAcl", true);
    setPrivate(entry, "m_accessLevel", PSAclEntry.AACE_DATA_QUERY);

    PSValidatorAdapter validator = new PSValidatorAdapter(null);
    PSSystemValidationException ex =
        assertThrows(PSSystemValidationException.class, () -> entry.validate(validator));
    assertEquals(DesignErrorCodes.ACL_SECURITY_LEVEL_INVALID.numericCode(), ex.getErrorCode());
  }

  @Test
  public void validateAppAclWithServerFlagsUsesSecurityLevelDesignCode() throws Exception {
    PSAclEntry entry = new PSAclEntry("editor", PSAclEntry.ACE_TYPE_ROLE);
    setPrivate(entry, "m_isServerAcl", false);
    setPrivate(entry, "m_accessLevel", PSAclEntry.SACE_ACCESS_DATA);

    PSValidatorAdapter validator = new PSValidatorAdapter(null);
    PSSystemValidationException ex =
        assertThrows(PSSystemValidationException.class, () -> entry.validate(validator));
    assertEquals(DesignErrorCodes.ACL_SECURITY_LEVEL_INVALID.numericCode(), ex.getErrorCode());
  }

  @Test
  public void validateInvalidTypeUsesTypeDesignCode() throws Exception {
    PSAclEntry entry = new PSAclEntry("orphan", PSAclEntry.ACE_TYPE_USER);
    setPrivate(entry, "m_type", 99);

    PSValidatorAdapter validator = new PSValidatorAdapter(null);
    PSSystemValidationException ex =
        assertThrows(PSSystemValidationException.class, () -> entry.validate(validator));
    assertEquals(DesignErrorCodes.ACL_TYPE_INVALID.numericCode(), ex.getErrorCode());
  }

  private static void setPrivate(Object target, String fieldName, Object value) throws Exception {
    Field field = PSAclEntry.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    if (value instanceof Integer) {
      field.setInt(target, ((Integer) value).intValue());
    } else if (value instanceof Boolean) {
      field.setBoolean(target, ((Boolean) value).booleanValue());
    } else {
      field.set(target, value);
    }
  }
}
