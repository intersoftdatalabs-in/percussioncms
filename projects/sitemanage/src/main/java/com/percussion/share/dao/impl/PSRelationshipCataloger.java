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
package com.percussion.share.dao.impl;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.share.dao.IPSRelationshipCataloger;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@PSSiteManageBean("relationshipCataloger")
public class PSRelationshipCataloger implements IPSRelationshipCataloger {

  private final IPSSystemWs systemWs;
  private final IPSIdMapper idMapper;
  private final PSItemDefManager itemDefManager;

  @Autowired
  public PSRelationshipCataloger(
      IPSIdMapper idMapper, PSItemDefManager itemDefManager, IPSSystemWs systemWs) {
    this.idMapper = idMapper;
    this.itemDefManager = itemDefManager;
    this.systemWs = systemWs;
  }

  @Override
  public List<String> findOwners(String id, String name, String contentType, String slotName) {
    var guid = idMapper.getGuid(id);
    var filter = new PSRelationshipFilter();
    filter.setName(name);
    if (contentType != null) {
      var ctid = getContentTypeId(contentType);
      filter.setOwnerContentTypeId(ctid);
    }
    var guids = systemWs.findOwners(guid, filter);
    var ids = new ArrayList<String>();
    guids.forEach(gid -> ids.add(idMapper.getString(gid)));
    return ids;
  }

  private long getContentTypeId(String contentType) {
    try {
      return itemDefManager.contentTypeNameToId(contentType);
    } catch (PSInvalidContentTypeException e) {
      throw new RuntimeException("Invalid content type name", e);
    }
  }
}
