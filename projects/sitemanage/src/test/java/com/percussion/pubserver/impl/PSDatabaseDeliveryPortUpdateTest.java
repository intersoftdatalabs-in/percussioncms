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

/** Port-only database delivery-server update. Other properties must not change. */
class PSDatabaseDeliveryPortUpdateTest {

  private static final String PASSWORD = "secret-db-pass";

  @Test
  void portOnlyBodyIsOnlyPort() {
    assertTrue(PSDatabaseDeliveryPortUpdate.isPortOnly(portOnly("3307")));
    assertTrue(PSFtpDeliveryPortUpdate.isPortOnly(portOnly("3307")));
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(portOnly("3307")));
  }

  @Test
  void namedHostOrExtraPropertiesAreNotPortOnly() {
    PSPublishServerInfo named = portOnly("3307");
    named.setServerName("Prod");
    assertFalse(PSDatabaseDeliveryPortUpdate.isPortOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "db.example")));
    assertFalse(PSDatabaseDeliveryPortUpdate.isPortOnly(hostOnly));
    assertTrue(PSDatabaseDeliveryHostUpdate.isHostOnly(hostOnly));

    PSPublishServerInfo withPassword = portOnly("3307");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD));
    withPassword.setProperties(props);
    assertFalse(PSDatabaseDeliveryPortUpdate.isPortOnly(withPassword));
    assertFalse(PSDatabaseDeliveryPortUpdate.isPortOnly(null));
  }

  @Test
  void applyReplacesPortAndLeavesHostUserDatabaseDriverAndPassword() throws Exception {
    PSPubServer server = mysqlServer("3306");
    PSDatabaseDeliveryPortUpdate.apply(server, portOnly("  3307  "));
    assertEquals("3307", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("cmsdb", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("rxdb", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("MYSQL", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("mysql-prod", server.getName());
    assertEquals(6, server.getProperties().size());
  }

  @Test
  void applyReplacesOraclePortAndLeavesSid() throws Exception {
    PSPubServer oracle = oracleServer("1521");
    PSDatabaseDeliveryPortUpdate.apply(oracle, portOnly("1522"));
    assertEquals("1522", value(oracle, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ORCL", value(oracle, IPSPubServerDao.PUBLISH_SID_PROPERTY));
    assertEquals("old-ora.example", value(oracle, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("cmsora", value(oracle, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSWORD, value(oracle, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  @Test
  void blankNonNumericOrOverlongPortDoesNotWrite() {
    PSPubServer blank = mysqlServer("3306");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryPortUpdate.apply(blank, portOnly("   ")));
    assertTrue(
        blankError.getMessage().contains(PSDatabaseDeliveryPortUpdate.DATABASE_PORT_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSWORD));
    assertUnchanged(blank, "3306");

    for (String bad : List.of("33a", "33.6", "-1", "33 06")) {
      PSPubServer server = mysqlServer("3306");
      PSValidationException error =
          assertThrows(
              PSValidationException.class,
              () -> PSDatabaseDeliveryPortUpdate.apply(server, portOnly(bad)));
      assertTrue(error.getMessage().contains(PSDatabaseDeliveryPortUpdate.DATABASE_PORT_NOT_NUMERIC));
      assertFalse(error.getMessage().contains(PASSWORD));
      assertUnchanged(server, "3306");
    }

    PSPubServer overlong = mysqlServer("3306");
    String tooLong = "9".repeat(PSDatabaseDeliveryPortUpdate.MAX_DATABASE_PORT_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryPortUpdate.apply(overlong, portOnly(tooLong)));
    assertTrue(
        longError.getMessage().contains(PSDatabaseDeliveryPortUpdate.DATABASE_PORT_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
    assertUnchanged(overlong, "3306");
  }

  @Test
  void localAndFtpDriversDoNotChangePort() {
    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryPortUpdate.apply(local, portOnly("3307")));
    assertTrue(
        localError.getMessage().contains(PSDatabaseDeliveryPortUpdate.DATABASE_PORT_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_PORT_PROPERTY).isEmpty());
    assertEquals(2, local.getProperties().size());

    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    ftp.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "2121");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryPortUpdate.apply(ftp, portOnly("3307")));
    assertTrue(ftpError.getMessage().contains(PSDatabaseDeliveryPortUpdate.DATABASE_PORT_DRIVER));
    assertEquals("2121", value(ftp, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("old.example", value(ftp, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
  }

  @Test
  void mssqlAndMysql8AreDatabaseDrivers() throws Exception {
    PSPubServer mssql = mysqlServer("1433");
    mssql.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MSSQL");
    PSDatabaseDeliveryPortUpdate.apply(mssql, portOnly("1434"));
    assertEquals("1434", value(mssql, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("old.example", value(mssql, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));

    PSPubServer mysql8 = mysqlServer("3306");
    mysql8.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MYSQL8");
    PSDatabaseDeliveryPortUpdate.apply(mysql8, portOnly("3308"));
    assertEquals("3308", value(mysql8, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
  }

  private static void assertUnchanged(PSPubServer server, String port) {
    assertEquals(port, value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("cmsdb", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("rxdb", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer mysqlServer(String port) {
    PSPubServer server = new PSPubServer();
    server.setName("mysql-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MYSQL");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, port);
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsdb");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, "rxdb");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPubServer oracleServer(String port) {
    PSPubServer server = new PSPubServer();
    server.setName("ora-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Oracle");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "old-ora.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, port);
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsora");
    server.addProperty(IPSPubServerDao.PUBLISH_SID_PROPERTY, "ORCL");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPublishServerInfo portOnly(String port) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_PORT_PROPERTY, port)));
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
