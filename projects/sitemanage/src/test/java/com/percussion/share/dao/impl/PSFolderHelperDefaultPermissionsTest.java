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
package com.percussion.share.dao.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSObjectAclEntry;
import com.percussion.share.dao.PSFolderPermissionUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * CORE_SERVER_INITIALIZED default ACL rewrite must be idempotent when Everyone already has ADMIN
 * (#3282). Assets folder CONTENTID=7 has no seed ACL, so first persist still runs after NEXTNUMBER
 * is aligned.
 */
@Tag("UnitTest")
class PSFolderHelperDefaultPermissionsTest {

  @Test
  void skipRewriteWhenEveryoneAlreadyAdmin() {
    PSFolder folder = new PSFolder("Assets", -1, PSObjectAclEntry.ACCESS_ADMIN, "assets");
    folder
        .getAcl()
        .add(
            new PSObjectAclEntry(
                PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL,
                PSObjectAclEntry.ACL_ENTRY_EVERYONE,
                PSFolderPermissionUtils.ADMIN_ACCESS));
    assertTrue(PSFolderHelper.shouldSkipDefaultAclRewrite(folder));
  }

  @Test
  void rewriteWhenFolderHasNoEveryoneAcl() {
    PSFolder folder = new PSFolder("Assets", -1, PSObjectAclEntry.ACCESS_ADMIN, "assets");
    assertFalse(PSFolderHelper.shouldSkipDefaultAclRewrite(folder));
  }

  @Test
  void rewriteWhenEveryoneIsReadOnlySeed() {
    PSFolder folder = new PSFolder("Assets", -1, PSObjectAclEntry.ACCESS_ADMIN, "assets");
    folder
        .getAcl()
        .add(
            new PSObjectAclEntry(
                PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL,
                PSObjectAclEntry.ACL_ENTRY_EVERYONE,
                PSObjectAclEntry.ACCESS_READ));
    assertFalse(PSFolderHelper.shouldSkipDefaultAclRewrite(folder));
  }
}
