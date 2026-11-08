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

/** Host-only FTPS delivery-server update. Other properties must not change. */
class PSFtpsDeliveryHostUpdateTest {

  private static final String PASSPHRASE = "secret-passphrase";
  private static final String PRIVATE_KEY = "stored-private-key";

  @Test
  void hostOnlyBodyIsOnlyServerIp() {
    assertTrue(PSFtpsDeliveryHostUpdate.isHostOnly(hostOnly("ftps.example")));
  }

  @Test
  void namedOrExtraPropertiesAreNotHostOnly() {
    PSPublishServerInfo named = hostOnly("ftps.example");
    named.setServerName("Ftps Prod");
    assertFalse(PSFtpsDeliveryHostUpdate.isHostOnly(named));

    PSPublishServerInfo withPort = hostOnly("ftps.example");
    List<PSPublishServerProperty> props = new ArrayList<>(withPort.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "990"));
    withPort.setProperties(props);
    assertFalse(PSFtpsDeliveryHostUpdate.isHostOnly(withPort));
    assertFalse(PSFtpsDeliveryHostUpdate.isHostOnly(null));
  }

  @Test
  void applyReplacesHostAndLeavesPortUserFolderPassphraseAndKey() throws Exception {
    PSPubServer server = ftpsServer("old.example");
    PSFtpsDeliveryHostUpdate.apply(server, hostOnly("  ftps.new.example  "));
    assertEquals("ftps.new.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("990", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ftpsuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    assertEquals("FTPS", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(7, server.getProperties().size());
  }

  @Test
  void blankOrOverlongHostDoesNotWrite() {
    PSPubServer blank = ftpsServer("old.example");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSFtpsDeliveryHostUpdate.apply(blank, hostOnly("   ")));
    assertTrue(blankError.getMessage().contains(PSFtpsDeliveryHostUpdate.FTPS_HOST_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSPHRASE));
    assertFalse(blankError.getMessage().contains(PRIVATE_KEY));
    assertUnchanged(blank, "old.example");

    PSPubServer overlong = ftpsServer("old.example");
    String tooLong = "h".repeat(PSFtpsDeliveryHostUpdate.MAX_FTPS_HOST_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSFtpsDeliveryHostUpdate.apply(overlong, hostOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSFtpsDeliveryHostUpdate.FTPS_HOST_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSPHRASE));
    assertFalse(longError.getMessage().contains(PRIVATE_KEY));
    assertUnchanged(overlong, "old.example");
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
            () -> PSFtpsDeliveryHostUpdate.apply(local, hostOnly("ftps.example")));
    assertTrue(error.getMessage().contains(PSFtpsDeliveryHostUpdate.FTPS_HOST_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY).isEmpty());
    assertEquals(2, local.getProperties().size());
  }

  @Test
  void ftpAndSftpAreNotUpdated() {
    for (String driver : List.of("FTP", "SFTP")) {
      PSPubServer server = ftpsServer("old.example");
      server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
      assertThrows(
          PSValidationException.class,
          () -> PSFtpsDeliveryHostUpdate.apply(server, hostOnly("ftps.example")));
      assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
      assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
      assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
      assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    }
  }

  private static void assertUnchanged(PSPubServer server, String host) {
    assertEquals(host, value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("990", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ftpsuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
  }

  private static PSPubServer ftpsServer(String host) {
    PSPubServer server = new PSPubServer();
    server.setName("ftps-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTPS");
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, host);
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "990");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "ftpsuser");
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/folder");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSPHRASE);
    server.addProperty(IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY, PRIVATE_KEY);
    return server;
  }

  private static PSPublishServerInfo hostOnly(String host) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, host)));
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
