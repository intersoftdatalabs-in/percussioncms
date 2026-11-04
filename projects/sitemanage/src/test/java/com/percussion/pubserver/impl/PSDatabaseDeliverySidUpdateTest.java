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

/** SID-only Oracle delivery-server update. Other properties must not change. */
class PSDatabaseDeliverySidUpdateTest {

  private static final String PASSWORD = "secret-ora-pass";

  @Test
  void sidOnlyBodyIsOnlySid() {
    assertTrue(PSDatabaseDeliverySidUpdate.isSidOnly(sidOnly("ORCL2")));
    assertFalse(PSDatabaseDeliveryDatabaseNameUpdate.isDatabaseNameOnly(sidOnly("ORCL2")));
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(sidOnly("ORCL2")));
    assertFalse(PSDatabaseDeliveryPortUpdate.isPortOnly(sidOnly("ORCL2")));
    assertFalse(PSDatabaseDeliveryUserUpdate.isUserOnly(sidOnly("ORCL2")));
  }

  @Test
  void namedHostUserOrDatabasePropertiesAreNotSidOnly() {
    PSPublishServerInfo named = sidOnly("ORCL2");
    named.setServerName("Prod");
    assertFalse(PSDatabaseDeliverySidUpdate.isSidOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "ora.example")));
    assertFalse(PSDatabaseDeliverySidUpdate.isSidOnly(hostOnly));

    PSPublishServerInfo databaseOnly = new PSPublishServerInfo();
    databaseOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, "rxnew")));
    assertFalse(PSDatabaseDeliverySidUpdate.isSidOnly(databaseOnly));

    PSPublishServerInfo withPassword = sidOnly("ORCL2");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD));
    withPassword.setProperties(props);
    assertFalse(PSDatabaseDeliverySidUpdate.isSidOnly(withPassword));
    assertFalse(PSDatabaseDeliverySidUpdate.isSidOnly(null));
  }

  @Test
  void applyReplacesSidAndLeavesHostPortUserSchemaDriverAndPassword() throws Exception {
    PSPubServer server = oracleServer("ORCL");
    PSDatabaseDeliverySidUpdate.apply(server, sidOnly("  ORCL2  "));
    assertEquals("ORCL2", value(server, IPSPubServerDao.PUBLISH_SID_PROPERTY));
    assertEquals("old-ora.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("1521", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("cmsora", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("SYSTEM", value(server, IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("Oracle", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("ora-prod", server.getName());
    assertEquals(7, server.getProperties().size());
  }

  @Test
  void blankOrOverlongSidDoesNotWrite() {
    PSPubServer blank = oracleServer("ORCL");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliverySidUpdate.apply(blank, sidOnly("   ")));
    assertTrue(blankError.getMessage().contains(PSDatabaseDeliverySidUpdate.ORACLE_SID_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSWORD));
    assertUnchanged(blank, "ORCL");

    PSPubServer overlong = oracleServer("ORCL");
    String tooLong = "s".repeat(PSDatabaseDeliverySidUpdate.MAX_ORACLE_SID_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliverySidUpdate.apply(overlong, sidOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSDatabaseDeliverySidUpdate.ORACLE_SID_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
    assertUnchanged(overlong, "ORCL");
  }

  @Test
  void mysqlMssqlLocalAndFtpDriversDoNotChangeSid() {
    PSPubServer mysql = mysqlServer();
    PSValidationException mysqlError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliverySidUpdate.apply(mysql, sidOnly("ORCL2")));
    assertTrue(mysqlError.getMessage().contains(PSDatabaseDeliverySidUpdate.ORACLE_SID_DRIVER));
    assertEquals("rxdb", value(mysql, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertTrue(mysql.getPropertyValue(IPSPubServerDao.PUBLISH_SID_PROPERTY).isEmpty());

    PSPubServer mssql = mysqlServer();
    mssql.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MSSQL");
    PSValidationException mssqlError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliverySidUpdate.apply(mssql, sidOnly("ORCL2")));
    assertTrue(mssqlError.getMessage().contains(PSDatabaseDeliverySidUpdate.ORACLE_SID_DRIVER));

    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliverySidUpdate.apply(local, sidOnly("ORCL2")));
    assertTrue(localError.getMessage().contains(PSDatabaseDeliverySidUpdate.ORACLE_SID_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_SID_PROPERTY).isEmpty());

    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    ftp.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "ftpuser");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliverySidUpdate.apply(ftp, sidOnly("ORCL2")));
    assertTrue(ftpError.getMessage().contains(PSDatabaseDeliverySidUpdate.ORACLE_SID_DRIVER));
    assertEquals("ftpuser", value(ftp, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertTrue(ftp.getPropertyValue(IPSPubServerDao.PUBLISH_SID_PROPERTY).isEmpty());
  }

  @Test
  void oracleDriverMatchIsCaseInsensitive() throws Exception {
    PSPubServer mixed = oracleServer("ORCL");
    mixed.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "oracle");
    PSDatabaseDeliverySidUpdate.apply(mixed, sidOnly("XE"));
    assertEquals("XE", value(mixed, IPSPubServerDao.PUBLISH_SID_PROPERTY));
    assertEquals("cmsora", value(mixed, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
  }

  private static void assertUnchanged(PSPubServer server, String sid) {
    assertEquals(sid, value(server, IPSPubServerDao.PUBLISH_SID_PROPERTY));
    assertEquals("old-ora.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("1521", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("cmsora", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("SYSTEM", value(server, IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer oracleServer(String sid) {
    PSPubServer server = new PSPubServer();
    server.setName("ora-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Oracle");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "old-ora.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "1521");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsora");
    server.addProperty(IPSPubServerDao.PUBLISH_SID_PROPERTY, sid);
    server.addProperty(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY, "SYSTEM");
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

  private static PSPublishServerInfo sidOnly(String sid) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_SID_PROPERTY, sid)));
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
