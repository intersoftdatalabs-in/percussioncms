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

/** Schema-only Oracle delivery-server update. Other properties must not change. */
class PSDatabaseDeliverySchemaUpdateTest {

  private static final String PASSWORD = "secret-ora-pass";

  @Test
  void schemaOnlyBodyIsOnlySchema() {
    assertTrue(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(schemaOnly("APP")));
    assertFalse(PSDatabaseDeliverySidUpdate.isSidOnly(schemaOnly("APP")));
    assertFalse(PSDatabaseDeliveryDatabaseNameUpdate.isDatabaseNameOnly(schemaOnly("APP")));
    assertFalse(PSDatabaseDeliveryOwnerUpdate.isOwnerOnly(schemaOnly("APP")));
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(schemaOnly("APP")));
    assertFalse(PSDatabaseDeliveryPortUpdate.isPortOnly(schemaOnly("APP")));
    assertFalse(PSDatabaseDeliveryUserUpdate.isUserOnly(schemaOnly("APP")));
  }

  @Test
  void namedHostUserSidOrDatabasePropertiesAreNotSchemaOnly() {
    PSPublishServerInfo named = schemaOnly("APP");
    named.setServerName("Prod");
    assertFalse(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "ora.example")));
    assertFalse(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(hostOnly));

    PSPublishServerInfo sidOnly = new PSPublishServerInfo();
    sidOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_SID_PROPERTY, "ORCL")));
    assertFalse(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(sidOnly));

    PSPublishServerInfo databaseOnly = new PSPublishServerInfo();
    databaseOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, "rxnew")));
    assertFalse(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(databaseOnly));

    PSPublishServerInfo withPassword = schemaOnly("APP");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD));
    withPassword.setProperties(props);
    assertFalse(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(withPassword));
    assertFalse(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(null));
  }

  @Test
  void applyReplacesSchemaAndLeavesHostPortUserSidDriverAndPassword() throws Exception {
    PSPubServer server = oracleServer("SYSTEM");
    PSDatabaseDeliverySchemaUpdate.apply(server, schemaOnly("  APP  "));
    assertEquals("APP", value(server, IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY));
    assertEquals("old-ora.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("1521", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("cmsora", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("ORCL", value(server, IPSPubServerDao.PUBLISH_SID_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("Oracle", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("ora-prod", server.getName());
    assertEquals(7, server.getProperties().size());
  }

  @Test
  void blankOrOverlongSchemaDoesNotWrite() {
    PSPubServer blank = oracleServer("SYSTEM");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliverySchemaUpdate.apply(blank, schemaOnly("   ")));
    assertTrue(
        blankError.getMessage().contains(PSDatabaseDeliverySchemaUpdate.ORACLE_SCHEMA_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSWORD));
    assertUnchanged(blank, "SYSTEM");

    PSPubServer overlong = oracleServer("SYSTEM");
    String tooLong = "s".repeat(PSDatabaseDeliverySchemaUpdate.MAX_ORACLE_SCHEMA_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliverySchemaUpdate.apply(overlong, schemaOnly(tooLong)));
    assertTrue(
        longError.getMessage().contains(PSDatabaseDeliverySchemaUpdate.ORACLE_SCHEMA_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
    assertUnchanged(overlong, "SYSTEM");
  }

  @Test
  void mysqlMssqlLocalAndFtpDriversDoNotChangeSchema() {
    PSPubServer mysql = mysqlServer();
    PSValidationException mysqlError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliverySchemaUpdate.apply(mysql, schemaOnly("APP")));
    assertTrue(
        mysqlError.getMessage().contains(PSDatabaseDeliverySchemaUpdate.ORACLE_SCHEMA_DRIVER));
    assertEquals("rxdb", value(mysql, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertTrue(mysql.getPropertyValue(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY).isEmpty());

    PSPubServer mssql = mysqlServer();
    mssql.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MSSQL");
    PSValidationException mssqlError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliverySchemaUpdate.apply(mssql, schemaOnly("APP")));
    assertTrue(
        mssqlError.getMessage().contains(PSDatabaseDeliverySchemaUpdate.ORACLE_SCHEMA_DRIVER));

    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliverySchemaUpdate.apply(local, schemaOnly("APP")));
    assertTrue(
        localError.getMessage().contains(PSDatabaseDeliverySchemaUpdate.ORACLE_SCHEMA_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY).isEmpty());

    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    ftp.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "ftpuser");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliverySchemaUpdate.apply(ftp, schemaOnly("APP")));
    assertTrue(ftpError.getMessage().contains(PSDatabaseDeliverySchemaUpdate.ORACLE_SCHEMA_DRIVER));
    assertEquals("ftpuser", value(ftp, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertTrue(ftp.getPropertyValue(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY).isEmpty());
  }

  @Test
  void oracleDriverMatchIsCaseInsensitive() throws Exception {
    PSPubServer mixed = oracleServer("SYSTEM");
    mixed.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "oracle");
    PSDatabaseDeliverySchemaUpdate.apply(mixed, schemaOnly("HR"));
    assertEquals("HR", value(mixed, IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY));
    assertEquals("cmsora", value(mixed, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("ORCL", value(mixed, IPSPubServerDao.PUBLISH_SID_PROPERTY));
  }

  private static void assertUnchanged(PSPubServer server, String schema) {
    assertEquals(schema, value(server, IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY));
    assertEquals("old-ora.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("1521", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("cmsora", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("ORCL", value(server, IPSPubServerDao.PUBLISH_SID_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer oracleServer(String schema) {
    PSPubServer server = new PSPubServer();
    server.setName("ora-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Oracle");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "old-ora.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "1521");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsora");
    server.addProperty(IPSPubServerDao.PUBLISH_SID_PROPERTY, "ORCL");
    server.addProperty(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY, schema);
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

  private static PSPublishServerInfo schemaOnly(String schema) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY, schema)));
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
