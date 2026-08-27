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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.data.PSNodeDefinition;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.locking.data.PSObjectLock;
import com.percussion.utils.guid.IPSGuid;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * Load ({@code nodeDef.getGUID()}) and save ({@code contentTypeLockObjectId}) must share the packed
 * lock objectId so {@code findLockByObjectId} finds a lock created with {@code lock=true} (issue
 * #3772).
 */
class PSContentDesignWsContentTypeLockObjectIdTest {

  private static final long PERC_PAGE_TYPE_ID = 1001L;

  private static final long PACKED_NODEDEF_PERC_PAGE = 8_589_935_593L;

  @Test
  void contentTypeLockObjectIdIsPackedNodeDefDesignGuid() {
    IPSGuid id = PSContentDesignWs.contentTypeLockObjectId(PERC_PAGE_TYPE_ID);
    assertEquals(PSTypeEnum.NODEDEF.getOrdinal(), id.getType());
    assertEquals(PERC_PAGE_TYPE_ID, id.getUUID());
    assertEquals(PACKED_NODEDEF_PERC_PAGE, new PSDesignGuid(id).getValue());
    assertEquals(PACKED_NODEDEF_PERC_PAGE, PSGuidUtils.toFullLong(id));
  }

  @Test
  void loadGuidAndSaveGuidSharePackedLockObjectId() throws Exception {
    PSNodeDefinition nodeDef = new PSNodeDefinition();
    nodeDef.setGUID(new PSGuid(PSTypeEnum.NODEDEF, PERC_PAGE_TYPE_ID));
    IPSGuid loadId = nodeDef.getGUID();
    IPSGuid saveId = PSContentDesignWs.contentTypeLockObjectId(PERC_PAGE_TYPE_ID);

    assertEquals(PSGuidUtils.toFullLong(loadId), PSGuidUtils.toFullLong(saveId));
    assertEquals(PACKED_NODEDEF_PERC_PAGE, PSGuidUtils.toFullLong(saveId));
    // Trap that caused PUT OBJECT_NOT_LOCKED: uuid-only longValue() != persisted objectId
    assertEquals(PERC_PAGE_TYPE_ID, loadId.longValue());
    assertNotEquals(loadId.longValue(), PSGuidUtils.toFullLong(saveId));
  }

  @Test
  void persistedLockObjectIdMatchesSaveLookup() throws Exception {
    IPSGuid loadId = new PSGuid(PSTypeEnum.NODEDEF, PERC_PAGE_TYPE_ID);
    IPSGuid saveId = PSContentDesignWs.contentTypeLockObjectId(PERC_PAGE_TYPE_ID);

    PSObjectLock lock = new PSObjectLock();
    Method setObjectId = PSObjectLock.class.getDeclaredMethod("setObjectId", IPSGuid.class);
    setObjectId.setAccessible(true);
    setObjectId.invoke(lock, loadId);

    Field objectId = PSObjectLock.class.getDeclaredField("objectId");
    objectId.setAccessible(true);
    long stored = objectId.getLong(lock);

    assertEquals(stored, PSGuidUtils.toFullLong(saveId));
    assertEquals(stored, PSGuidUtils.toFullLong(loadId));
    assertEquals(PACKED_NODEDEF_PERC_PAGE, stored);
  }

  @Test
  void contentTypeSaveVersionPrefersLockThenExistingNodeNeverMinusOneWhenNodeExists() {
    assertEquals(7, PSContentDesignWs.contentTypeSaveVersion(7, 3));
    assertEquals(0, PSContentDesignWs.contentTypeSaveVersion(0, 9));
    assertEquals(4, PSContentDesignWs.contentTypeSaveVersion(null, 4));
    assertEquals(4, PSContentDesignWs.contentTypeSaveVersion(-1, 4));
    assertEquals(-1, PSContentDesignWs.contentTypeSaveVersion(null, null));
    assertEquals(-1, PSContentDesignWs.contentTypeSaveVersion(-1, null));
  }

  @Test
  void rootCauseMessageSkipsUnfilledTemplate() {
    RuntimeException root = new IllegalArgumentException("dataset type mismatch");
    Exception wrapped = new RuntimeException("An unknown exception occurred while communicating with the server: {0}", root);
    assertEquals("dataset type mismatch", PSContentDesignWs.rootCauseMessage(wrapped));
    assertEquals("unknown error", PSContentDesignWs.rootCauseMessage(null));
  }
}
