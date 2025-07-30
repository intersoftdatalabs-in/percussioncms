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

// REFACTORED: CP-JAVA11
package com.percussion.pubserver;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.services.pubserver.IPSDatabasePubServerFilesService;
import com.percussion.services.pubserver.data.PSDatabasePubServer;
import com.percussion.services.pubserver.data.PSDatabasePubServer.DriverType;
import com.percussion.services.pubserver.impl.PSDatabasePubServerFilesService;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.util.PSPurgableTempFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Test {@link IPSDatabasePubServerFilesService}.
 * Sunny Sal: "Database pub server files, let's get transactional!"
 */
public class PSDatabasePubServerFilesServiceTest {

    private static final String NAME_SUFFIX = "_TestDs";
    private static final String DB_SUFFIX = "_db";
    private static final String LOCALHOST = "localhost";
    private static final String TEST_USER = "test_user";
    private static final String OWNER_ODB = "dbo";
    private static final String PASSWORD_DEMO = "demo";
    private static final String ORACLE_SID = "EX";

    private PSPurgableTempFile m_datasourceFile;
    private PSPurgableTempFile m_loginConfigFile;
    private PSPurgableTempFile m_serverBeanFile;
    private IPSDatabasePubServerFilesService m_fileService;

    @BeforeEach
    void setUp() throws Exception {
        m_datasourceFile = loadXmlFile("rx-ds");
        m_loginConfigFile = loadXmlFile("login-config");
        m_serverBeanFile = loadXmlFile("server-beans");

        m_fileService = new PSDatabasePubServerFilesService();
        m_fileService.setDatasourceConfigFile(m_datasourceFile);
        m_fileService.setLoginConfigFile(m_loginConfigFile);
        m_fileService.setServerBeanFile(m_serverBeanFile);
    }

    @AfterEach
    void tearDown() {
        m_datasourceFile.delete();
        m_loginConfigFile.delete();
        m_serverBeanFile.delete();
    }

    @Test
    void testRetrieve() {
        var dbServers = m_fileService.getDatabasePubServers();
        assertEquals(3, dbServers.size());

        var siteServers = m_fileService.getSiteDatabasePubServers();
        assertEquals(0, siteServers.size());
    }

    @Test
    void testAdd() throws PSDataServiceException {
        addDatabasePubServer(DriverType.MSSQL, "mssql", 1000L);
        addDatabasePubServer(DriverType.ORACLE, "oracle", 200L);
        addDatabasePubServer(DriverType.MYSQL, "mysql", null);

        var siteServers = m_fileService.getSiteDatabasePubServers();
        assertEquals(2, siteServers.size());
    }

    @Test
    void testDelete() throws PSDataServiceException {
        var myServer = addDatabasePubServer(DriverType.MSSQL, "mssql", 1000L);
        var dbServers = m_fileService.getDatabasePubServers();
        m_fileService.deleteDatabasePubServer(myServer);
        var dbServers2 = m_fileService.getDatabasePubServers();
        assertEquals(dbServers.size() - 1, dbServers2.size());
    }

    @Test
    void testModifySameType() throws PSDataServiceException, CloneNotSupportedException {
        var myServer = addDatabasePubServer(DriverType.MSSQL, "mssql", 1000L);
        var pubServer = (PSDatabasePubServer) myServer.clone();
        pubServer.setServer(myServer.getServer() + "_abc2");
        pubServer.setPort(myServer.getPort() + 100);
        pubServer.setDatabase(myServer.getDatabase() + "_abc");
        pubServer.setUserName(myServer.getUserName() + "_100");
        pubServer.setPassword(myServer.getPassword() + "200");

        var dbServers = m_fileService.getDatabasePubServers();
        m_fileService.saveDatabasePubServer(pubServer);

        var dbServers2 = m_fileService.getDatabasePubServers();
        assertEquals(dbServers.size(), dbServers2.size());
        var pubServer2 = dbServers2.get(dbServers2.size() - 1);

        assertNotEquals(myServer, pubServer2);
        assertEquals(pubServer, pubServer2);
    }

    @Test
    void testModifyDifferentType() throws PSDataServiceException, CloneNotSupportedException {
        var myServer = addDatabasePubServer(DriverType.MSSQL, "mssql", 1000L);
        var pubServer = (PSDatabasePubServer) myServer.clone();
        pubServer.setDriverType(DriverType.MYSQL);
        pubServer.setServer(myServer.getServer() + "_abc2");
        pubServer.setPort(myServer.getPort() + 100);
        pubServer.setDatabase(myServer.getDatabase() + "_abc");
        pubServer.setUserName(myServer.getUserName() + "_100");
        pubServer.setPassword(myServer.getPassword() + "200");

        var dbServers = m_fileService.getDatabasePubServers();
        m_fileService.saveDatabasePubServer(pubServer);

        var dbServers2 = m_fileService.getDatabasePubServers();
        assertEquals(dbServers.size(), dbServers2.size());
        var pubServer2 = dbServers2.get(dbServers2.size() - 1);

        assertNotEquals(myServer, pubServer2);
        assertEquals(pubServer, pubServer2);
    }

