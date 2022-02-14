/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.ant.install;

import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.utils.container.adapters.DtsConnectorConfigurationAdapterTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/***
 * Test the ant task that creates the DTS properties
 */
public class TestUpdateDTSConfiguration {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static String STAGING_PATH = "Staging/Deployment";
    private static String STAGING_PATH_WIN = "Staging\\Deployment";

    @Before
    public void setup(){
        PSSecureXMLUtils.setupJAXPDefaults();
    }

    @Test
    public void testCompare(){
        assertTrue("/test/test/Staging/Deployment".contains(STAGING_PATH));
        assertTrue("C:\\Vineet\\5.3_19052020_DTS_P_S\\Staging\\Deployment\\Server\\webapps\\manager\\images".contains(STAGING_PATH_WIN));

    }


    @Test
    public void testProperties() throws IOException {
        Path root = temporaryFolder.getRoot().toPath();

        InputStream srcWinLax = DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/utils/container/PercussionServer.lax");
        InputStream srcLinuxLax = DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/utils/container/PercussionServer.bin.lax");
        InputStream srcInstallProps = DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/utils/container/jetty/base/etc/installation.properties");
        InputStream srcLoginConf = DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/utils/container/jetty/base/etc/login.conf");
        InputStream srcPercDsXML= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/utils/container/jetty/base/etc/perc-ds.xml");
        InputStream srcPercDsProperties= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/utils/container/jetty/base/etc/perc-ds-derby.properties");
        InputStream srcProdDTSXML= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/utils/container/Deployment/Server/conf/server.xml");
        InputStream srcStageDTSXML= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/utils/container/Staging/Deployment/Server/conf/server.xml");
        InputStream srcProdDTSXML53= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/utils/container/Deployment/Server/conf/server.xml.5.3");
        InputStream srcStageDTSXML53= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/utils/container/Staging/Deployment/Server/conf/server.xml.5.3");

        temporaryFolder.newFolder("jetty","base","etc");
        temporaryFolder.newFolder("Deployment","Server","conf");
        temporaryFolder.newFolder("Staging", "Deployment","Server","conf");

        Files.copy(srcWinLax,root.resolve("PercussionServer.lax"));
        Files.copy(srcLinuxLax,root.resolve("PercussionServer.bin.lax"));
        Files.copy(srcInstallProps,root.resolve("jetty/base/etc/installation.properties"));
        Files.copy(srcLoginConf,root.resolve("jetty/base/etc/login.conf"));
        Files.copy(srcPercDsXML,root.resolve("jetty/base/etc/perc-ds.xml"));
        Files.copy(srcPercDsProperties,root.resolve("jetty/base/etc/perc-ds.properties"));
        Files.copy(srcProdDTSXML,root.resolve("Deployment/Server/conf/server.xml"));
        Files.copy(srcStageDTSXML,root.resolve("Staging/Deployment/Server/conf/server.xml"));
        Files.copy(srcProdDTSXML53,root.resolve("Deployment/Server/conf/server.xml.5.3"));
        Files.copy(srcStageDTSXML53,root.resolve("Staging/Deployment/Server/conf/server.xml.5.3"));
        Files.createDirectory(root.resolve("Staging/Deployment/Server/conf/perc"));
        Files.createDirectory(root.resolve("Deployment/Server/conf/perc"));

        PSUpdateDTSConfiguration task = new PSUpdateDTSConfiguration();
        task.setRootDir(root.toAbsolutePath().toString());

        task.execute();

        Properties stagingProps = new Properties();
        try(FileInputStream in = new FileInputStream(root.resolve("Staging/Deployment/Server/conf/perc/perc-catalina.properties").toAbsolutePath().toString())){
            stagingProps.load(in) ;

            assertEquals("9970", stagingProps.get("http.port"));

            assertEquals("9443", stagingProps.get("https.port"));

        }

        Properties prodProps = new Properties();
        try(FileInputStream in = new FileInputStream(root.resolve("Deployment/Server/conf/perc/perc-catalina.properties").toAbsolutePath().toString())){
            prodProps.load(in) ;

            assertEquals("9980", prodProps.get("http.port"));


            assertEquals("8443", prodProps.get("https.port"));


        }

    }

