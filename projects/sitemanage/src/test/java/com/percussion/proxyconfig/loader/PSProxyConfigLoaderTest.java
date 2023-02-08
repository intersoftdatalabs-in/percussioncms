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
package com.percussion.proxyconfig.loader;

import com.percussion.proxyconfig.data.PSProxyConfig;
import com.percussion.proxyconfig.service.impl.PSProxyConfigLoader;
import com.percussion.proxyconfig.service.impl.ProxyConfig;
import com.percussion.proxyconfig.service.impl.ProxyConfig.Protocols;
import com.percussion.proxyconfig.service.impl.ProxyConfigurations;
import com.percussion.share.dao.PSSerializerUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Santiago M. Murchio
 *
 */
public class PSProxyConfigLoaderTest
{

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private String rxdeploydir;

    @Before
    public void setup() throws IOException {

        rxdeploydir = System.getProperty("rxdeploydir");
        System.setProperty("rxdeploydir", temporaryFolder.getRoot().getAbsolutePath());
    }

    @After
    public void teardown(){
        if(rxdeploydir != null)
            System.setProperty("rxdeploydir",rxdeploydir);
    }

    @Test
    public void testGetProxyConfigurations_ConfigFileDoesNotExist() throws Exception
    {
        PSProxyConfigLoader loader = getProxyConfigLoader("fileDoesNotExist.xml");

        assertTrue("The list of proxy configurations should not be null.", loader.getProxyConfigurations() != null);
        assertTrue("The list of proxy configurations should be empty.", loader.getProxyConfigurations().isEmpty());
    }

    @Test
    public void testGetProxyConfigurations_NoDeliveryServers() throws Exception
    {
        PSProxyConfigLoader loader = getProxyConfigLoader("ProxyConfigTest_Empty.xml");

        assertTrue("The list of proxy configurations should not be null.", loader.getProxyConfigurations() != null);
        assertTrue("The list of proxy configurations should be empty.", loader.getProxyConfigurations().isEmpty());
    }
    
    @Test
    public void testGetProxyConfigurations_SomeDeliveryServers() throws Exception
    {
        PSProxyConfigLoader loader = getProxyConfigLoader("ProxyConfigTest_ThreeProxies.xml");

        assertTrue("The list of proxy configurations should not be null.", loader.getProxyConfigurations() != null);
        assertTrue("The list of proxy configurations should have 3 elements.",
                loader.getProxyConfigurations().size() == 3);

        List<String> protocols1 = new ArrayList<String>();
        protocols1.add("HTTP");
        protocols1.add("HTTPS");
        PSProxyConfig proxy1 = new PSProxyConfig("localhost", "1531", "admin1", "demo", protocols1);
        
        List<String> protocols2 = new ArrayList<String>();
        protocols2.add("LDAP");
        PSProxyConfig proxy2 = new PSProxyConfig("percussion.com", "1531", "admin2", "demo", protocols2);

        List<String> protocols3 = new ArrayList<String>();
        protocols3.add("LDAPS");
        PSProxyConfig proxy3 = new PSProxyConfig("google.com", "1622", "admin2", "demo", protocols3);
        
        assertTrue("The list of proxies should contain the proxy 1", loader.getProxyConfigurations().contains(proxy1));
        assertTrue("The list of proxies should contain the proxy 2", loader.getProxyConfigurations().contains(proxy2));
        assertTrue("The list of proxies should contain the proxy 3", loader.getProxyConfigurations().contains(proxy3));
    }
    