    @Test
    @Disabled("Integration test for available drivers, enable if needed.")
    void testGetAvailableDrivers() {
        Map<String, Boolean> availableDrivers = m_fileService.getAvailableDrivers();
        assertEquals(DriverType.values().length, availableDrivers.size());
        // For default, oracle and MSSQL should be available
        assertTrue(availableDrivers.get("ORACLE"));
        assertTrue(availableDrivers.get("MSSQL"));
    }

    @Test
    void testIsValidConnection() {
        boolean isRealTest = false;

        var pubServer = createDbPubServer(DriverType.MSSQL, "mssql", 1000L);
        pubServer.setUserName("sa");
        pubServer.setPassword("demo");
        pubServer.setDatabase("cmlite");
        String errorMsg = null;
        if (isRealTest)
            errorMsg = m_fileService.testDatabasePubServer(pubServer);
        assertNull(errorMsg);

        var pubServerBad = createDbPubServer(DriverType.MSSQL, "mssql_2", 2000L);
        pubServerBad.setUserName("sa");
        pubServerBad.setPassword("demo");
        pubServerBad.setDatabase("cmlite_unknown");
        errorMsg = m_fileService.testDatabasePubServer(pubServerBad);
        assertNotNull(errorMsg);
        if (isRealTest)
            assertTrue(errorMsg.startsWith("Cannot open database \"cmlite_unknown\""));

        pubServerBad = createDbPubServer(DriverType.MSSQL, "mssql_3", 3000L);
        pubServerBad.setUserName("sa");
        pubServerBad.setPassword("demo_2");
        pubServerBad.setDatabase("cmlite");
        errorMsg = m_fileService.testDatabasePubServer(pubServerBad);
        assertNotNull(errorMsg);
        if (isRealTest)
            assertTrue(errorMsg.startsWith("Login failed for user 'sa'."));
    }

    @Test
    void testAddAndIsModifiedServer() {
        Long siteId = 123L;
        assertFalse(m_fileService.isServerModified(siteId, "Server1"));
        m_fileService.addModifiedServer(siteId, "Server1");
        assertTrue(m_fileService.isServerModified(siteId, "Server1"));
    }

    private PSDatabasePubServer addDatabasePubServer(DriverType type, String namePrefix, Long siteId) throws PSDataServiceException {
        var dbServers = m_fileService.getDatabasePubServers();
        var pubServer = createDbPubServer(type, namePrefix, siteId);
        m_fileService.saveDatabasePubServer(pubServer);

        var dbServers2 = m_fileService.getDatabasePubServers();
        assertEquals(dbServers.size() + 1, dbServers2.size());

        // validate server name, port
        var testServer = dbServers2.get(dbServers2.size() - 1);
        validateDatabasePubServer(testServer, namePrefix, siteId);

        return testServer;
    }

    private void validateDatabasePubServer(PSDatabasePubServer s, String namePrefix, Long siteId) {
        var type = s.getDriverType();
        assertEquals(namePrefix + NAME_SUFFIX, s.getName());
        assertEquals(LOCALHOST, s.getServer());
        assertEquals(type.getDefaultPort(), s.getPort());

        if (type != DriverType.ORACLE)
            assertEquals(namePrefix + DB_SUFFIX, s.getDatabase());

        if (type == DriverType.ORACLE) {
            assertTrue(isEmpty(s.getDatabase()));
            assertEquals(ORACLE_SID, s.getOracleSid());
            assertEquals(TEST_USER, s.getOwner());
        }
        if (type == DriverType.MSSQL)
            assertEquals(OWNER_ODB, s.getOwner());

        assertEquals(TEST_USER, s.getUserName());
        assertEquals(PASSWORD_DEMO, s.getPassword());

        if (siteId == null) {
            assertNull(s.getSiteId());
        } else {
            assertEquals(siteId.longValue(), s.getSiteId().longValue());
        }
    }

    private PSDatabasePubServer createDbPubServer(DriverType type, String namePrefix, Long siteId) {
        var s = new PSDatabasePubServer();
        s.setDriverType(type);
        s.setSiteId(siteId);
        s.setName(namePrefix + NAME_SUFFIX);
        s.setServer(LOCALHOST);
        s.setPort(type.getDefaultPort());
        s.setUserName(TEST_USER);
        s.setPassword(PASSWORD_DEMO);
        if (type != DriverType.ORACLE)
            s.setDatabase(namePrefix + DB_SUFFIX);

        if (type == DriverType.MSSQL) {
            s.setOwner(OWNER_ODB);
        } else if (type == DriverType.ORACLE) {
            s.setOwner(TEST_USER);
            s.setOracleSid(ORACLE_SID);
        }
        return s;
    }

    private PSPurgableTempFile loadXmlFile(String filename) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(filename + ".xml")) {
            byte[] data = IOUtils.toByteArray(in);
            var file = new PSPurgableTempFile(filename + "-", ".xml", null);
            FileUtils.writeByteArrayToFile(file, data);
            return file;
        }
    }
}
