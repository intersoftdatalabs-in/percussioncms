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

/** Folder-only FTP delivery-server update. Other properties must not change. */
class PSFtpDeliveryFolderUpdateTest {

  private static final String PASSWORD = "secret-pass";

  @Test
  void folderOnlyBodyIsOnlyFolder() {
    assertTrue(PSFtpDeliveryFolderUpdate.isFolderOnly(folderOnly("/remote/new")));
    assertTrue(PSFtpDeliveryFolderUpdate.isFolderOnly(folderOnly("   ")));
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(folderOnly("/remote/new")));
    assertFalse(PSFtpDeliveryPortUpdate.isPortOnly(folderOnly("/remote/new")));
    assertFalse(PSFtpDeliveryUserUpdate.isUserOnly(folderOnly("/remote/new")));
  }

  @Test
  void namedHostPortUserOrExtraPropertiesAreNotFolderOnly() {
    PSPublishServerInfo named = folderOnly("/remote/new");
    named.setServerName("Prod");
    assertFalse(PSFtpDeliveryFolderUpdate.isFolderOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "ftp.example")));
    assertFalse(PSFtpDeliveryFolderUpdate.isFolderOnly(hostOnly));
    assertTrue(PSFtpDeliveryHostUpdate.isHostOnly(hostOnly));

    PSPublishServerInfo portOnly = new PSPublishServerInfo();
    portOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "2222")));
    assertFalse(PSFtpDeliveryFolderUpdate.isFolderOnly(portOnly));
    assertTrue(PSFtpDeliveryPortUpdate.isPortOnly(portOnly));

    PSPublishServerInfo userOnly = new PSPublishServerInfo();
    userOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsftp")));
    assertFalse(PSFtpDeliveryFolderUpdate.isFolderOnly(userOnly));
    assertTrue(PSFtpDeliveryUserUpdate.isUserOnly(userOnly));

    PSPublishServerInfo withPassword = folderOnly("/remote/new");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD));
    withPassword.setProperties(props);
    assertFalse(PSFtpDeliveryFolderUpdate.isFolderOnly(withPassword));
    assertFalse(PSFtpDeliveryFolderUpdate.isFolderOnly(null));
  }

  @Test
  void applyReplacesFolderAndLeavesHostPortUserAndPassword() throws Exception {
    PSPubServer server = ftpServer("/kept/folder");
    PSFtpDeliveryFolderUpdate.apply(server, folderOnly("  /remote/new  "));
    assertEquals("/remote/new", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("2121", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("FTP", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(6, server.getProperties().size());
  }

  @Test
  void blankFolderClearsStoredFolderAndLeavesTheRest() throws Exception {
    PSPubServer server = ftpServer("/kept/folder");
    PSFtpDeliveryFolderUpdate.apply(server, folderOnly("   "));
    assertEquals("", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("2121", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("FTP", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(6, server.getProperties().size());
  }

  @Test
  void overlongFolderDoesNotWrite() {
    PSPubServer overlong = ftpServer("/kept/folder");
    String tooLong = "/".repeat(PSFtpDeliveryFolderUpdate.MAX_FTP_FOLDER_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSFtpDeliveryFolderUpdate.apply(overlong, folderOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSFtpDeliveryFolderUpdate.FTP_FOLDER_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
    assertUnchanged(overlong, "/kept/folder");
  }

  @Test
  void nullInfoDoesNotClearFolder() {
    PSPubServer server = ftpServer("/kept/folder");
    PSValidationException error =
        assertThrows(
            PSValidationException.class, () -> PSFtpDeliveryFolderUpdate.apply(server, null));
    assertTrue(error.getMessage().contains(PSFtpDeliveryFolderUpdate.FTP_FOLDER_MISSING));
    assertFalse(error.getMessage().contains(PASSWORD));
    assertUnchanged(server, "/kept/folder");
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
            () -> PSFtpDeliveryFolderUpdate.apply(local, folderOnly("/remote/new")));
    assertTrue(error.getMessage().contains(PSFtpDeliveryFolderUpdate.FTP_FOLDER_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(2, local.getProperties().size());
  }

  @Test
  void ftpsSftpAndS3AreNotUpdated() {
    for (String driver : List.of("FTPS", "SFTP", "AMAZONS3")) {
      PSPubServer server = ftpServer("/kept/folder");
      server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
      assertThrows(
          PSValidationException.class,
          () -> PSFtpDeliveryFolderUpdate.apply(server, folderOnly("/remote/new")));
      assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
      assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
      assertEquals("ftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
      assertEquals("2121", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    }
  }

  private static void assertUnchanged(PSPubServer server, String folder) {
    assertEquals(folder, value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("2121", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer ftpServer(String folder) {
    PSPubServer server = new PSPubServer();
    server.setName("ftp-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "2121");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "ftpuser");
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, folder);
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPublishServerInfo folderOnly(String folder) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, folder)));
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
