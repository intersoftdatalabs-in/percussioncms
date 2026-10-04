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

package com.percussion.role.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.metadata.data.PSMetadata;
import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.role.service.IPSRoleService;
import com.percussion.services.security.IPSBackEndRoleMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Stored role homepage metadata: blank deletes, aliases canonicalize, unset reads as null. */
@Tag("UnitTest")
class PSRoleServiceHomepageWriteTest {

  private IPSMetadataService metadata;
  private PSRoleService service;
  private String key;

  @BeforeEach
  void setUp() {
    metadata = Mockito.mock(IPSMetadataService.class);
    service = new PSRoleService(null, Mockito.mock(IPSBackEndRoleMgr.class), null, metadata, null);
    key = IPSRoleService.META_DATA_HOMEPAGE_PREFIX + "Author";
  }

  @Test
  void blankDeletesStoredHomepage() throws Exception {
    when(metadata.find(key)).thenReturn(new PSMetadata(key, "Explorer"));

    service.writeHomepage("Author", "   ");

    verify(metadata).delete(key);
    verify(metadata, never()).save(any());
  }

  @Test
  void blankWithNothingStoredDoesNotSave() throws Exception {
    when(metadata.find(key)).thenReturn(null);

    service.writeHomepage("Author", "");

    verify(metadata, never()).delete(any());
    verify(metadata, never()).save(any());
  }

  @Test
  void aliasIsStoredCanonically() throws Exception {
    when(metadata.find(key)).thenReturn(null);

    service.writeHomepage("Author", "explorer");

    ArgumentCaptor<PSMetadata> saved = ArgumentCaptor.forClass(PSMetadata.class);
    verify(metadata).save(saved.capture());
    assertEquals(key, saved.getValue().getKey());
    assertEquals("Explorer", saved.getValue().getData());
    verify(metadata, never()).delete(any());
  }

  @Test
  void unknownNonBlankIsStoredAsHome() throws Exception {
    when(metadata.find(key)).thenReturn(new PSMetadata(key, "Explorer"));

    service.writeHomepage("Author", "NotAPage");

    ArgumentCaptor<PSMetadata> saved = ArgumentCaptor.forClass(PSMetadata.class);
    verify(metadata).save(saved.capture());
    assertEquals("Home", saved.getValue().getData());
    verify(metadata, never()).delete(any());
  }

  @Test
  void unsetReadIsNullAndStoredValueIsReturned() throws Exception {
    when(metadata.find(key)).thenReturn(null);
    assertNull(service.readStoredHomepage("Author"));

    when(metadata.find(key)).thenReturn(new PSMetadata(key, "Explorer"));
    assertEquals("Explorer", service.readStoredHomepage("Author"));

    when(metadata.find(key)).thenReturn(new PSMetadata(key, "  "));
    assertNull(service.readStoredHomepage("Author"));
  }
}