    /**
     * Test the results when an existing perc-catalina.properties is present.  make sure the configured values
     * are not lost.
     */
    @Test
    public void testDTSUpgradeExistingCatalinaProps() throws IOException {

        Path root = temporaryFolder.getRoot().toPath().resolve("8to8upgrade");
        root.toFile().mkdirs();

        InputStream srcProdDTSXML= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/ant/install/mockinstall/Deployment/Server/conf/server.xml");
        InputStream srcStageDTSXML= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/ant/install/mockinstall/Staging/Deployment/Server/conf/server.xml");

        InputStream srcProdDTSXML53= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/ant/install/mockinstall/Deployment/Server/conf/server.xml.5.3");
        InputStream srcStageDTSXML53= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/ant/install/mockinstall/Staging/Deployment/Server/conf/server.xml.5.3");

        InputStream srcProdDTSXMLBAK= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/ant/install/mockinstall/Deployment/Server/conf/server.xml.bak");
     //   InputStream srcStageDTSXMLBAK= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/ant/install/mockinstall/Staging/Deployment/Server/conf/server.xml.bak");

        InputStream srcProdDTSProps= DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/ant/install/mockinstall/Deployment/Server/conf/perc/perc-catalina.properties");
        InputStream srcStageDTSProps = DtsConnectorConfigurationAdapterTest.class.getResourceAsStream("/com/percussion/ant/install/mockinstall/Staging/Deployment/Server/conf/perc/perc-catalina.properties");

        temporaryFolder.newFolder("8to8upgrade", "Deployment","Server","conf", "perc");
        temporaryFolder.newFolder("8to8upgrade", "Staging", "Deployment","Server","conf","perc");

        Files.copy(srcProdDTSXML,root.resolve("Deployment/Server/conf/server.xml"));
        Files.copy(srcStageDTSXML,root.resolve("Staging/Deployment/Server/conf/server.xml"));
        Files.copy(srcProdDTSXML53,root.resolve("Deployment/Server/conf/server.xml.5.3"));
        Files.copy(srcStageDTSXML53,root.resolve("Staging/Deployment/Server/conf/server.xml.5.3"));
        Files.copy(srcProdDTSXMLBAK,root.resolve("Deployment/Server/conf/server.xml.bak"));
     //   Files.copy(srcStageDTSXMLBAK,root.resolve("Staging/Deployment/Server/conf/server.xml.bak"));
        Files.copy(srcProdDTSProps,root.resolve("Deployment/Server/conf/perc/perc-catalina.properties"));
        Files.copy(srcStageDTSProps,root.resolve("Staging/Deployment/Server/conf/perc/perc-catalina.properties"));

        PSUpdateDTSConfiguration task = new PSUpdateDTSConfiguration();
        task.setRootDir(root.toAbsolutePath().toString());

        task.execute();

        //validate that existing properties not overwritten
        Properties prodProps = new Properties();
        try (InputStream input = new FileInputStream(
                root.resolve("Deployment" + File.separator +
                        "Server" + File.separator + "conf" + File.separator + "perc" + File.separator+ "perc-catalina.properties").toFile())) {

            prodProps.load(input);
            assertEquals("29980",prodProps.getProperty("http.port"));
            assertEquals("28443", prodProps.getProperty("https.port"));
            assertEquals("somepassword",prodProps.getProperty("https.keystorePass"));
            assertEquals("somepassword",prodProps.getProperty("certificateKeystorePassword"));
            assertEquals("TLSv1.2,TLSv1.3",prodProps.getProperty("https.sslEnabledProtocols"));
            assertEquals("TLSv1.2,TLSv1.3",prodProps.getProperty("https.protocols"));
            assertEquals("conf/A.keystore",prodProps.getProperty("https.keystoreFile"));
            assertEquals("28443", prodProps.getProperty("http.redirectPort"));

        }


    }
}
