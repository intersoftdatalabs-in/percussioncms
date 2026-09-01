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
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.ui.IPSUiDesignWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * CX search definition catalog (UI-06 list/detail/write, UI-08 field criteria) plus design-search
 * execute façade for Explorer (#2505 / #2409 slice B). Admin create/save/delete persist through
 * {@link IPSUiDesignWs} — the same design web service SOAP uses. Execute is not invoked on write.
 */
@PSSiteManageBean
@Lazy
public class SearchAdaptor implements ISearchAdaptor {

  private static final Logger log = LogManager.getLogger(SearchAdaptor.class);

  static final String ADMIN_REQUIRED = "Admin role required to create, update, or delete searches";

  /** Product default page size when design max is unset/unlimited and client omits maxResults. */
  static final int DEFAULT_PAGE_SIZE = 25;

  /** Catalog-level capability notes. Attached on detail only (REST-GAPS-02 list dedup). */
  static final List<String> DESIGN_GAPS =
      List.of(
          "Views are a separate catalog (Developer Views / UI-07)",
          "Search rename is not supported on PUT (name is the catalog key)");

  /**
   * Installer / perc.System search names — field criteria are not mutated from this catalog (409).
   */
  static final List<String> PACKAGED_SEARCH_NAMES = List.of("Default_Search", "RC_Search");

  private static final List<String> RESULT_COLUMNS =
      List.of("sys_contentid", "sys_title", "sys_contenttypename");

  private final IPSUiDesignWs designWs;
  private final IPSFolderHelper folderHelper;
  private final IPSIdMapper idMapper;
  private final BooleanSupplier adminChecker;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  @Autowired
  public SearchAdaptor(
      IPSUiDesignWs designWs, IPSFolderHelper folderHelper, IPSIdMapper idMapper) {
    this(designWs, folderHelper, idMapper, null);
  }

  /** Package-visible for unit tests. */
  SearchAdaptor(
      IPSUiDesignWs designWs,
      IPSFolderHelper folderHelper,
      IPSIdMapper idMapper,
      BooleanSupplier adminChecker) {
    this.designWs = designWs;
    this.folderHelper = folderHelper;
    this.idMapper = idMapper;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
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
  public SearchDef createSearch(SearchDef body) {
    requireAdmin();
    requireSessionUserForWrite();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String name = requireValidName(body.getName());
    assertNameUnique(name);
    String type = resolveSearchType(body.getType(), true);
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSSearch> created = designWs.createSearches(List.of(name), List.of(type), session, user);
      if (created == null || created.isEmpty() || created.get(0) == null) {
        throw new IllegalStateException("Design WS createSearches returned empty");
      }
      PSSearch domain = created.get(0);
      applyWritableFields(domain, body);
      designWs.saveSearches(List.of(domain), true, session, user);
      return toDef(reloadAfterWrite(domain, name, session, user), true);
    } catch (WebApplicationException | IllegalStateException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      if (isAlreadyExistsFailure(e)) {
        throw new WebApplicationException("Search already exists: " + name, 409);
      }
      throw e;
    } catch (PSLockErrorException e) {
      throw new WebApplicationException(
          "Could not create search; design lock required or held by another user", 409);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("create", e);
    } catch (PSErrorException e) {
      log.error("Failed to create search {}", name, e);
      throw new IllegalStateException("Failed to create search", e);
    }
  }

  @Override
  public SearchDef saveSearch(String idOrName, SearchDef body) {
    requireAdmin();
    requireSessionUserForWrite();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    if (!isSafeSearchKey(idOrName)) {
      return null;
    }
    PSSearch existing = findPsSearchByKey(idOrName.trim());
    if (existing == null) {
      return null;
    }
    rejectViewWrite(existing);
    IPSGuid id = existing.getGUID();
    if (id == null) {
      return null;
    }
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSSearch> loaded =
          designWs.loadSearches(List.of(id), true, false, session, user);
      if (loaded == null || loaded.isEmpty() || loaded.get(0) == null) {
        return null;
      }
      PSSearch domain = loaded.get(0);
      applyWritableFields(domain, body);
      designWs.saveSearches(List.of(domain), true, session, user);
      String catalogName = StringUtils.defaultIfBlank(domain.getName(), idOrName.trim());
      return toDef(reloadAfterWrite(domain, catalogName, session, user), true);
    } catch (WebApplicationException | IllegalArgumentException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      if (isNotFound(e, id)) {
        return null;
      }
      throw new WebApplicationException(
          "Could not update search; design lock required or held by another user", 409);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("update", e);
    }
  }

  @Override
  public boolean deleteSearch(String idOrName) {
    requireAdmin();
    requireSessionUserForWrite();
    if (!isSafeSearchKey(idOrName)) {
      return false;
    }
    PSSearch existing = findPsSearchByKey(idOrName.trim());
    if (existing == null) {
      return false;
    }
    rejectViewWrite(existing);
    IPSGuid id = existing.getGUID();
    if (id == null) {
      return false;
    }
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSSearch> locked =
          designWs.loadSearches(List.of(id), true, false, session, user);
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        throw new WebApplicationException(
            "Could not delete search; design lock required or held by another user", 409);
      }
      designWs.deleteSearches(List.of(id), false, session, user);
      return true;
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      if (isNotFound(e, id)) {
        return false;
      }
      throw new WebApplicationException(
          "Could not delete search; design lock required or held by another user", 409);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("delete", e);
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
    // find+loadSearches remaps H2 rows to View_All (same hole as UI-07).
    // findAllSearches returns the objects already parsed from getSearches.xml.
    List<PSSearch> all = designWs.findAllSearches();
    return all != null ? all : List.of();
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
      if (found == null) {
        found = loadSearchByNameSummary(trimmed);
      }
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
    out.sort(Comparator.comparingInt(SearchFieldSummary::getPosition));
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
   * Apply GET-exposed writable fields: label, description, type, displayFormatId, and field
   * criteria when {@code body.fields} is non-null. Custom URL is persisted when type is custom;
   * execute is never invoked from write.
   */
  static void applyWritableFields(PSSearch domain, SearchDef body) {
    if (domain == null || body == null) {
      return;
    }
    if (StringUtils.isNotBlank(body.getLabel())) {
      domain.setDisplayName(body.getLabel().trim());
    }
    if (body.getDescription() != null) {
      domain.setDescription(body.getDescription());
    }
    String type = resolveSearchType(body.getType(), false);
    if (type != null) {
      domain.setType(type);
      if (PSSearch.TYPE_CUSTOMSEARCH.equals(type)) {
        domain.setCustom(true);
      }
    }
    if (StringUtils.isNotBlank(body.getDisplayFormatId())) {
      domain.setDisplayFormatId(body.getDisplayFormatId().trim());
    }
    boolean custom =
        domain.isCustomSearch()
            || PSSearch.TYPE_CUSTOMSEARCH.equals(type);
    if (custom && StringUtils.isNotBlank(body.getUrl())) {
      domain.setUrl(body.getUrl().trim());
    }
    if (body.getFields() != null) {
      if (isPackagedSearch(domain.getName())) {
        throw new WebApplicationException(
            "Packaged/system searches cannot be field-edited", 409);
      }
      applyFields(domain, body.getFields());
    }
  }

  /** Installer catalog searches are not field-edited from this REST surface. */
  static boolean isPackagedSearch(String name) {
    if (StringUtils.isBlank(name)) {
      return false;
    }
    String key = name.trim();
    for (String packaged : PACKAGED_SEARCH_NAMES) {
      if (packaged.equalsIgnoreCase(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Replace native field criteria from the REST list. {@code null} fields on the body leave the
   * existing list unchanged (label-only PUT). An empty list clears all criteria. Existing fields
   * are updated in place so save does not drop the parent search row.
   */
  static void applyFields(PSSearch domain, List<SearchFieldSummary> fields) {
    if (domain == null || fields == null) {
      return;
    }
    PSSFields container = domain.getFieldContainer();
    List<PSSearchField> desired = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    int index = 0;
    for (SearchFieldSummary dto : fields) {
      if (dto == null) {
        continue;
      }
      String fieldName = requireValidFieldName(dto.getFieldName());
      String key = fieldName.toLowerCase(Locale.ROOT);
      if (!seen.add(key)) {
        throw new IllegalArgumentException("duplicate field: " + fieldName);
      }
      String label =
          StringUtils.isNotBlank(dto.getDisplayName()) ? dto.getDisplayName().trim() : fieldName;
      String fieldType = resolveFieldType(dto.getFieldType());
      String operator = resolveFieldOperator(dto.getOperator());
      PSSearchField sf = findFieldByName(container, fieldName);
      if (sf == null) {
        sf = new PSSearchField(fieldName, label, "", fieldType, "");
      } else {
        if (StringUtils.isNotBlank(dto.getDisplayName())) {
          sf.setDisplayName(label);
        }
        sf.setFieldType(fieldType);
      }
      applyFieldValue(sf, operator, dto.getFieldValue());
      sf.setPosition(dto.getPosition() >= 0 ? dto.getPosition() : index);
      desired.add(sf);
      index++;
    }
    if (container == null) {
      domain.setFields(desired.iterator());
      return;
    }
    List<PSSearchField> toRemove = new ArrayList<>();
    for (int i = 0; i < container.size(); i++) {
      Object o = container.get(i);
      if (!(o instanceof PSSearchField existing) || existing == null) {
        continue;
      }
      String existingName = existing.getFieldName();
      if (existingName == null
          || !seen.contains(existingName.toLowerCase(Locale.ROOT))) {
        toRemove.add(existing);
      }
    }
    if (!toRemove.isEmpty()) {
      container.removeFields(toRemove.iterator());
    }
    for (PSSearchField sf : desired) {
      if (findFieldByName(container, sf.getFieldName()) == null) {
        container.add(sf);
      }
    }
    reorderFields(container, desired);
  }

  /**
   * Align container order with the PUT list. Uses {@link PSSFields#move} so existing rows are not
   * marked for delete (unlike {@link PSSFields#set}).
   */
  static void reorderFields(PSSFields container, List<PSSearchField> desired) {
    if (container == null || desired == null || desired.isEmpty()) {
      return;
    }
    for (int i = 0; i < desired.size() && i < container.size(); i++) {
      PSSearchField want = desired.get(i);
      if (want == null || StringUtils.isBlank(want.getFieldName())) {
        continue;
      }
      Object at = container.get(i);
      if (at instanceof PSSearchField current
          && want.getFieldName().equalsIgnoreCase(current.getFieldName())) {
        current.setPosition(i);
        continue;
      }
      int from = indexOfField(container, want.getFieldName());
      if (from >= 0 && from != i) {
        container.move(from, i);
      }
    }
  }

  static int indexOfField(PSSFields container, String fieldName) {
    if (container == null || StringUtils.isBlank(fieldName)) {
      return -1;
    }
    String key = fieldName.trim().toLowerCase(Locale.ROOT);
    for (int i = 0; i < container.size(); i++) {
      Object o = container.get(i);
      if (o instanceof PSSearchField sf
          && sf.getFieldName() != null
          && key.equals(sf.getFieldName().toLowerCase(Locale.ROOT))) {
        return i;
      }
    }
    return -1;
  }

  static PSSearchField findFieldByName(PSSFields container, String fieldName) {
    if (container == null || StringUtils.isBlank(fieldName)) {
      return null;
    }
    String key = fieldName.trim().toLowerCase(Locale.ROOT);
    for (int i = 0; i < container.size(); i++) {
      Object o = container.get(i);
      if (o instanceof PSSearchField sf
          && sf.getFieldName() != null
          && key.equals(sf.getFieldName().toLowerCase(Locale.ROOT))) {
        return sf;
      }
    }
    return null;
  }

  static String requireValidFieldName(String raw) {
    if (StringUtils.isBlank(raw)) {
      throw new IllegalArgumentException("unknown field");
    }
    String fieldName = raw.trim();
    if (containsWhitespace(fieldName)) {
      throw new IllegalArgumentException("unknown field: " + fieldName);
    }
    if (fieldName.contains("*") || fieldName.contains("%")) {
      throw new IllegalArgumentException("unknown field: " + fieldName);
    }
    if (fieldName.contains("..")
        || fieldName.indexOf('/') >= 0
        || fieldName.indexOf('\\') >= 0
        || fieldName.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("unknown field: " + fieldName);
    }
    if (fieldName.length() > PSSearchField.FIELDNAME_LENGTH) {
      throw new IllegalArgumentException("unknown field: " + fieldName);
    }
    return fieldName;
  }

  static String resolveFieldType(String raw) {
    if (StringUtils.isBlank(raw)) {
      return PSSearchField.TYPE_TEXT;
    }
    String t = raw.trim();
    if (t.equalsIgnoreCase(PSSearchField.TYPE_TEXT)
        || t.equalsIgnoreCase("text")) {
      return PSSearchField.TYPE_TEXT;
    }
    if (t.equalsIgnoreCase(PSSearchField.TYPE_NUMBER)
        || t.equalsIgnoreCase("number")) {
      return PSSearchField.TYPE_NUMBER;
    }
    if (t.equalsIgnoreCase(PSSearchField.TYPE_DATE)
        || t.equalsIgnoreCase("date")) {
      return PSSearchField.TYPE_DATE;
    }
    throw new IllegalArgumentException("unknown field type: " + raw);
  }

  static String resolveFieldOperator(String raw) {
    if (StringUtils.isBlank(raw)) {
      return PSSearchField.OP_LIKE;
    }
    String op = raw.trim();
    if (op.equals("=") || op.equalsIgnoreCase("eq") || op.equalsIgnoreCase("equals")) {
      return PSSearchField.OP_EQUALS;
    }
    String[] allowed =
        new String[] {
          PSSearchField.OP_EQUALS,
          PSSearchField.OP_NOTEQUAL,
          PSSearchField.OP_LESSTHAN,
          PSSearchField.OP_LESSTHANEQUAL,
          PSSearchField.OP_GREATERTHAN,
          PSSearchField.OP_GREATERTHANEQUAL,
          PSSearchField.OP_ISNULL,
          PSSearchField.OP_ISNOTNULL,
          PSSearchField.OP_IN,
          PSSearchField.OP_NOTIN,
          PSSearchField.OP_LIKE,
          PSSearchField.OP_NOTLIKE,
          PSSearchField.OP_BETWEEN
        };
    for (String candidate : allowed) {
      if (candidate.equalsIgnoreCase(op)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("unknown field operator: " + raw);
  }

  static void applyFieldValue(PSSearchField field, String operator, String value) {
    if (field == null) {
      return;
    }
    String op = StringUtils.defaultIfBlank(operator, PSSearchField.OP_LIKE);
    if (PSSearchField.OP_ISNULL.equalsIgnoreCase(op)
        || PSSearchField.OP_ISNOTNULL.equalsIgnoreCase(op)) {
      field.setFieldValues(op, List.of());
      return;
    }
    String v = value == null ? "" : value;
    if (v.length() > PSSearchField.FIELDVALUE_LENGTH) {
      throw new IllegalArgumentException(
          "field value must not exceed " + PSSearchField.FIELDVALUE_LENGTH + " characters");
    }
    field.setFieldValue(op, v);
  }

  /**
   * Map REST type aliases to {@link PSSearch} TYPE_* values. Blank on create defaults to
   * {@link PSSearch#TYPE_STANDARDSEARCH}. View types are rejected (UI-07).
   *
   * @param creating when true, blank type becomes StandardSearch; when false, blank means leave
   *     unchanged ({@code null})
   */
  static String resolveSearchType(String raw, boolean creating) {
    if (StringUtils.isBlank(raw)) {
      return creating ? PSSearch.TYPE_STANDARDSEARCH : null;
    }
    String t = raw.trim();
    if (t.equalsIgnoreCase(PSSearch.TYPE_STANDARDSEARCH)
        || t.equalsIgnoreCase("standard")
        || t.equalsIgnoreCase("_standard")) {
      return PSSearch.TYPE_STANDARDSEARCH;
    }
    if (t.equalsIgnoreCase(PSSearch.TYPE_CUSTOMSEARCH)
        || t.equalsIgnoreCase("custom")
        || t.equalsIgnoreCase("_custom")) {
      return PSSearch.TYPE_CUSTOMSEARCH;
    }
    if (t.equalsIgnoreCase(PSSearch.TYPE_USERSEARCH)
        || t.equalsIgnoreCase("user")
        || t.equalsIgnoreCase("usersearch")) {
      return PSSearch.TYPE_USERSEARCH;
    }
    if (t.equalsIgnoreCase(PSSearch.TYPE_VIEW) || t.equalsIgnoreCase("view")) {
      throw new IllegalArgumentException(
          "Views are not writable on /services/searches; use /services/views");
    }
    throw new IllegalArgumentException("Invalid search type: " + raw);
  }

  static String requireValidName(String raw) {
    if (StringUtils.isBlank(raw)) {
      throw new IllegalArgumentException("name is required");
    }
    String name = raw.trim();
    if (containsWhitespace(name)) {
      throw new IllegalArgumentException("name cannot contain whitespace");
    }
    if (name.contains("*") || name.contains("%")) {
      throw new IllegalArgumentException("name must not contain wildcards");
    }
    if (!isSafeSearchKey(name)) {
      throw new IllegalArgumentException("invalid name");
    }
    if (name.length() > PSSearch.INTERNALNAME_LENGTH) {
      throw new IllegalArgumentException(
          "name must not exceed " + PSSearch.INTERNALNAME_LENGTH + " characters");
    }
    return name;
  }

  private static boolean containsWhitespace(String name) {
    for (int i = 0; i < name.length(); i++) {
      if (Character.isWhitespace(name.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * After {@code saveSearches}, the row must be visible to {@code findSearches} (H2 REST
   * UI-06: POST 200 then GET list/detail 404 when the XML resource cache or INSERT was
   * skipped). Load by GUID only after the catalog lists the name.
   */
  private PSSearch reloadAfterWrite(PSSearch saved, String name, String session, String user) {
    try {
      PSSearch fromCatalog = matchLoaded(loadAllSearches(), name);
      if (fromCatalog != null) {
        return fromCatalog;
      }
      fromCatalog = loadSearchByNameSummary(name);
      if (fromCatalog != null) {
        return fromCatalog;
      }
      if (saved != null && saved.getGUID() != null) {
        List<PSSearch> loaded =
            designWs.loadSearches(List.of(saved.getGUID()), false, false, session, user);
        if (loaded != null && !loaded.isEmpty() && loaded.get(0) != null
            && !loaded.get(0).isView()) {
          fromCatalog = matchLoaded(loadAllSearches(), name);
          if (fromCatalog != null) {
            return fromCatalog;
          }
        }
      }
    } catch (PSErrorResultsException e) {
      log.error("Failed to reload search {} after persist", name, e);
      throw new IllegalStateException("Failed to reload search after persist", e);
    } catch (Exception e) {
      log.error("Failed to reload search {} after persist", name, e);
      throw new IllegalStateException("Failed to reload search after persist", e);
    }
    throw new IllegalStateException(
        "Search was saved but is not visible to findSearches: " + name);
  }

  /**
   * Name-filtered catalog load. {@code findAllSearches} can omit a just-saved row on H2 while
   * {@code findSearches(name)} still returns the summary.
   */
  private PSSearch loadSearchByNameSummary(String name) throws Exception {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    List<IPSCatalogSummary> summaries = designWs.findSearches(name, null);
    if (!nameExists(summaries, name)) {
      return null;
    }
    List<IPSGuid> guids = new ArrayList<>();
    for (IPSCatalogSummary summary : summaries) {
      if (summary != null
          && name.equalsIgnoreCase(StringUtils.defaultString(summary.getName()))
          && summary.getGUID() != null) {
        guids.add(summary.getGUID());
      }
    }
    if (guids.isEmpty()) {
      return null;
    }
    List<PSSearch> loaded =
        designWs.loadSearches(guids, false, false, currentSession(), currentUser());
    PSSearch found = matchLoaded(loaded, name);
    if (found != null && found.isView()) {
      return null;
    }
    return found;
  }

  private void assertNameUnique(String name) {
    try {
      if (nameExists(designWs.findSearches(name, null), name)
          || nameExists(designWs.findViews(name, null), name)) {
        throw new WebApplicationException("Search already exists: " + name, 409);
      }
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSErrorException e) {
      log.error("Failed to catalog searches while checking uniqueness for {}", name, e);
      throw new IllegalStateException("Failed to catalog searches", e);
    }
  }

  static boolean nameExists(List<IPSCatalogSummary> summaries, String name) {
    if (summaries == null || StringUtils.isBlank(name)) {
      return false;
    }
    for (IPSCatalogSummary summary : summaries) {
      if (summary != null
          && name.equalsIgnoreCase(StringUtils.defaultString(summary.getName()))) {
        return true;
      }
    }
    return false;
  }

  private static void rejectViewWrite(PSSearch existing) {
    if (existing != null && existing.isView()) {
      throw new IllegalArgumentException(
          "Views are not writable on /services/searches; use /services/views");
    }
  }

  private void requireAdmin() {
    boolean allowed;
    try {
      allowed = adminChecker.getAsBoolean();
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.debug("Admin check failed: {}", e.getMessage());
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
    if (!allowed) {
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
  }

  boolean isCurrentUserAdmin() {
    if (userService == null) {
      return false;
    }
    try {
      PSCurrentUser current = userService.getCurrentUser();
      if (current == null || StringUtils.isBlank(current.getName())) {
        return false;
      }
      return userService.isAdminUser(current.getName());
    } catch (PSDataServiceException e) {
      log.debug("Unable to resolve current user for Admin check: {}", e.getMessage());
      return false;
    }
  }

  private static void requireSessionUserForWrite() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new WebApplicationException(
          "Request session/user required for search design write", Response.Status.FORBIDDEN);
    }
  }

  private static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  private static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }

  static boolean isAlreadyExistsFailure(IllegalArgumentException e) {
    return e != null && StringUtils.containsIgnoreCase(e.getMessage(), "already exists");
  }

  static boolean isNotFound(PSErrorResultsException e, IPSGuid requested) {
    if (e == null || requested == null) {
      return false;
    }
    Map<IPSGuid, Object> errors = e.getErrors();
    Map<IPSGuid, Object> results = e.getResults();
    boolean errored = errors != null && errors.containsKey(requested);
    boolean hasResult = results != null && results.containsKey(requested);
    return errored && !hasResult && !hasLockError(e);
  }

  static boolean hasLockError(PSErrorResultsException e) {
    if (e == null || e.getErrors() == null) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      if (isLockErrorObject(err)) {
        return true;
      }
    }
    return false;
  }

  static boolean isNotLockedError(PSErrorsException e) {
    if (e == null || e.getErrors() == null) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      if (isLockErrorObject(err)) {
        return true;
      }
    }
    return false;
  }

  static boolean isDependencyError(PSErrorsException e) {
    if (e == null || e.getErrors() == null) {
      return StringUtils.containsIgnoreCase(e != null ? e.getMessage() : null, "depend");
    }
    for (Object err : e.getErrors().values()) {
      String msg = errorMessage(err);
      if (StringUtils.containsIgnoreCase(msg, "depend")) {
        return true;
      }
    }
    return StringUtils.containsIgnoreCase(e.getMessage(), "depend");
  }

  private static boolean isLockErrorObject(Object err) {
    if (err instanceof PSLockErrorException) {
      return true;
    }
    if (err instanceof PSErrorException pe) {
      String msg = pe.getErrorMessage() != null ? pe.getErrorMessage() : pe.getMessage();
      return StringUtils.containsIgnoreCase(msg, "is not locked")
          || StringUtils.containsIgnoreCase(msg, "not locked for");
    }
    return StringUtils.containsIgnoreCase(String.valueOf(err), "is not locked");
  }

  private static String errorMessage(Object err) {
    if (err instanceof PSErrorException pe) {
      return StringUtils.defaultIfBlank(pe.getErrorMessage(), pe.getMessage());
    }
    return err != null ? String.valueOf(err) : null;
  }

  private RuntimeException mapSaveOrDeleteFailure(String verb, PSErrorsException e) {
    if (isNotLockedError(e)) {
      return new WebApplicationException(
          "Could not " + verb + " search; design lock required or held by another user", 409);
    }
    if (isDependencyError(e)) {
      return new WebApplicationException(
          "Search has dependents and cannot be deleted", 409);
    }
    log.error("Failed to {} search via UI design WS", verb, e);
    return new IllegalStateException("Failed to " + verb + " search", e);
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
