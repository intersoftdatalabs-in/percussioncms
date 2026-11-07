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
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Password-only database delivery-server update. Other properties must not change, file drivers
 * must stay on the FTP path, and the stored secret must never leak into an error message.
 */
class PSDatabaseDeliveryPasswordUpdateTest {

  private static final String PASSWORD = "secret-pass";

  @Test
  void passwordOnlyBodyIsOnlyPassword() {
    assertTrue(PSDatabaseDeliveryPasswordUpdate.isPasswordOnly(passwordOnly("new-secret")));
    assertTrue(PSFtpDeliveryPasswordUpdate.isPasswordOnly(passwordOnly("new-secret")));
    assertFalse(PSDatabaseDeliveryUserUpdate.isUserOnly(passwordOnly("new-secret")));
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(passwordOnly("new-secret")));
    assertFalse(PSDatabaseDeliveryPortUpdate.isPortOnly(passwordOnly("new-secret")));
    assertFalse(PSDatabaseDeliveryPasswordUpdate.isPasswordOnly(null));
  }

  @Test
  void namedHostPortUserOrExtraPropertiesAreNotPasswordOnly() {
    PSPublishServerInfo named = passwordOnly("new-secret");
    named.setServerName("Prod");
    assertFalse(PSDatabaseDeliveryPasswordUpdate.isPasswordOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "db.example")));
    assertFalse(PSDatabaseDeliveryPasswordUpdate.isPasswordOnly(hostOnly));

    PSPublishServerInfo userOnly = new PSPublishServerInfo();
    userOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsdb")));
    assertFalse(PSDatabaseDeliveryPasswordUpdate.isPasswordOnly(userOnly));

    PSPublishServerInfo twoProps = passwordOnly("new-secret");
    List<PSPublishServerProperty> props = new ArrayList<>(twoProps.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsdb"));
    twoProps.setProperties(props);
    assertFalse(PSDatabaseDeliveryPasswordUpdate.isPasswordOnly(twoProps));
    assertFalse(PSDatabaseDeliveryPasswordUpdate.isPasswordOnly(null));
  }

  @Test
  void applyReplacesPasswordAndLeavesHostPortUserDatabaseAndDriver() throws Exception {
    for (String driver : List.of("MYSQL", "MYSQL8", "MSSQL", "ORACLE")) {
      PSPubServer server = databaseServer(driver, PASSWORD);
      PSDatabaseDeliveryPasswordUpdate.apply(server, passwordOnly("N3w-secret!"));
      assertEquals("N3w-secret!", value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
      assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
      assertEquals("3306", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
      assertEquals("cmsdb", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
      assertEquals("rxdb", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
      assertEquals(driver, value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
      assertEquals(6, server.getProperties().size());
    }
  }

  @Test
  void surroundingWhitespaceIsTrimmedByTheWireLayerButBlankDoesNotWrite() throws Exception {
    PSPubServer spaced = databaseServer("MYSQL", PASSWORD);
    PSDatabaseDeliveryPasswordUpdate.apply(spaced, passwordOnly("  padded  "));
    assertEquals("padded", value(spaced, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));

    for (String blank : List.of("", "   ")) {
      PSPubServer server = databaseServer("MYSQL", PASSWORD);
      PSValidationException blankError =
          assertThrows(
              PSValidationException.class,
              () -> PSDatabaseDeliveryPasswordUpdate.apply(server, passwordOnly(blank)));
      assertTrue(
          blankError.getMessage().contains(PSDatabaseDeliveryPasswordUpdate.DATABASE_PASSWORD_REQUIRED));
      assertFalse(blankError.getMessage().contains(PASSWORD));
      assertUnchanged(server);
    }
  }

  @Test
  void overlongPasswordDoesNotWrite() {
    PSPubServer server = databaseServer("MYSQL", PASSWORD);
    String tooLong = "p".repeat(PSDatabaseDeliveryPasswordUpdate.MAX_DATABASE_PASSWORD_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSDatabaseDeliveryPasswordUpdate.apply(server, passwordOnly(tooLong)));
    assertTrue(
        longError.getMessage().contains(PSDatabaseDeliveryPasswordUpdate.DATABASE_PASSWORD_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
    assertFalse(longError.getMessage().contains(tooLong));
    assertUnchanged(server);
  }

  @Test
  void maskedPlaceholderIsNeverPersisted() {
    PSPubServer plain = databaseServer("MYSQL", PASSWORD);
    PSValidationException plainError =
        assertThrows(
            PSValidationException.class,
            () ->
                PSDatabaseDeliveryPasswordUpdate.apply(
                    plain, passwordOnly(PSPubServerService.PASSWORD_ENTRY)));
    assertTrue(
        plainError.getMessage().contains(PSDatabaseDeliveryPasswordUpdate.DATABASE_PASSWORD_REQUIRED));
    assertUnchanged(plain);

    PSPubServer encoded = databaseServer("MYSQL", PASSWORD);
    String encodedPlaceholder =
        Base64.getEncoder().encodeToString(PSPubServerService.PASSWORD_ENTRY.getBytes());
    // updateDeliveryPasswordOnly base64-decodes before apply (product norm); simulate that
    // ordering.
    String decodedPlaceholder = new String(Base64.getDecoder().decode(encodedPlaceholder));
    PSValidationException encodedError =
        assertThrows(
            PSValidationException.class,
            () ->
                PSDatabaseDeliveryPasswordUpdate.apply(encoded, passwordOnly(decodedPlaceholder)));
    assertFalse(encodedError.getMessage().contains(PASSWORD));
    assertUnchanged(encoded);
  }

  @Test
  void nonDatabaseDriversAreNotUpdated() {
    for (String driver : List.of("FTP", "Local", "FTPS", "SFTP", "AMAZONS3")) {
      PSPubServer server = databaseServer("MYSQL", PASSWORD);
      server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
      assertThrows(
          PSValidationException.class,
          () -> PSDatabaseDeliveryPasswordUpdate.apply(server, passwordOnly("N3w-secret!")));
      assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
      assertEquals("cmsdb", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    }
  }

  private static void assertUnchanged(PSPubServer server) {
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("cmsdb", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("3306", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("rxdb", value(server, IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
  }

  private static PSPubServer databaseServer(String driver, String password) {
    PSPubServer server = new PSPubServer();
    server.setName("mysql-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "3306");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsdb");
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, "rxdb");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, password);
    return server;
  }

  private static PSPublishServerInfo passwordOnly(String password) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, password)));
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
