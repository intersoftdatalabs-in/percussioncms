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

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pagemanagement.web.service.PSTestSiteData;
import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.pubserver.data.PSPublishServerProperty;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.share.test.PSRestTestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.util.List;

/**
 * Integration tests for publish server service.
 * Sunny Sal: "Publish server tests, rolling out!"
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PSPubServerServiceTest extends PSRestTestCase<PSPubServerServiceRestClient> {

    private static PSPubServerServiceRestClient pubServerRestServiceClient;
    private static PSTestSiteData testSiteData;
    private static final Logger log = LogManager.getLogger(PSPubServerServiceTest.class);

    @Override
    protected PSPubServerServiceRestClient getRestClient(String baseUrl) {
        return pubServerRestServiceClient;
    }

    @BeforeAll
    static void setupSuite() throws Exception {
        pubServerRestServiceClient = new PSPubServerServiceRestClient(baseUrl);
        setupClient(pubServerRestServiceClient);

        testSiteData = new PSTestSiteData();
        testSiteData.setUp();
    }

    @AfterAll
    static void tearDown() {
        try {
            testSiteData.tearDown();
        } catch (Exception e) {
            log.error("Failed to tear down test site data", e);
        }
    }

    @Test
    @Order(10)
    void test010GetServer() throws IPSPubServerService.PSPubServerServiceException {
        var siteId = testSiteData.getSiteRestClient().getProperties(testSiteData.site1.getName()).getId();
        var returnedServersList = pubServerRestServiceClient.getPubServerList(siteId);
        assertTrue(returnedServersList.size() > 0);

        var serverInfo = returnedServersList.get(0);
        var serverId = serverInfo.getServerId().toString();

        var returnedServer = pubServerRestServiceClient.getPubServer(siteId, serverId);
        assertNotNull(returnedServer);
    }

    @Test
    @Order(20)
    void test020GetServersList() throws IPSPubServerService.PSPubServerServiceException {
        var siteId = testSiteData.getSiteRestClient().getProperties(testSiteData.site1.getName()).getId();
        var returnedServersList = pubServerRestServiceClient.getPubServerList(siteId);
        assertTrue(returnedServersList.size() > 0);

        for (var info : returnedServersList) {
            if (info.getIsDefault())
                assertTrue(info.getCanIncrementalPublish());
            else
                assertFalse(info.getCanIncrementalPublish());
        }
    }

    @Test
    @Order(30)
    void test030CreateServer() throws IPSPubServerService.PSPubServerServiceException {
        var serverNameNoSpace = "testCreateServer";
        var serverName = " " + serverNameNoSpace + " ";
        var server = createPubServerInfo(serverName, "File");

        var siteId = testSiteData.getSiteRestClient().getProperties(testSiteData.site1.getName()).getId();
        var returnedServer = pubServerRestServiceClient.createPubServer(siteId, serverName, server);

        assertEquals(serverNameNoSpace, returnedServer.getServerName());
        assertEquals("File", returnedServer.getType());
        assertFalse(returnedServer.getCanIncrementalPublish());
        assertTrue(returnedServer.getIsFullPublishRequired());
    }

    @Test
    @Order(40)
    void test040CreateStagingServer() throws IPSPubServerService.PSPubServerServiceException {
        var serverNameNoSpace = "testCreateStagingServer" + System.currentTimeMillis();
        var serverName = " " + serverNameNoSpace + " ";
        var server = createPubServerInfo(serverName, "File");
        server.setServerType(PSPubServer.STAGING);
        var siteId = testSiteData.getSiteRestClient().getProperties(testSiteData.site1.getName()).getId();

        var returnedServer = pubServerRestServiceClient.createPubServer(siteId, serverName, server);

        assertEquals(serverNameNoSpace, returnedServer.getServerName());
        assertEquals("File", returnedServer.getType());
        assertTrue(returnedServer.getCanIncrementalPublish());
        assertTrue(returnedServer.getIsFullPublishRequired());
        assertEquals(PSPubServer.STAGING, returnedServer.getServerType());
    }

    @Test
    @Order(50)
    void test050InvalidPubServerName() {
        var siteId = testSiteData.getSiteRestClient().getProperties(testSiteData.site1.getName()).getId();
        var serverName = "test CreateServer";
        var server = createPubServerInfo(serverName, "File");
        assertThrows(Exception.class, () -> pubServerRestServiceClient.createPubServer(siteId, serverName, server),
                "Should fail as name contains space characters.");
    }

    private PSPublishServerInfo createPubServerInfo(String serverName, String type) {
        var server = new PSPublishServerInfo();
        server.setServerName(serverName);
        server.setType(type);
        server.setIsDefault(false);

        addServerProperty(server, "driver", "Local");
        addServerProperty(server, "HTML", "true");
        addServerProperty(server, "ownServer", "C:\\test");
        addServerProperty(server, "ownServerFlag", "true");
        addServerProperty(server, "defaultServerFlag", "false");
        addServerProperty(server, "XML", "false");
        return server;
    }

    @Test
    @Order(60)
    void test060UpdateServer() throws IPSPubServerService.PSPubServerServiceException {
        var siteId = testSiteData.getSiteRestClient().getProperties(testSiteData.site1.getName()).getId();
        var returnedServersList = pubServerRestServiceClient.getPubServerList(siteId);
        assertTrue(returnedServersList.size() > 0);

        var serverInfo = returnedServersList.get(0);
        assertNotNull(serverInfo);

        var oldServerId = serverInfo.getServerId().toString();
        var serverNameNoSpace = "testUpdateServer";
        var serverName = " " + serverNameNoSpace + " ";
        var newServerInfo = createPubServerInfo(serverName, "File");

        var updatedServer = pubServerRestServiceClient.updatePubServer(siteId, oldServerId, newServerInfo);

        assertEquals(serverNameNoSpace, updatedServer.getServerName());
        assertEquals("File", updatedServer.getType());
    }

    @Test
    @Order(70)
    void test070UpdateServerToFTPPublishing() throws IPSPubServerService.PSPubServerServiceException {
        var siteId = testSiteData.getSiteRestClient().getProperties(testSiteData.site1.getName()).getId();
        var returnedServersList = pubServerRestServiceClient.getPubServerList(siteId);
        assertTrue(returnedServersList.size() > 0);

        var serverInfo = returnedServersList.get(0);
        assertNotNull(serverInfo);

        var oldServerId = serverInfo.getServerId().toString();
        var newName = "Updated_Name_FTP_Server";

        serverInfo.setServerName(newName);
        serverInfo.setType("File");
        serverInfo.setIsDefault(false);

        addServerProperty(serverInfo, "driver", "FTP");
        addServerProperty(serverInfo, "XML", "false");
        addServerProperty(serverInfo, "HTML", "true");
        addServerProperty(serverInfo, "ownServerFlag", "false");
        addServerProperty(serverInfo, "defaultServerFlag", "true");
        addServerProperty(serverInfo, "folder", "/myfolder");
        addServerProperty(serverInfo, "serverip", "192.168.0.253");
        addServerProperty(serverInfo, "userid", "admin");
        addServerProperty(serverInfo, "port", "21");
        addServerProperty(serverInfo, "passwordFlag", "true");
        addServerProperty(serverInfo, "password", "testpercussion");
        addServerProperty(serverInfo, "secure", "false");
        addServerProperty(serverInfo, "privateKeyFlag", "false");
        addServerProperty(serverInfo, "privateKey", "");

        var updatedServer = pubServerRestServiceClient.updatePubServer(siteId, oldServerId, serverInfo);

        assertEquals(newName, updatedServer.getServerName());
        assertEquals("ftp", updatedServer.findProperty("driver").toLowerCase());
        assertEquals("192.168.0.253", updatedServer.findProperty("serverip"));
        assertEquals("passwordEntry", updatedServer.findProperty("password"));
        assertEquals("true", updatedServer.findProperty("passwordFlag"));
        assertEquals("false", updatedServer.findProperty("privateKeyFlag"));
        assertEquals("false", updatedServer.findProperty("secure"));
        assertEquals("false", updatedServer.findProperty("privateKeyFlag"));
        assertEquals("", updatedServer.findProperty("privateKey"));
    }

    @Test
    @Order(80)
    void test080UpdateServerToSFTPPublishingWithPrivateKey() throws IPSPubServerService.PSPubServerServiceException {
        var siteId = testSiteData.getSiteRestClient().getProperties(testSiteData.site1.getName()).getId();
        var returnedServersList = pubServerRestServiceClient.getPubServerList(siteId);
        assertTrue(returnedServersList.size() > 0);

        var serverInfo = returnedServersList.get(0);
        assertNotNull(serverInfo);

        var oldServerId = serverInfo.getServerId().toString();
        var newName = "Updated_Name_SFTP_Server";

        serverInfo.setServerName(newName);
        serverInfo.setType("File");
        serverInfo.setIsDefault(false);

        addServerProperty(serverInfo, "driver", "FTP");
        addServerProperty(serverInfo, "XML", "false");
        addServerProperty(serverInfo, "HTML", "true");
        addServerProperty(serverInfo, "ownServerFlag", "false");
        addServerProperty(serverInfo, "defaultServerFlag", "true");
        addServerProperty(serverInfo, "folder", "/myfolder");
        addServerProperty(serverInfo, "serverip", "192.168.0.253");
        addServerProperty(serverInfo, "userid", "admin");
        addServerProperty(serverInfo, "port", "21");
        addServerProperty(serverInfo, "passwordFlag", "true");
        addServerProperty(serverInfo, "password", "");
        addServerProperty(serverInfo, "secure", "true");
        addServerProperty(serverInfo, "privateKeyFlag", "true");
        addServerProperty(serverInfo, "privateKey", "test.txt");

        var updatedServer = pubServerRestServiceClient.updatePubServer(siteId, oldServerId, serverInfo);

        assertEquals(newName, updatedServer.getServerName());
        assertEquals("ftp", updatedServer.findProperty("driver").toLowerCase());
        assertEquals("192.168.0.253", updatedServer.findProperty("serverip"));
        assertEquals("", updatedServer.findProperty("password"));
        assertEquals("false", updatedServer.findProperty("passwordFlag"));
        assertEquals("true", updatedServer.findProperty("secure"));
        assertEquals("true", updatedServer.findProperty("privateKeyFlag"));
        assertEquals("test.txt", updatedServer.findProperty("privateKey"));
    }

    @Test
    @Order(90)
    void test090DeleteServer() throws IPSPubServerService.PSPubServerServiceException {
        var serverNameNoSpace = "testCreateServerForDeletion";
        var serverName = " " + serverNameNoSpace + " ";
        var server = createPubServerInfo(serverName, "File");

        var siteId = testSiteData.getSiteRestClient().getProperties(testSiteData.site1.getName()).getId();
        var returnedServer = pubServerRestServiceClient.createPubServer(siteId, serverNameNoSpace, server);

        assertEquals(serverNameNoSpace, returnedServer.getServerName());
        assertEquals("File", returnedServer.getType());

        var servers = pubServerRestServiceClient.deleteServer(siteId, returnedServer.getServerId().toString());
        for (var s : servers) {
            assertNotEquals(returnedServer.getServerName(), s.getServerName());
        }
    }

    @Test
    @Order(100)
    void test100AmazonS3Server() throws IPSPubServerService.PSPubServerServiceException {
        var siteId = testSiteData.getSiteRestClient().getProperties(testSiteData.site1.getName()).getId();
        var server = new PSPublishServerInfo();
        var serverName = "AmazonS3Server";
        server.setServerName(serverName);
        server.setType("File");
        server.setIsDefault(false);

        addServerProperty(server, "driver", "AMAZONS3");
        PSPublishServerInfo defServer = null;
        var returnedServersList = pubServerRestServiceClient.getPubServerList(siteId);
        for (var ps : returnedServersList) {
            if (ps.getIsDefault()) {
                defServer = ps;
                break;
            }
        }

        int preServersSize = returnedServersList.size();
        PSPublishServerInfo returnedServer = null;
        try {
            returnedServer = pubServerRestServiceClient.createPubServer(siteId, serverName, server);
        } catch (Exception e) {
            // ignore
        }
        assertNull(returnedServer, "returned pub server must be null");

        returnedServersList = pubServerRestServiceClient.getPubServerList(siteId);
        assertEquals(preServersSize, returnedServersList.size(), "number of servers is same");

        addServerProperty(server, "bucketlocation", "http://cm1-s3-publishing-test.s3-website-us-east-1.amazonaws.com");
        addServerProperty(server, "accesskey", "abcd");
        addServerProperty(server, "securitykey", "1234");
        var s3Server = pubServerRestServiceClient.createPubServer(siteId, serverName, server);
        assertFalse(s3Server.getIsDefault());

        assertEquals("http://cm1-s3-publishing-test.s3-website-us-east-1.amazonaws.com", s3Server.findProperty("bucketlocation"));
        assertEquals("abcd", s3Server.findProperty("accesskey"));
        assertEquals("1234", s3Server.findProperty("securitykey"));

        returnedServersList = pubServerRestServiceClient.getPubServerList(siteId);
        assertEquals(preServersSize + 1, returnedServersList.size(), "number of servers is one more than original");

        s3Server.setIsDefault(true);
        addServerProperty(s3Server, "accesskey", "efgh");

        pubServerRestServiceClient.updatePubServer(siteId, s3Server.getServerId().toString(), s3Server);
        var updatedServer = pubServerRestServiceClient.getPubServer(siteId, s3Server.getServerId().toString());
        assertTrue(updatedServer.getIsDefault());
        assertEquals("efgh", updatedServer.findProperty("accesskey"));

        var locDefServer = createPubServerInfo(siteId, "LocalDefServer");
        locDefServer.setIsDefault(true);
        pubServerRestServiceClient.createPubServer(siteId, "LocalDefServer", locDefServer);

        pubServerRestServiceClient.deleteServer(siteId, s3Server.getServerId().toString());

        returnedServersList = pubServerRestServiceClient.getPubServerList(siteId);
        assertEquals(preServersSize + 1, returnedServersList.size(), "number of servers is same as original");
    }

    private void addServerProperty(PSPublishServerInfo server, String name, String value) {
        var serverProperty = new PSPublishServerProperty();
        serverProperty.setKey(name);
        serverProperty.setValue(value);
        server.getProperties().add(serverProperty);
    }
}
