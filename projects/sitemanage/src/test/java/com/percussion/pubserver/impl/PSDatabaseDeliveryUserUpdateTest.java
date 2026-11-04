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

/** User-id-only database delivery-server update. Other properties must not change. */
class PSDatabaseDeliveryUserUpdateTest {

  private static final String PASSWORD = "secret-db-pass";

  @Test
  void userOnlyBodyIsOnlyUserId() {
    assertTrue(PSDatabaseDeliveryUserUpdate.isUserOnly(userOnly("cmsnew")));
    assertTrue(PSFtpDeliveryUserUpdate.isUserOnly(userOnly("cmsnew")));
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(userOnly("cmsnew")));
    assertFalse(PSDatabaseDeliveryPortUpdate.isPortOnly(userOnly("cmsnew")));
  }

  @Test
  void namedHostPortOrExtraPropertiesAreNotUserOnly() {
    PSPublishServerInfo named = userOnly("cmsnew");
    named.setServerName("Prod");
    assertFalse(PSDatabaseDeliveryUserUpdate.isUserOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "db.example")));
    assertFalse(PSDatabaseDeliveryUserUpdate.isUserOnly(hostOnly));
    assertTrue(PSDatabaseDeliveryHostUpdate.isHostOnly(hostOnly));

    PSPublishServerInfo portOnly = new PSPublishServerInfo();
    portOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "3307")));
    assertFalse(PSDatabaseDeliveryUserUpdate.isUserOnly(portOnly));
    assertTrue(PSDatabaseDeliveryPortUpdate.isPortOnly(portOnly));

    PSPublishServerInfo withPassword = userOnly("cmsnew");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD));
    withPassword.setProperties(props);
    assertFalse(PSDatabaseDeliveryUserUpdate.isUserOnly(withPassword));
    assertFalse(PSDatabaseDeliveryUserUpdate.isUserOnly(null));
  }

  @Test
  void applyReplacesUserAndLeavesHostPortDatabaseDriverAndPassword() throws Exception {
    PSPubServer server = mysqlServer("cmsdb");
    PSDatabaseDeliveryUserUpdate.apply(server, userOnly("  cmsnew  "));
    assertEquals("cmsnew", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("3306", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("rxdb", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("MYSQL", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("mysql-prod", server.getName());
    assertEquals(6, server.getProperties().size());
  }

  @Test
  void applyReplacesOracleUserAndLeavesSid() throws Exception {
    PSPubServer oracle = oracleServer("cmsora");
    PSDatabaseDeliveryUserUpdate.apply(oracle, userOnly("oranew"));
    assertEquals("oranew", value(oracle, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("ORCL", value(oracle, IPSPubServerDao.PUBLISH_SID_PROPERTY));
    assertEquals("old-ora.example", value(oracle, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("1521", value(oracle, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals(PASSWORD, value(oracle, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  @Test
  void blankOrOverlongUserDoesNotWrite() {
    PSPubServer blank = mysqlServer("cmsdb");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryUserUpdate.apply(blank, userOnly("   ")));
    assertTrue(
        blankError.getMessage().contains(PSDatabaseDeliveryUserUpdate.DATABASE_USER_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSWORD));
    assertUnchanged(blank, "cmsdb");

    PSPubServer overlong = mysqlServer("cmsdb");
    String tooLong = "u".repeat(PSDatabaseDeliveryUserUpdate.MAX_DATABASE_USER_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryUserUpdate.apply(overlong, userOnly(tooLong)));
    assertTrue(
        longError.getMessage().contains(PSDatabaseDeliveryUserUpdate.DATABASE_USER_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
    assertUnchanged(overlong, "cmsdb");
  }

  @Test
  void localAndFtpDriversDoNotChangeUser() {
    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryUserUpdate.apply(local, userOnly("cmsnew")));
    assertTrue(
        localError.getMessage().contains(PSDatabaseDeliveryUserUpdate.DATABASE_USER_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY).isEmpty());
    assertEquals(2, local.getProperties().size());

    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    ftp.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "ftpuser");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryUserUpdate.apply(ftp, userOnly("cmsnew")));
    assertTrue(ftpError.getMessage().contains(PSDatabaseDeliveryUserUpdate.DATABASE_USER_DRIVER));
    assertEquals("ftpuser", value(ftp, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("old.example", value(ftp, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
  }

  @Test
  void mssqlAndMysql8AreDatabaseDrivers() throws Exception {
    PSPubServer mssql = mysqlServer("cmsdb");
    mssql.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MSSQL");
    PSDatabaseDeliveryUserUpdate.apply(mssql, userOnly("sqlnew"));
    assertEquals("sqlnew", value(mssql, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("old.example", value(mssql, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));

    PSPubServer mysql8 = mysqlServer("cmsdb");
    mysql8.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MYSQL8");
    PSDatabaseDeliveryUserUpdate.apply(mysql8, userOnly("mysql8new"));
    assertEquals("mysql8new", value(mysql8, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
  }

  private static void assertUnchanged(PSPubServer server, String userId) {
    assertEquals(userId, value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("3306", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("rxdb", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer mysqlServer(String userId) {
    PSPubServer server = new PSPubServer();
    server.setName("mysql-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MYSQL");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "3306");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, userId);
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, "rxdb");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPubServer oracleServer(String userId) {
    PSPubServer server = new PSPubServer();
    server.setName("ora-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Oracle");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "old-ora.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "1521");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, userId);
    server.addProperty(IPSPubServerDao.PUBLISH_SID_PROPERTY, "ORCL");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPublishServerInfo userOnly(String userId) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, userId)));
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
