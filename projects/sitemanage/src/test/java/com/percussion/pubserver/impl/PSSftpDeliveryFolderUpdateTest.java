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

/** Folder-only SFTP delivery-server update. Other properties must not change. */
class PSSftpDeliveryFolderUpdateTest {

  private static final String PASSPHRASE = "secret-passphrase";
  private static final String PRIVATE_KEY = "stored-private-key";

  @Test
  void folderOnlyBodyIsOnlyFolder() {
    assertTrue(PSSftpDeliveryFolderUpdate.isFolderOnly(folderOnly("/remote/new")));
    assertTrue(PSSftpDeliveryFolderUpdate.isFolderOnly(folderOnly("   ")));
  }

  @Test
  void namedOrExtraPropertiesAreNotFolderOnly() {
    PSPublishServerInfo named = folderOnly("/remote/new");
    named.setServerName("Sftp Prod");
    assertFalse(PSSftpDeliveryFolderUpdate.isFolderOnly(named));

    PSPublishServerInfo withHost = folderOnly("/remote/new");
    List<PSPublishServerProperty> props = new ArrayList<>(withHost.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "sftp.example"));
    withHost.setProperties(props);
    assertFalse(PSSftpDeliveryFolderUpdate.isFolderOnly(withHost));
    assertFalse(PSSftpDeliveryFolderUpdate.isFolderOnly(null));
  }

  @Test
  void applyReplacesFolderAndLeavesHostPortUserPassphraseAndKey() throws Exception {
    PSPubServer server = sftpServer("/kept/folder");
    PSSftpDeliveryFolderUpdate.apply(server, folderOnly("  /remote/new  "));
    assertEquals("/remote/new", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("sftp.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("22", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("sftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    assertEquals("SFTP", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(7, server.getProperties().size());
  }

  @Test
  void blankFolderClearsTheStoredFolder() throws Exception {
    PSPubServer server = sftpServer("/kept/folder");
    PSSftpDeliveryFolderUpdate.apply(server, folderOnly("   "));
    assertEquals("", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("sftp.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
  }

  @Test
  void overlongFolderDoesNotWrite() {
    PSPubServer server = sftpServer("/kept/folder");
    String tooLong = "/".repeat(PSSftpDeliveryFolderUpdate.MAX_SFTP_FOLDER_LENGTH + 1);
    PSValidationException error =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryFolderUpdate.apply(server, folderOnly(tooLong)));
    assertTrue(error.getMessage().contains(PSSftpDeliveryFolderUpdate.SFTP_FOLDER_TOO_LONG));
    assertFalse(error.getMessage().contains(PASSPHRASE));
    assertFalse(error.getMessage().contains(PRIVATE_KEY));
    assertUnchanged(server);
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
            () -> PSSftpDeliveryFolderUpdate.apply(local, folderOnly("/remote/new")));
    assertTrue(error.getMessage().contains(PSSftpDeliveryFolderUpdate.SFTP_FOLDER_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(2, local.getProperties().size());
  }

  @Test
  void ftpFtpsAndDatabaseAreNotUpdated() {
    for (String driver : List.of("FTP", "FTPS", "MYSQL")) {
      PSPubServer server = sftpServer("/kept/folder");
      server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
      assertThrows(
          PSValidationException.class,
          () -> PSSftpDeliveryFolderUpdate.apply(server, folderOnly("/remote/new")));
      assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
      assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
      assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    }
  }

  private static void assertUnchanged(PSPubServer server) {
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("sftp.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("22", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("sftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
  }

  private static PSPubServer sftpServer(String folder) {
    PSPubServer server = new PSPubServer();
    server.setName("sftp-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "SFTP");
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "sftp.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "22");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "sftpuser");
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, folder);
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSPHRASE);
    server.addProperty(IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY, PRIVATE_KEY);
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
