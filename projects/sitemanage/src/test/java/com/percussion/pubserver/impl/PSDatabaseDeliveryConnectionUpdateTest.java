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

/**
 * Connection-triple (host, user id, password) database delivery-server update. Other properties
 * must not change and the stored secret must never surface in an error message.
 */
class PSDatabaseDeliveryConnectionUpdateTest {

  private static final String PASSWORD = "secret-db-pass";

  private static final String NEW_PASSWORD = "rotated-db-pass";

  @Test
  void connectionTripleBodyIsConnectionOnly() {
    assertTrue(
        PSDatabaseDeliveryConnectionUpdate.isConnectionOnly(
            connectionOnly("db.example", "cmsdb", PASSWORD)));
    assertFalse(
        PSDatabaseDeliveryHostUpdate.isHostOnly(connectionOnly("db.example", "cmsdb", PASSWORD)));
    assertFalse(
        PSDatabaseDeliveryPasswordUpdate.isPasswordOnly(
            connectionOnly("db.example", "cmsdb", PASSWORD)));
  }

  @Test
  void singleFieldBodiesAreNotConnectionOnly() {
    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "db.example")));
    assertFalse(PSDatabaseDeliveryConnectionUpdate.isConnectionOnly(hostOnly));

    PSPublishServerInfo passwordOnly = new PSPublishServerInfo();
    passwordOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, NEW_PASSWORD)));
    assertFalse(PSDatabaseDeliveryConnectionUpdate.isConnectionOnly(passwordOnly));
    assertFalse(PSDatabaseDeliveryConnectionUpdate.isConnectionOnly(null));
  }

  @Test
  void namedOrExtraPropertiesAreNotConnectionOnly() {
    PSPublishServerInfo named = connectionOnly("db.example", "cmsdb", NEW_PASSWORD);
    named.setServerName("Prod");
    assertFalse(PSDatabaseDeliveryConnectionUpdate.isConnectionOnly(named));

    PSPublishServerInfo withPort = connectionOnly("db.example", "cmsdb", NEW_PASSWORD);
    List<PSPublishServerProperty> props = new ArrayList<>(withPort.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "3307"));
    withPort.setProperties(props);
    assertFalse(PSDatabaseDeliveryConnectionUpdate.isConnectionOnly(withPort));

    PSPublishServerInfo missingPassword = new PSPublishServerInfo();
    missingPassword.setProperties(
        List.of(
            property(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "db.example"),
            property(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsdb")));
    assertFalse(PSDatabaseDeliveryConnectionUpdate.isConnectionOnly(missingPassword));

    PSPublishServerInfo mixedCaseKeys = new PSPublishServerInfo();
    mixedCaseKeys.setProperties(
        List.of(
            property("Server", "db.example"),
            property("UserID", "cmsdb"),
            property("PASSWORD", NEW_PASSWORD)));
    assertTrue(PSDatabaseDeliveryConnectionUpdate.isConnectionOnly(mixedCaseKeys));
  }

  @Test
  void applyReplacesTripleAndLeavesPortDatabaseDriverAndName() throws Exception {
    PSPubServer server = mysqlServer("old.example", "cmsdb");
    PSDatabaseDeliveryConnectionUpdate.apply(
        server, connectionOnly("  db.new.example  ", "  cmsnew  ", NEW_PASSWORD));
    assertEquals(
        "db.new.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("cmsnew", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(NEW_PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("3306", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("rxdb", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertEquals("MYSQL", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("mysql-prod", server.getName());
    assertEquals(6, server.getProperties().size());
  }

  @Test
  void blankHostUserOrPasswordDoesNotWriteAndHidesTheSecret() {
    PSPubServer blankHost = mysqlServer("old.example", "cmsdb");
    PSValidationException hostError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryConnectionUpdate.apply(blankHost, connectionOnly("   ", "cmsdb", NEW_PASSWORD)));
    assertTrue(
        hostError.getMessage().contains(PSDatabaseDeliveryHostUpdate.DATABASE_HOST_REQUIRED));
    assertFalse(hostError.getMessage().contains(NEW_PASSWORD));
    assertFalse(hostError.getMessage().contains(PASSWORD));
    assertUnchanged(blankHost);

    PSPubServer blankUser = mysqlServer("old.example", "cmsdb");
    PSValidationException userError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryConnectionUpdate.apply(blankUser, connectionOnly("db.example", "  ", NEW_PASSWORD)));
    assertTrue(
        userError.getMessage().contains(PSDatabaseDeliveryUserUpdate.DATABASE_USER_REQUIRED));
    assertFalse(userError.getMessage().contains(NEW_PASSWORD));
    assertUnchanged(blankUser);

    PSPubServer blankPassword = mysqlServer("old.example", "cmsdb");
    PSValidationException passwordError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryConnectionUpdate.apply(blankPassword, connectionOnly("db.example", "cmsdb", "   ")));
    assertTrue(
        passwordError
            .getMessage()
            .contains(PSDatabaseDeliveryPasswordUpdate.DATABASE_PASSWORD_REQUIRED));
    assertUnchanged(blankPassword);
  }

  @Test
  void maskedPlaceholderPasswordDoesNotWrite() {
    PSPubServer server = mysqlServer("old.example", "cmsdb");
    PSValidationException placeholderError =
        assertThrows(
            PSValidationException.class,
            () ->
                PSDatabaseDeliveryConnectionUpdate.apply(
                    server,
                    connectionOnly(
                        "db.example", "cmsdb", PSPubServerService.PASSWORD_ENTRY)));
    assertTrue(
        placeholderError
            .getMessage()
            .contains(PSDatabaseDeliveryPasswordUpdate.DATABASE_PASSWORD_REQUIRED));
    assertUnchanged(server);
  }

  @Test
  void overlongValuesDoNotWrite() {
    String tooLongHost = "h".repeat(PSDatabaseDeliveryHostUpdate.MAX_DATABASE_HOST_LENGTH + 1);
    PSPubServer longHost = mysqlServer("old.example", "cmsdb");
    PSValidationException hostError =
        assertThrows(
            PSValidationException.class,
            () ->
                PSDatabaseDeliveryConnectionUpdate.apply(
                    longHost, connectionOnly(tooLongHost, "cmsdb", NEW_PASSWORD)));
    assertTrue(
        hostError.getMessage().contains(PSDatabaseDeliveryHostUpdate.DATABASE_HOST_TOO_LONG));
    assertUnchanged(longHost);

    String tooLongPassword =
        "p".repeat(PSDatabaseDeliveryPasswordUpdate.MAX_DATABASE_PASSWORD_LENGTH + 1);
    PSPubServer longPassword = mysqlServer("old.example", "cmsdb");
    PSValidationException passwordError =
        assertThrows(
            PSValidationException.class,
            () ->
                PSDatabaseDeliveryConnectionUpdate.apply(
                    longPassword, connectionOnly("db.example", "cmsdb", tooLongPassword)));
    assertTrue(
        passwordError
            .getMessage()
            .contains(PSDatabaseDeliveryPasswordUpdate.DATABASE_PASSWORD_TOO_LONG));
    assertFalse(passwordError.getMessage().contains(tooLongPassword));
    assertUnchanged(longPassword);
  }

  @Test
  void localAndFtpDriversDoNotChangeConnection() {
    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () ->
                PSDatabaseDeliveryConnectionUpdate.apply(
                    local, connectionOnly("db.example", "cmsdb", NEW_PASSWORD)));
    assertTrue(
        localError.getMessage().contains(PSDatabaseDeliveryHostUpdate.DATABASE_HOST_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME).isEmpty());

    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "ftp.example");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () ->
                PSDatabaseDeliveryConnectionUpdate.apply(
                    ftp, connectionOnly("db.example", "cmsdb", NEW_PASSWORD)));
    assertTrue(
        ftpError.getMessage().contains(PSDatabaseDeliveryHostUpdate.DATABASE_HOST_DRIVER));
    assertEquals("ftp.example", value(ftp, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
  }

  private static void assertUnchanged(PSPubServer server) {
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME));
    assertEquals("cmsdb", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("3306", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("rxdb", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
  }

  private static PSPubServer mysqlServer(String host, String userId) {
    PSPubServer server = new PSPubServer();
    server.setName("mysql-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MYSQL");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, host);
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "3306");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, userId);
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, "rxdb");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPublishServerInfo connectionOnly(String host, String userId, String password) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(
        List.of(
            property(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, host),
            property(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, userId),
            property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, password)));
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
