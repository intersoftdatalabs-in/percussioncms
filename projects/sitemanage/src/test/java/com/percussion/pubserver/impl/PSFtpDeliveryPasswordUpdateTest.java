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
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Password-only FTP delivery-server update. Other properties must not change and the stored
 * secret must never leak into an error message.
 */
class PSFtpDeliveryPasswordUpdateTest {

  private static final String PASSWORD = "secret-pass";

  @Test
  void passwordOnlyBodyIsOnlyPassword() {
    assertTrue(PSFtpDeliveryPasswordUpdate.isPasswordOnly(passwordOnly("new-secret")));
    assertFalse(PSFtpDeliveryUserUpdate.isUserOnly(passwordOnly("new-secret")));
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(passwordOnly("new-secret")));
    assertFalse(PSFtpDeliveryPortUpdate.isPortOnly(passwordOnly("new-secret")));
    assertFalse(PSFtpDeliveryFolderUpdate.isFolderOnly(passwordOnly("new-secret")));
  }

  @Test
  void namedHostPortUserOrExtraPropertiesAreNotPasswordOnly() {
    PSPublishServerInfo named = passwordOnly("new-secret");
    named.setServerName("Prod");
    assertFalse(PSFtpDeliveryPasswordUpdate.isPasswordOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "ftp.example")));
    assertFalse(PSFtpDeliveryPasswordUpdate.isPasswordOnly(hostOnly));
    assertTrue(PSFtpDeliveryHostUpdate.isHostOnly(hostOnly));

    PSPublishServerInfo userOnly = new PSPublishServerInfo();
    userOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsftp")));
    assertFalse(PSFtpDeliveryPasswordUpdate.isPasswordOnly(userOnly));

    PSPublishServerInfo twoProps = passwordOnly("new-secret");
    List<PSPublishServerProperty> props = new ArrayList<>(twoProps.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "cmsftp"));
    twoProps.setProperties(props);
    assertFalse(PSFtpDeliveryPasswordUpdate.isPasswordOnly(twoProps));
    assertFalse(PSFtpDeliveryPasswordUpdate.isPasswordOnly(null));
  }

  @Test
  void applyReplacesPasswordAndLeavesHostPortUserFolderAndDriver() throws Exception {
    PSPubServer server = ftpServer(PASSWORD);
    PSFtpDeliveryPasswordUpdate.apply(server, passwordOnly("N3w-secret!"));
    assertEquals("N3w-secret!", value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("2121", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("ftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("FTP", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals(6, server.getProperties().size());
  }

  @Test
  void surroundingWhitespaceIsTrimmedByTheWireLayerButBlankDoesNotWrite() throws Exception {
    PSPubServer spaced = ftpServer(PASSWORD);
    PSFtpDeliveryPasswordUpdate.apply(spaced, passwordOnly("  padded  "));
    assertEquals("padded", value(spaced, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));

    for (String blank : List.of("", "   ")) {
      PSPubServer server = ftpServer(PASSWORD);
      PSValidationException blankError =
          assertThrows(
              PSValidationException.class,
              () -> PSFtpDeliveryPasswordUpdate.apply(server, passwordOnly(blank)));
      assertTrue(blankError.getMessage().contains(PSFtpDeliveryPasswordUpdate.FTP_PASSWORD_REQUIRED));
      assertFalse(blankError.getMessage().contains(PASSWORD));
      assertUnchanged(server);
    }
  }

  @Test
  void overlongPasswordDoesNotWrite() {
    PSPubServer server = ftpServer(PASSWORD);
    String tooLong = "p".repeat(PSFtpDeliveryPasswordUpdate.MAX_FTP_PASSWORD_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSFtpDeliveryPasswordUpdate.apply(server, passwordOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSFtpDeliveryPasswordUpdate.FTP_PASSWORD_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSWORD));
    assertFalse(longError.getMessage().contains(tooLong));
    assertUnchanged(server);
  }

  @Test
  void maskedPlaceholderIsNeverPersisted() {
    PSPubServer plain = ftpServer(PASSWORD);
    PSValidationException plainError =
        assertThrows(
            PSValidationException.class,
            () ->
                PSFtpDeliveryPasswordUpdate.apply(
                    plain, passwordOnly(PSPubServerService.PASSWORD_ENTRY)));
    assertTrue(plainError.getMessage().contains(PSFtpDeliveryPasswordUpdate.FTP_PASSWORD_REQUIRED));
    assertUnchanged(plain);

    PSPubServer encoded = ftpServer(PASSWORD);
    String encodedPlaceholder =
        Base64.getEncoder().encodeToString(PSPubServerService.PASSWORD_ENTRY.getBytes());
    // updateDeliveryPasswordOnly base64-decodes before apply (product norm); simulate that ordering.
    String decodedPlaceholder = new String(Base64.getDecoder().decode(encodedPlaceholder));
    PSValidationException encodedError =
        assertThrows(
            PSValidationException.class,
            () -> PSFtpDeliveryPasswordUpdate.apply(encoded, passwordOnly(decodedPlaceholder)));
    assertFalse(encodedError.getMessage().contains(PASSWORD));
    assertUnchanged(encoded);
  }

  @Test
  void nonFtpDriversAreNotUpdated() {
    for (String driver : List.of("Local", "FTPS", "SFTP", "AMAZONS3", "MYSQL", "Oracle")) {
      PSPubServer server = ftpServer(PASSWORD);
      server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, driver);
      assertThrows(
          PSValidationException.class,
          () -> PSFtpDeliveryPasswordUpdate.apply(server, passwordOnly("N3w-secret!")));
      assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
      assertEquals("ftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    }
  }

  private static void assertUnchanged(PSPubServer server) {
    assertEquals(PASSWORD, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("ftpuser", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("2121", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
  }

  private static PSPubServer ftpServer(String password) {
    PSPubServer server = new PSPubServer();
    server.setName("ftp-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "2121");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "ftpuser");
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/folder");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, password);
    return server;
  }

  private static PSPublishServerInfo passwordOnly(String password) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, password)));
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
