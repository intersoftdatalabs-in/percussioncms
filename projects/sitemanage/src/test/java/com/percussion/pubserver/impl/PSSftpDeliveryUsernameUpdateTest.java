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

/** Username-only SFTP delivery-server update. Other properties must not change. */
class PSSftpDeliveryUsernameUpdateTest {

  private static final String PASSPHRASE = "secret-passphrase";
  private static final String PRIVATE_KEY = "stored-private-key";

  @Test
  void usernameOnlyBodyIsOnlyUserid() {
    assertTrue(PSSftpDeliveryUsernameUpdate.isUsernameOnly(usernameOnly("sftpuser")));
    assertTrue(PSFtpDeliveryUserUpdate.isUserOnly(usernameOnly("sftpuser")));
    assertFalse(PSSftpDeliveryHostUpdate.isHostOnly(usernameOnly("sftpuser")));
  }

  @Test
  void namedServerOrExtraPropertiesAreNotUsernameOnly() {
    PSPublishServerInfo named = usernameOnly("sftpuser");
    named.setServerName("Sftp Prod");
    assertFalse(PSSftpDeliveryUsernameUpdate.isUsernameOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "sftp.example")));
    assertFalse(PSSftpDeliveryUsernameUpdate.isUsernameOnly(hostOnly));
    assertTrue(PSSftpDeliveryHostUpdate.isHostOnly(hostOnly));

    PSPublishServerInfo withPassword = usernameOnly("sftpuser");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSPHRASE));
    withPassword.setProperties(props);
    assertFalse(PSSftpDeliveryUsernameUpdate.isUsernameOnly(withPassword));
    assertFalse(PSSftpDeliveryUsernameUpdate.isUsernameOnly(null));
  }

  @Test
  void applyReplacesUsernameAndLeavesHostPortFolderPassphraseAndKey() throws Exception {
    PSPubServer server = sftpServer("sftpuser");
    PSSftpDeliveryUsernameUpdate.apply(server, usernameOnly("  nightsftp  "));
    assertEquals("nightsftp", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("22", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    assertEquals("SFTP", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(7, server.getProperties().size());
  }

  @Test
  void blankOrOverlongUsernameDoesNotWrite() {
    PSPubServer blank = sftpServer("sftpuser");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryUsernameUpdate.apply(blank, usernameOnly("   ")));
    assertTrue(blankError.getMessage().contains(PSSftpDeliveryUsernameUpdate.SFTP_USERNAME_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSPHRASE));
    assertFalse(blankError.getMessage().contains(PRIVATE_KEY));
    assertUnchanged(blank, "sftpuser");

    PSPubServer overlong = sftpServer("sftpuser");
    String tooLong = "u".repeat(PSSftpDeliveryUsernameUpdate.MAX_SFTP_USERNAME_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryUsernameUpdate.apply(overlong, usernameOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSSftpDeliveryUsernameUpdate.SFTP_USERNAME_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSPHRASE));
    assertFalse(longError.getMessage().contains(PRIVATE_KEY));
    assertUnchanged(overlong, "sftpuser");
  }

  @Test
  void localDriverDoesNotChangeFolder() {
    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException error =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryUsernameUpdate.apply(local, usernameOnly("nightsftp")));
    assertTrue(error.getMessage().contains(PSSftpDeliveryUsernameUpdate.SFTP_USERNAME_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY).isEmpty());
    assertEquals(2, local.getProperties().size());
  }

  @Test
  void ftpFtpsAndDatabaseDriversAreNotUpdated() {
    for (String driver : List.of("FTP", "FTPS", "MYSQL", "MSSQL", "ORACLE")) {
      PSPubServer server = sftpServer("sftpuser");
      server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
      assertThrows(
          PSValidationException.class,
          () -> PSSftpDeliveryUsernameUpdate.apply(server, usernameOnly("nightsftp")));
      assertEquals("sftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
      assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
      assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
      assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    }
  }

  private static void assertUnchanged(PSPubServer server, String username) {
    assertEquals(username, value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("22", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
  }

  private static PSPubServer sftpServer(String username) {
    PSPubServer server = new PSPubServer();
    server.setName("sftp-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "SFTP");
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "22");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, username);
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/folder");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSPHRASE);
    server.addProperty(IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY, PRIVATE_KEY);
    return server;
  }

  private static PSPublishServerInfo usernameOnly(String username) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, username)));
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
