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

/** Owner-only MSSQL delivery-server update. Other properties must not change. */
class PSDatabaseDeliveryOwnerUpdateTest {

  private static final String PASSWORD = "secret-mssql-pass";

  @Test
  void ownerOnlyBodyIsOnlyOwner() {
    assertTrue(PSDatabaseDeliveryOwnerUpdate.isOwnerOnly(ownerOnly("dbo2")));
    assertFalse(PSDatabaseDeliveryDatabaseNameUpdate.isDatabaseNameOnly(ownerOnly("dbo2")));
    assertFalse(PSDatabaseDeliverySidUpdate.isSidOnly(ownerOnly("dbo2")));
    assertFalse(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(ownerOnly("dbo2")));
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(ownerOnly("dbo2")));
    assertFalse(PSDatabaseDeliveryPortUpdate.isPortOnly(ownerOnly("dbo2")));
    assertFalse(PSDatabaseDeliveryUserUpdate.isUserOnly(ownerOnly("dbo2")));
  }

  @Test
  void namedHostUserOrDatabasePropertiesAreNotOwnerOnly() {
    PSPublishServerInfo named = ownerOnly("dbo2");
    named.setServerName("Prod");
    assertFalse(PSDatabaseDeliveryOwnerUpdate.isOwnerOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "sql.example")));
    assertFalse(PSDatabaseDeliveryOwnerUpdate.isOwnerOnly(hostOnly));

    PSPublishServerInfo databaseOnly = new PSPublishServerInfo();
    databaseOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, "rxnew")));
    assertFalse(PSDatabaseDeliveryOwnerUpdate.isOwnerOnly(databaseOnly));

    PSPublishServerInfo sidOnly = new PSPublishServerInfo();
    sidOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_SID_PROPERTY, "ORCL")));
    assertFalse(PSDatabaseDeliveryOwnerUpdate.isOwnerOnly(sidOnly));

    PSPublishServerInfo withPassword = ownerOnly("dbo2");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD));
    withPassword.setProperties(props);
    assertFalse(PSDatabaseDeliveryOwnerUpdate.isOwnerOnly(withPassword));
    assertFalse(PSDatabaseDeliveryOwnerUpdate.isOwnerOnly(null));
  }

  @Test
  void applyReplacesOwnerAndLeavesHostPortUserDatabaseDriverAndPassword() throws Exception {
    PSPubServer server = mssqlServer("dbo");
    PSDatabaseDeliveryOwnerUpdate.apply(server, ownerOnly("  sa  "));
    assertEquals("sa", value(server, IPSPubServerDao.PUBLISH_OWNER_PROPERTY));
    assertEquals("old-sql.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("1433", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("cmsdb", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("rxdb", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("MSSQL", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("sql-prod", server.getName());
    assertEquals(7, server.getProperties().size());
  }

  @Test
  void blankOrOverlongOwnerDoesNotWrite() {
    PSPubServer blank = mssqlServer("dbo");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryOwnerUpdate.apply(blank, ownerOnly("   ")));
    assertTrue(blankError.getMessage().contains(PSDatabaseDeliveryOwnerUpdate.MSSQL_OWNER_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSWORD));
    assertUnchanged(blank, "dbo");

    PSPubServer overlong = mssqlServer("dbo");
    String tooLong = "o".repeat(PSDatabaseDeliveryOwnerUpdate.MAX_MSSQL_OWNER_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryOwnerUpdate.apply(overlong, ownerOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSDatabaseDeliveryOwnerUpdate.MSSQL_OWNER_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
    assertUnchanged(overlong, "dbo");
  }

  @Test
  void mysqlOracleLocalAndFtpDriversDoNotChangeOwner() {
    PSPubServer mysql = mysqlServer();
    PSValidationException mysqlError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryOwnerUpdate.apply(mysql, ownerOnly("dbo2")));
    assertTrue(mysqlError.getMessage().contains(PSDatabaseDeliveryOwnerUpdate.MSSQL_OWNER_DRIVER));
    assertEquals("rxdb", value(mysql, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertTrue(mysql.getPropertyValue(IPSPubServerDao.PUBLISH_OWNER_PROPERTY).isEmpty());

    PSPubServer oracle = new PSPubServer();
    oracle.setName("ora-prod");
    oracle.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Oracle");
    oracle.addProperty(IPSPubServerDao.PUBLISH_SID_PROPERTY, "ORCL");
    PSValidationException oracleError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryOwnerUpdate.apply(oracle, ownerOnly("dbo2")));
    assertTrue(oracleError.getMessage().contains(PSDatabaseDeliveryOwnerUpdate.MSSQL_OWNER_DRIVER));
    assertEquals("ORCL", value(oracle, IPSPubServerDao.PUBLISH_SID_PROPERTY));
    assertTrue(oracle.getPropertyValue(IPSPubServerDao.PUBLISH_OWNER_PROPERTY).isEmpty());

    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryOwnerUpdate.apply(local, ownerOnly("dbo2")));
    assertTrue(localError.getMessage().contains(PSDatabaseDeliveryOwnerUpdate.MSSQL_OWNER_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_OWNER_PROPERTY).isEmpty());

    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    ftp.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "ftpuser");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryOwnerUpdate.apply(ftp, ownerOnly("dbo2")));
    assertTrue(ftpError.getMessage().contains(PSDatabaseDeliveryOwnerUpdate.MSSQL_OWNER_DRIVER));
    assertEquals("ftpuser", value(ftp, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertTrue(ftp.getPropertyValue(IPSPubServerDao.PUBLISH_OWNER_PROPERTY).isEmpty());
  }

  @Test
  void mssqlDriverMatchIsCaseInsensitive() throws Exception {
    PSPubServer mixed = mssqlServer("dbo");
    mixed.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "mssql");
    PSDatabaseDeliveryOwnerUpdate.apply(mixed, ownerOnly("sa"));
    assertEquals("sa", value(mixed, IPSPubServerDao.PUBLISH_OWNER_PROPERTY));
    assertEquals("cmsdb", value(mixed, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
  }

  private static void assertUnchanged(PSPubServer server, String owner) {
    assertEquals(owner, value(server, IPSPubServerDao.PUBLISH_OWNER_PROPERTY));
    assertEquals("old-sql.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("1433", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("cmsdb", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("rxdb", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer mssqlServer(String owner) {
    PSPubServer server = new PSPubServer();
    server.setName("sql-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MSSQL");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "old-sql.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "1433");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsdb");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, "rxdb");
    server.addProperty(IPSPubServerDao.PUBLISH_OWNER_PROPERTY, owner);
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPubServer mysqlServer() {
    PSPubServer server = new PSPubServer();
    server.setName("mysql-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MYSQL");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "3306");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsdb");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, "rxdb");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPublishServerInfo ownerOnly(String owner) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_OWNER_PROPERTY, owner)));
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
