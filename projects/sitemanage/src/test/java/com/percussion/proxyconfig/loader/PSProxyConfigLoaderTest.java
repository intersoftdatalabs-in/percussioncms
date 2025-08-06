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
// REFACTORED: CP-JAVA11
package com.percussion.proxyconfig.loader;

import com.percussion.proxyconfig.data.PSProxyConfig;
import com.percussion.proxyconfig.service.impl.PSProxyConfigLoader;
import com.percussion.proxyconfig.service.impl.ProxyConfig;
import com.percussion.proxyconfig.service.impl.ProxyConfig.Protocols;
import com.percussion.proxyconfig.service.impl.ProxyConfigurations;
import com.percussion.share.dao.PSSerializerUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PSProxyConfigLoader}.
 * Sunny Sal says: "Proxy configs, assemble!"
 */
public class PSProxyConfigLoaderTest {

    @TempDir
    Path tempDir;
    private String rxdeploydir;

    @BeforeEach
    void setup() throws IOException {
        rxdeploydir = System.getProperty("rxdeploydir");
        System.setProperty("rxdeploydir", tempDir.toFile().getAbsolutePath());
    }

    @AfterEach
    void teardown() {
        if (rxdeploydir != null) {
            System.setProperty("rxdeploydir", rxdeploydir);
        }
    }

    @Test
    void testGetProxyConfigurations_ConfigFileDoesNotExist() {
        var loader = getProxyConfigLoader("fileDoesNotExist.xml");
        assertNotNull(loader.getProxyConfigurations(), "The list of proxy configurations should not be null.");
        assertTrue(loader.getProxyConfigurations().isEmpty(), "The list of proxy configurations should be empty.");
    }

    @Test
    void testGetProxyConfigurations_NoDeliveryServers() {
        var loader = getProxyConfigLoader("ProxyConfigTest_Empty.xml");
        assertNotNull(loader.getProxyConfigurations(), "The list of proxy configurations should not be null.");
        assertTrue(loader.getProxyConfigurations().isEmpty(), "The list of proxy configurations should be empty.");
    }

    @Test
    void testGetProxyConfigurations_SomeDeliveryServers() {
        var loader = getProxyConfigLoader("ProxyConfigTest_ThreeProxies.xml");
        assertNotNull(loader.getProxyConfigurations(), "The list of proxy configurations should not be null.");
        assertEquals(3, loader.getProxyConfigurations().size(), "The list of proxy configurations should have 3 elements.");

        var protocols1 = List.of("HTTP", "HTTPS");
        var proxy1 = new PSProxyConfig("localhost", "1531", "admin1", "demo", protocols1);

        var protocols2 = List.of("LDAP");
        var proxy2 = new PSProxyConfig("percussion.com", "1531", "admin2", "demo", protocols2);

        var protocols3 = List.of("LDAPS");
        var proxy3 = new PSProxyConfig("google.com", "1622", "admin2", "demo", protocols3);

        assertTrue(loader.getProxyConfigurations().contains(proxy1), "The list of proxies should contain the proxy 1");
        assertTrue(loader.getProxyConfigurations().contains(proxy2), "The list of proxies should contain the proxy 2");
        assertTrue(loader.getProxyConfigurations().contains(proxy3), "The list of proxies should contain the proxy 3");
    }

