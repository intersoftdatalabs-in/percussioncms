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

/** Database-name-only MySQL/MSSQL delivery-server update. Other properties must not change. */
class PSDatabaseDeliveryDatabaseNameUpdateTest {

  private static final String PASSWORD = "secret-db-pass";

  @Test
  void databaseNameOnlyBodyIsOnlyDatabase() {
    assertTrue(PSDatabaseDeliveryDatabaseNameUpdate.isDatabaseNameOnly(nameOnly("rxnew")));
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(nameOnly("rxnew")));
    assertFalse(PSDatabaseDeliveryPortUpdate.isPortOnly(nameOnly("rxnew")));
    assertFalse(PSDatabaseDeliveryUserUpdate.isUserOnly(nameOnly("rxnew")));
  }

  @Test
  void namedHostUserOrSidPropertiesAreNotDatabaseNameOnly() {
    PSPublishServerInfo named = nameOnly("rxnew");
    named.setServerName("Prod");
    assertFalse(PSDatabaseDeliveryDatabaseNameUpdate.isDatabaseNameOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "db.example")));
    assertFalse(PSDatabaseDeliveryDatabaseNameUpdate.isDatabaseNameOnly(hostOnly));

    PSPublishServerInfo sidOnly = new PSPublishServerInfo();
    sidOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_SID_PROPERTY, "ORCL2")));
    assertFalse(PSDatabaseDeliveryDatabaseNameUpdate.isDatabaseNameOnly(sidOnly));

    PSPublishServerInfo withPassword = nameOnly("rxnew");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD));
    withPassword.setProperties(props);
    assertFalse(PSDatabaseDeliveryDatabaseNameUpdate.isDatabaseNameOnly(withPassword));
    assertFalse(PSDatabaseDeliveryDatabaseNameUpdate.isDatabaseNameOnly(null));
  }

  @Test
  void applyReplacesDatabaseAndLeavesHostPortUserDriverAndPassword() throws Exception {
    PSPubServer server = mysqlServer("rxdb");
    PSDatabaseDeliveryDatabaseNameUpdate.apply(server, nameOnly("  rxnew  "));
    assertEquals("rxnew", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("3306", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("cmsdb", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("MYSQL", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("mysql-prod", server.getName());
    assertEquals(6, server.getProperties().size());
  }

  @Test
  void applyReplacesMssqlDatabaseAndLeavesOwner() throws Exception {
    PSPubServer mssql = mysqlServer("rxdb");
    mssql.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MSSQL");
    mssql.addProperty(IPSPubServerDao.PUBLISH_OWNER_PROPERTY, "dbo");
    PSDatabaseDeliveryDatabaseNameUpdate.apply(mssql, nameOnly("sqlnew"));
    assertEquals("sqlnew", value(mssql, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals("dbo", value(mssql, IPSPubServerDao.PUBLISH_OWNER_PROPERTY));
    assertEquals("old.example", value(mssql, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("cmsdb", value(mssql, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
  }

  @Test
  void mysql8IsAMysqlDriver() throws Exception {
    PSPubServer mysql8 = mysqlServer("rxdb");
    mysql8.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MYSQL8");
    PSDatabaseDeliveryDatabaseNameUpdate.apply(mysql8, nameOnly("rx8new"));
    assertEquals("rx8new", value(mysql8, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals("cmsdb", value(mysql8, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
  }

  @Test
  void blankOrOverlongDatabaseDoesNotWrite() {
    PSPubServer blank = mysqlServer("rxdb");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryDatabaseNameUpdate.apply(blank, nameOnly("   ")));
    assertTrue(
        blankError
            .getMessage()
            .contains(PSDatabaseDeliveryDatabaseNameUpdate.DATABASE_NAME_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSWORD));
    assertUnchanged(blank, "rxdb");

    PSPubServer overlong = mysqlServer("rxdb");
    String tooLong = "d".repeat(PSDatabaseDeliveryDatabaseNameUpdate.MAX_DATABASE_NAME_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryDatabaseNameUpdate.apply(overlong, nameOnly(tooLong)));
    assertTrue(
        longError
            .getMessage()
            .contains(PSDatabaseDeliveryDatabaseNameUpdate.DATABASE_NAME_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
    assertUnchanged(overlong, "rxdb");
  }

  @Test
  void oracleLocalAndFtpDriversDoNotChangeDatabaseOrSid() {
    PSPubServer oracle = oracleServer();
    PSValidationException oracleError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryDatabaseNameUpdate.apply(oracle, nameOnly("rxnew")));
    assertTrue(
        oracleError
            .getMessage()
            .contains(PSDatabaseDeliveryDatabaseNameUpdate.DATABASE_NAME_DRIVER));
    assertEquals("ORCL", value(oracle, IPSPubServerDao.PUBLISH_SID_PROPERTY));
    assertEquals("cmsora", value(oracle, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertTrue(oracle.getPropertyValue(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY).isEmpty());

    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryDatabaseNameUpdate.apply(local, nameOnly("rxnew")));
    assertTrue(
        localError
            .getMessage()
            .contains(PSDatabaseDeliveryDatabaseNameUpdate.DATABASE_NAME_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY).isEmpty());

    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    ftp.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "ftpuser");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryDatabaseNameUpdate.apply(ftp, nameOnly("rxnew")));
    assertTrue(
        ftpError.getMessage().contains(PSDatabaseDeliveryDatabaseNameUpdate.DATABASE_NAME_DRIVER));
    assertEquals("ftpuser", value(ftp, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertTrue(ftp.getPropertyValue(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY).isEmpty());
  }

  private static void assertUnchanged(PSPubServer server, String databaseName) {
    assertEquals(databaseName, value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("3306", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("cmsdb", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer mysqlServer(String databaseName) {
    PSPubServer server = new PSPubServer();
    server.setName("mysql-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MYSQL");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "3306");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsdb");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, databaseName);
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPubServer oracleServer() {
    PSPubServer server = new PSPubServer();
    server.setName("ora-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Oracle");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "old-ora.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "1521");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsora");
    server.addProperty(IPSPubServerDao.PUBLISH_SID_PROPERTY, "ORCL");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPublishServerInfo nameOnly(String databaseName) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, databaseName)));
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
