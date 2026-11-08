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

/**
 * Passphrase-only SFTP delivery-server update. The passphrase shares the {@code password} wire
 * shape with the FTP/database password-only paths; the stored driver selects this SFTP apply.
 * Other properties (including the stored private key) must not change and the stored secret must
 * never leak into an error message.
 */
class PSSftpDeliveryPassphraseUpdateTest {

  private static final String PASSPHRASE = "sftp-passphrase";
  private static final String NEW_PASSPHRASE = "new-sftp-passphrase";
  private static final String PRIVATE_KEY = "stored-private-key";

  @Test
  void passphraseOnlyBodySharesPasswordOnlyWireShape() {
    assertTrue(PSSftpDeliveryPassphraseUpdate.isPassphraseOnly(passphraseOnly(NEW_PASSPHRASE)));
    assertTrue(PSFtpDeliveryPasswordUpdate.isPasswordOnly(passphraseOnly(NEW_PASSPHRASE)));
    assertTrue(PSDatabaseDeliveryPasswordUpdate.isPasswordOnly(passphraseOnly(NEW_PASSPHRASE)));
    assertFalse(PSSftpDeliveryHostUpdate.isHostOnly(passphraseOnly(NEW_PASSPHRASE)));
    assertFalse(PSSftpDeliveryPrivateKeyUpdate.isPrivateKeyOnly(passphraseOnly(NEW_PASSPHRASE)));
    assertFalse(PSLocalDeliveryFolderUpdate.isFolderOnly(passphraseOnly(NEW_PASSPHRASE)));
    assertFalse(
        PSAmazonS3DeliverySecretKeyUpdate.isSecretKeyOnly(passphraseOnly(NEW_PASSPHRASE)));
    assertFalse(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(passphraseOnly(NEW_PASSPHRASE)));

    PSPublishServerInfo upper = new PSPublishServerInfo();
    upper.setProperties(List.of(property("Password", NEW_PASSPHRASE)));
    assertTrue(PSSftpDeliveryPassphraseUpdate.isPassphraseOnly(upper));
  }

