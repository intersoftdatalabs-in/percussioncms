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
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.ui.IPSUiDesignWs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    // Catalog identity comes from find summaries (unique INTERNALNAME). Bulk
    // loadDisplayFormats(all GUIDs) can replay the first format (By_Author)
    // for every row (#3269 / #3200). Prefer the single bulk call when every
    // loaded name is unique and matches the summary; otherwise per-name load
    // (then GUID load) and last a summary-only row.
    var summaries = designWs.findDisplayFormats(null, null);
    var uniqueByName = new LinkedHashMap<String, IPSCatalogSummary>();
    if (summaries != null) {
      for (var summary : summaries) {
        if (summary == null) {
          continue;
        }
        String name = summary.getName();
        if (name == null || name.isBlank()) {
          continue;
        }
        uniqueByName.putIfAbsent(name, summary);
      }
    }
    if (uniqueByName.isEmpty()) {
      return new ArrayList<>();
    }
    List<DisplayFormat> fromBulk = tryCopyBulkLoad(uniqueByName);
    if (fromBulk != null) {
      return fromBulk;
    }
    var ret = new ArrayList<DisplayFormat>(uniqueByName.size());
    for (var summary : uniqueByName.values()) {
      ret.add(copyUniqueSummary(summary));
    }
    return ret;
  }

  /**
   * One {@code loadDisplayFormats} when every row is a distinct named format.
   *
   * @return copied rows, or {@code null} to use the per-name path (replay / miss)
   */
  private List<DisplayFormat> tryCopyBulkLoad(LinkedHashMap<String, IPSCatalogSummary> uniqueByName)
      throws PSCmsException, PSUnknownNodeTypeException {
    var guids = new ArrayList<IPSGuid>(uniqueByName.size());
    for (var summary : uniqueByName.values()) {
      if (summary.getGUID() == null) {
        return null;
      }
      guids.add(summary.getGUID());
    }
    List<PSDisplayFormat> loaded;
    try {
      var currentUser = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
      var currentSession = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
      loaded = designWs.loadDisplayFormats(guids, false, false, currentSession, currentUser);
    } catch (PSErrorResultsException | RuntimeException e) {
      log.debug("Bulk display-format load failed, using per-name path: {}", e.toString());
      return null;
    }
    if (loaded == null || loaded.size() != uniqueByName.size()) {
      return null;
    }
    var expectedNames = new ArrayList<>(uniqueByName.keySet());
    Set<String> seen = new HashSet<>();
    var ret = new ArrayList<DisplayFormat>(loaded.size());
    for (int i = 0; i < loaded.size(); i++) {
      PSDisplayFormat format = loaded.get(i);
      if (format == null) {
        return null;
      }
      String loadedName = format.getName();
      if (loadedName == null
          || loadedName.isBlank()
          || !seen.add(loadedName.toLowerCase(Locale.ROOT))) {
        return null;
      }
      if (!namesMatchIgnoreCase(expectedNames.get(i), loadedName)) {
        return null;
      }
      ret.add(copyDisplayFormat(format));
    }
    return ret;
  }

  /**
   * Name-matched load, then GUID load, then summary-only identity. Name match
   * requires a non-null loaded name (avoids {@code equalsIgnoreCase(null)} NPE).
   */
  private DisplayFormat copyUniqueSummary(IPSCatalogSummary summary)
      throws PSCmsException, PSUnknownNodeTypeException {
    String name = summary.getName();
    PSDisplayFormat loaded = designWs.findDisplayFormat(name);
    if (loaded != null && namesMatchIgnoreCase(name, loaded.getName())) {
      return copyDisplayFormat(loaded);
    }
    if (summary.getGUID() != null) {
      PSDisplayFormat byGuid = designWs.findDisplayFormat(summary.getGUID());
      if (byGuid != null && namesMatchIgnoreCase(name, byGuid.getName())) {
        return copyDisplayFormat(byGuid);
      }
    }
    return copyFromCatalogSummary(summary);
  }

  private static boolean namesMatchIgnoreCase(String expected, String actual) {
    return expected != null && actual != null && expected.equalsIgnoreCase(actual);
  }

  private DisplayFormatPropertyList copyDisplayFormatProps(PSDFProperties props) {
    // TODO: Implement property copying if needed.
    return new DisplayFormatPropertyList(new ArrayList<>());
  }

  /**
   * Last-resort catalog row when name and GUID loads missed or replayed another
   * format. {@code IPSCatalogSummary} does not expose columns, community, or
   * valid-for flags — those stay Java defaults. Detail GET by name still loads
   * the full format.
   *
   * @param summary unique-name catalog hit; never {@code null}
   * @return REST row with name/label/guid from the summary
   */
  private DisplayFormat copyFromCatalogSummary(IPSCatalogSummary summary) {
    var ret = new DisplayFormat();
    ret.setName(summary.getName());
    ret.setInternalName(summary.getName());
    ret.setLabel(summary.getLabel());
    ret.setDisplayName(summary.getLabel());
    ret.setDescription(summary.getDescription());
    Guid mapped = copyGuid(summary.getGUID());
    ret.setGuid(mapped);
    int displayId = 0;
    if (summary.getGUID() != null) {
      displayId = summary.getGUID().getUUID();
    }
    if (displayId > 0) {
      ret.setDisplayId(displayId);
    }
    ret.setGuidString(plainGuidString(mapped, displayId));
    return ret;
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
