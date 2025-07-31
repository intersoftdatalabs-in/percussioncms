// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import com.percussion.design.objectstore.PSRoleProvider;
import com.percussion.design.objectstore.PSServerConfiguration;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Directory object store class testing, including constructors,
 * <code>PSComponent</code> functionality, accessors and XML functionality.
 *
 * This test requires e2srv running the autotest directory on port 390, it
 * is excluded to run on nightly builds.
 */
@Tag("IntegrationTest")
public class PSRoleCatalogerTest {

    public PSRoleCatalogerTest() {}

    /**
     * Test component constructors and accessors.
     *
     * @throws Exception if any exceptions occur or assertions fail.
     */
    @Test
    public void testCataloging() throws Exception {
        var config = getServerConfig();
        PSSecurityProviderPool.init(config);

        var roleProviders = config.getRoleProviders();
        if (roleProviders.hasNext()) {
            var roleProvider = (PSRoleProvider) roleProviders.next();

            var properties = new Properties();
            properties.put(PSSecurityProvider.PROVIDER_NAME, roleProvider.getName());

            var cataloger = new PSRoleCataloger(properties, config);

            Set<?> subjects = cataloger.getSubjects("Admin", null);
            assertFalse(subjects.isEmpty());

            subjects = cataloger.getSubjects("Admin", "%1");
            assertFalse(subjects.isEmpty(), "testCataloging: filtered '%1'");

            subjects = cataloger.getSubjects("Admin", "s%1");
            assertFalse(subjects.isEmpty(), "testCataloging: filtered 's%1'");
        } else {
            fail("testCataloging: missing role provider");
        }
    }

    /**
     * Reads the server configuration XML from the unit test resources and adds
     * all required directory server definitions.
     *
     * @return a valid server configuration with all required directory server
     *    definitions, never <code>null<code>.
     * @throws Exception for any errors.
     */
    private PSServerConfiguration getServerConfig() throws Exception {
        try (InputStream input = PSRoleCataloger.class.getResourceAsStream(RESOURCE_PATH + "config.xml")) {
            Document doc = PSXmlDocumentBuilder.createXmlDocument(
                    new InputStreamReader(input), false);
            return new PSServerConfiguration(doc);
        }
    }

    /**
     * Defines the path to the files used by this unit test, relative from the
     * rhythmyx root.
     */
    private static final String RESOURCE_PATH = "/com/percussion/security/";
}
