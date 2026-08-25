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
package com.percussion.services.guidmgr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link PSGuidUtils#toFullLong(IPSGuid)} must keep type bits when host is 0 so
 * lock queries match {@code PSObjectLock} objectId (issue #3772).
 */
class PSGuidUtilsToFullLongTest {

  /** percPage typeId; packed NODEDEF+1001 is the live PUT OBJECT_NOT_LOCKED id. */
  private static final long PERC_PAGE_TYPE_ID = 1001L;

  private static final long PACKED_NODEDEF_PERC_PAGE = 8_589_935_593L;

  @Test
  void hostZeroNodeDefLongValueIsUuidOnlyButToFullLongIsPacked() {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, PERC_PAGE_TYPE_ID);
    assertEquals(0, guid.getHostId());
    assertEquals(PERC_PAGE_TYPE_ID, guid.longValue());
    assertEquals(PACKED_NODEDEF_PERC_PAGE, new PSDesignGuid(guid).getValue());
    assertEquals(PACKED_NODEDEF_PERC_PAGE, PSGuidUtils.toFullLong(guid));
    assertNotEquals(guid.longValue(), PSGuidUtils.toFullLong(guid));
  }

  @Test
  void toFullLongListUsesPackedValuesNotUuidOnly() {
    IPSGuid loadId = new PSGuid(PSTypeEnum.NODEDEF, PERC_PAGE_TYPE_ID);
    IPSGuid saveId = new PSDesignGuid(PSTypeEnum.NODEDEF, PERC_PAGE_TYPE_ID);
    assertEquals(List.of(PACKED_NODEDEF_PERC_PAGE), PSGuidUtils.toFullLongList(List.of(loadId)));
    assertEquals(
        PSGuidUtils.toFullLongList(List.of(loadId)), PSGuidUtils.toFullLongList(List.of(saveId)));
  }

  @Test
  void toFullLongRejectsNull() {
    assertThrows(NullPointerException.class, () -> PSGuidUtils.toFullLong(null));
  }

  @Test
  void toFullLongListRejectsNull() {
    assertThrows(NullPointerException.class, () -> PSGuidUtils.toFullLongList(null));
  }

  @Test
  void hostNonZeroKeepsFullPackedValue() {
    IPSGuid guid = new PSGuid(12L, PSTypeEnum.NODEDEF, PERC_PAGE_TYPE_ID);
    assertEquals(guid.longValue(), PSGuidUtils.toFullLong(guid));
    assertEquals(new PSDesignGuid(guid).getValue(), PSGuidUtils.toFullLong(guid));
  }
}
