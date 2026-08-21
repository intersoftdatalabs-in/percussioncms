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
package com.percussion.share.dao.impl;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import jakarta.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/** Implements {@link IPSIdMapper}. */
@Provider
@PSSiteManageBean("sys_idMapper")
public class PSIdMapper implements IPSIdMapper {

  private static final String LOCAL_CONTENT_KEY = "PSX_LOCAL_CONTENT";
  private IPSGuidManager guidMgr;
  private IPSContentDesignWs contentDesignWs;

  @Autowired
  public PSIdMapper(IPSGuidManager guidMgr, IPSContentDesignWs contentDesignWs) {
    notNull(guidMgr);
    notNull(contentDesignWs);
    this.guidMgr = guidMgr;
    this.contentDesignWs = contentDesignWs;
  }

  @Override
  public IPSGuid getGuid(String id) {
    notNull(id);
    notEmpty(id);
    Long contentId = parseBareNumericContentId(id);
    if (contentId != null) {
      return getGuidFromContentId(contentId);
    }
    return guidMgr.makeGuid(id);
  }

  @Override
  public IPSGuid getGuid(String id, PSTypeEnum type) {
    notNull(id);
    notEmpty(id);
    return guidMgr.makeGuid(id, type);
  }

  @Override
  public IPSGuid getGuid(String id, PSTypeEnum type, boolean forceType) {
    notNull(id);
    notEmpty(id);
    return guidMgr.makeGuid(id, type, forceType);
  }

  @Override
  public int getContentId(IPSGuid guid) {
    notNull(guid);
    return ((PSLegacyGuid) guid).getContentId();
  }

  @Override
  public int getContentId(String guid) {
    notEmpty(guid);
    return getContentId(getGuid(guid));
  }

  @Override
  public IPSGuid getItemGuid(String id) {
    notEmpty(id);
    var guid = getGuid(id);
    return contentDesignWs.getItemGuid(guid);
  }

  @Override
  public List<IPSGuid> getGuids(List<String> ids) {
    notNull(ids);
    var guids = new ArrayList<IPSGuid>();
    ids.forEach(id -> guids.add(getGuid(id)));
    return guids;
  }

  @Override
  public String getString(IPSGuid id) {
    if (id == null) throw new IllegalArgumentException("id may not be null");
    return id.toString();
  }

  @Override
  public List<String> getStrings(List<IPSGuid> ids) {
    notNull(ids);
    var result = new ArrayList<String>();
    ids.forEach(id -> result.add(getString(id)));
    return result;
  }

  @Override
  public String getString(PSLocator locator) {
    if (locator == null) throw new IllegalArgumentException("locator may not be null");
    return getString(getGuid(locator));
  }

  @Override
  public IPSGuid getGuid(PSLocator locator) {
    if (locator == null) throw new IllegalArgumentException("locator may not be null");
    return guidMgr.makeGuid(locator);
  }

  @Override
  public PSLocator getLocator(String id) {
    if (StringUtils.isBlank(id)) throw new IllegalArgumentException("id may not be blank");
    var guid = getGuid(id);
    return getLocator(guid);
  }

  @Override
  public PSLocator getLocator(IPSGuid id) {
    if (id == null) throw new IllegalArgumentException("id may not be null");
    var guid = contentDesignWs.getItemGuid(id);
    return guidMgr.makeLocator(guid);
  }

  @Override
  public int getLocalContentId() {
    return guidMgr.createId(LOCAL_CONTENT_KEY);
  }

  @Override
  public IPSGuid getGuidFromContentId(long id) {
    return guidMgr.makeGuid(id, PSTypeEnum.LEGACY_CONTENT);
  }

  /**
   * Explorer Preview / editor view call {@code GET .../workflow/checkIn/{id}} with a
   * bare numeric content id (FastForward sample {@code 594}). {@link
   * com.percussion.services.guidmgr.data.PSGuid} treats a single numeric token with
   * type bits 32–39 equal to zero as undetermined unless a {@link PSTypeEnum} is
   * supplied. Those tokens are legacy content ids.
   *
   * @param id never {@code null}
   * @return the content id, or {@code null} when {@code id} is a hyphenated GUID
   *     string or a packed long that already carries a type
   */
  static Long parseBareNumericContentId(String id) {
    var trimmed = id.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c < '0' || c > '9') {
        return null;
      }
    }
    try {
      long raw = Long.parseLong(trimmed);
      int type = (int) ((raw >>> 32) & 0xFFL);
      return type == 0 ? raw : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
