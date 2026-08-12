/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import com.percussion.cms.objectstore.PSSFields;
import com.percussion.cms.objectstore.PSSearch;
import com.percussion.cms.objectstore.PSSearchField;
import com.percussion.rest.Guid;
import com.percussion.rest.views.IViewAdaptor;
import com.percussion.rest.views.ViewDef;
import com.percussion.rest.views.ViewExecuteRequest;
import com.percussion.rest.views.ViewExecuteResult;
import com.percussion.rest.views.ViewFieldSummary;
import com.percussion.rest.views.ViewResultItem;
import com.percussion.search.IPSExecutableSearch;
import com.percussion.search.IPSSearchResultRow;
import com.percussion.search.PSExecutableSearchFactory;
import com.percussion.search.PSWSSearchResponse;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.data.PSPagedObjectList;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.ui.IPSUiDesignWs;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * CX view definition catalog (UI-07) plus standard-view execute façade for Explorer (#3115 / #3110
 * V1). Loads designs via {@link IPSUiDesignWs#findViews} / {@link IPSUiDesignWs#loadViews} — not
 * the search catalog.
 */
@PSSiteManageBean
@Lazy
public class ViewAdaptor implements IViewAdaptor {

  private static final Logger log = LogManager.getLogger(ViewAdaptor.class);

  /** Product default page size when design max is unset/unlimited and client omits maxResults. */
  static final int DEFAULT_PAGE_SIZE = 25;

  /**
   * Explicit 400 for Inbox-family custom URL views. Dedicated runner is #3118 — this façade must
   * not silently no-op.
   */
  static final String CUSTOM_VIEW_EXECUTE_UNSUPPORTED =
      "Custom URL views cannot be executed via this endpoint. Inbox and other custom-URL views"
          + " require a dedicated runner (issue #3118)";

  /** Catalog-level capability notes. Attached on detail only (REST-GAPS-02 list dedup). */
  static final List<String> DESIGN_GAPS =
      List.of(
          "View create / update / delete not supported via this API",
          "View field criterion editing not supported via this API",
          "Custom URL views (Inbox family) cannot be executed via this API; see Inbox / #3118",
          "Searches are a separate catalog (Developer Searches / UI-06)");

  private static final List<String> RESULT_COLUMNS =
      List.of("sys_contentid", "sys_title", "sys_contenttypename");

  private final IPSUiDesignWs designWs;
  private final IPSFolderHelper folderHelper;
  private final IPSIdMapper idMapper;

  @Autowired
  public ViewAdaptor(
      IPSUiDesignWs designWs, IPSFolderHelper folderHelper, IPSIdMapper idMapper) {
    this.designWs = designWs;
    this.folderHelper = folderHelper;
    this.idMapper = idMapper;
  }

  @Override
  public List<ViewDef> listViews() {
    try {
      List<PSSearch> loaded = loadAllViews();
      List<ViewDef> out = new ArrayList<>();
      for (PSSearch s : loaded) {
        if (s != null) {
          // REST-GAPS-02: list rows omit identical designGaps; detail re-attaches them.
          out.add(toDef(s, false));
        }
      }
      out.sort(
          Comparator.comparing(
              ViewDef::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
      return out;
    } catch (Exception e) {
      log.error("Failed to list views", e);
      throw new IllegalStateException("Failed to list views", e);
    }
  }

  @Override
  public ViewDef findViewByKey(String idOrName) {
    if (!isSafeViewKey(idOrName)) {
      return null;
    }
    try {
      PSSearch found = findPsViewByKey(idOrName.trim());
      return found != null ? toDef(found, true) : null;
    } catch (RuntimeException e) {
      // list failures already wrap; propagate for 500
      throw e;
    }
  }

  @Override
  public ViewExecuteResult executeView(String idOrName, ViewExecuteRequest request) {
    if (!isSafeViewKey(idOrName)) {
      return null;
    }
    ViewExecuteRequest effective = normalizeRequest(request);
    try {
      PSSearch design = findPsViewByKey(idOrName.trim());
      if (design == null) {
        return null;
      }
      if (design.isCustomView()) {
        throw new IllegalArgumentException(CUSTOM_VIEW_EXECUTE_UNSUPPORTED);
      }

      // Clone so folder/max overrides do not dirty a shared design instance
      PSSearch search = (PSSearch) design.clone();
      applyExecuteOverrides(search, effective);

      List<ViewResultItem> allItems = runDesignView(search);
      sortItems(allItems, effective.getSortColumn(), effective.getSortOrder());

      int startIndex = effective.getStartIndex() != null ? effective.getStartIndex() : 1;
      Integer maxResults = effective.getMaxResults();
      PSPagedObjectList<ViewResultItem> page =
          PSPagedObjectList.getPage(allItems, startIndex, maxResults);

      ViewExecuteResult result = new ViewExecuteResult();
      result.setChildren(new ArrayList<>(page.getChildrenInPage()));
      result.setTotalCount(page.getChildrenCount() != null ? page.getChildrenCount() : allItems.size());
      result.setStartIndex(page.getStartIndex() != null ? page.getStartIndex() : startIndex);
      result.setViewName(design.getName());
      result.setDisplayFormatId(design.getDisplayFormatId());
      return result;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to execute view {}", idOrName, e);
      throw new IllegalStateException("Failed to execute view", e);
    }
  }

  /**
   * Normalize and validate optional execute overrides. Returns a non-null request with defaults
   * applied where needed for validation only (maxResults default applied later from design when
   * client omits it).
   */
  static ViewExecuteRequest normalizeRequest(ViewExecuteRequest request) {
    ViewExecuteRequest out = request != null ? request : new ViewExecuteRequest();
    Integer start = out.getStartIndex();
    if (start != null && start < 1) {
      throw new IllegalArgumentException("startIndex must be >= 1");
    }
    Integer max = out.getMaxResults();
    if (max != null && max < 1) {
      throw new IllegalArgumentException("maxResults must be >= 1");
    }
    String sortOrder = out.getSortOrder();
    if (StringUtils.isNotBlank(sortOrder)) {
      String trimmed = sortOrder.trim().toLowerCase(Locale.ROOT);
      if (!"asc".equals(trimmed) && !"desc".equals(trimmed)) {
        throw new IllegalArgumentException("sortOrder must be asc or desc");
      }
      out.setSortOrder(trimmed);
    }
    if (StringUtils.isNotBlank(out.getSortColumn())) {
      out.setSortColumn(out.getSortColumn().trim());
    }
    if (StringUtils.isNotBlank(out.getFolderPath())) {
      out.setFolderPath(out.getFolderPath().trim());
    }
    return out;
  }

  static void applyExecuteOverrides(PSSearch search, ViewExecuteRequest req) {
    if (req == null || search == null) {
      return;
    }
    if (StringUtils.isNotBlank(req.getFolderPath())) {
      search.setProperty(PSSearch.PROP_FOLDER_PATH, req.getFolderPath().trim());
      // Default recurse when client scopes by folder (Explorer parity)
      if (StringUtils.isBlank(search.getProperty(PSSearch.PROP_FOLDER_PATH_RECURSE))) {
        search.setProperty(PSSearch.PROP_FOLDER_PATH_RECURSE, "true");
      }
    }
    if (req.getMaxResults() != null && req.getMaxResults() >= 1) {
      search.setMaximumNumber(req.getMaxResults());
    } else if (search.getMaximumResultSize() <= 0) {
      search.setMaximumNumber(DEFAULT_PAGE_SIZE);
    }
  }

  static void sortItems(List<ViewResultItem> items, String sortColumn, String sortOrder) {
    if (items == null || items.isEmpty() || StringUtils.isBlank(sortColumn)) {
      return;
    }
    String col = sortColumn.trim().toLowerCase(Locale.ROOT);
    int dir = "desc".equalsIgnoreCase(StringUtils.defaultString(sortOrder)) ? -1 : 1;
    Comparator<String> nullSafe = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
    Comparator<ViewResultItem> cmp;
    if ("sys_title".equals(col) || "title".equals(col) || "name".equals(col)) {
      cmp =
          Comparator.comparing(
              i -> StringUtils.defaultIfBlank(i.getTitle(), i.getName()), nullSafe);
    } else if ("type".equals(col) || "sys_contenttypename".equals(col)) {
      cmp = Comparator.comparing(ViewResultItem::getType, nullSafe);
    } else if ("folderpath".equals(col) || "path".equals(col)) {
      cmp = Comparator.comparing(ViewResultItem::getFolderPath, nullSafe);
    } else {
      return;
    }
    if (dir < 0) {
      cmp = cmp.reversed();
    }
    items.sort(cmp);
  }

  /**
   * Execute the design view ({@link PSSearch} of type view) via the local executable search path
   * (operators preserved). Package-visible for unit tests that subclass/spy can override.
   */
  List<ViewResultItem> runDesignView(PSSearch search) throws Exception {
    var request = PSWebserviceUtils.getRequest();
    if (request == null) {
      throw new IllegalStateException("No active request context for view execute");
    }
    IPSExecutableSearch executable =
        PSExecutableSearchFactory.createExecutableSearch(request, RESULT_COLUMNS, search);
    PSWSSearchResponse response = executable.executeSearch();
    return mapSearchResponse(response);
  }

  List<ViewResultItem> mapSearchResponse(PSWSSearchResponse response) {
    List<ViewResultItem> out = new ArrayList<>();
    if (response == null) {
      return out;
    }
    Iterator<IPSSearchResultRow> rows = response.getRows();
    while (rows != null && rows.hasNext()) {
      IPSSearchResultRow row = rows.next();
      if (row == null) {
        continue;
      }
      ViewResultItem item = mapResultRow(row);
      if (item != null) {
        out.add(item);
      }
    }
    return out;
  }

  ViewResultItem mapResultRow(IPSSearchResultRow row) {
    String contentId = row.getColumnValue("sys_contentid");
    if (StringUtils.isBlank(contentId)) {
      return null;
    }
    ViewResultItem item = new ViewResultItem();
    String title = row.getColumnValue("sys_title");
    String type = row.getColumnValue("sys_contenttypename");
    item.setName(title);
    item.setTitle(title);
    item.setType(type);

    try {
      var myGuid = PSGuidUtils.makeGuid(Integer.parseInt(contentId.trim()), PSTypeEnum.LEGACY_CONTENT);
      String stringId = idMapper.getString(myGuid);
      item.setId(stringId);
      if (folderHelper.getParentFolderId(myGuid, false) == null) {
        log.debug(
            "Item (id = {}) is not in a folder. It will not be included in the view results.",
            contentId);
        return null;
      }
      PSItemProperties props = folderHelper.findItemPropertiesById(stringId);
      if (props != null) {
        item.setId(props.getId() != null ? props.getId() : stringId);
        item.setName(StringUtils.defaultIfBlank(props.getName(), title));
        item.setTitle(
            StringUtils.defaultIfBlank(
                props.getSummary(), StringUtils.defaultIfBlank(props.getName(), title)));
        item.setFolderPath(props.getPath());
        if (StringUtils.isNotBlank(props.getType())) {
          item.setType(props.getType());
        }
      }
    } catch (NumberFormatException nfe) {
      log.debug("Skipping non-numeric content id from view row: {}", contentId);
      return null;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        log.warn("Could not enrich view result for content id {}: {}", contentId, e.toString());
      } else {
        log.debug("Could not enrich view result for content id {}: {}", contentId, e.toString());
      }
      item.setId(contentId);
    }
    return item;
  }

  private List<PSSearch> loadAllViews() throws Exception {
    List<IPSCatalogSummary> summaries = designWs.findViews(null, null);
    if (summaries == null || summaries.isEmpty()) {
      return List.of();
    }
    List<IPSGuid> guids = new ArrayList<>();
    for (IPSCatalogSummary sum : summaries) {
      if (sum != null && sum.getGUID() != null) {
        guids.add(sum.getGUID());
      }
    }
    if (guids.isEmpty()) {
      return List.of();
    }
    String currentUser = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
    String currentSession = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    List<PSSearch> loaded = designWs.loadViews(guids, false, false, currentSession, currentUser);
    return loaded != null ? loaded : List.of();
  }

  /** Resolve design view by name, GUID string, or numeric id. */
  PSSearch findPsViewByKey(String key) {
    try {
      List<PSSearch> loaded = loadAllViews();
      for (PSSearch s : loaded) {
        if (s == null) {
          continue;
        }
        if (key.equalsIgnoreCase(s.getName())) {
          return s;
        }
        if (s.getGUID() != null) {
          String gsv = s.getGUID().toString();
          if (StringUtils.isNotBlank(gsv) && key.equalsIgnoreCase(gsv)) {
            return s;
          }
          String untyped = s.getGUID().toStringUntyped();
          if (StringUtils.isNotBlank(untyped) && key.equalsIgnoreCase(untyped)) {
            return s;
          }
        }
        if (String.valueOf(s.getId()).equals(key)) {
          return s;
        }
      }
      return null;
    } catch (Exception e) {
      log.error("Failed to resolve view {}", key, e);
      throw new IllegalStateException("Failed to resolve view", e);
    }
  }

  /** Maps design view meta; includes designGaps by default (detail / unit-test path). */
  static ViewDef toDef(PSSearch s) {
    return toDef(s, true);
  }

  /**
   * @param includeDesignGaps when false, omits the shared static gap list (list-catalog path)
   */
  static ViewDef toDef(PSSearch s, boolean includeDesignGaps) {
    ViewDef d = new ViewDef();
    if (s.getGUID() != null) {
      d.setGuid(copyGuid(s.getGUID()));
    }
    d.setId(s.getId());
    d.setName(s.getName());
    d.setLabel(s.getLabel());
    d.setDescription(s.getDescription());
    d.setType(s.getType());
    d.setDisplayFormatId(s.getDisplayFormatId());
    d.setUrl(s.getUrl());
    d.setParentCategory(s.getParentCategory());
    d.setMaximumResultSize(s.getMaximumResultSize());
    d.setView(s.isView());
    d.setCustomView(s.isCustomView());
    d.setStandardView(s.isStandardView());
    d.setUserCustomizable(s.isUserCustomizable());
    d.setCaseSensitive(s.isCaseSensitive());
    d.setFields(mapFields(s.getFieldContainer()));
    d.setDesignGaps(includeDesignGaps ? new ArrayList<>(DESIGN_GAPS) : null);
    return d;
  }

  /** Attach catalog designGaps to a list projection for the detail response. */
  static ViewDef withDesignGaps(ViewDef listRow) {
    if (listRow == null) {
      return null;
    }
    listRow.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    return listRow;
  }

  static List<ViewFieldSummary> mapFields(PSSFields fields) {
    List<ViewFieldSummary> out = new ArrayList<>();
    if (fields == null) {
      return out;
    }
    for (int i = 0; i < fields.size(); i++) {
      Object o = fields.get(i);
      if (!(o instanceof PSSearchField sf) || sf == null) {
        continue;
      }
      ViewFieldSummary row = new ViewFieldSummary();
      row.setFieldName(sf.getFieldName());
      row.setDisplayName(sf.getDisplayName());
      row.setOperator(sf.getOperator());
      row.setFieldValue(sf.getFieldValue());
      row.setFieldType(sf.getFieldType());
      row.setPosition(sf.getPosition());
      out.add(row);
    }
    return out;
  }

  private static Guid copyGuid(IPSGuid guid) {
    Guid g = new Guid();
    g.setHostId(guid.getHostId());
    g.setLongValue(guid.longValue());
    g.setStringValue(guid.toString());
    g.setType(guid.getType());
    g.setUuid(guid.getUUID());
    g.setUntypedString(guid.toStringUntyped());
    return g;
  }

  /**
   * Single path component / guid token only — reject traversal and separators ({@code
   * java/path-injection}).
   */
  static boolean isSafeViewKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0;
  }
}