    @Test
    @Ignore("TODO: Fix me. Test is failing on build server after change: 202c39871bbb51429e6cde1f3c08e8fe9145d139")
    public void testConvertToEncryptedPassword() throws Exception
    {
        File tempConfigFile = createTempConfigFileBasedOn(this.getClass().getResourceAsStream(
                "ProxyConfigTest_ThreeProxies.xml"));

        @SuppressWarnings("unused")
        PSProxyConfigLoader loader = new PSProxyConfigLoader(tempConfigFile);

        // Original config file should have its passwords encrypted
        InputStream in2 = new FileInputStream(tempConfigFile);
        ProxyConfigurations config = PSSerializerUtils.unmarshalWithValidation(in2, ProxyConfigurations.class);

        assertTrue("The proxy configurations list should have 3 elements", config.getConfigs().size() == 3);

        ProxyConfig proxy1 = config.getConfigs().get(0);
        Protocols protocolsProxy1 = proxy1.getProtocols();
        assertTrue("Proxy 1: password should be encrypted", proxy1.getPassword().isEncrypted() == true);
        assertTrue("Proxy 1: host should be localhost", proxy1.getHost().equals("localhost"));
        assertTrue("Proxy 1: user should be 'admin1'", proxy1.getUser().equals("admin1"));
        assertTrue("Proxy 1: protocols should contain 'HTTP'", protocolsProxy1.getProtocols().contains("HTTP"));
        assertTrue("Proxy 1: protocols should contain 'HTTPS'", protocolsProxy1.getProtocols().contains("HTTPS"));        
        assertTrue("Proxy 1: protocols should contain 2 elements", protocolsProxy1.getProtocols().size() == 2);        
        
        ProxyConfig proxy2 = config.getConfigs().get(1);
        Protocols protocolsProxy2 = proxy2.getProtocols();
        assertTrue("Proxy 2: password should be encrypted", proxy2.getPassword().isEncrypted() == true);
        assertTrue("Proxy 2: host should be 'percussion.com'", proxy2.getHost().equals("percussion.com"));
        assertTrue("Proxy 2: user should be 'admin2'", proxy2.getUser().equals("admin2"));
        assertTrue("Proxy 2: protocols should contain 'LDAP'", protocolsProxy2.getProtocols().contains("LDAP"));
        assertTrue("Proxy 2: protocols should contain 1 element", protocolsProxy2.getProtocols().size() == 1);        
        
        ProxyConfig proxy3 = config.getConfigs().get(2);
        Protocols protocolsProxy3 = proxy3.getProtocols();
        assertTrue("Proxy 3: password should be encrypted", proxy3.getPassword().isEncrypted() == true);
        assertTrue("Proxy 3: host should be 'google.com'", proxy3.getHost().equals("google.com"));
        assertTrue("Proxy 3: user should be 'admin2'", proxy3.getUser().equals("admin2"));
        assertTrue("Proxy 3: protocols should contain 'LDAPS'", protocolsProxy3.getProtocols().contains("LDAPS"));
        assertTrue("Proxy 3: protocols should contain 1 element", protocolsProxy3.getProtocols().size() == 1);        
        
    }
    
    @Test
    @Ignore("TODO: Fix me.  Test is failing on CI server")
    public void testLoadAlreadyEncryptedConfigFile() throws Exception
    {
        File tempConfigFile = createTempConfigFileBasedOn(this.getClass().getResourceAsStream(
                "ProxyConfigTest_EncryptedPassword.xml"));
        
        // Start delivery info loader with encrypted passwords
        @SuppressWarnings("unused")
        PSProxyConfigLoader loader = new PSProxyConfigLoader(tempConfigFile);
        
        // Original config file should have its passwords encrypted
        InputStream in2 = new FileInputStream(tempConfigFile);
        ProxyConfigurations config = PSSerializerUtils.unmarshalWithValidation(in2, ProxyConfigurations.class);
        
        assertTrue("The proxy configurations list should have 2 elements", config.getConfigs().size() == 2);

        ProxyConfig proxy1 = config.getConfigs().get(0);
        Protocols protocolsProxy1 = proxy1.getProtocols();
        assertTrue("Proxy 1: password should be encrypted", proxy1.getPassword().isEncrypted() == true);
        assertTrue("Proxy 1: host should be localhost", proxy1.getHost().equals("localhost"));
        assertTrue("Proxy 1: user should be 'admin1'", proxy1.getUser().equals("admin1"));
        assertTrue("Proxy 1: password should be '7cf3be70d83a6948'",
                proxy1.getPassword().getValue().equals("7cf3be70d83a6948"));
        assertTrue("Proxy 1: protocols should contain 'HTTP'", protocolsProxy1.getProtocols().contains("HTTP"));
        assertTrue("Proxy 1: protocols should contain 'HTTPS'", protocolsProxy1.getProtocols().contains("HTTPS"));        
        assertTrue("Proxy 1: protocols should contain 2 elements", protocolsProxy1.getProtocols().size() == 2);        
        
        ProxyConfig proxy2 = config.getConfigs().get(1);
        Protocols protocolsProxy2 = proxy2.getProtocols();
        assertTrue("Proxy 2: password should be encrypted", proxy2.getPassword().isEncrypted() == true);
        assertTrue("Proxy 2: host should be 'percussion.com'", proxy2.getHost().equals("percussion.com"));
        assertTrue("Proxy 2: user should be 'admin2'", proxy2.getUser().equals("admin2"));
        assertTrue("Proxy 2: password should be '7cf3be70d83a6948'",
                proxy2.getPassword().getValue().equals("7cf3be70d83a6948"));
        assertTrue("Proxy 2: protocols should contain 'LDAP'", protocolsProxy2.getProtocols().contains("LDAP"));
        assertTrue("Proxy 2: protocols should contain 1 element", protocolsProxy2.getProtocols().size() == 1);        
        
    }
    
