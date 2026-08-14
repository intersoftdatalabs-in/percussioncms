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
package com.percussion.services.security;

import com.percussion.services.security.data.PSAclImpl;
import com.percussion.utils.guid.IPSGuid;

/**
 * Merge a converted REST/wire ACL onto an existing Hibernate identity so save
 * updates {@code PSX_ACLS} instead of inserting a duplicate {@code PK_PSX_ACLS}
 * (#3384).
 */
public final class PSAclPersistMerger {

  private PSAclPersistMerger() {}

  /**
   * Copy incoming name/description/entries onto {@code existing} while keeping
   * the persisted SYSID, version, and object identity when the wire object
   * omitted them.
   *
   * @param existing loaded ACL with a real SYSID, or {@code null} when this is
   *     a first insert
   * @param incoming converted payload, not {@code null} when {@code existing} is
   *     {@code null}
   * @return the instance Hibernate should merge/persist
   */
  public static PSAclImpl mergeOntoExisting(PSAclImpl existing, PSAclImpl incoming) {
    if (existing == null) {
      return incoming;
    }
    if (incoming == null || existing == incoming) {
      return existing;
    }
    Integer version = existing.getVersion();
    long id = existing.getId();
    IPSGuid guid = existing.getGUID();
    long objectId = existing.getObjectId();
    int objectType = existing.getObjectType();
    existing.merge(incoming);
    if (incoming.getVersion() == null && version != null) {
      existing.setVersion(version);
    }
    if (existing.getId() == 0 && id != 0) {
      existing.setId(id);
    }
    if (guid != null && (existing.getGUID() == null || existing.getGUID().longValue() == 0)) {
      existing.setGUID(guid);
    }
    if (incoming.getObjectId() <= 0 && objectId > 0) {
      existing.setObjectId(objectId);
    }
    if (incoming.getObjectType() <= 0 && objectType > 0) {
      existing.setObjectType(objectType);
    }
    return existing;
  }
}