    @Test
    @Disabled("TODO: Fix me. Test is failing on build server after change: 202c39871bbb51429e6cde1f3c08e8fe9145d139")
    void testConvertToEncryptedPassword() throws Exception {
        var tempConfigFile = createTempConfigFileBasedOn(getClass().getResourceAsStream("ProxyConfigTest_ThreeProxies.xml"));
        new PSProxyConfigLoader(tempConfigFile);

        try (var in2 = new FileInputStream(tempConfigFile)) {
            var config = PSSerializerUtils.unmarshalWithValidation(in2, ProxyConfigurations.class);
            assertEquals(3, config.getConfigs().size(), "The proxy configurations list should have 3 elements");

            var proxy1 = config.getConfigs().get(0);
            var protocolsProxy1 = proxy1.getProtocols();
            assertTrue(proxy1.getPassword().isEncrypted(), "Proxy 1: password should be encrypted");
            assertEquals("localhost", proxy1.getHost(), "Proxy 1: host should be localhost");
            assertEquals("admin1", proxy1.getUser(), "Proxy 1: user should be 'admin1'");
            assertTrue(protocolsProxy1.getProtocols().contains("HTTP"), "Proxy 1: protocols should contain 'HTTP'");
            assertTrue(protocolsProxy1.getProtocols().contains("HTTPS"), "Proxy 1: protocols should contain 'HTTPS'");
            assertEquals(2, protocolsProxy1.getProtocols().size(), "Proxy 1: protocols should contain 2 elements");

            var proxy2 = config.getConfigs().get(1);
            var protocolsProxy2 = proxy2.getProtocols();
            assertTrue(proxy2.getPassword().isEncrypted(), "Proxy 2: password should be encrypted");
            assertEquals("percussion.com", proxy2.getHost(), "Proxy 2: host should be 'percussion.com'");
            assertEquals("admin2", proxy2.getUser(), "Proxy 2: user should be 'admin2'");
            assertTrue(protocolsProxy2.getProtocols().contains("LDAP"), "Proxy 2: protocols should contain 'LDAP'");
            assertEquals(1, protocolsProxy2.getProtocols().size(), "Proxy 2: protocols should contain 1 element");

            var proxy3 = config.getConfigs().get(2);
            var protocolsProxy3 = proxy3.getProtocols();
            assertTrue(proxy3.getPassword().isEncrypted(), "Proxy 3: password should be encrypted");
            assertEquals("google.com", proxy3.getHost(), "Proxy 3: host should be 'google.com'");
            assertEquals("admin2", proxy3.getUser(), "Proxy 3: user should be 'admin2'");
            assertTrue(protocolsProxy3.getProtocols().contains("LDAPS"), "Proxy 3: protocols should contain 'LDAPS'");
            assertEquals(1, protocolsProxy3.getProtocols().size(), "Proxy 3: protocols should contain 1 element");
        }
    }

    @Test
    @Disabled("TODO: Fix me.  Test is failing on CI server")
    void testLoadAlreadyEncryptedConfigFile() throws Exception {
        var tempConfigFile = createTempConfigFileBasedOn(getClass().getResourceAsStream("ProxyConfigTest_EncryptedPassword.xml"));
        new PSProxyConfigLoader(tempConfigFile);

        try (var in2 = new FileInputStream(tempConfigFile)) {
            var config = PSSerializerUtils.unmarshalWithValidation(in2, ProxyConfigurations.class);
            assertEquals(2, config.getConfigs().size(), "The proxy configurations list should have 2 elements");

            var proxy1 = config.getConfigs().get(0);
            var protocolsProxy1 = proxy1.getProtocols();
            assertTrue(proxy1.getPassword().isEncrypted(), "Proxy 1: password should be encrypted");
            assertEquals("localhost", proxy1.getHost(), "Proxy 1: host should be localhost");
            assertEquals("admin1", proxy1.getUser(), "Proxy 1: user should be 'admin1'");
            assertEquals("7cf3be70d83a6948", proxy1.getPassword().getValue(), "Proxy 1: password should be '7cf3be70d83a6948'");
            assertTrue(protocolsProxy1.getProtocols().contains("HTTP"), "Proxy 1: protocols should contain 'HTTP'");
            assertTrue(protocolsProxy1.getProtocols().contains("HTTPS"), "Proxy 1: protocols should contain 'HTTPS'");
            assertEquals(2, protocolsProxy1.getProtocols().size(), "Proxy 1: protocols should contain 2 elements");

            var proxy2 = config.getConfigs().get(1);
            var protocolsProxy2 = proxy2.getProtocols();
            assertTrue(proxy2.getPassword().isEncrypted(), "Proxy 2: password should be encrypted");
            assertEquals("percussion.com", proxy2.getHost(), "Proxy 2: host should be 'percussion.com'");
            assertEquals("admin2", proxy2.getUser(), "Proxy 2: user should be 'admin2'");
            assertEquals("7cf3be70d83a6948", proxy2.getPassword().getValue(), "Proxy 2: password should be '7cf3be70d83a6948'");
            assertTrue(protocolsProxy2.getProtocols().contains("LDAP"), "Proxy 2: protocols should contain 'LDAP'");
            assertEquals(1, protocolsProxy2.getProtocols().size(), "Proxy 2: protocols should contain 1 element");
        }
    }