    @Test
    @Ignore("TODO: Fix me.  Test is failing on CI server")
    public void testLoadMixedPasswordsConfigFile() throws Exception
    {
        File tempConfigFile = createTempConfigFileBasedOn(this.getClass().getResourceAsStream("ProxyConfigTest_MixedPasswords.xml"));
        
        @SuppressWarnings("unused")
        PSProxyConfigLoader loader = new PSProxyConfigLoader(tempConfigFile);
        
        InputStream in2 = new FileInputStream(tempConfigFile);
        ProxyConfigurations config = PSSerializerUtils.unmarshalWithValidation(in2, ProxyConfigurations.class);
        
        assertTrue("The proxy configurations list should have 2 elements", config.getConfigs().size() == 2);

        ProxyConfig proxy1 = config.getConfigs().get(0);
        Protocols protocolsProxy1 = proxy1.getProtocols();
        assertTrue("Proxy 1: password should be encrypted", proxy1.getPassword().isEncrypted() == true);
        assertTrue("Proxy 1: host should be localhost", proxy1.getHost().equals("localhost"));
        assertTrue("Proxy 1: user should be 'admin1'", proxy1.getUser().equals("admin1"));
        assertTrue("Proxy 1: password should be '7cf3be70d83a6948'",
                proxy1.getPassword().getValue().equals("7cf3be70d83a6948"));
        assertTrue("Proxy 1: protocols should contain 'HTTP'", protocolsProxy1.getProtocols().contains("HTTP"));
        assertTrue("Proxy 1: protocols should contain 'HTTPS'", protocolsProxy1.getProtocols().contains("HTTPS"));        
        assertTrue("Proxy 1: protocols should contain 2 elements", protocolsProxy1.getProtocols().size() == 2);        
        
        ProxyConfig proxy2 = config.getConfigs().get(1);
        Protocols protocolsProxy2 = proxy2.getProtocols();
        assertTrue("Proxy 2: password should be encrypted", proxy2.getPassword().isEncrypted() == true);
        assertTrue("Proxy 2: host should be 'percussion.com'", proxy2.getHost().equals("percussion.com"));
        assertTrue("Proxy 2: user should be 'admin2'", proxy2.getUser().equals("admin2"));
        assertTrue("Proxy 2: protocols should contain 'LDAP'", protocolsProxy2.getProtocols().contains("LDAP"));
        assertTrue("Proxy 2: protocols should contain 1 element", protocolsProxy2.getProtocols().size() == 1);        
    }

    @Test
    public void testPasswordsMustBeDecrypted() throws Exception
    {
        File tempConfigFile = createTempConfigFileBasedOn(this.getClass().getResourceAsStream(
                "ProxyConfigTest_EncryptedPassword.xml"));
        
        PSProxyConfigLoader loader = new PSProxyConfigLoader(tempConfigFile);
        
        assertTrue("The list of proxy configurations should not be null.", loader.getProxyConfigurations() != null);
        assertTrue("The list of proxy configurations should have 2 elements.",
                loader.getProxyConfigurations().size() == 2);
        
        List<String> protocols1 = new ArrayList<String>();
        protocols1.add("HTTP");
        protocols1.add("HTTPS");
        PSProxyConfig proxy1 = new PSProxyConfig("localhost", "1531", "admin1", "demo", protocols1);
        
        List<String> protocols2 = new ArrayList<String>();
        protocols2.add("LDAP");
        PSProxyConfig proxy2 = new PSProxyConfig("percussion.com", "1531", "admin2", "demo", protocols2);

        assertTrue("The list of proxies should contain the proxy 1", loader.getProxyConfigurations().contains(proxy1));
        assertTrue("The list of proxies should contain the proxy 2", loader.getProxyConfigurations().contains(proxy2));
    }
    
    /**
     * @param configFile
     * @return The proxy configuration loader class
     */
    private PSProxyConfigLoader getProxyConfigLoader(String configFile)
    {
        try
        {
            URL url = this.getClass().getResource(configFile);
            if(url == null)
            {
                return new PSProxyConfigLoader(new File(configFile));
            }
            else
            {
                return new PSProxyConfigLoader(new File(url.toURI()));
            }
        }
        catch (URISyntaxException e)
        {
            fail("Could not load Proxy configuration file " + configFile);
            return null;
        }
    }
    
    private File createTempConfigFileBasedOn(InputStream baseConfigFile) throws Exception
    {
        File tempConfigFile = File.createTempFile("ProxyConfigurations", ".xml");
        OutputStream out = new FileOutputStream(tempConfigFile);
        InputStream in = baseConfigFile;
        
        IOUtils.copy(in, out);
        
        return tempConfigFile;
    }
    
}
