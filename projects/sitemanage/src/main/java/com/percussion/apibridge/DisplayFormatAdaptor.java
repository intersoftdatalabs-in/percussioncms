/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.apibridge;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSDFColumns;
import com.percussion.cms.objectstore.PSDFProperties;
import com.percussion.cms.objectstore.PSDisplayColumn;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.rest.Guid;
import com.percussion.rest.displayformat.*;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.ui.IPSUiDesignWs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/** Provides the API implementation for the Display Format Resource. */
@PSSiteManageBean
@Lazy
public class DisplayFormatAdaptor implements IDisplayFormatAdaptor {

  private static final Logger log = LogManager.getLogger(DisplayFormatAdaptor.class);

  private final IPSUiDesignWs designWs;

  @Autowired
  public DisplayFormatAdaptor(IPSUiDesignWs designWs) {
    this.designWs = designWs;
  }

  @Override
  public List<DisplayFormat> createDisplayFormats(List<String> names, String session, String user) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void deleteDisplayFormats(
      List<IPSGuid> ids, boolean ignoreDependencies, String session, String user) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public List<DisplayFormat> findAllDisplayFormats()
      throws PSCmsException, PSErrorResultsException, PSUnknownNodeTypeException {
    var ret = new ArrayList<DisplayFormat>();
    var displayFormats = designWs.findDisplayFormats(null, null);
    var guids = new ArrayList<IPSGuid>();
    for (var c : displayFormats) {
      guids.add(c.getGUID());
    }
    var currentUser = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
    var currentSession = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    var dfs = designWs.loadDisplayFormats(guids, false, false, currentSession, currentUser);
    for (var f : dfs) {
      ret.add(copyDisplayFormat(f));
    }
    return ret;
  }

  private DisplayFormatPropertyList copyDisplayFormatProps(PSDFProperties props) {
    // TODO: Implement property copying if needed.
    return new DisplayFormatPropertyList(new ArrayList<>());
  }

  private DisplayFormat copyDisplayFormat(PSDisplayFormat f)
      throws PSCmsException, PSUnknownNodeTypeException {
    var ret = new DisplayFormat();
    if (f.getPropertyContainer() != null) {
      ret.setProperties(copyDisplayFormatProps(f.getPropertyContainer()));
    }
    ret.setInternalName(f.getInternalName());
    if (f.getColumnContainer() != null) {
      ret.setColumns(copyDisplayFormatColumns(f.getColumnContainer()));
    }
    if (f.getAllowedCommunities() != null) {
      ret.setAllowedCommunities(copyAllowedCommunities(f.getAllowedCommunities()));
    }
    ret.setAscendingSort(f.isAscendingSort());
    ret.setDescendingSort(f.isDescendingSort());
    ret.setValidForRelatedContent(f.isValidForRelatedContent());
    ret.setValidForViewsAndSearches(f.isValidForViewsAndSearches());
    ret.setValidForFolder(f.isValidForFolder());
    ret.setInvalidFolderFieldNames(f.getInvalidFolderFieldNames());
    ret.setDisplayId(f.getDisplayId());
    ret.setName(f.getName());
    ret.setLabel(f.getLabel());
    ret.setDescription(f.getDescription());
    ret.setDisplayName(f.getDisplayName());
    // Always map GUID so Developer detail / Object ACL can bind objectGuid
    // (issues #2689 / #2951 / #3200). PSDisplayFormat#getGUID is expected
    // non-null for persisted formats; copyGuid still accepts null defensively.
    Guid mapped = copyGuid(f.getGUID());
    ret.setGuid(mapped);
    ret.setGuidString(plainGuidString(mapped, f.getDisplayId()));
    return ret;
  }

  /**
   * Plain {@code host-type-uuid} for SPA binding when nested Guid JSON is hard to read.
   *
   * @param mapped REST Guid from {@link #copyGuid}; may be null
   * @param displayId native display id; used when mapped string is blank
   * @return wire string, or {@code null} when neither source has a usable id
   */
  private static String plainGuidString(Guid mapped, int displayId) {
    if (mapped != null && mapped.getStringValue().isPresent()) {
      String sv = mapped.getStringValue().get();
      if (sv != null && !sv.isBlank()) {
        return sv.trim();
      }
    }
    if (displayId > 0) {
      return new PSGuid(PSTypeEnum.DISPLAY_FORMAT, displayId).toString();
    }
    return null;
  }

