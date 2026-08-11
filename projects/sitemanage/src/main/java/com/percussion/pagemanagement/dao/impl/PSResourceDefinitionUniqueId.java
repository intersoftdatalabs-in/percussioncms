// REFACTORED: CP-JAVA11
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
package com.percussion.pagemanagement.dao.impl;

import static org.apache.commons.lang3.StringUtils.*;

import com.percussion.pagemanagement.service.IPSResourceDefinitionService;
import com.percussion.pagemanagement.service.IPSResourceDefinitionService.PSResourceDefinitionInvalidIdException;
import java.util.Objects;

/** Unique identifier for a resource definition, composed of groupId and localId. */
public class PSResourceDefinitionUniqueId {
  private String groupId;
  private String localId;

  public PSResourceDefinitionUniqueId(String uniqueId)
      throws PSResourceDefinitionInvalidIdException {
    super();
    init(uniqueId);
  }

  public PSResourceDefinitionUniqueId(String groupId, String localId)
      throws PSResourceDefinitionInvalidIdException {
    super();
    init(groupId, localId);
  }

  public final void init(String groupId, String localId) throws PSResourceDefinitionInvalidIdException {
    setGroupId(groupId);
    setLocalId(localId);
  }

  public String getGroupId() {
    return groupId;
  }

  public final void setGroupId(String groupId) throws PSResourceDefinitionInvalidIdException {
    validateId("groupId", groupId);
    this.groupId = groupId;
  }

  public String getLocalId() {
    return localId;
  }

  public final void setLocalId(String localId) throws PSResourceDefinitionInvalidIdException {
    validateId("localId", localId);
    this.localId = localId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSResourceDefinitionUniqueId)) return false;
    var that = (PSResourceDefinitionUniqueId) o;
    return Objects.equals(getGroupId(), that.getGroupId())
        && Objects.equals(getLocalId(), that.getLocalId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getGroupId(), getLocalId());
  }

  public String getUniqueId() {
    return groupId + IPSResourceDefinitionService.NAMESPACE_SEPARATOR + localId;
  }

  public final void init(String uniqueId) throws PSResourceDefinitionInvalidIdException {
    if (isBlank(uniqueId)) {
      throw new PSResourceDefinitionInvalidIdException(
          "PSResourceDefinitionUniqueId cannot be blank");
    }
    var pair = split(uniqueId, ".");
    if (pair.length == 2 && isNotBlank(pair[0]) && isNotBlank(pair[1])) {
      setGroupId(pair[0]);
      setLocalId(pair[1]);
    } else {
      throw new PSResourceDefinitionInvalidIdException(
          "PSResourceDefinitionUniqueId is not valid. value: " + uniqueId);
    }
  }

  public static void validateId(String name, String id)
      throws PSResourceDefinitionInvalidIdException {
    if (isBlank(id)) {
      throw new PSResourceDefinitionInvalidIdException(name + " cannot be blank");
    }
    if (contains(id, ".")) {
      throw new PSResourceDefinitionInvalidIdException(
          name + " cannot contain " + IPSResourceDefinitionService.NAMESPACE_SEPARATOR);
    }
  }
}
