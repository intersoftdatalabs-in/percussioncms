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

/** User-id-only FTPS delivery-server update. Other properties must not change. */
class PSFtpsDeliveryUserUpdateTest {

  private static final String PASSWORD = "secret-pass";

  @Test
  void userOnlyBodyIsOnlyUserId() {
    assertTrue(PSFtpsDeliveryUserUpdate.isUserOnly(userOnly("cmsftps")));
    assertFalse(PSFtpsDeliveryHostUpdate.isHostOnly(userOnly("cmsftps")));
    assertFalse(PSFtpsDeliveryPortUpdate.isPortOnly(userOnly("cmsftps")));
  }

  @Test
  void namedHostPortOrExtraPropertiesAreNotUserOnly() {
    PSPublishServerInfo named = userOnly("cmsftps");
    named.setServerName("Prod");
    assertFalse(PSFtpsDeliveryUserUpdate.isUserOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "ftps.example")));
    assertFalse(PSFtpsDeliveryUserUpdate.isUserOnly(hostOnly));
    assertTrue(PSFtpsDeliveryHostUpdate.isHostOnly(hostOnly));

    PSPublishServerInfo portOnly = new PSPublishServerInfo();
    portOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "990")));
    assertFalse(PSFtpsDeliveryUserUpdate.isUserOnly(portOnly));
    assertTrue(PSFtpsDeliveryPortUpdate.isPortOnly(portOnly));

    PSPublishServerInfo withPassword = userOnly("cmsftps");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD));
    withPassword.setProperties(props);
    assertFalse(PSFtpsDeliveryUserUpdate.isUserOnly(withPassword));
    assertFalse(PSFtpsDeliveryUserUpdate.isUserOnly(null));
  }

  @Test
  void applyReplacesUserAndLeavesHostPortFolderAndPassword() throws Exception {
    PSPubServer server = ftpsServer("ftpsuser");
    PSFtpsDeliveryUserUpdate.apply(server, userOnly("  cmsftps  "));
    assertEquals("cmsftps", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("990", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("FTPS", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(6, server.getProperties().size());
  }

  @Test
  void blankOrOverlongUserDoesNotWrite() {
    PSPubServer blank = ftpsServer("ftpsuser");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSFtpsDeliveryUserUpdate.apply(blank, userOnly("   ")));
    assertTrue(blankError.getMessage().contains(PSFtpsDeliveryUserUpdate.FTPS_USER_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSWORD));
    assertUnchanged(blank, "ftpsuser");

    PSPubServer overlong = ftpsServer("ftpsuser");
    String tooLong = "u".repeat(PSFtpsDeliveryUserUpdate.MAX_FTPS_USER_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSFtpsDeliveryUserUpdate.apply(overlong, userOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSFtpsDeliveryUserUpdate.FTPS_USER_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
    assertUnchanged(overlong, "ftpsuser");
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
            () -> PSFtpsDeliveryUserUpdate.apply(local, userOnly("cmsftps")));
    assertTrue(error.getMessage().contains(PSFtpsDeliveryUserUpdate.FTPS_USER_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY).isEmpty());
    assertEquals(2, local.getProperties().size());
  }

  @Test
  void ftpSftpAndS3AreNotUpdated() {
    for (String driver : List.of("FTP", "SFTP", "AMAZONS3")) {
      PSPubServer server = ftpsServer("ftpsuser");
      server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
      assertThrows(
          PSValidationException.class,
          () -> PSFtpsDeliveryUserUpdate.apply(server, userOnly("cmsftps")));
      assertEquals("ftpsuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
      assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
      assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
      assertEquals("990", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    }
  }

  private static void assertUnchanged(PSPubServer server, String userId) {
    assertEquals(userId, value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("990", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer ftpsServer(String userId) {
    PSPubServer server = new PSPubServer();
    server.setName("ftps-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTPS");
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "990");
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
