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
package com.percussion.pubserver.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.pubserver.data.PSPublishServerProperty;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.share.service.exception.PSValidationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Host-only database delivery-server update. Other properties must not change. */
class PSDatabaseDeliveryHostUpdateTest {

  private static final String PASSWORD = "secret-db-pass";

  @Test
  void hostOnlyBodyIsOnlyServer() {
    assertTrue(PSDatabaseDeliveryHostUpdate.isHostOnly(hostOnly("db.example")));
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(hostOnly("db.example")));
  }

  @Test
  void namedOrExtraPropertiesAreNotHostOnly() {
    PSPublishServerInfo named = hostOnly("db.example");
    named.setServerName("Prod");
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(named));

    PSPublishServerInfo withPassword = hostOnly("db.example");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD));
    withPassword.setProperties(props);
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(withPassword));
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(null));
  }

  @Test
  void ftpServerIpBodyIsNotDatabaseHostOnly() {
    PSPublishServerInfo ftpHost = new PSPublishServerInfo();
    ftpHost.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "ftp.example")));
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(ftpHost));
    assertTrue(PSFtpDeliveryHostUpdate.isHostOnly(ftpHost));
  }

  @Test
  void applyReplacesHostAndLeavesPortUserDatabaseDriverAndPassword() throws Exception {
    PSPubServer server = mysqlServer("old.example");
    PSDatabaseDeliveryHostUpdate.apply(server, hostOnly("  db.new.example  "));
    assertEquals(
        "db.new.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("3306", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("cmsdb", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("rxdb", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("MYSQL", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("mysql-prod", server.getName());
    assertEquals(6, server.getProperties().size());
  }

  @Test
  void applyReplacesOracleHostAndLeavesSid() throws Exception {
    PSPubServer oracle = oracleServer("old-ora.example");
    PSDatabaseDeliveryHostUpdate.apply(oracle, hostOnly("ora.new.example"));
    assertEquals(
        "ora.new.example", value(oracle, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("ORCL", value(oracle, IPSPubServerDao.PUBLISH_SID_PROPERTY));
    assertEquals("1521", value(oracle, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("cmsora", value(oracle, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSWORD, value(oracle, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  @Test
  void blankOrOverlongHostDoesNotWrite() {
    PSPubServer blank = mysqlServer("old.example");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryHostUpdate.apply(blank, hostOnly("   ")));
    assertTrue(
        blankError.getMessage().contains(PSDatabaseDeliveryHostUpdate.DATABASE_HOST_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSWORD));
    assertUnchanged(blank, "old.example");

    PSPubServer overlong = mysqlServer("old.example");
    String tooLong = "h".repeat(PSDatabaseDeliveryHostUpdate.MAX_DATABASE_HOST_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryHostUpdate.apply(overlong, hostOnly(tooLong)));
    assertTrue(
        longError.getMessage().contains(PSDatabaseDeliveryHostUpdate.DATABASE_HOST_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
    assertUnchanged(overlong, "old.example");
  }

  @Test
  void localAndFtpDriversDoNotChangeHost() {
    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryHostUpdate.apply(local, hostOnly("db.example")));
    assertTrue(
        localError.getMessage().contains(PSDatabaseDeliveryHostUpdate.DATABASE_HOST_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME).isEmpty());
    assertEquals(2, local.getProperties().size());

    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryHostUpdate.apply(ftp, hostOnly("db.example")));
    assertTrue(ftpError.getMessage().contains(PSDatabaseDeliveryHostUpdate.DATABASE_HOST_DRIVER));
    assertEquals("old.example", value(ftp, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
  }

  @Test
  void mssqlAndMysql8AreDatabaseDrivers() {
    assertTrue(PSDatabaseDeliveryHostUpdate.isDatabaseDeliveryDriver("MYSQL"));
    assertTrue(PSDatabaseDeliveryHostUpdate.isDatabaseDeliveryDriver(" mysql8 "));
    assertTrue(PSDatabaseDeliveryHostUpdate.isDatabaseDeliveryDriver("MSSQL"));
    assertTrue(PSDatabaseDeliveryHostUpdate.isDatabaseDeliveryDriver("Oracle"));
    assertFalse(PSDatabaseDeliveryHostUpdate.isDatabaseDeliveryDriver("FTP"));
    assertFalse(PSDatabaseDeliveryHostUpdate.isDatabaseDeliveryDriver("Local"));
  }

  private static void assertUnchanged(PSPubServer server, String host) {
    assertEquals(host, value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("3306", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("cmsdb", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("rxdb", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer mysqlServer(String host) {
    PSPubServer server = new PSPubServer();
    server.setName("mysql-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MYSQL");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, host);
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "3306");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsdb");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, "rxdb");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPubServer oracleServer(String host) {
    PSPubServer server = new PSPubServer();
    server.setName("ora-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Oracle");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, host);
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "1521");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsora");
    server.addProperty(IPSPubServerDao.PUBLISH_SID_PROPERTY, "ORCL");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPublishServerInfo hostOnly(String host) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, host)));
    return info;
  }

  private static PSPublishServerProperty property(String key, String value) {
    PSPublishServerProperty property = new PSPublishServerProperty();
    property.setKey(key);
    property.setValue(value);
    return property;
  }

  private static String value(PSPubServer server, String key) {
    return server.getPropertyValue(key).orElse("");
  }
}
