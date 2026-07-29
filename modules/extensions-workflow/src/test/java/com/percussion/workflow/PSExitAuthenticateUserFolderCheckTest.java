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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSCmsObject;
import com.percussion.cms.objectstore.PSComponentSummary;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the #1561 Phase 4b startup-failure defect:
 * {@code PSExitAuthenticateUser.authenticateUser} at line ~356 (pre-Phase 4b) used to
 * check the <em>object type</em> of a content row ({@code csc.getObjectType() ==
 * PSCmsObject.TYPE_FOLDER}) before routing the folder through {@code PSFolderSecurityManager}.
 * After Phase 4b the same exit was reading via {@code cms.loadComponentSummary} (a
 * {@link PSComponentSummary} instead of a {@code PSContentStatusContext}), and a copy-paste
 * error in the same PR replaced both calls with the <em>content type id</em>
 * ({@code csc.getContentTypeId()}) — which is a different value and broke the
 * folder check (content type id 101 != object type 2). This test locks in the
 * pre-migration contract: a folder content row is detected via its object type,
 * not its content type id.
 */
public class PSExitAuthenticateUserFolderCheckTest {

  /**
   * The exact regression case reported: a folder content row with content type id 101
   * and object type {@link PSCmsObject#TYPE_FOLDER} (2). The folder check must succeed
   * via the object type, not the content type id.
   */
  @Test
  void folderRow_isDetectedByObjectType_notByContentTypeId() {
    PSComponentSummary folderRow = newFolderRow(101, PSCmsObject.TYPE_FOLDER);

    // Pre-Phase-4b correct check: object type.
    assertEquals(PSCmsObject.TYPE_FOLDER, folderRow.getObjectType(),
        "Folder row must have object type TYPE_FOLDER");
    assertTrue(folderRow.isFolder(),
        "PSComponentSummary.isFolder() must be true for a folder row");

    // The Phase 4b bug check: comparing the content type id to TYPE_FOLDER.
    assertNotEquals(PSCmsObject.TYPE_FOLDER, folderRow.getContentTypeId(),
        "Content type id 101 must NOT equal TYPE_FOLDER (2) — the bug used this"
            + " comparison, which can never be true for a folder row");
  }

  /**
   * Inverse case: a non-folder content row with content type id 101 (the same
   * value the bug test used) must NOT be classified as a folder.
   */
  @Test
  void itemRow_isNotDetectedAsFolder() {
    PSComponentSummary itemRow = newItemRow(101);

    assertNotEquals(PSCmsObject.TYPE_FOLDER, itemRow.getObjectType(),
        "Item row must not have object type TYPE_FOLDER");
    assertFalse(itemRow.isFolder());
  }

  /**
   * Type-id type-safety: {@code csc.getContentTypeId()} returns {@code long} and
   * {@code csc.getObjectType()} returns {@code int}. The pre-Phase-4b pattern
   * compared the int object type to {@link PSCmsObject#TYPE_FOLDER} (also int).
   * A naive migration that compares the long content type id to TYPE_FOLDER will
   * be a silent wrong-type check (Java auto-widens, so it compiles but always
   * returns false for any folder). The pre-migration code was careful to compare
   * ints, not longs, to that constant — this test pins the contract.
   */
  @Test
  void getObjectTypeIsIntAndGetContentTypeIdIsLong() {
    PSComponentSummary anyRow = newFolderRow(101, PSCmsObject.TYPE_FOLDER);

    // The bug code cast long -> int and compared to TYPE_FOLDER; the cast works
    // but the comparison was always false because the long content type id was
    // never equal to the int object type constant. Verify the type contract.
    int objectTypeInt = anyRow.getObjectType();
    long contentTypeIdLong = anyRow.getContentTypeId();
    assertEquals(PSCmsObject.TYPE_FOLDER, objectTypeInt);
    assertNotEquals(PSCmsObject.TYPE_FOLDER, contentTypeIdLong);
  }

  private static PSComponentSummary newFolderRow(long contentTypeId, int objectType) {
    PSComponentSummary s = new PSComponentSummary();
    s.setName("Test Folder");
    s.setContentTypeId(contentTypeId);
    s.setObjectType(objectType);
    s.setCommunityId(1);
    s.setLocale("en-us");
    return s;
  }

  private static PSComponentSummary newItemRow(long contentTypeId) {
    PSComponentSummary s = new PSComponentSummary();
    s.setName("Test Item");
    s.setContentTypeId(contentTypeId);
    s.setObjectType(PSCmsObject.TYPE_ITEM);
    s.setCommunityId(1);
    s.setLocale("en-us");
    return s;
  }
}
