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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.pubserver.data.PSPublishServerProperty;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.pubserver.data.PSPubServerProperty;
import com.percussion.sitemanage.service.IPSSiteDataService.PublishType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Behavior preservation for the sibling-complexity extraction on {@link PSPubServerService}
 * (issue #5964). The extracted {@code resolve*} / {@code is*} helpers must decide exactly as the
 * previous inline branches in {@code setPublishType}, {@code validateProperties}, and
 * {@code toPSPublishServerInfo} did.
 */
class PSPubServerServiceSiblingComplexityTest {

  @Test
  void localDriverResolvesToFilesystemUnlessXml() {
    assertEquals(
        PublishType.filesystem.toString(),
        PSPubServerService.resolvePublishType("File", "Local", null, true, "HTML"));
    assertEquals(
        PublishType.filesystem_only.toString(),
        PSPubServerService.resolvePublishType("File", "Local", null, false, "XML"));
  }

  @Test
  void ftpDriverResolvesDefaultOnlyAndSecureVariants() {
    assertEquals(
        PublishType.ftp.toString(),
        PSPubServerService.resolvePublishType("File", "FTP", "false", true, "HTML"));
    assertEquals(
        PublishType.ftp_only.toString(),
        PSPubServerService.resolvePublishType("File", "FTP", "false", false, "HTML"));
    assertEquals(
        PublishType.sftp.toString(),
        PSPubServerService.resolvePublishType("File", "FTP", "true", true, "HTML"));
    assertEquals(
        PublishType.sftp_only.toString(),
        PSPubServerService.resolvePublishType("File", "FTP", "true", false, "HTML"));
  }

  @Test
  void ftpXmlDefaultFallsBackToFileOnly() {
    assertEquals(
        PublishType.ftp_only.toString(),
        PSPubServerService.resolvePublishType("File", "FTP", "false", true, "XML"));
    assertEquals(
        PublishType.sftp_only.toString(),
        PSPubServerService.resolvePublishType("File", "FTP", "true", true, "XML"));
  }

  @Test
  void sftpDriverResolvesDefaultOnlyAndXmlVariants() {
    assertEquals(
        PublishType.sftp.toString(),
        PSPubServerService.resolvePublishType("File", "SFTP", null, true, "HTML"));
    assertEquals(
        PublishType.sftp_only.toString(),
        PSPubServerService.resolvePublishType("File", "SFTP", null, false, "HTML"));
    assertEquals(
        PublishType.sftp_only.toString(),
        PSPubServerService.resolvePublishType("File", "SFTP", null, true, "XML"));
  }

  @Test
  void ftpsDriverResolvesDefaultHtmlOnly() {
    assertEquals(
        PublishType.ftps.toString(),
        PSPubServerService.resolvePublishType("File", "FTPS", null, true, "HTML"));
    assertEquals(
        PublishType.ftps_only.toString(),
        PSPubServerService.resolvePublishType("File", "FTPS", null, true, "XML"));
    assertEquals(
        PublishType.ftps_only.toString(),
        PSPubServerService.resolvePublishType("File", "FTPS", null, false, "HTML"));
  }

  @Test
  void amazonS3DriverResolvesDefaultOnly() {
    assertEquals(
        PublishType.amazon_s3.toString(),
        PSPubServerService.resolvePublishType("File", "AMAZONS3", null, true, "HTML"));
    assertEquals(
        PublishType.amazon_s3_only.toString(),
        PSPubServerService.resolvePublishType("File", "AMAZONS3", null, false, "HTML"));
  }

  @Test
  void databaseTypeResolvesRegardlessOfDriver() {
    assertEquals(
        PublishType.database.toString(),
        PSPubServerService.resolvePublishType("Database", "Oracle", null, true, "HTML"));
    assertEquals(
        PublishType.database.toString(),
        PSPubServerService.resolvePublishType("Database", "MSSQL", null, false, "HTML"));
  }

  @Test
  void unknownCombinationsResolveToNull() {
    assertNull(PSPubServerService.resolvePublishType("File", "Unknown", null, true, "HTML"));
    assertNull(PSPubServerService.resolvePublishType("Unknown", "Local", null, true, "HTML"));
  }

  @Test
  void filePublishTypesCoverAllFileFlavors() {
    for (PublishType type :
        new PublishType[] {
          PublishType.filesystem,
          PublishType.ftp,
          PublishType.sftp,
          PublishType.filesystem_only,
          PublishType.ftp_only,
          PublishType.ftps,
          PublishType.ftps_only,
          PublishType.sftp_only,
          PublishType.amazon_s3,
          PublishType.amazon_s3_only
        }) {
      assertTrue(
          PSPubServerService.isFilePublishType(type.toString()), "expected file type: " + type);
    }
    assertFalse(PSPubServerService.isFilePublishType(PublishType.database.toString()));
    assertFalse(PSPubServerService.isFilePublishType("Unknown"));
    assertFalse(PSPubServerService.isFilePublishType(null));
  }

  @Test
  void readoutExcludesFlagBackedProperties() {
    assertTrue(
        PSPubServerService.isReadoutExcluded(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertTrue(
        PSPubServerService.isReadoutExcluded(IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY));
    assertTrue(
        PSPubServerService.isReadoutExcluded(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(
        PSPubServerService.isReadoutExcluded(IPSPubServerDao.PUBLISH_OWN_SERVER_PROPERTY));
    assertTrue(
        PSPubServerService.isReadoutExcluded(IPSPubServerDao.PUBLISH_FORMAT_PROPERTY));
    assertFalse(
        PSPubServerService.isReadoutExcluded(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertFalse(PSPubServerService.isReadoutExcluded(IPSPubServerDao.PUBLISH_SERVER_PROPERTY));
  }

  @Test
  void adminUrlPropertyMatchesServerPropertyOnly() {
    assertTrue(
        PSPubServerService.isAdminUrlProperty(IPSPubServerDao.PUBLISH_SERVER_PROPERTY));
    assertFalse(
        PSPubServerService.isAdminUrlProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
  }

  @Test
  void ftpPropertySetSelectsSecureAndKeyVariants() {
    String[] ftpExpected = {
      IPSPubServerDao.PUBLISH_DRIVER_PROPERTY,
      "XML",
      "HTML",
      IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY,
      IPSPubServerDao.PUBLISH_PORT_PROPERTY,
      IPSPubServerDao.PUBLISH_USER_ID_PROPERTY,
      IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY,
      "ownServerFlag",
      "defaultServerFlag",
      IPSPubServerDao.PUBLISH_FOLDER_PROPERTY
    };
    assertArrayEquals(
        ftpExpected, PSPubServerService.resolveFtpPropertySet("FTP", "false", false));
    assertArrayEquals(
        ftpExpected, PSPubServerService.resolveFtpPropertySet("FTPS", "false", false));
  }

  @Test
  void sftpPropertySetUpgradesToSecureAndDropsPasswordWithKey() {
    String[] withPass = PSPubServerService.resolveFtpPropertySet("SFTP", null, false);
    assertTrue(List.of(withPass).contains(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    String[] withoutPass = PSPubServerService.resolveFtpPropertySet("SFTP", "false", true);
    assertFalse(List.of(withoutPass).contains(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY));
    assertTrue(List.of(withoutPass).contains(IPSPubServerDao.PUBLISH_PORT_PROPERTY));
  }

  @Test
  void databasePropertySetSelectsDriverDialect() {
    String[] oracle = PSPubServerService.resolveDatabasePropertySet("Oracle");
    assertTrue(List.of(oracle).contains(IPSPubServerDao.PUBLISH_SID_PROPERTY));
    assertTrue(List.of(oracle).contains(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY));

    String[] mssql = PSPubServerService.resolveDatabasePropertySet("MSSQL");
    assertTrue(List.of(mssql).contains(IPSPubServerDao.PUBLISH_OWNER_PROPERTY));

    String[] generic = PSPubServerService.resolveDatabasePropertySet("MySQL");
    assertTrue(List.of(generic).contains(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY));
    assertFalse(List.of(generic).contains(IPSPubServerDao.PUBLISH_OWNER_PROPERTY));
  }

  @Test
  void sftpOnlyWithFtpDriverRepairsDriverProperty() {
    Set<PSPubServerProperty> stored = new HashSet<>();
    PSPubServerProperty driver = new PSPubServerProperty();
    driver.setName(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY);
    driver.setValue("FTP");
    stored.add(driver);

    PSPublishServerInfo serverInfo = new PSPublishServerInfo();
    PSPublishServerProperty readout = new PSPublishServerProperty();
    readout.setKey(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY);
    readout.setValue("FTP");
    serverInfo.getProperties().add(readout);

    PSPubServerService.repairSftpDriverProperty(stored, serverInfo);

    assertTrue(
        serverInfo.getProperties().stream()
            .anyMatch(
                p ->
                    IPSPubServerDao.PUBLISH_DRIVER_PROPERTY.equals(p.getKey())
                        && "SFTP".equals(p.getValue())));
  }

  @Test
  void sftpOnlyWithSftpDriverLeavesReadoutAlone() {
    Set<PSPubServerProperty> stored = new HashSet<>();
    PSPubServerProperty driver = new PSPubServerProperty();
    driver.setName(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY);
    driver.setValue("SFTP");
    stored.add(driver);

    PSPublishServerInfo serverInfo = new PSPublishServerInfo();

    PSPubServerService.repairSftpDriverProperty(stored, serverInfo);

    assertTrue(serverInfo.getProperties().isEmpty());
  }
}
