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

/** Folder-only FTPS delivery-server update. Other properties must not change. */
class PSFtpsDeliveryFolderUpdateTest {

  private static final String PASSWORD = "secret-pass";

  @Test
  void folderOnlyBodyIsOnlyFolder() {
    assertTrue(PSFtpsDeliveryFolderUpdate.isFolderOnly(folderOnly("/remote/new")));
    assertTrue(PSFtpsDeliveryFolderUpdate.isFolderOnly(folderOnly("   ")));
    assertFalse(PSFtpsDeliveryHostUpdate.isHostOnly(folderOnly("/remote/new")));
    assertFalse(PSFtpsDeliveryPortUpdate.isPortOnly(folderOnly("/remote/new")));
    assertFalse(PSFtpsDeliveryUserUpdate.isUserOnly(folderOnly("/remote/new")));
  }

  @Test
  void namedHostPortOrExtraPropertiesAreNotFolderOnly() {
    PSPublishServerInfo named = folderOnly("/remote/new");
    named.setServerName("Ftps Prod");
    assertFalse(PSFtpsDeliveryFolderUpdate.isFolderOnly(named));

    PSPublishServerInfo withHost = folderOnly("/remote/new");
    List<PSPublishServerProperty> props = new ArrayList<>(withHost.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example"));
    withHost.setProperties(props);
    assertFalse(PSFtpsDeliveryFolderUpdate.isFolderOnly(withHost));

    PSPublishServerInfo userOnly = new PSPublishServerInfo();
    userOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "ftpsuser")));
    assertFalse(PSFtpsDeliveryFolderUpdate.isFolderOnly(userOnly));
    assertTrue(PSFtpsDeliveryUserUpdate.isUserOnly(userOnly));

    assertFalse(PSFtpsDeliveryFolderUpdate.isFolderOnly(null));
  }

  @Test
  void applyReplacesFolderAndLeavesHostPortUserAndPassword() throws Exception {
    PSPubServer server = ftpsServer("/kept/folder");
    PSFtpsDeliveryFolderUpdate.apply(server, folderOnly("  /remote/new  "));
    assertEquals("/remote/new", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("990", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ftpsuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("FTPS", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(6, server.getProperties().size());
  }

  @Test
  void blankFolderClearsTheStoredFolder() throws Exception {
    PSPubServer server = ftpsServer("/kept/folder");
    PSFtpsDeliveryFolderUpdate.apply(server, folderOnly("   "));
    assertEquals("", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("990", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ftpsuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  @Test
  void nullBodyDoesNotClearTheStoredFolder() {
    PSPubServer server = ftpsServer("/kept/folder");
    PSValidationException error =
        assertThrows(
            PSValidationException.class, () -> PSFtpsDeliveryFolderUpdate.apply(server, null));
    assertTrue(error.getMessage().contains(PSFtpsDeliveryFolderUpdate.FTPS_FOLDER_MISSING));
    assertFalse(error.getMessage().contains(PASSWORD));
    assertUnchanged(server, "/kept/folder");
  }

  @Test
  void overlongFolderDoesNotWrite() {
    PSPubServer server = ftpsServer("/kept/folder");
    String tooLong = "/".repeat(PSFtpsDeliveryFolderUpdate.MAX_FTPS_FOLDER_LENGTH + 1);
    PSValidationException error =
        assertThrows(
            PSValidationException.class,
            () -> PSFtpsDeliveryFolderUpdate.apply(server, folderOnly(tooLong)));
    assertTrue(error.getMessage().contains(PSFtpsDeliveryFolderUpdate.FTPS_FOLDER_TOO_LONG));
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
            () -> PSFtpsDeliveryFolderUpdate.apply(local, folderOnly("/remote/new")));
    assertTrue(error.getMessage().contains(PSFtpsDeliveryFolderUpdate.FTPS_FOLDER_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(2, local.getProperties().size());
  }

  @Test
  void ftpSftpAndS3AreNotUpdated() {
    for (String driver : List.of("FTP", "SFTP", "AMAZONS3")) {
      PSPubServer server = ftpsServer("/kept/folder");
      server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
      assertThrows(
          PSValidationException.class,
          () -> PSFtpsDeliveryFolderUpdate.apply(server, folderOnly("/remote/new")));
      assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
      assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
      assertEquals("ftpsuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
      assertEquals("990", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    }
  }

  private static void assertUnchanged(PSPubServer server, String folder) {
    assertEquals(folder, value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("990", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ftpsuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer ftpsServer(String folder) {
    PSPubServer server = new PSPubServer();
    server.setName("ftps-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTPS");
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "990");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "ftpsuser");
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
