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

/** Host-only FTP delivery-server update. Other properties must not change. */
class PSFtpDeliveryHostUpdateTest {

  private static final String PASSWORD = "secret-pass";

  @Test
  void hostOnlyBodyIsOnlyServerIp() {
    assertTrue(PSFtpDeliveryHostUpdate.isHostOnly(hostOnly("ftp.example")));
  }

  @Test
  void namedOrExtraPropertiesAreNotHostOnly() {
    PSPublishServerInfo named = hostOnly("ftp.example");
    named.setServerName("Prod");
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(named));

    PSPublishServerInfo withPassword = hostOnly("ftp.example");
    List<PSPublishServerProperty> props = new ArrayList<>(withPassword.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD));
    withPassword.setProperties(props);
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(withPassword));
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(null));
  }

  @Test
  void applyReplacesHostAndLeavesPortUserFolderAndPassword() throws Exception {
    PSPubServer server = ftpServer("old.example");
    PSFtpDeliveryHostUpdate.apply(server, hostOnly("  ftp.new.example  "));
    assertEquals("ftp.new.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("2121", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("FTP", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(6, server.getProperties().size());
  }

  @Test
  void blankOrOverlongHostDoesNotWrite() {
    PSPubServer blank = ftpServer("old.example");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSFtpDeliveryHostUpdate.apply(blank, hostOnly("   ")));
    assertTrue(blankError.getMessage().contains(PSFtpDeliveryHostUpdate.FTP_HOST_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSWORD));
    assertUnchanged(blank, "old.example");

    PSPubServer overlong = ftpServer("old.example");
    String tooLong = "h".repeat(PSFtpDeliveryHostUpdate.MAX_FTP_HOST_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSFtpDeliveryHostUpdate.apply(overlong, hostOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSFtpDeliveryHostUpdate.FTP_HOST_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
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
            () -> PSFtpDeliveryHostUpdate.apply(local, hostOnly("ftp.example")));
    assertTrue(error.getMessage().contains(PSFtpDeliveryHostUpdate.FTP_HOST_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY).isEmpty());
    assertEquals(2, local.getProperties().size());
  }

  @Test
  void ftpsAndSftpAreNotUpdated() {
    for (String driver : List.of("FTPS", "SFTP")) {
      PSPubServer server = ftpServer("old.example");
      server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
      assertThrows(
          PSValidationException.class,
          () -> PSFtpDeliveryHostUpdate.apply(server, hostOnly("ftp.example")));
      assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
      assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
      assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    }
  }

  private static void assertUnchanged(PSPubServer server, String host) {
    assertEquals(host, value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("2121", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
  }

  private static PSPubServer ftpServer(String host) {
    PSPubServer server = new PSPubServer();
    server.setName("ftp-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, host);
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "2121");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "ftpuser");
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/folder");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSWORD);
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
