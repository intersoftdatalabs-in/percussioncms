/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import com.percussion.cms.objectstore.PSSFields;
import com.percussion.cms.objectstore.PSSearch;
import com.percussion.cms.objectstore.PSSearchField;
import com.percussion.rest.Guid;
import com.percussion.rest.searches.ISearchAdaptor;
import com.percussion.rest.searches.SearchDef;
import com.percussion.rest.searches.SearchExecuteRequest;
import com.percussion.rest.searches.SearchExecuteResult;
import com.percussion.rest.searches.SearchFieldSummary;
import com.percussion.rest.searches.SearchResultItem;
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
 * Read-only CX search definition catalog (UI-06) plus design-search execute façade for Explorer
 * (#2505 / #2409 slice B).
 */
@PSSiteManageBean
@Lazy
public class SearchAdaptor implements ISearchAdaptor {

  private static final Logger log = LogManager.getLogger(SearchAdaptor.class);

  /** Product default page size when design max is unset/unlimited and client omits maxResults. */
  static final int DEFAULT_PAGE_SIZE = 25;

  /** Catalog-level capability notes. Attached on detail only (REST-GAPS-02 list dedup). */
  static final List<String> DESIGN_GAPS =
      List.of(
          "Search create / update / delete not supported via this API",
          "Search field criterion editing not supported via this API",
          "Views are a separate catalog (Developer Views / UI-07)");

  private static final List<String> RESULT_COLUMNS =
      List.of("sys_contentid", "sys_title", "sys_contenttypename");

  private final IPSUiDesignWs designWs;
  private final IPSFolderHelper folderHelper;
  private final IPSIdMapper idMapper;

  @Autowired
  public SearchAdaptor(
      IPSUiDesignWs designWs, IPSFolderHelper folderHelper, IPSIdMapper idMapper) {
    this.designWs = designWs;
    this.folderHelper = folderHelper;
    this.idMapper = idMapper;
  }

  @Override
  public List<SearchDef> listSearches() {
    return listSearches(false);
  }

  /**
   * Developer catalog is searches-only. Explorer saved-search picker passes {@code
   * includeViews=true} so the default All view ({@code View_All}) is listed and executable.
   */
  @Override
  public List<SearchDef> listSearches(boolean includeViews) {
    List<SearchDef> out = new ArrayList<>();
    Exception searchFailure = null;
    try {
      addSearchDefs(out, loadAllSearches());
    } catch (Exception e) {
      searchFailure = e;
      log.error("Failed to list searches", e);
    }
    Exception viewFailure = null;
    if (includeViews) {
      try {
        addSearchDefs(out, loadAllViews());
      } catch (Exception e) {
        viewFailure = e;
        log.error("Failed to list views for the Explorer search catalog", e);
      }
    }
    if (out.isEmpty() && (searchFailure != null || viewFailure != null)) {
      Exception cause = searchFailure != null ? searchFailure : viewFailure;
      throw new IllegalStateException("Failed to list searches", cause);
    }
    out.sort(
        Comparator.comparing(
            SearchDef::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    return out;
  }

  @Override
  public SearchDef findSearchByKey(String idOrName) {
    if (!isSafeSearchKey(idOrName)) {
      return null;
    }
    try {
      PSSearch found = findPsSearchByKey(idOrName.trim());
      return found != null ? toDef(found, true) : null;
    } catch (RuntimeException e) {
      // list failures already wrap; propagate for 500
      throw e;
    }
  }

  @Override
  public SearchExecuteResult executeSearch(String idOrName, SearchExecuteRequest request) {
    if (!isSafeSearchKey(idOrName)) {
      return null;
    }
    SearchExecuteRequest effective = normalizeRequest(request);
    try {
      PSSearch design = findPsSearchByKey(idOrName.trim());
      if (design == null) {
        return null;
      }
      if (design.isCustomSearch() || design.isCustomView()) {
        throw new IllegalArgumentException(
            "Custom URL searches cannot be executed via this endpoint");
      }

      // Clone so folder/max overrides do not dirty a shared design instance
      PSSearch search = (PSSearch) design.clone();
      applyExecuteOverrides(search, effective);

      List<SearchResultItem> allItems = runDesignSearch(search);
      sortItems(allItems, effective.getSortColumn(), effective.getSortOrder());

      int startIndex = effective.getStartIndex() != null ? effective.getStartIndex() : 1;
      Integer maxResults = effective.getMaxResults();
      PSPagedObjectList<SearchResultItem> page =
          PSPagedObjectList.getPage(allItems, startIndex, maxResults);

      SearchExecuteResult result = new SearchExecuteResult();
      result.setChildren(new ArrayList<>(page.getChildrenInPage()));
      result.setTotalCount(page.getChildrenCount() != null ? page.getChildrenCount() : allItems.size());
      result.setStartIndex(page.getStartIndex() != null ? page.getStartIndex() : startIndex);
      result.setSearchName(design.getName());
      result.setDisplayFormatId(design.getDisplayFormatId());
      return result;
    } catch (RuntimeException e) {
      // IllegalArgumentException (400) and other runtime errors propagate to JAX-RS as-is
      throw e;
    } catch (Exception e) {
      log.error("Failed to execute search {}", idOrName, e);
      throw new IllegalStateException("Failed to execute search", e);
    }
  }

  /**
   * Normalize and validate optional execute overrides. Returns a non-null request with defaults
   * applied where needed for validation only (maxResults default applied later from design when
   * client omits it).
   */
  static SearchExecuteRequest normalizeRequest(SearchExecuteRequest request) {
    SearchExecuteRequest out = request != null ? request : new SearchExecuteRequest();
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
    out.setFolderPath(normalizeFolderPath(out.getFolderPath()));
    return out;
  }

  /**
   * Repository folder filter for execute ({@code //Sites/...}).
   *
   * <p>Explorer seeds {@code /} as the tree root. {@code getIdByPath("//")} is not a folder
   * — {@code PSLocalExecutableSearch} wraps that as a nameless {@code IOException} and the
   * operator sees {@code An error occurred while using search web service. Message:
   * java.io.IOException} (#3517). Drop root / blank; convert a single leading slash to the
   * {@code //} form {@code getIdByPath} requires.
   *
   * <p>CMS paths always use {@code /} (not OS file separators). Backslashes and a drive
   * letter are stripped only so a pasted Windows-style string still becomes a repository
   * path.
   *
   * @return {@code //Sites/...} form, or {@code null} when the path is empty or Explorer
   *     root (unscoped execute)
   */
  static String normalizeFolderPath(String path) {
    if (StringUtils.isBlank(path)) {
      return null;
    }
    String p = path.trim().replace('\\', '/');
    if (p.length() >= 2 && Character.isLetter(p.charAt(0)) && p.charAt(1) == ':') {
      p = p.substring(2);
    }
    p = p.replaceAll("/{2,}", "/");
    if (p.isEmpty() || "/".equals(p)) {
      return null;
    }
    if (!p.startsWith("/")) {
      p = "/" + p;
    }
    if ("/".equals(p)) {
      return null;
    }
    return "/" + p;
  }

  static void applyExecuteOverrides(PSSearch search, SearchExecuteRequest req) {
    if (req == null || search == null) {
      return;
    }
    String folderPath = normalizeFolderPath(req.getFolderPath());
    if (folderPath != null) {
      search.setProperty(PSSearch.PROP_FOLDER_PATH, folderPath);
      // Default recurse when client scopes by folder (Explorer parity)
      if (StringUtils.isBlank(search.getProperty(PSSearch.PROP_FOLDER_PATH_RECURSE))) {
        search.setProperty(PSSearch.PROP_FOLDER_PATH_RECURSE, "true");
      }
    }
    if (req.getMaxResults() != null && req.getMaxResults() >= 1) {
      search.setMaximumNumber(req.getMaxResults());
    } else if (search.getMaximumResultSize() <= 0) {
      // Unlimited / unset design max → product default page size for execute
      search.setMaximumNumber(DEFAULT_PAGE_SIZE);
    }
  }

  static void sortItems(List<SearchResultItem> items, String sortColumn, String sortOrder) {
    if (items == null || items.isEmpty() || StringUtils.isBlank(sortColumn)) {
      return;
    }
    String col = sortColumn.trim().toLowerCase(Locale.ROOT);
    int dir = "desc".equalsIgnoreCase(StringUtils.defaultString(sortOrder)) ? -1 : 1;
    Comparator<String> nullSafe = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
    Comparator<SearchResultItem> cmp;
    if ("sys_title".equals(col) || "title".equals(col) || "name".equals(col)) {
      cmp =
          Comparator.comparing(
              i -> StringUtils.defaultIfBlank(i.getTitle(), i.getName()), nullSafe);
    } else if ("type".equals(col) || "sys_contenttypename".equals(col)) {
      cmp = Comparator.comparing(SearchResultItem::getType, nullSafe);
    } else if ("folderpath".equals(col) || "path".equals(col)) {
      cmp = Comparator.comparing(SearchResultItem::getFolderPath, nullSafe);
    } else {
      // Unsupported sort column: leave natural order (engine order)
      return;
    }
    if (dir < 0) {
      cmp = cmp.reversed();
    }
    items.sort(cmp);
  }

  /**
   * Execute the design {@link PSSearch} via the local executable search path (operators preserved).
   * Package-visible for unit tests that subclass/spy can override.
   */
  List<SearchResultItem> runDesignSearch(PSSearch search) throws Exception {
    var request = PSWebserviceUtils.getRequest();
    if (request == null) {
      throw new IllegalStateException("No active request context for search execute");
    }
    IPSExecutableSearch executable =
        PSExecutableSearchFactory.createExecutableSearch(request, RESULT_COLUMNS, search);
    PSWSSearchResponse response = executable.executeSearch();
    return mapSearchResponse(response);
  }

  List<SearchResultItem> mapSearchResponse(PSWSSearchResponse response) {
    List<SearchResultItem> out = new ArrayList<>();
    if (response == null) {
      return out;
    }
    Iterator<IPSSearchResultRow> rows = response.getRows();
    while (rows != null && rows.hasNext()) {
      IPSSearchResultRow row = rows.next();
      if (row == null) {
        continue;
      }
      SearchResultItem item = mapResultRow(row);
      if (item != null) {
        out.add(item);
      }
    }
    return out;
  }

  SearchResultItem mapResultRow(IPSSearchResultRow row) {
    String contentId = row.getColumnValue("sys_contentid");
    if (StringUtils.isBlank(contentId)) {
      return null;
    }
    SearchResultItem item = new SearchResultItem();
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
            "Item (id = {}) is not in a folder. It will not be included in the search results.",
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
      log.debug("Skipping non-numeric content id from search row: {}", contentId);
      return null;
    } catch (Exception e) {
      // Keep partial row so execute still returns engine hits when enrichment fails.
      // Expected gaps (checked/service) stay DEBUG; unexpected runtime at WARN for ops.
      if (e instanceof RuntimeException) {
        log.warn("Could not enrich search result for content id {}: {}", contentId, e.toString());
      } else {
        log.debug("Could not enrich search result for content id {}: {}", contentId, e.toString());
      }
      item.setId(contentId);
    }
    return item;
  }

  private List<PSSearch> loadAllSearches() throws Exception {
    return loadCatalog(false);
  }

  private List<PSSearch> loadAllViews() throws Exception {
    return loadCatalog(true);
  }

  private List<PSSearch> loadCatalog(boolean views) throws Exception {
    List<IPSCatalogSummary> summaries =
        views ? designWs.findViews(null, null) : designWs.findSearches(null, null);
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
    List<PSSearch> loaded =
        views
            ? designWs.loadViews(guids, false, false, currentSession, currentUser)
            : designWs.loadSearches(guids, false, false, currentSession, currentUser);
    return loaded != null ? loaded : List.of();
  }

  private static void addSearchDefs(List<SearchDef> out, List<PSSearch> loaded) {
    if (loaded == null) {
      return;
    }
    for (PSSearch s : loaded) {
      if (s != null) {
        // REST-GAPS-02: list rows omit identical designGaps; detail re-attaches them.
        out.add(toDef(s, false));
      }
    }
  }

  /** Resolve design search or view by name, label, GUID string, or numeric id. */
  PSSearch findPsSearchByKey(String key) {
    if (!isSafeSearchKey(key)) {
      return null;
    }
    String trimmed = key.trim();
    Exception first = null;
    boolean searchCatalogOk = false;
    boolean viewCatalogOk = false;
    try {
      PSSearch found = matchLoaded(loadAllSearches(), trimmed);
      searchCatalogOk = true;
      if (found != null) {
        return found;
      }
    } catch (Exception e) {
      first = e;
      log.error("Failed to resolve search {} from search catalog", trimmed, e);
    }
    try {
      PSSearch found = matchLoaded(loadAllViews(), trimmed);
      viewCatalogOk = true;
      if (found != null) {
        return found;
      }
    } catch (Exception e) {
      log.error("Failed to resolve search {} from view catalog", trimmed, e);
      if (first == null) {
        first = e;
      }
    }
    // Only 500 when both catalogs failed. One healthy catalog + miss is 404.
    if (first != null && !searchCatalogOk && !viewCatalogOk) {
      throw new IllegalStateException("Failed to resolve search", first);
    }
    return null;
  }

  static PSSearch matchLoaded(List<PSSearch> loaded, String key) {
    if (loaded == null || StringUtils.isBlank(key)) {
      return null;
    }
    PSSearch labelMatch = null;
    for (PSSearch s : loaded) {
      if (s == null) {
        continue;
      }
      if (key.equalsIgnoreCase(s.getName())) {
        return s;
      }
      if (s.getGUID() != null) {
        // IPSGuid has no Optional string; guard blank/sentinel toString before match
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
      if (labelMatch == null && key.equalsIgnoreCase(s.getLabel())) {
        labelMatch = s;
      }
    }
    return labelMatch;
  }

  /** Maps design search meta; includes designGaps by default (detail / unit-test path). */
  static SearchDef toDef(PSSearch s) {
    return toDef(s, true);
  }

  /**
   * @param includeDesignGaps when false, omits the shared static gap list (list-catalog path)
   */
  static SearchDef toDef(PSSearch s, boolean includeDesignGaps) {
    SearchDef d = new SearchDef();
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
    d.setUserSearch(s.isUserSearch());
    // Custom views are URL-backed like custom searches — picker must not execute them.
    d.setCustomSearch(s.isCustomSearch() || s.isCustomView());
    d.setStandardSearch(s.isStandardSearch());
    d.setUserCustomizable(s.isUserCustomizable());
    d.setCaseSensitive(s.isCaseSensitive());
    d.setFields(mapFields(s.getFieldContainer()));
    d.setDesignGaps(includeDesignGaps ? new ArrayList<>(DESIGN_GAPS) : null);
    return d;
  }

  static List<SearchFieldSummary> mapFields(PSSFields fields) {
    List<SearchFieldSummary> out = new ArrayList<>();
    if (fields == null) {
      return out;
    }
    for (int i = 0; i < fields.size(); i++) {
      Object o = fields.get(i);
      if (!(o instanceof PSSearchField sf) || sf == null) {
        continue;
      }
      SearchFieldSummary row = new SearchFieldSummary();
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
  static boolean isSafeSearchKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0;
  }
}
