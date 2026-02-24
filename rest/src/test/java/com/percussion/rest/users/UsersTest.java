/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11

package com.percussion.rest.users;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.rest.MainTest;
import org.junit.jupiter.api.Test;

public class UsersTest extends MainTest {

  @Test
  public void testNeverNull() {
    var u = new User();

    assertNotNull(u.getBookmarkedPages(), "Should never be null");
    assertNotNull(u.getEmailAddress(), "Should never be null");
    assertNotNull(u.getFirstName(), "Should never be null");
    assertNotNull(u.getLastName(), "Should never be null");
    assertNotNull(u.getPersonalPage(), "Should never be null");
    assertNotNull(u.getPersonAssets(), "Should never be null");
    assertNotNull(u.getRecentAssetFolders(), "Should never be null");
    assertNotNull(u.getRecentAssetTypes(), "Should never be null");
    assertNotNull(u.getRecentPages(), "Should never be null");
    assertNotNull(u.getRecentSiteFolders(), "Should never be null");
    assertNotNull(u.getRoles(), "Should never be null");
    assertNotNull(u.getRecentTemplates(), "Should never be null");
    assertNotNull(u.getUserName(), "Should never be null");
    assertNotNull(u.getUserType(), "Should never be null");
  }
}
