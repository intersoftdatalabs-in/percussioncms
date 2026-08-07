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
package com.percussion.fastforward.sfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.design.objectstore.PSLocator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for {@link PSSite} path helpers generics cleanup (issue #2323 batch 2). Does not
 * exercise relationship processor (no server); covers empty-path rendering and typed list API.
 */
class PSSitePathListTest {

  @Test
  void renderSiteFolderPathLocatorsEmptyListIsRootSlash() throws PSCmsException {
    List<PSLocator> empty = Collections.emptyList();
    assertEquals("/", PSSite.renderSiteFolderPathLocators(empty));
  }

  @Test
  void renderSiteFolderPathEmptyFoldersIsRootSlash() throws PSCmsException {
    List<PSFolder> empty = new ArrayList<>();
    assertEquals("/", PSSite.renderSiteFolderPath(empty));
  }

  @Test
  void buildFolderPathListRejectsNonPositiveRoot() {
    try {
      PSSite.buildFolderPathList(0, new PSLocator(1, 1), false);
      assertTrue(false, "expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // ok
    } catch (PSCmsException e) {
      assertTrue(false, "unexpected PSCmsException: " + e.getMessage());
    }
  }

  @Test
  void buildFolderPathListRejectsNullLocator() {
    try {
      PSSite.buildFolderPathList(1, null, false);
      assertTrue(false, "expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // ok
    } catch (PSCmsException e) {
      assertTrue(false, "unexpected PSCmsException: " + e.getMessage());
    }
  }
}