    @Test
    @Disabled("TODO: Fix me.  Test is failing on CI server")
    void testLoadMixedPasswordsConfigFile() throws Exception {
        var tempConfigFile = createTempConfigFileBasedOn(getClass().getResourceAsStream("ProxyConfigTest_MixedPasswords.xml"));
        new PSProxyConfigLoader(tempConfigFile);

        try (var in2 = new FileInputStream(tempConfigFile)) {
            var config = PSSerializerUtils.unmarshalWithValidation(in2, ProxyConfigurations.class);
            assertEquals(2, config.getConfigs().size(), "The proxy configurations list should have 2 elements");

            var proxy1 = config.getConfigs().get(0);
            var protocolsProxy1 = proxy1.getProtocols();
            assertTrue(proxy1.getPassword().isEncrypted(), "Proxy 1: password should be encrypted");
            assertEquals("localhost", proxy1.getHost(), "Proxy 1: host should be localhost");
            assertEquals("admin1", proxy1.getUser(), "Proxy 1: user should be 'admin1'");
            assertEquals("7cf3be70d83a6948", proxy1.getPassword().getValue(), "Proxy 1: password should be '7cf3be70d83a6948'");
            assertTrue(protocolsProxy1.getProtocols().contains("HTTP"), "Proxy 1: protocols should contain 'HTTP'");
            assertTrue(protocolsProxy1.getProtocols().contains("HTTPS"), "Proxy 1: protocols should contain 'HTTPS'");
            assertEquals(2, protocolsProxy1.getProtocols().size(), "Proxy 1: protocols should contain 2 elements");

            var proxy2 = config.getConfigs().get(1);
            var protocolsProxy2 = proxy2.getProtocols();
            assertTrue(proxy2.getPassword().isEncrypted(), "Proxy 2: password should be encrypted");
            assertEquals("percussion.com", proxy2.getHost(), "Proxy 2: host should be 'percussion.com'");
            assertEquals("admin2", proxy2.getUser(), "Proxy 2: user should be 'admin2'");
            assertTrue(protocolsProxy2.getProtocols().contains("LDAP"), "Proxy 2: protocols should contain 'LDAP'");
            assertEquals(1, protocolsProxy2.getProtocols().size(), "Proxy 2: protocols should contain 1 element");
        }
    }

    @Test
    void testPasswordsMustBeDecrypted() throws Exception {
        var tempConfigFile = createTempConfigFileBasedOn(getClass().getResourceAsStream("ProxyConfigTest_EncryptedPassword.xml"));
        var loader = new PSProxyConfigLoader(tempConfigFile);

        assertNotNull(loader.getProxyConfigurations(), "The list of proxy configurations should not be null.");
        assertEquals(2, loader.getProxyConfigurations().size(), "The list of proxy configurations should have 2 elements.");

        var protocols1 = List.of("HTTP", "HTTPS");
        var proxy1 = new PSProxyConfig("localhost", "1531", "admin1", "demo", protocols1);

        var protocols2 = List.of("LDAP");
        var proxy2 = new PSProxyConfig("percussion.com", "1531", "admin2", "demo", protocols2);

        assertTrue(loader.getProxyConfigurations().contains(proxy1), "The list of proxies should contain the proxy 1");
        assertTrue(loader.getProxyConfigurations().contains(proxy2), "The list of proxies should contain the proxy 2");
    }

    private PSProxyConfigLoader getProxyConfigLoader(String configFile) {
        try {
            URL url = getClass().getResource(configFile);
            if (url == null) {
                return new PSProxyConfigLoader(new File(configFile));
            } else {
                return new PSProxyConfigLoader(new File(url.toURI()));
            }
        } catch (URISyntaxException e) {
            fail("Could not load Proxy configuration file " + configFile);
            return null;
        }
    }

    private File createTempConfigFileBasedOn(InputStream baseConfigFile) throws IOException {
        var tempConfigFile = File.createTempFile("ProxyConfigurations", ".xml");
        try (var out = new FileOutputStream(tempConfigFile); var in = baseConfigFile) {
            IOUtils.copy(in, out);
        }
        return tempConfigFile;
    }
}
