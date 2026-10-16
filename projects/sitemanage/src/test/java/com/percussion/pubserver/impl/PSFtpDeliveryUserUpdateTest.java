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

/** User-id-only FTP delivery-server update. Other properties must not change. */
class PSFtpDeliveryUserUpdateTest {

  private static final String PASSWORD = "secret-pass";

  @Test
  void userOnlyBodyIsOnlyUserId() {
    assertTrue(PSFtpDeliveryUserUpdate.isUserOnly(userOnly("cmsftp")));
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(userOnly("cmsftp")));
    assertFalse(PSFtpDeliveryPortUpdate.isPortOnly(userOnly("cmsftp")));
  }

  @Test
  void namedHostPortOrExtraPropertiesAreNotUserOnly() {
    PSPublishServerInfo named = userOnly("cmsftp");
    named.setServerName("Prod");
    assertFalse(PSFtpDeliveryUserUpdate.isUserOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "ftp.example")));
    assertFalse(PSFtpDeliveryUserUpdate.isUserOnly(hostOnly));
    assertTrue(PSFtpDeliveryHostUpdate.isHostOnly(hostOnly));

    PSPublishServerInfo portOnly = new PSPublishServerInfo();
    portOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "2222")));
    assertFalse(PSFtpDeliveryUserUpdate.isUserOnly(portOnly));
    assertTrue(PSFtpDeliveryPortUpdate.isPortOnly(portOnly));

    PSPublishServerInfo withPassword = userOnly("cmsftp");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD));
    withPassword.setProperties(props);
    assertFalse(PSFtpDeliveryUserUpdate.isUserOnly(withPassword));
    assertFalse(PSFtpDeliveryUserUpdate.isUserOnly(null));
  }

  @Test
  void applyReplacesUserAndLeavesHostPortFolderAndPassword() throws Exception {
    PSPubServer server = ftpServer("ftpuser");
    PSFtpDeliveryUserUpdate.apply(server, userOnly("  cmsftp  "));
    assertEquals("cmsftp", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("2121", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("FTP", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(6, server.getProperties().size());
  }

  @Test
  void blankOrOverlongUserDoesNotWrite() {
    PSPubServer blank = ftpServer("ftpuser");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class, () -> PSFtpDeliveryUserUpdate.apply(blank, userOnly("   ")));
    assertTrue(blankError.getMessage().contains(PSFtpDeliveryUserUpdate.FTP_USER_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSWORD));
    assertUnchanged(blank, "ftpuser");

    PSPubServer overlong = ftpServer("ftpuser");
    String tooLong = "u".repeat(PSFtpDeliveryUserUpdate.MAX_FTP_USER_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSFtpDeliveryUserUpdate.apply(overlong, userOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSFtpDeliveryUserUpdate.FTP_USER_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
    assertUnchanged(overlong, "ftpuser");
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
            () -> PSFtpDeliveryUserUpdate.apply(local, userOnly("cmsftp")));
    assertTrue(error.getMessage().contains(PSFtpDeliveryUserUpdate.FTP_USER_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY).isEmpty());
    assertEquals(2, local.getProperties().size());
  }

  @Test
  void ftpsSftpAndS3AreNotUpdated() {
    for (String driver : List.of("FTPS", "SFTP", "AMAZONS3")) {
      PSPubServer server = ftpServer("ftpuser");
      server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
      assertThrows(
          PSValidationException.class, () -> PSFtpDeliveryUserUpdate.apply(server, userOnly("cmsftp")));
      assertEquals("ftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
      assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
      assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
      assertEquals("2121", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    }
  }

  private static void assertUnchanged(PSPubServer server, String userId) {
    assertEquals(userId, value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("2121", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer ftpServer(String userId) {
    PSPubServer server = new PSPubServer();
    server.setName("ftp-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "2121");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, userId);
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/folder");
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
