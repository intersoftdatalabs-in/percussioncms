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

/** Folder-only Local delivery-server update. Driver, name, and other properties must not change. */
class PSLocalDeliveryFolderUpdateTest {

  private static final String SERVER_NAME = "local-files";

  @Test
  void folderOnlyBodyIsOnlyFolder() {
    assertTrue(PSLocalDeliveryFolderUpdate.isFolderOnly(folderOnly("/night-local")));
    assertTrue(PSLocalDeliveryFolderUpdate.isFolderOnly(folderOnly("   ")));
    assertTrue(PSFtpDeliveryFolderUpdate.isFolderOnly(folderOnly("/night-local")));
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(folderOnly("/night-local")));
    assertFalse(PSFtpDeliveryPortUpdate.isPortOnly(folderOnly("/night-local")));
    assertFalse(PSFtpDeliveryUserUpdate.isUserOnly(folderOnly("/night-local")));
  }

  @Test
  void namedHostPortUserOrExtraPropertiesAreNotFolderOnly() {
    PSPublishServerInfo named = folderOnly("/night-local");
    named.setServerName("Prod");
    assertFalse(PSLocalDeliveryFolderUpdate.isFolderOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "ftp.example")));
    assertFalse(PSLocalDeliveryFolderUpdate.isFolderOnly(hostOnly));

    PSPublishServerInfo withDriver = folderOnly("/night-local");
    List<PSPublishServerProperty> props = new ArrayList<>(withDriver.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local"));
    withDriver.setProperties(props);
    assertFalse(PSLocalDeliveryFolderUpdate.isFolderOnly(withDriver));
    assertFalse(PSLocalDeliveryFolderUpdate.isFolderOnly(null));
  }

  @Test
  void applyReplacesFolderAndLeavesNameAndDriver() throws Exception {
    PSPubServer server = localServer("/kept/local");
    PSLocalDeliveryFolderUpdate.apply(server, folderOnly("  /night-local  "));
    assertEquals("/night-local", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("Local", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(SERVER_NAME, server.getName());
    assertEquals(2, server.getProperties().size());
  }

  @Test
  void blankFolderClearsStoredFolderAndLeavesTheRest() throws Exception {
    PSPubServer server = localServer("/kept/local");
    PSLocalDeliveryFolderUpdate.apply(server, folderOnly("   "));
    assertEquals("", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("Local", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(SERVER_NAME, server.getName());
    assertEquals(2, server.getProperties().size());
  }

  @Test
  void storedFolderIsTrimmedTextNotAFilesystemJoin() throws Exception {
    PSPubServer server = localServer("/kept/local");
    PSLocalDeliveryFolderUpdate.apply(server, folderOnly("  /publish/out  "));
    assertEquals("/publish/out", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
  }

  @Test
  void overlongFolderDoesNotWrite() {
    PSPubServer overlong = localServer("/kept/local");
    String tooLong = "/".repeat(PSLocalDeliveryFolderUpdate.MAX_LOCAL_FOLDER_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSLocalDeliveryFolderUpdate.apply(overlong, folderOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSLocalDeliveryFolderUpdate.LOCAL_FOLDER_TOO_LONG));
    assertUnchanged(overlong, "/kept/local");
  }

  @Test
  void nullInfoDoesNotClearFolder() {
    PSPubServer server = localServer("/kept/local");
    PSValidationException error =
        assertThrows(
            PSValidationException.class, () -> PSLocalDeliveryFolderUpdate.apply(server, null));
    assertTrue(error.getMessage().contains(PSLocalDeliveryFolderUpdate.LOCAL_FOLDER_MISSING));
    assertUnchanged(server, "/kept/local");
  }

  @Test
  void ftpDriverDoesNotChangeFolder() {
    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/ftp");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    PSValidationException error =
        assertThrows(
            PSValidationException.class,
            () -> PSLocalDeliveryFolderUpdate.apply(ftp, folderOnly("/night-local")));
    assertTrue(error.getMessage().contains(PSLocalDeliveryFolderUpdate.LOCAL_FOLDER_DRIVER));
    assertEquals("/kept/ftp", value(ftp, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("old.example", value(ftp, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals(3, ftp.getProperties().size());
  }

  @Test
  void ftpsSftpAndS3AreNotUpdated() {
    for (String driver : List.of("FTP", "FTPS", "SFTP", "AMAZONS3")) {
      PSPubServer server = localServer("/kept/local");
      server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
      assertThrows(
          PSValidationException.class,
          () -> PSLocalDeliveryFolderUpdate.apply(server, folderOnly("/night-local")));
      assertEquals("/kept/local", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
      assertEquals(SERVER_NAME, server.getName());
    }
  }

  private static void assertUnchanged(PSPubServer server, String folder) {
    assertEquals(folder, value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("Local", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(SERVER_NAME, server.getName());
  }

  private static PSPubServer localServer(String folder) {
    PSPubServer server = new PSPubServer();
    server.setName(SERVER_NAME);
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, folder);
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
