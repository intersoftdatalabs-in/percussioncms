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
package com.percussion.proxyconfig.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.proxyconfig.data.PSProxyConfig;
import com.percussion.proxyconfig.service.IPSProxyConfigService;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Tests for {@link PSProxyConfigService}.
 * Sunny Sal: "Proxy config service, ready for action!"
 */
public class PSProxyConfigServiceTest {

    @Test
    void testFindAll() throws Exception {
        var tempProxyConfigFile = createTempConfigFileBasedOn(getClass().getResourceAsStream("ProxyConfigTest_ValidMultipleConfigs.xml"));
        var proxyConfigService = new PSProxyConfigService(tempProxyConfigFile);
        var configurations = proxyConfigService.findAll();
        assertNotNull(configurations);
    }

    @Test
    void testFindByProtocol() throws Exception {
        var tempProxyConfigFile = createTempConfigFileBasedOn(getClass().getResourceAsStream("ProxyConfigTest_ValidMultipleConfigs.xml"));
        var proxyConfigService = new PSProxyConfigService(tempProxyConfigFile);
        // Test finding an existing configuration value, case insensitive.
        var proxyConfig = proxyConfigService.findByProtocol("HTTP");
        var proxyConfig2 = proxyConfigService.findByProtocol("http");
        assertNotNull(proxyConfig);
        assertNotNull(proxyConfig2);
        assertEquals(proxyConfig, proxyConfig2);
        // Test that for a nonexistent protocol in the config file, no config is found.
        var proxyConfig3 = proxyConfigService.findByProtocol("another protocol");
        assertNull(proxyConfig3);
    }

    private File createTempConfigFileBasedOn(InputStream baseConfigFile) throws Exception {
        var tempConfigFile = File.createTempFile("proxyconfig", ".xml");
        tempConfigFile.deleteOnExit();
        try (OutputStream out = new FileOutputStream(tempConfigFile); InputStream in = baseConfigFile) {
            IOUtils.copy(in, out);
        }
        return tempConfigFile;
    }
}
