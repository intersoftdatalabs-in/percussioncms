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
package com.percussion.cloudservice.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.percussion.licensemanagement.service.impl.PSLicenseService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSRenderService;
import com.percussion.share.dao.IPSFolderHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Path-injection regression for {@link PSCloudService#generateThumbUrl}: siteName and pageId are
 * validated with {@code requireSafeFileName} before any File I/O under {@code PSServer.getRxDir()}.
 *
 * <p>Instantiates {@link PSCloudService} via its public constructor with Mockito-mocked
 * dependencies (same pattern as {@code PSImportThemeHelperPathInjectionTest}) — no {@code
 * sun.misc.Unsafe}.
 */
public class PSCloudServicePathInjectionTest {

  private static PSCloudService service() {
    return new PSCloudService(
        mock(IPSFolderHelper.class),
        mock(IPSRenderService.class),
        mock(IPSPageService.class),
        mock(PSLicenseService.class));
  }

  @Test
  @DisplayName("generateThumbUrl rejects parent traversal in siteName before File I/O")
  void rejectsTraversalInSiteName() {
    PSCloudService svc = service();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> svc.generateThumbUrl("page1", "../escape"),
            "siteName traversal must be rejected by requireSafeFileName");
    assertTrue(
        ex.getMessage() != null && !ex.getMessage().isBlank(),
        "validator should produce a message");
  }

  @Test
  @DisplayName("generateThumbUrl rejects parent traversal in pageId before File I/O")
  void rejectsTraversalInPageId() {
    PSCloudService svc = service();
    assertThrows(
        IllegalArgumentException.class,
        () -> svc.generateThumbUrl("../escape", "goodSite"),
        "pageId traversal must be rejected by requireSafeFileName");
  }

  @Test
  @DisplayName("generateThumbUrl rejects path separators in siteName")
  void rejectsSlashInSiteName() {
    PSCloudService svc = service();
    assertThrows(
        IllegalArgumentException.class, () -> svc.generateThumbUrl("page1", "site/name"));
    assertThrows(
        IllegalArgumentException.class, () -> svc.generateThumbUrl("page1", "site\\name"));
  }

  @Test
  @DisplayName("generateThumbUrl rejects path separators in pageId")
  void rejectsSlashInPageId() {
    PSCloudService svc = service();
    assertThrows(
        IllegalArgumentException.class, () -> svc.generateThumbUrl("page/id", "goodSite"));
  }

  @Test
  @DisplayName("generateThumbUrl rejects NUL in siteName")
  void rejectsNulInSiteName() {
    PSCloudService svc = service();
    assertThrows(
        IllegalArgumentException.class, () -> svc.generateThumbUrl("page1", "site\0name"));
  }
}
