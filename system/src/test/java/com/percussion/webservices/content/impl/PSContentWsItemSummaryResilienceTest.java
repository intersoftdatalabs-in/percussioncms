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
package com.percussion.webservices.content.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import org.junit.jupiter.api.Test;

/** Missing FF nav types must not fail folder listing (#3410). */
class PSContentWsItemSummaryResilienceTest {

  @Test
  void contentTypeNameIfRegisteredReturnsNullWhenTypeMissing() throws Exception {
    var mgr = mock(PSItemDefManager.class);
    when(mgr.contentTypeIdToName(313L)).thenThrow(new PSInvalidContentTypeException("313"));
    when(mgr.contentTypeIdToName(315L)).thenThrow(new PSInvalidContentTypeException("315"));
    assertNull(PSContentWs.contentTypeNameIfRegistered(mgr, 313L));
    assertNull(PSContentWs.contentTypeNameIfRegistered(mgr, 315L));
  }

  @Test
  void contentTypeNameIfRegisteredReturnsNameWhenHandlerExists() throws Exception {
    var mgr = mock(PSItemDefManager.class);
    when(mgr.contentTypeIdToName(101L)).thenReturn("Folder");
    assertEquals("Folder", PSContentWs.contentTypeNameIfRegistered(mgr, 101L));
  }

  @Test
  void contentTypeNameIfRegisteredReturnsNullForBlankOrNullManager() throws Exception {
    assertNull(PSContentWs.contentTypeNameIfRegistered(null, 101L));
    var mgr = mock(PSItemDefManager.class);
    when(mgr.contentTypeIdToName(101L)).thenReturn("  ");
    assertNull(PSContentWs.contentTypeNameIfRegistered(mgr, 101L));
  }
}
