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

/** Port-only SFTP delivery-server update. Other properties must not change. */
class PSSftpDeliveryPortUpdateTest {

  private static final String PASSPHRASE = "secret-passphrase";
  private static final String PRIVATE_KEY = "stored-private-key";

  @Test
  void portOnlyBodyIsOnlyPort() {
    assertTrue(PSSftpDeliveryPortUpdate.isPortOnly(portOnly("2222")));
    assertTrue(PSFtpDeliveryPortUpdate.isPortOnly(portOnly("2222")));
    assertFalse(PSSftpDeliveryHostUpdate.isHostOnly(portOnly("2222")));
  }

  @Test
  void namedHostOrExtraPropertiesAreNotPortOnly() {
    PSPublishServerInfo named = portOnly("2222");
    named.setServerName("Sftp Prod");
    assertFalse(PSSftpDeliveryPortUpdate.isPortOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "sftp.example")));
    assertFalse(PSSftpDeliveryPortUpdate.isPortOnly(hostOnly));
    assertTrue(PSSftpDeliveryHostUpdate.isHostOnly(hostOnly));

    PSPublishServerInfo withPassword = portOnly("2222");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSPHRASE));
    withPassword.setProperties(props);
    assertFalse(PSSftpDeliveryPortUpdate.isPortOnly(withPassword));
    assertFalse(PSSftpDeliveryPortUpdate.isPortOnly(null));
  }

  @Test
  void applyReplacesPortAndLeavesHostUserFolderPassphraseAndKey() throws Exception {
    PSPubServer server = sftpServer("22");
    PSSftpDeliveryPortUpdate.apply(server, portOnly("  2222  "));
    assertEquals("2222", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("sftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    assertEquals("SFTP", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(7, server.getProperties().size());
  }

  @Test
  void blankNonNumericOrOverlongPortDoesNotWrite() {
    PSPubServer blank = sftpServer("22");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPortUpdate.apply(blank, portOnly("   ")));
    assertTrue(blankError.getMessage().contains(PSSftpDeliveryPortUpdate.SFTP_PORT_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSPHRASE));
    assertFalse(blankError.getMessage().contains(PRIVATE_KEY));
    assertUnchanged(blank, "22");

    for (String bad : List.of("21a", "21.5", "-1", "22 22")) {
      PSPubServer server = sftpServer("22");
      PSValidationException error =
          assertThrows(
              PSValidationException.class,
              () -> PSSftpDeliveryPortUpdate.apply(server, portOnly(bad)));
      assertTrue(error.getMessage().contains(PSSftpDeliveryPortUpdate.SFTP_PORT_NOT_NUMERIC));
      assertFalse(error.getMessage().contains(PASSPHRASE));
      assertFalse(error.getMessage().contains(PRIVATE_KEY));
      assertUnchanged(server, "22");
    }

    PSPubServer overlong = sftpServer("22");
    String tooLong = "9".repeat(PSSftpDeliveryPortUpdate.MAX_SFTP_PORT_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPortUpdate.apply(overlong, portOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSSftpDeliveryPortUpdate.SFTP_PORT_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSPHRASE));
    assertFalse(longError.getMessage().contains(PRIVATE_KEY));
    assertUnchanged(overlong, "22");
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
            () -> PSSftpDeliveryPortUpdate.apply(local, portOnly("2222")));
    assertTrue(error.getMessage().contains(PSSftpDeliveryPortUpdate.SFTP_PORT_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_PORT_PROPERTY).isEmpty());
    assertEquals(2, local.getProperties().size());
  }

  @Test
  void ftpAndFtpsAreNotUpdated() {
    for (String driver : List.of("FTP", "FTPS")) {
      PSPubServer server = sftpServer("22");
      server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
      assertThrows(
          PSValidationException.class,
          () -> PSSftpDeliveryPortUpdate.apply(server, portOnly("2222")));
      assertEquals("22", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
      assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
      assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
      assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    }
  }

  private static void assertUnchanged(PSPubServer server, String port) {
    assertEquals(port, value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("sftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
  }

  private static PSPubServer sftpServer(String port) {
    PSPubServer server = new PSPubServer();
    server.setName("sftp-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "SFTP");
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, port);
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "sftpuser");
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/folder");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSPHRASE);
    server.addProperty(IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY, PRIVATE_KEY);
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