  /**
   * Copy a design-object GUID into the REST {@link Guid} DTO with a guaranteed
   * {@code stringValue} ({@code host-type-uuid}) for SPA Object ACL binding.
   *
   * @param guid design GUID; may be null only in defensive/edge cases
   * @return REST Guid DTO, or {@code null} when {@code guid} is null
   */
  private Guid copyGuid(IPSGuid guid) {
    if (guid == null) {
      // Defensive: interface type does not prove non-null at compile time.
      return null;
    }
    var g = new Guid();
    g.setHostId(guid.getHostId());
    g.setLongValue(guid.longValue());
    g.setType(guid.getType());
    g.setUuid(guid.getUUID());
    String asString = guid.toString();
    if (asString == null || asString.isBlank()) {
      // Defensive: synthesize the same wire form PSGuid#toString uses.
      asString = guid.getHostId() + "-" + guid.getType() + "-" + guid.getUUID();
    }
    g.setStringValue(asString);
    try {
      g.setUntypedString(guid.toStringUntyped());
    } catch (RuntimeException e) {
      log.debug("Could not copy untyped GUID string: {}", e.toString());
    }
    return g;
  }

  private Map<Guid, String> copyAllowedCommunities(Map<IPSGuid, String> allowedCommunities) {
    var ret = new HashMap<Guid, String>();
    for (var e : allowedCommunities.entrySet()) {
      ret.put(copyGuid(e.getKey()), e.getValue());
    }
    return ret;
  }

  private DisplayFormatColumnList copyDisplayFormatColumns(PSDFColumns columnContainer) {
    var ret = new DisplayFormatColumnList(new ArrayList<>());
    for (int i = 0; i < columnContainer.size(); i++) {
      var col = (PSDisplayColumn) columnContainer.get(i);
      var dfc = new DisplayFormatColumn();
      dfc.setAscendingSort(col.isAscendingSort());
      dfc.setCategorized(col.isCategorized());
      dfc.setDateType(col.isDateType());
      dfc.setDescendingSort(col.isDescendingSort());
      dfc.setDescription(col.getDescription());
      dfc.setDisplayId(col.getDisplayId());
      dfc.setDisplayName(col.getDisplayName());
      dfc.setImageType(col.isImageType());
      dfc.setNumberType(col.isNumberType());
      dfc.setPosition(col.getPosition());
      dfc.setWidth(col.getWidth());
      dfc.setRenderType(col.getRenderType());
      dfc.setTextType(col.isTextType());
      dfc.setSource(col.getSource());
      dfc.setSortOrder(dfc.isAscendingSort());
      ret.add(dfc);
    }
    return ret;
  }

  @Override
  public DisplayFormat findDisplayFormat(IPSGuid id)
      throws PSCmsException, PSUnknownNodeTypeException {
    PSDisplayFormat f = designWs.findDisplayFormat(id);
    return f == null ? null : copyDisplayFormat(f);
  }

  @Override
  public DisplayFormat findDisplayFormat(String name)
      throws PSCmsException, PSUnknownNodeTypeException {
    PSDisplayFormat f = designWs.findDisplayFormat(name);
    return f == null ? null : copyDisplayFormat(f);
  }

  @Override
  public void saveDisplayFormats(
      List<DisplayFormat> displayFormats, boolean release, String session, String user) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public DisplayFormat findDisplayFormatByKey(String idOrName) {
    if (!isSafeDisplayFormatKey(idOrName)) {
      return null;
    }
    String key = idOrName.trim();
    try {
      return findDisplayFormat(key);
    } catch (PSCmsException | PSUnknownNodeTypeException e) {
      // Expected miss → fall through to GUID parse / null
      log.debug("Display format not found by name {}: {}", key, e.toString());
    }
    try {
      var guid = new com.percussion.services.guidmgr.data.PSGuid(key);
      return findDisplayFormat((IPSGuid) guid);
    } catch (IllegalArgumentException e) {
      log.debug("Invalid display format GUID syntax: {}", e.getMessage());
      return null;
    } catch (PSCmsException | PSUnknownNodeTypeException e) {
      log.debug("Display format not found by GUID {}: {}", key, e.toString());
      return null;
    }
  }

  /**
   * Single path component / guid token only — reject traversal and separators ({@code
   * java/path-injection}).
   */
  static boolean isSafeDisplayFormatKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0;
  }
}
