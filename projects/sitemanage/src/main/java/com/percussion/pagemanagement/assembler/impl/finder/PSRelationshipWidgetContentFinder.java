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

package com.percussion.pagemanagement.assembler.impl.finder;

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.injectDependencies;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.pagemanagement.assembler.PSWidgetInstance;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.impl.finder.PSContentFinderBase;
import com.percussion.services.assembly.impl.finder.PSRelationshipFinderUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.utils.guid.IPSGuid;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

/**
 * Find contents related to a page and/or template by Active Assembly relationships where the
 * "sys_slotid" property equals to the specified widget instance.
 *
 * <p>The parameters of the finder are:
 *
 * <ul>
 *   <li><b>max_results</b> - Optional parameter. It is the maximum number of the returned result
 *       from the find method if specified, zero or negative indicates no limit. It defaults to zero
 *       if not specified.
 *   <li><b>order_by</b> - Optional parameter. If it is specified, then the returned items will be
 *       re-ordered according to the specified value; otherwise the returned items are ordered by
 *       {@link PSContentFinderBase.ContentItem}.
 * </ul>
 *
 * @author YuBingChen
 */
@Transactional(readOnly = true, noRollbackFor = Exception.class)
public class PSRelationshipWidgetContentFinder extends PSWidgetContentFinder {
  public static final String IS_MATCH_BY_NAME = "IS_MATCH_BY_NAME";

  @Override
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
    m_finderUtils.init();
    injectDependencies(this);
  }

  public List<PSRelationship> findRelationshipByOwner(IPSGuid owner) {
    return m_finderUtils.findRelationshipByOwner(owner);
  }

  public boolean isMatchRelationship(
      PSRelationship rel, WidgetCriteria criteria, Map<String, Object> params) {
    return m_finderUtils.isTargetRelationship(rel, criteria, params);
  }

  @Override
  protected Set<ContentItem> getContentItems(
      IPSAssemblyItem sourceItem, PSWidgetInstance widget, Map<String, Object> params)
      throws PSNotFoundException {
    if (!(widget instanceof PSWidgetInstance)) {
      throw new IllegalArgumentException("Cannot create widget criteria from object: " + widget);
    }
    var criteria = new WidgetCriteria(widget);
    return m_finderUtils.getContentItems(sourceItem, criteria, params);
  }

  protected Comparator<ContentItem> getComparator(PSWidgetInstance widget)
      throws PSNotFoundException {
    return new ContentItemOrder(widget);
  }

  private class ContentItemOrder implements Comparator<ContentItem> {
    private final WidgetCriteria m_criteria;

    public ContentItemOrder(PSWidgetInstance widget) throws PSNotFoundException {
      m_criteria = new WidgetCriteria(widget);
    }

    @Override
    public int compare(ContentItem s1, ContentItem s2) {
      notNull(s1);
      notNull(s2);

      if (isBlank(m_criteria.widgetName)) {
        return compareUnnamed(s1, s2);
      }
      if (isBlank(s1.getWidgetName()) && isNotBlank(s2.getWidgetName())) {
        return 1;
      }
      if (isNotBlank(s1.getWidgetName()) && isBlank(s2.getWidgetName())) {
        return -1;
      }
      return compareUnnamed(s1, s2);
    }

    private int compareUnnamed(ContentItem s1, ContentItem s2) {
      if (s1.getSortrank() != s2.getSortrank()) {
        return Integer.compare(s1.getSortrank(), s2.getSortrank());
      }
      var id1 = Optional.ofNullable(s1.getRelationshipId()).orElse(s1.getItemId());
      var id2 = Optional.ofNullable(s2.getRelationshipId()).orElse(s2.getItemId());
      return Long.compare(id1.longValue(), id2.longValue());
    }
  }

  public static class WidgetCriteria {
    private final long widgetId;
    private final String widgetName;
    private final long contentTypeId;

    public WidgetCriteria(PSWidgetInstance widget) throws PSNotFoundException {
      notNull(widget, "widget");
      var w = widget;
      var ctType =
          w.getDefinition().getWidgetPrefs().map(prefs -> prefs.getContenttypeName()).orElse(null);
      try {
        contentTypeId =
            StringUtils.isEmpty(ctType)
                ? -1L
                : PSItemDefManager.getInstance().contentTypeNameToId(ctType);
      } catch (PSInvalidContentTypeException e) {
        var errMsg = "Cannot find content type name = " + ctType;
        m_log.error(errMsg, e);
        throw new PSNotFoundException(errMsg);
      }
      widgetId = Long.parseLong(w.getItem().getId());
      widgetName = w.getItem().getName().orElse(null);
    }

    public String getWidgetName() {
      return widgetName;
    }
  }

  private class ContentFinderUtils extends PSRelationshipFinderUtils<WidgetCriteria> {
    @Override
    protected boolean isTargetRelationship(
        PSRelationship rel, WidgetCriteria criteria, Map<String, Object> params) {
      if (!isMatchByName(params)) {
        return super.matchesSlotId(rel, criteria.widgetId);
      }
      var relWidgetName = rel.getProperty(PSRelationshipConfig.PDU_WIDGET_NAME);
      if (isBlank(relWidgetName) && isBlank(criteria.widgetName)) {
        return super.matchesSlotId(rel, criteria.widgetId);
      }
      if (isBlank(relWidgetName) && isNotBlank(criteria.widgetName)) {
        return super.matchesSlotId(rel, criteria.widgetId);
      }
      if (!equalsIgnoreCase(criteria.widgetName, relWidgetName)) {
        return false;
      }
      return matchContentType(rel, criteria);
    }

    private boolean isMatchByName(Map<String, Object> params) {
      if (params == null) {
        return true;
      }
      var val = params.get(IS_MATCH_BY_NAME);
      return val instanceof Boolean ? (Boolean) val : true;
    }

    private boolean matchContentType(PSRelationship r, WidgetCriteria criteria) {
      var depId = r.getDependent().getId();
      var item = getCmsMgr().findItemEntry(depId);
      return item != null && item.getContentTypeId() == criteria.contentTypeId;
    }

    public List<PSRelationship> findRelationshipByOwner(IPSGuid id) {
      return super.getRelationships(id);
    }
  }

  private IPSCmsObjectMgr getCmsMgr() {
    if (m_cmsMgr != null) {
      return m_cmsMgr;
    }
    m_cmsMgr = PSCmsObjectMgrLocator.getObjectManager();
    return m_cmsMgr;
  }

  private IPSCmsObjectMgr m_cmsMgr;
  private final ContentFinderUtils m_finderUtils = new ContentFinderUtils();

  private static final Logger m_log = LogManager.getLogger(PSRelationshipWidgetContentFinder.class);
}
