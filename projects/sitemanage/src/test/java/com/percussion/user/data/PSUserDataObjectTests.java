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
package com.percussion.user.data;

import static com.percussion.test.TestAssertions.*;
import static com.percussion.test.TestAssertions.*;

import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.data.PSDataObjectTestCase;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSBeanValidationUtils;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.user.data.PSLdapConfig.PSLdapServer;
import com.percussion.user.data.PSLdapConfig.PSLdapServer.CatalogType;
import jakarta.xml.bind.UnmarshalException;
import java.util.HashSet;
import org.junit.jupiter.api.*;

/** Tests for user data objects and LDAP config XML validation. // REFACTORED: CP-JAVA11 */
public class PSUserDataObjectTests {

  public static class PSLdapConfigTest extends PSDataObjectTestCase<PSLdapConfig> {

    @Override
    public PSLdapConfig getObject() {
      var c = new PSLdapConfig();
      var s = new PSLdapServer();

      s.setHost("stuff.com");
      s.setCatalogType(CatalogType.shallow);
      s.setPort(3000);
      var organizationalUnits = new HashSet<String>();
      organizationalUnits.add("asdfasdf");
      s.setOrganizationalUnits(organizationalUnits);
      s.setPassword("hidden");
      s.setUser("coolio");

      c.setServer(s);

      return c;
    }

    @Test
    public void testValidXml() throws Exception {
      var config = loadXml("ValidLdapConfig.xml");
      assertNotNull(config.getServer());
      validate(config);
    }

    @Test
    public void testInValidXml() throws Exception {
      assertThrows(
          PSBeanValidationException.class,
          () -> {
            var config = loadXml("InvalidLdapConfig.xml");
            assertNotNull(config.getServer());
            validate(config);
          });
    }

    @Test
    public void testBadXml() {
      assertThrows(UnmarshalException.class, () -> loadXml("BadXmlLdapConfig.xml"));
    }

    @Test
    public void testBadXmlMissingOrgUnits() {
      assertThrows(UnmarshalException.class, () -> loadXml("BadXmlOrgUnitsLdapConfig.xml"));
    }

    private PSSpringValidationException validate(PSLdapConfig c)
        throws PSSpringValidationException {
      return PSBeanValidationUtils.validate(c.getServer()).throwIfInvalid();
    }

    private PSLdapConfig loadXml(String name) throws Exception {
      try (var is = getClass().getResourceAsStream(name)) {
        return PSSerializerUtils.unmarshalWithValidation(is, PSLdapConfig.class);
      }
    }
  }
}
