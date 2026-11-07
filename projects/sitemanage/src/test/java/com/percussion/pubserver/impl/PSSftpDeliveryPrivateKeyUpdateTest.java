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

/**
 * Private-key-only SFTP delivery-server update. Other properties (including the stored
 * passphrase) must not change and the stored key must never leak into an error message.
 */
class PSSftpDeliveryPrivateKeyUpdateTest {

  private static final String PASSPHRASE = "sftp-passphrase";
  private static final String PRIVATE_KEY = "stored-private-key";
  private static final String NEW_PRIVATE_KEY = "new-private-key";

  @Test
  void privateKeyOnlyBodyIsOnlyPrivateKey() {
    assertTrue(PSSftpDeliveryPrivateKeyUpdate.isPrivateKeyOnly(privateKeyOnly(NEW_PRIVATE_KEY)));
    assertFalse(PSFtpDeliveryPasswordUpdate.isPasswordOnly(privateKeyOnly(NEW_PRIVATE_KEY)));
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(privateKeyOnly(NEW_PRIVATE_KEY)));
    assertFalse(PSLocalDeliveryFolderUpdate.isFolderOnly(privateKeyOnly(NEW_PRIVATE_KEY)));
    assertFalse(
        PSAmazonS3DeliverySecretKeyUpdate.isSecretKeyOnly(privateKeyOnly(NEW_PRIVATE_KEY)));
    assertFalse(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(privateKeyOnly(NEW_PRIVATE_KEY)));

    PSPublishServerInfo upper = new PSPublishServerInfo();
    upper.setProperties(List.of(property("PrivateKey", NEW_PRIVATE_KEY)));
    assertTrue(PSSftpDeliveryPrivateKeyUpdate.isPrivateKeyOnly(upper));
  }

  @Test
  void namedHostPortUserFolderPasswordOrS3BodiesAreNotPrivateKeyOnly() {
    PSPublishServerInfo named = privateKeyOnly(NEW_PRIVATE_KEY);
    named.setServerName("sftp-prod");
    assertFalse(PSSftpDeliveryPrivateKeyUpdate.isPrivateKeyOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "h")));
    assertFalse(PSSftpDeliveryPrivateKeyUpdate.isPrivateKeyOnly(hostOnly));

    PSPublishServerInfo passwordOnly = new PSPublishServerInfo();
    passwordOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, "p")));
    assertFalse(PSSftpDeliveryPrivateKeyUpdate.isPrivateKeyOnly(passwordOnly));

    PSPublishServerInfo withFolder = privateKeyOnly(NEW_PRIVATE_KEY);
    List<PSPublishServerProperty> props = new ArrayList<>(withFolder.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/folder"));
    withFolder.setProperties(props);
    assertFalse(PSSftpDeliveryPrivateKeyUpdate.isPrivateKeyOnly(withFolder));
    assertFalse(PSSftpDeliveryPrivateKeyUpdate.isPrivateKeyOnly(null));
  }

  @Test
  void applyReplacesPrivateKeyAndLeavesHostPortUserPassphraseFolderNameDriver()
      throws Exception {
    PSPubServer server = sftpServer();
    PSSftpDeliveryPrivateKeyUpdate.apply(server, privateKeyOnly("  " + NEW_PRIVATE_KEY + "  "));
    assertEquals(NEW_PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("22", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("sftp-user", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals("SFTP", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("sftp-prod", server.getName());
    assertEquals(7, server.getProperties().size());
  }

  @Test
  void blankOrOverlongPrivateKeyDoesNotWrite() {
    PSPubServer blank = sftpServer();
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPrivateKeyUpdate.apply(blank, privateKeyOnly("   ")));
    assertTrue(
        blankError.getMessage().contains(PSSftpDeliveryPrivateKeyUpdate.SFTP_PRIVATE_KEY_REQUIRED));
    assertFalse(blankError.getMessage().contains(PRIVATE_KEY));
    assertFalse(blankError.getMessage().contains(PASSPHRASE));
    assertUnchanged(blank);

    PSPubServer overlong = sftpServer();
    String tooLong = "k".repeat(PSSftpDeliveryPrivateKeyUpdate.MAX_SFTP_PRIVATE_KEY_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPrivateKeyUpdate.apply(overlong, privateKeyOnly(tooLong)));
    assertTrue(
        longError
            .getMessage()
            .contains(PSSftpDeliveryPrivateKeyUpdate.SFTP_PRIVATE_KEY_TOO_LONG));
    assertFalse(longError.getMessage().contains(PRIVATE_KEY));
    assertFalse(longError.getMessage().contains(tooLong));
    assertUnchanged(overlong);
  }

  @Test
  void ftpFtpsLocalDatabaseAndS3DriversDoNotChangePrivateKey() {
    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPrivateKeyUpdate.apply(ftp, privateKeyOnly(NEW_PRIVATE_KEY)));
    assertTrue(
        ftpError.getMessage().contains(PSSftpDeliveryPrivateKeyUpdate.SFTP_PRIVATE_KEY_DRIVER));
    assertTrue(ftp.getPropertyValue(IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY).isEmpty());

    PSPubServer ftps = new PSPubServer();
    ftps.setName("ftps-prod");
    ftps.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTPS");
    assertThrows(
        PSValidationException.class,
        () -> PSSftpDeliveryPrivateKeyUpdate.apply(ftps, privateKeyOnly(NEW_PRIVATE_KEY)));

    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPrivateKeyUpdate.apply(local, privateKeyOnly(NEW_PRIVATE_KEY)));
    assertTrue(
        localError.getMessage().contains(PSSftpDeliveryPrivateKeyUpdate.SFTP_PRIVATE_KEY_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));

    PSPubServer oracle = new PSPubServer();
    oracle.setName("ora-prod");
    oracle.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Oracle");
    PSValidationException oracleError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPrivateKeyUpdate.apply(oracle, privateKeyOnly(NEW_PRIVATE_KEY)));
    assertTrue(
        oracleError.getMessage().contains(PSSftpDeliveryPrivateKeyUpdate.SFTP_PRIVATE_KEY_DRIVER));

    PSPubServer s3 = new PSPubServer();
    s3.setName("s3-prod");
    s3.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "AMAZONS3");
    PSValidationException s3Error =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPrivateKeyUpdate.apply(s3, privateKeyOnly(NEW_PRIVATE_KEY)));
    assertTrue(
        s3Error.getMessage().contains(PSSftpDeliveryPrivateKeyUpdate.SFTP_PRIVATE_KEY_DRIVER));
  }

  @Test
  void sftpDriverMatchIsCaseInsensitive() throws Exception {
    PSPubServer lower = sftpServer();
    lower.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "sftp");
    PSSftpDeliveryPrivateKeyUpdate.apply(lower, privateKeyOnly(NEW_PRIVATE_KEY));
    assertEquals(NEW_PRIVATE_KEY, value(lower, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    assertEquals(PASSPHRASE, value(lower, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("old.example", value(lower, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
  }

  private static void assertUnchanged(PSPubServer server) {
    assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("22", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("sftp-user", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
  }

  private static PSPubServer sftpServer() {
    PSPubServer server = new PSPubServer();
    server.setName("sftp-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "SFTP");
    server.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "22");
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "sftp-user");
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, PASSPHRASE);
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/folder");
    server.addProperty(IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY, PRIVATE_KEY);
    return server;
  }

  private static PSPublishServerInfo privateKeyOnly(String privateKey) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY, privateKey)));
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
