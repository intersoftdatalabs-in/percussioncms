/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.pubserver.data.PSPublishServerProperty;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.service.exception.PSValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Default-flag-only delivery-server update. Name, driver, type, and properties stay. */
class PSDeliveryServerDefaultUpdateTest {

  @Test
  void defaultOnlyBodyIsOnlyIsDefault() {
    assertTrue(PSDeliveryServerDefaultUpdate.isDefaultOnly(defaultOnly(true)));
    assertTrue(PSDeliveryServerDefaultUpdate.isDefaultOnly(defaultOnly(false)));
    assertFalse(PSDeliveryServerRename.isNameOnly(defaultOnly(true)));
    assertFalse(PSLocalDeliveryFolderUpdate.isFolderOnly(defaultOnly(true)));
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(defaultOnly(true)));
  }

  @Test
  void nameTypeDescriptionOrPropertiesAreNotDefaultOnly() {
    PSPublishServerInfo named = defaultOnly(true);
    named.setServerName("NightLocal");
    assertFalse(PSDeliveryServerDefaultUpdate.isDefaultOnly(named));

    PSPublishServerInfo typed = defaultOnly(true);
    typed.setType("File");
    assertFalse(PSDeliveryServerDefaultUpdate.isDefaultOnly(typed));

    PSPublishServerInfo described = defaultOnly(true);
    described.setDescription("kept");
    assertFalse(PSDeliveryServerDefaultUpdate.isDefaultOnly(described));

    PSPublishServerInfo withFolder = defaultOnly(true);
    withFolder.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local")));
    assertFalse(PSDeliveryServerDefaultUpdate.isDefaultOnly(withFolder));
    assertFalse(PSDeliveryServerDefaultUpdate.isDefaultOnly(null));

    PSPublishServerInfo missingFlag = new PSPublishServerInfo();
    assertFalse(PSDeliveryServerDefaultUpdate.isDefaultOnly(missingFlag));
  }

  @Test
  void falseOrMissingFlagDoesNotValidate() {
    PSValidationException missing =
        assertThrows(
            PSValidationException.class, () -> PSDeliveryServerDefaultUpdate.validate(null));
    assertTrue(missing.getMessage().contains(PSDeliveryServerDefaultUpdate.DEFAULT_FLAG_REQUIRED));

    PSValidationException unmark =
        assertThrows(
            PSValidationException.class,
            () -> PSDeliveryServerDefaultUpdate.validate(defaultOnly(false)));
    assertTrue(unmark.getMessage().contains(PSDeliveryServerDefaultUpdate.DEFAULT_FLAG_REQUIRED));
  }

  @Test
  void trueFlagValidatesAndAlreadyDefaultSkipsWrite() throws Exception {
    PSDeliveryServerDefaultUpdate.validate(defaultOnly(true));
    PSPubServer server = namedServer(9L, "stage");
    PSPubServer current = namedServer(9L, "stage");
    assertTrue(PSDeliveryServerDefaultUpdate.alreadyDefault(server, current));
    assertFalse(PSDeliveryServerDefaultUpdate.alreadyDefault(server, namedServer(3L, "prod")));
    assertFalse(PSDeliveryServerDefaultUpdate.alreadyDefault(server, null));
    assertEquals("stage", server.getName());
    assertEquals("Local", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("/kept/local", value(server, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
  }

  @Test
  void applyStoresSiteDefaultIdAndLeavesPublishTypes() {
    IPSSite site = mock(IPSSite.class);
    PSPubServer previous = namedServer(3L, "prod");
    previous.setPublishType("filesystem");
    PSPubServer selected = namedServer(9L, "stage");
    selected.setPublishType("filesystem_only");
    PSDeliveryServerDefaultUpdate.apply(site, selected);
    verify(site).setDefaultPubServer(9L);
    assertEquals("filesystem", previous.getPublishType());
    assertEquals("filesystem_only", selected.getPublishType());
    assertThrows(
        IllegalArgumentException.class, () -> PSDeliveryServerDefaultUpdate.apply(null, selected));
    assertThrows(
        IllegalArgumentException.class, () -> PSDeliveryServerDefaultUpdate.apply(site, null));
  }

  private static PSPubServer namedServer(long id, String name) {
    PSPubServer server = new PSPubServer();
    server.setServerId(id);
    server.setName(name);
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    return server;
  }

  private static PSPublishServerInfo defaultOnly(boolean isDefault) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setIsDefault(isDefault);
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
