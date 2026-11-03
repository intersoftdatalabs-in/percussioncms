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
import java.util.List;
import org.junit.jupiter.api.Test;

/** Name-only delivery-server update. Driver, folder, type, default flag, and properties stay. */
class PSDeliveryServerRenameTest {

  private static final String PASSWORD = "secret-pass";

  @Test
  void nameOnlyBodyIsOnlyServerName() {
    assertTrue(PSDeliveryServerRename.isNameOnly(nameOnly("NightLocal")));
    assertTrue(PSDeliveryServerRename.isNameOnly(nameOnly("   ")));
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(nameOnly("NightLocal")));
    assertFalse(PSLocalDeliveryFolderUpdate.isFolderOnly(nameOnly("NightLocal")));
  }

  @Test
  void typeDefaultOrPropertiesAreNotNameOnly() {
    PSPublishServerInfo typed = nameOnly("NightLocal");
    typed.setType("File");
    assertFalse(PSDeliveryServerRename.isNameOnly(typed));

    PSPublishServerInfo flagged = nameOnly("NightLocal");
    flagged.setIsDefault(Boolean.TRUE);
    assertFalse(PSDeliveryServerRename.isNameOnly(flagged));

    PSPublishServerInfo withFolder = nameOnly("NightLocal");
    withFolder.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local")));
    assertFalse(PSDeliveryServerRename.isNameOnly(withFolder));
    assertFalse(PSDeliveryServerRename.isNameOnly(null));

    PSPublishServerInfo missingName = new PSPublishServerInfo();
    assertFalse(PSDeliveryServerRename.isNameOnly(missingName));
  }

  @Test
  void applyReplacesNameAndLeavesDriverFolderDefaultAndPassword() throws Exception {
    PSPubServer server = ftpServer("ftp-prod");
    String name = PSDeliveryServerRename.normalizedName(nameOnly("  Night-FTP  "));
    PSDeliveryServerRename.apply(server, name);
    assertEquals("Night-FTP", server.getName());
    assertUnchangedProperties(server);
  }

  @Test
  void blankOverlongAndInvalidNamesDoNotWrite() {
    PSPubServer blank = ftpServer("ftp-prod");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSDeliveryServerRename.normalizedName(nameOnly("   ")));
    assertTrue(blankError.getMessage().contains(PSDeliveryServerRename.NAME_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSWORD));
    assertEquals("ftp-prod", blank.getName());
    assertUnchangedProperties(blank);

    PSPubServer overlong = ftpServer("ftp-prod");
    String tooLong = "n".repeat(PSDeliveryServerRename.MAX_NAME_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSDeliveryServerRename.normalizedName(nameOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSDeliveryServerRename.NAME_TOO_LONG));
    assertEquals("ftp-prod", overlong.getName());

    PSPubServer invalid = ftpServer("ftp-prod");
    PSValidationException invalidError =
        assertThrows(
            PSValidationException.class,
            () -> PSDeliveryServerRename.normalizedName(nameOnly("has space")));
    assertTrue(invalidError.getMessage().contains(PSDeliveryServerRename.NAME_INVALID));
    assertEquals("ftp-prod", invalid.getName());
    assertUnchangedProperties(invalid);
  }

  @Test
  void localDriverRenameDoesNotTouchFolder() throws Exception {
    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    String name = PSDeliveryServerRename.normalizedName(nameOnly("local-renamed"));
    PSDeliveryServerRename.apply(local, name);
    assertEquals("local-renamed", local.getName());
    assertEquals("Local", value(local, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(2, local.getProperties().size());
  }

  private static void assertUnchangedProperties(PSPubServer server) {
    assertEquals("FTP", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("2121", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer ftpServer(String name) {
    PSPubServer server = new PSPubServer();
    server.setName(name);
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "2121");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "ftpuser");
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/folder");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
    return server;
  }

  private static PSPublishServerInfo nameOnly(String name) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setServerName(name);
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
