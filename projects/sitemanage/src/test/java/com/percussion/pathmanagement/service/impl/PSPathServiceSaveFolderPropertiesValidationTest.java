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
package com.percussion.pathmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.pathmanagement.data.PSFolderProperties;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.ui.service.IPSUiService;
import com.percussion.ui.service.impl.PSCm1ListViewHelper;
import com.percussion.user.service.IPSUserService;
import com.percussion.webservices.publishing.IPSPublishingWs;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit coverage for {@link PSPathService#saveFolderProperties} null/blank id gates (#2749).
 *
 * <p>Prevents reintroduction of Apache Validate.notNull NPEs when the Jackson UNWRAP_ROOT_VALUE
 * body is missing or {@code id} is blank.
 */
@ExtendWith(MockitoExtension.class)
class PSPathServiceSaveFolderPropertiesValidationTest {

  @Mock IPSFolderHelper folderHelper;
  @Mock IPSIdMapper idMapper;
  @Mock IPSPublishingWs publishingWs;
  @Mock IPSUiService uiService;
  @Mock IPSUserService userService;
  @Mock PSCm1ListViewHelper listViewHelper;
  @Mock IPSRecycleService recycleService;

  PSPathService service;

  @BeforeEach
  void setUp() {
    service =
        new PSPathService(
            folderHelper,
            publishingWs,
            idMapper,
            uiService,
            userService,
            listViewHelper,
            recycleService);
  }

  @Test
  void nullBody_throwsValidationNotNpeOnGetGuid() throws Exception {
    Exception ex = assertThrows(Exception.class, () -> service.saveFolderProperties(null));
    assertTrue(
        ex instanceof PSValidationException
            || (ex.getMessage() != null && !ex.getMessage().isBlank()),
        "expected validation failure, got " + ex.getClass().getName() + ": " + ex.getMessage());
    verify(idMapper, never()).getGuid(org.mockito.ArgumentMatchers.anyString());
    verify(folderHelper, never()).saveFolderProperties(any(PSFolderProperties.class));
  }

  @Test
  void blankId_throwsValidationNotNpeOnGetGuid() throws Exception {
    PSFolderProperties props = new PSFolderProperties();
    props.setName("Design");
    props.setId("  ");
    Exception ex = assertThrows(Exception.class, () -> service.saveFolderProperties(props));
    assertTrue(
        ex instanceof PSValidationException
            || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("id")),
        "expected id validation, got " + ex);
    verify(idMapper, never()).getGuid(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void validId_delegatesToFolderHelper() throws Exception {
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    PSFolderPermission perm = new PSFolderPermission();
    perm.setAccessLevel(PSFolderPermission.Access.ADMIN);
    props.setPermission(perm);

    when(idMapper.getGuid("16777215-101-703")).thenReturn(new PSLegacyGuid(703, 1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> service.saveFolderProperties(props));
    verify(folderHelper).saveFolderProperties(props);
  }
}