  @Test
  void namedHostPortUserFolderPrivateKeyOrS3BodiesAreNotPassphraseOnly() {
    PSPublishServerInfo named = passphraseOnly(NEW_PASSPHRASE);
    named.setServerName("sftp-prod");
    assertFalse(PSSftpDeliveryPassphraseUpdate.isPassphraseOnly(named));

    PSPublishServerInfo hostOnly = new PSPublishServerInfo();
    hostOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "h")));
    assertFalse(PSSftpDeliveryPassphraseUpdate.isPassphraseOnly(hostOnly));

    PSPublishServerInfo privateKeyOnly = new PSPublishServerInfo();
    privateKeyOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY, "k")));
    assertFalse(PSSftpDeliveryPassphraseUpdate.isPassphraseOnly(privateKeyOnly));

    PSPublishServerInfo withFolder = passphraseOnly(NEW_PASSPHRASE);
    List<PSPublishServerProperty> props = new ArrayList<>(withFolder.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/folder"));
    withFolder.setProperties(props);
    assertFalse(PSSftpDeliveryPassphraseUpdate.isPassphraseOnly(withFolder));
    assertFalse(PSSftpDeliveryPassphraseUpdate.isPassphraseOnly(null));
  }

  @Test
  void applyReplacesPassphraseAndLeavesHostPortUserFolderPrivateKeyNameDriver()
      throws Exception {
    PSPubServer server = sftpServer();
    PSSftpDeliveryPassphraseUpdate.apply(server, passphraseOnly("  " + NEW_PASSPHRASE + "  "));
    assertEquals(NEW_PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals("old.example", value(server, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
    assertEquals("22", value(server, IPSPubServerDao.PUBLISH_PORT_PROPERTY));
    assertEquals("sftp-user", value(server, IPSPubServerDao.PUBLISH_USER_ID_PROPERTY));
    assertEquals("/kept/folder", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    assertEquals("SFTP", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("sftp-prod", server.getName());
    assertEquals(7, server.getProperties().size());
  }

  @Test
  void blankPlaceholderOrOverlongPassphraseDoesNotWrite() {
    PSPubServer blank = sftpServer();
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPassphraseUpdate.apply(blank, passphraseOnly("   ")));
    assertTrue(
        blankError.getMessage().contains(PSSftpDeliveryPassphraseUpdate.SFTP_PASSPHRASE_REQUIRED));
    assertFalse(blankError.getMessage().contains(PASSPHRASE));
    assertFalse(blankError.getMessage().contains(PRIVATE_KEY));
    assertUnchanged(blank);

    PSPubServer placeholder = sftpServer();
    PSValidationException placeholderError =
        assertThrows(
            PSValidationException.class,
            () ->
                PSSftpDeliveryPassphraseUpdate.apply(
                    placeholder, passphraseOnly(PSPubServerService.PASSWORD_ENTRY)));
    assertTrue(
        placeholderError
            .getMessage()
            .contains(PSSftpDeliveryPassphraseUpdate.SFTP_PASSPHRASE_REQUIRED));
    assertUnchanged(placeholder);

    PSPubServer overlong = sftpServer();
    String tooLong = "p".repeat(PSSftpDeliveryPassphraseUpdate.MAX_SFTP_PASSPHRASE_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPassphraseUpdate.apply(overlong, passphraseOnly(tooLong)));
    assertTrue(
        longError.getMessage().contains(PSSftpDeliveryPassphraseUpdate.SFTP_PASSPHRASE_TOO_LONG));
    assertFalse(longError.getMessage().contains(PASSPHRASE));
    assertFalse(longError.getMessage().contains(tooLong));
    assertUnchanged(overlong);
  }

  @Test
  void ftpFtpsLocalDatabaseAndS3DriversDoNotChangePassphrase() {
    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    ftp.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, "ftp-secret");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPassphraseUpdate.apply(ftp, passphraseOnly(NEW_PASSPHRASE)));
    assertTrue(
        ftpError.getMessage().contains(PSSftpDeliveryPassphraseUpdate.SFTP_PASSPHRASE_DRIVER));
    assertEquals("ftp-secret", value(ftp, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));

    PSPubServer ftps = new PSPubServer();
    ftps.setName("ftps-prod");
    ftps.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTPS");
    assertThrows(
        PSValidationException.class,
        () -> PSSftpDeliveryPassphraseUpdate.apply(ftps, passphraseOnly(NEW_PASSPHRASE)));

    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPassphraseUpdate.apply(local, passphraseOnly(NEW_PASSPHRASE)));
    assertTrue(
        localError.getMessage().contains(PSSftpDeliveryPassphraseUpdate.SFTP_PASSPHRASE_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));

    PSPubServer oracle = new PSPubServer();
    oracle.setName("ora-prod");
    oracle.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Oracle");
    PSValidationException oracleError =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPassphraseUpdate.apply(oracle, passphraseOnly(NEW_PASSPHRASE)));
    assertTrue(
        oracleError.getMessage().contains(PSSftpDeliveryPassphraseUpdate.SFTP_PASSPHRASE_DRIVER));

    PSPubServer s3 = new PSPubServer();
    s3.setName("s3-prod");
    s3.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "AMAZONS3");
    PSValidationException s3Error =
        assertThrows(
            PSValidationException.class,
            () -> PSSftpDeliveryPassphraseUpdate.apply(s3, passphraseOnly(NEW_PASSPHRASE)));
    assertTrue(
        s3Error.getMessage().contains(PSSftpDeliveryPassphraseUpdate.SFTP_PASSPHRASE_DRIVER));
  }

  @Test
  void sftpDriverMatchIsCaseInsensitive() throws Exception {
    PSPubServer lower = sftpServer();
    lower.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "sftp");
    PSSftpDeliveryPassphraseUpdate.apply(lower, passphraseOnly(NEW_PASSPHRASE));
    assertEquals(NEW_PASSPHRASE, value(lower, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals(PRIVATE_KEY, value(lower, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    assertEquals("old.example", value(lower, IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY));
  }

  private static void assertUnchanged(PSPubServer server) {
    assertEquals(PASSPHRASE, value(server, IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertEquals(PRIVATE_KEY, value(server, IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
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

  private static PSPublishServerInfo passphraseOnly(String passphrase) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, passphrase)));
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
