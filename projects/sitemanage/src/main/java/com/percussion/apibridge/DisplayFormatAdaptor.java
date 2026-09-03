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
import com.percussion.cms.objectstore.IPSDbComponent;
import com.percussion.cms.objectstore.PSComponentProcessorProxy;
import com.percussion.cms.objectstore.PSKey;
import com.percussion.cms.objectstore.PSDFColumns;
import com.percussion.cms.objectstore.PSDFMultiProperty;
import com.percussion.cms.objectstore.PSDFProperties;
import com.percussion.cms.objectstore.PSDisplayColumn;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.cms.objectstore.PSProcessorProxy;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.rest.Guid;
import com.percussion.rest.displayformat.*;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.PSRoleMgrLocator;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.server.PSRequest;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Provides the API implementation for the Display Format Resource.
 *
 * <p>GET catalog uses {@link IPSUiDesignWs#findDisplayFormats}. Admin create/update persist
 * through the same design WS SOAP uses ({@code createDisplayFormats} / {@code loadDisplayFormats}
 * / {@code saveDisplayFormats}). Admin delete loads with a design lock, marks the native format
 * for deletion, and persists via the Workbench objectstore processor so {@code
 * updateDisplayFormats} receives the XML document {@code PSTransactionSet} requires. Locator-only
 * {@code deleteDisplayFormats} does not supply that document ({@code Xml Document Expected}). No
 * new SOAP methods.
 */
@PSSiteManageBean
@Lazy
public class DisplayFormatAdaptor implements IDisplayFormatAdaptor {

  static final String ADMIN_REQUIRED =
      "Admin role required to create, update, or delete display formats";

  private static final Logger log = LogManager.getLogger(DisplayFormatAdaptor.class);

  private final IPSUiDesignWs designWs;
  private final BooleanSupplier adminChecker;
  private final LockedDisplayFormatXmlDeleter xmlDeleter;
  private final Supplier<Map<IPSGuid, String>> communityCatalog;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  @Autowired
  public DisplayFormatAdaptor(IPSUiDesignWs designWs) {
    this(designWs, null, null);
  }

  /** Package-visible for unit tests. */
  DisplayFormatAdaptor(IPSUiDesignWs designWs, BooleanSupplier adminChecker) {
    this(designWs, adminChecker, null);
  }

  /**
   * Package-visible for unit tests that stub XML persist (locator {@code deleteDisplayFormats}
   * does not supply the update document).
   */
  DisplayFormatAdaptor(
      IPSUiDesignWs designWs,
      BooleanSupplier adminChecker,
      LockedDisplayFormatXmlDeleter xmlDeleter) {
    this(designWs, adminChecker, xmlDeleter, null);
  }

  /**
   * Package-visible for unit tests that stub the community catalog (unknown community is 400).
   */
  DisplayFormatAdaptor(
      IPSUiDesignWs designWs,
      BooleanSupplier adminChecker,
      LockedDisplayFormatXmlDeleter xmlDeleter,
      Supplier<Map<IPSGuid, String>> communityCatalog) {
    this.designWs = designWs;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.xmlDeleter = xmlDeleter != null ? xmlDeleter : this::deleteLockedViaComponentXml;
    this.communityCatalog =
        communityCatalog != null ? communityCatalog : this::loadCommunityCatalog;
  }

  /**
   * Persist a locked, loaded display format by supplying component XML (Workbench processor
   * {@code delete(IPSDbComponent)}), then release the design lock.
   */
  @FunctionalInterface
  interface LockedDisplayFormatXmlDeleter {
    void delete(PSDisplayFormat nativeDf, IPSGuid id, String session, String user)
        throws PSCmsException;
  }

  @Override
  public List<DisplayFormat> createDisplayFormats(List<String> names, String session, String user) {
    try {
      List<PSDisplayFormat> created = designWs.createDisplayFormats(names, session, user);
      if (created == null || created.isEmpty()) {
        return new ArrayList<>();
      }
      List<DisplayFormat> out = new ArrayList<>(created.size());
      for (PSDisplayFormat nativeDf : created) {
        if (nativeDf != null) {
          out.add(copyDisplayFormat(nativeDf));
        }
      }
      return out;
    } catch (IllegalArgumentException e) {
      if (isAlreadyExistsFailure(e)) {
        throw new WebApplicationException(e.getMessage(), 409);
      }
      throw e;
    } catch (PSErrorException e) {
      throw new IllegalStateException("Failed to create display formats", e);
    } catch (PSCmsException | PSUnknownNodeTypeException e) {
      throw new IllegalStateException("Failed to map display formats", e);
    }
  }

  @Override
  public DisplayFormat createDisplayFormat(DisplayFormat body) {
    requireAdmin();
    requireSessionUserForWrite();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String name = requireValidName(firstNonBlank(body.getName(), body.getInternalName()));
    assertNameUnique(name);
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSDisplayFormat> created = designWs.createDisplayFormats(List.of(name), session, user);
      if (created == null || created.isEmpty() || created.get(0) == null) {
        throw new IllegalStateException("Design WS createDisplayFormats returned empty");
      }
      PSDisplayFormat nativeDf = created.get(0);
      applyWritableFields(nativeDf, body);
      designWs.saveDisplayFormats(List.of(nativeDf), true, session, user);
      return reload(nativeDf, name);
    } catch (WebApplicationException | IllegalStateException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      if (isAlreadyExistsFailure(e)) {
        throw new WebApplicationException("Display format already exists: " + name, 409);
      }
      throw e;
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("create", e);
    } catch (PSErrorException e) {
      throw new IllegalStateException("Failed to create display format", e);
    } catch (PSCmsException | PSUnknownNodeTypeException e) {
      throw new IllegalStateException("Failed to map display format", e);
    }
  }

  @Override
  public DisplayFormat updateDisplayFormat(String idOrName, DisplayFormat body) {
    requireAdmin();
    requireSessionUserForWrite();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    if (!isSafeDisplayFormatKey(idOrName)) {
      return null;
    }
    DisplayFormat existing = findDisplayFormatByKey(idOrName);
    if (existing == null) {
      return null;
    }
    IPSGuid id = resolveGuid(existing);
    if (id == null) {
      return null;
    }
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSDisplayFormat> loaded =
          designWs.loadDisplayFormats(List.of(id), true, false, session, user);
      if (loaded == null || loaded.isEmpty() || loaded.get(0) == null) {
        throw new WebApplicationException(
            "Could not update display format; design lock required or held by another user", 409);
      }
      PSDisplayFormat nativeDf = loaded.get(0);
      if (!namesMatchIgnoreCase(existing.getName(), nativeDf.getName())
          && nativeDf.getDisplayId() != existing.getDisplayId()) {
        throw new WebApplicationException(
            "Could not update display format; loaded identity did not match " + idOrName, 409);
      }
      applyWritableFields(nativeDf, body);
      designWs.saveDisplayFormats(List.of(nativeDf), true, session, user);
      return reload(nativeDf, nativeDf.getName());
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      if (isNotFound(e, id)) {
        return null;
      }
      if (hasLockError(e)) {
        throw new WebApplicationException(
            "Could not update display format; design lock required or held by another user", 409);
      }
      log.error("Failed to load display format for update: {}", id, e);
      throw new IllegalStateException("Failed to update display format", e);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("update", e);
    } catch (PSCmsException | PSUnknownNodeTypeException e) {
      throw new IllegalStateException("Failed to map display format", e);
    }
  }

  @Override
  public boolean deleteDisplayFormat(String idOrName) {
    requireAdmin();
    requireSessionUserForWrite();
    if (!isSafeDisplayFormatKey(idOrName)) {
      return false;
    }
    DisplayFormat existing = findDisplayFormatByKey(idOrName);
    if (existing == null) {
      return false;
    }
    IPSGuid id = resolveGuid(existing);
    if (id == null) {
      return false;
    }
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSDisplayFormat> locked =
          designWs.loadDisplayFormats(List.of(id), true, false, session, user);
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        throw new WebApplicationException(
            "Could not delete display format; design lock required or held by another user", 409);
      }
      PSDisplayFormat nativeDf =
          nativeForDelete(locked.get(0), existing, idOrName);
      nativeDf.markForDeletion();
      xmlDeleter.delete(nativeDf, id, session, user);
      return true;
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      if (isNotFound(e, id)) {
        return false;
      }
      throw new WebApplicationException(
          "Could not delete display format; design lock required or held by another user", 409);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("delete", e);
    } catch (PSCmsException e) {
      throw mapSaveOrDeleteFailure(
          "delete", wrapCmsDeleteFailure(id, e));
    }
  }

  /**
   * Workbench path: dependency check, component XML save-delete, release lock. Locator-only {@code
   * deleteDisplayFormats} posts {@code DBActionType=DELETE} with no input document; {@code
   * updateDisplayFormats} is an XML datasource and throws {@code Xml Document Expected}. {@code
   * saveDisplayFormats} of a marked object still locator-deletes first when a lock version is
   * present, so this uses {@link PSComponentProcessorProxy#delete(com.percussion.cms.objectstore.IPSDbComponent)} instead.
   */
  void deleteLockedViaComponentXml(
      PSDisplayFormat nativeDf, IPSGuid id, String session, String user) throws PSCmsException {
    PSErrorException dep = PSWebserviceUtils.checkDependencies(id);
    if (dep != null) {
      throw new WebApplicationException(
          "Display format has dependents and cannot be deleted", 409);
    }
    ensureMarkedForDeletion(nativeDf);
    Object rawReq = PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
    if (!(rawReq instanceof PSRequest req)) {
      throw new WebApplicationException(
          "Request session/user required for display format design write",
          Response.Status.FORBIDDEN);
    }
    PSComponentProcessorProxy proxy =
        new PSComponentProcessorProxy(PSProcessorProxy.PROCTYPE_SERVERLOCAL, req);
    int deleted = proxy.delete(nativeDf);
    log.info(
        "Display format XML delete name={} displayId={} deletedRows={}",
        nativeDf.getName(),
        nativeDf.getDisplayId(),
        deleted);
    if (deleted <= 0) {
      throw new PSCmsException(
          0,
          "Display format XML delete removed 0 rows for "
              + nativeDf.getName()
              + " (displayId="
              + nativeDf.getDisplayId()
              + ")");
    }
    try {
      PSWebserviceUtils.releaseLocks(List.of(id), session, user);
    } catch (RuntimeException e) {
      log.debug("Could not release display-format lock after delete: {}", e.toString());
    }
  }

  /**
   * {@code loadDisplayFormats} can replay another catalog row (By_Type) with {@code displayId=-1}.
   * Never persist-delete that object — build a persisted stub from the catalog identity.
   */
  static PSDisplayFormat nativeForDelete(
      PSDisplayFormat loaded, DisplayFormat existing, String idOrName) throws PSCmsException {
    String requested = firstNonBlank(idOrName, existing != null ? existing.getName() : null);
    if (loaded != null
        && loaded.getDisplayId() > 0
        && namesMatchIgnoreCase(requested, loaded.getName())) {
      return loaded;
    }
    return stubFromExisting(existing);
  }

  static PSDisplayFormat stubFromExisting(DisplayFormat existing) throws PSCmsException {
    if (existing == null) {
      throw new PSCmsException(0, "display format is required for XML delete");
    }
    int displayId = existing.getDisplayId();
    if (displayId <= 0 && existing.getGuid() != null) {
      displayId = existing.getGuid().getUuid();
    }
    if (displayId <= 0) {
      throw new PSCmsException(
          0, "Refusing XML delete of unpersisted display format " + existing.getName());
    }
    PSDisplayFormat stub = new PSDisplayFormat();
    PSKey key = PSDisplayFormat.createKey(new String[] {String.valueOf(displayId)});
    stub.setLocator(key);
    String name = firstNonBlank(existing.getName(), existing.getInternalName());
    if (name != null) {
      stub.setName(name);
      stub.setInternalName(name);
    }
    if (stub.getDisplayId() <= 0) {
      throw new PSCmsException(0, "Delete stub displayId must be persisted");
    }
    return stub;
  }

  /**
   * Loaded formats must be persisted before {@link PSDisplayFormat#markForDeletion()} will change
   * state; otherwise {@code toDbXml} emits no DELETE action and the processor reports success with
   * 0 rows removed.
   */
  static void ensureMarkedForDeletion(PSDisplayFormat nativeDf) throws PSCmsException {
    if (nativeDf == null) {
      throw new PSCmsException(0, "display format is required for XML delete");
    }
    if (!nativeDf.isPersisted()) {
      PSKey loc = nativeDf.getLocator();
      if (loc != null) {
        loc.setPersisted(true);
        nativeDf.setLocator(loc);
      }
    }
    nativeDf.markForDeletion();
    if (nativeDf.getState() != IPSDbComponent.DBSTATE_MARKEDFORDELETE) {
      throw new PSCmsException(
          0,
          "Could not mark display format for deletion (state="
              + nativeDf.getState()
              + " persisted="
              + nativeDf.isPersisted()
              + " name="
              + nativeDf.getName()
              + ")");
    }
  }

  private static PSErrorsException wrapCmsDeleteFailure(IPSGuid id, PSCmsException e) {
    PSErrorsException errors = new PSErrorsException();
    errors.addError(id, e);
    return errors;
  }

  @Override
  public void deleteDisplayFormats(
      List<IPSGuid> ids, boolean ignoreDependencies, String session, String user) {
    try {
      designWs.deleteDisplayFormats(ids, ignoreDependencies, session, user);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("delete", e);
    }
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
    ret.setAllowedCommunities(copyCommunitiesFromNative(f));
    ret.setSortedColumnNames(f.getSortedColumnName());
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
    if (mapped != null && mapped.getStringValue() != null) {
      String sv = mapped.getStringValue();
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

  /**
   * Wire list of community GUID + name. Empty list is all communities ({@code
   * sys_community=-1}); there is no distinct empty/none visibility.
   */
  private List<DisplayFormatCommunity> copyCommunitiesFromNative(PSDisplayFormat f) {
    List<DisplayFormatCommunity> out = new ArrayList<>();
    if (f == null) {
      return out;
    }
    if (f.doesPropertyHaveValue(
        PSDisplayFormat.PROP_COMMUNITY, PSDisplayFormat.PROP_COMMUNITY_ALL)) {
      return out;
    }
    Iterator<PSDFMultiProperty> props = f.getProperties();
    if (props == null) {
      return out;
    }
    Map<IPSGuid, String> catalog = communityCatalog.get();
    if (catalog == null) {
      catalog = Map.of();
    }
    boolean sawCommunity = false;
    while (props.hasNext()) {
      PSDFMultiProperty prop = props.next();
      if (prop == null || !PSDisplayFormat.PROP_COMMUNITY.equals(prop.getName())) {
        continue;
      }
      sawCommunity = true;
      Iterator<?> values = prop.iterator();
      while (values.hasNext()) {
        Object raw = values.next();
        String value = raw == null ? "" : String.valueOf(raw);
        if (value.isBlank() || PSDisplayFormat.PROP_COMMUNITY_ALL.equals(value)) {
          return new ArrayList<>();
        }
        long id;
        try {
          id = Long.parseLong(value);
        } catch (NumberFormatException e) {
          log.debug("Skipping non-numeric community property {}", value);
          continue;
        }
        IPSGuid ig = new PSGuid(PSTypeEnum.COMMUNITY_DEF, id);
        Guid g = copyGuid(ig);
        String key =
            g != null && g.getStringValue() != null && !g.getStringValue().isBlank()
                ? g.getStringValue()
                : value;
        out.add(new DisplayFormatCommunity(key, catalogCommunityName(catalog, ig)));
      }
    }
    if (!sawCommunity) {
      return new ArrayList<>();
    }
    return out;
  }

  private static String catalogCommunityName(Map<IPSGuid, String> catalog, IPSGuid ig) {
    if (catalog == null || ig == null) {
      return "";
    }
    String byGuid = catalog.get(ig);
    if (byGuid != null) {
      return byGuid;
    }
    for (var e : catalog.entrySet()) {
      if (e.getKey() != null && e.getKey().longValue() == ig.longValue()) {
        return e.getValue() == null ? "" : e.getValue();
      }
    }
    return "";
  }

  private Map<IPSGuid, String> loadCommunityCatalog() {
    try {
      IPSBackEndRoleMgr roles = PSRoleMgrLocator.getBackEndRoleManager();
      List<PSCommunity> all = roles.findCommunitiesByName(null);
      Map<IPSGuid, String> out = new LinkedHashMap<>();
      if (all == null) {
        return out;
      }
      for (PSCommunity community : all) {
        if (community == null || community.getGUID() == null || community.getName() == null) {
          continue;
        }
        out.put(community.getGUID(), community.getName());
      }
      return out;
    } catch (RuntimeException e) {
      log.debug("Could not catalog communities for display format visibility: {}", e.toString());
      return Map.of();
    }
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
    if (id == null) {
      return null;
    }
    PSDisplayFormat f = designWs.findDisplayFormat(id);
    if (f == null) {
      return null;
    }
    DisplayFormat copy = copyDisplayFormat(f);
    if (identityMatchesKey(copy, id.toString()) || f.getDisplayId() == id.getUUID()) {
      return copy;
    }
    return null;
  }

  @Override
  public DisplayFormat findDisplayFormat(String name)
      throws PSCmsException, PSUnknownNodeTypeException {
    PSDisplayFormat nativeDf = loadNativeByName(name);
    return nativeDf == null ? null : copyDisplayFormat(nativeDf);
  }

  /**
   * Load the native format whose internal name matches {@code name}. {@code
   * IPSUiDesignWs#findDisplayFormat(String)} can replay another format (typically
   * By_Author) for a newly created name (#3269). When the loaded name does not
   * match, resolve the catalog summary GUID and load that object.
   */
  private PSDisplayFormat loadNativeByName(String name) throws PSCmsException {
    if (name == null || name.isBlank()) {
      return null;
    }
    PSDisplayFormat byName = designWs.findDisplayFormat(name);
    if (byName != null && namesMatchIgnoreCase(name, byName.getName())) {
      return byName;
    }
    List<IPSCatalogSummary> summaries;
    try {
      summaries = designWs.findDisplayFormats(name, null);
      if (summaries == null || summaries.isEmpty()) {
        summaries = designWs.findDisplayFormats(null, null);
      }
    } catch (RuntimeException e) {
      log.debug("Catalog lookup failed for display format {}: {}", name, e.toString());
      return null;
    }
    if (summaries == null) {
      return null;
    }
    String session = currentSession();
    String user = currentUser();
    for (IPSCatalogSummary summary : summaries) {
      if (summary == null || !namesMatchIgnoreCase(name, summary.getName())) {
        continue;
      }
      IPSGuid guid = summary.getGUID();
      if (guid == null) {
        continue;
      }
      PSDisplayFormat byGuid = designWs.findDisplayFormat(guid);
      if (byGuid != null && namesMatchIgnoreCase(name, byGuid.getName())) {
        return byGuid;
      }
      try {
        List<PSDisplayFormat> loaded =
            designWs.loadDisplayFormats(List.of(guid), false, false, session, user);
        if (loaded != null
            && !loaded.isEmpty()
            && loaded.get(0) != null
            && namesMatchIgnoreCase(name, loaded.get(0).getName())) {
          return loaded.get(0);
        }
      } catch (PSErrorResultsException | RuntimeException e) {
        log.debug("GUID load failed for display format {}: {}", name, e.toString());
      }
    }
    return null;
  }

  @Override
  public void saveDisplayFormats(
      List<DisplayFormat> displayFormats, boolean release, String session, String user) {
    if (displayFormats == null || displayFormats.isEmpty()) {
      throw new IllegalArgumentException("displayFormats is required");
    }
    List<PSDisplayFormat> natives = new ArrayList<>();
    for (DisplayFormat dto : displayFormats) {
      if (dto == null) {
        continue;
      }
      IPSGuid id = resolveGuid(dto);
      if (id == null) {
        throw new IllegalArgumentException("display format guid is required to save");
      }
      try {
        List<PSDisplayFormat> loaded =
            designWs.loadDisplayFormats(List.of(id), true, false, session, user);
        if (loaded == null || loaded.isEmpty() || loaded.get(0) == null) {
          throw new WebApplicationException(
              "Could not save display format; design lock required or held by another user", 409);
        }
        PSDisplayFormat nativeDf = loaded.get(0);
        applyWritableFields(nativeDf, dto);
        natives.add(nativeDf);
      } catch (PSErrorResultsException e) {
        if (hasLockError(e)) {
          throw new WebApplicationException(
              "Could not save display format; design lock required or held by another user", 409);
        }
        throw new IllegalStateException("Failed to load display format for save", e);
      }
    }
    if (natives.isEmpty()) {
      throw new IllegalArgumentException("displayFormats is required");
    }
    try {
      designWs.saveDisplayFormats(natives, release, session, user);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("save", e);
    }
  }

  @Override
  public DisplayFormat findDisplayFormatByKey(String idOrName) {
    if (!isSafeDisplayFormatKey(idOrName)) {
      return null;
    }
    String key = idOrName.trim();
    try {
      DisplayFormat byName = findDisplayFormat(key);
      if (identityMatchesKey(byName, key)) {
        return byName;
      }
    } catch (PSCmsException | PSUnknownNodeTypeException e) {
      // Expected miss → catalog exact-name / GUID parse
      log.debug("Display format not found by name {}: {}", key, e.toString());
    }
    DisplayFormat fromCatalog = findExactCatalogCopy(key);
    if (fromCatalog != null) {
      return fromCatalog;
    }
    try {
      var guid = new com.percussion.services.guidmgr.data.PSGuid(key);
      DisplayFormat fromGuidCatalog = findCatalogCopyByGuid(guid);
      if (fromGuidCatalog != null) {
        return fromGuidCatalog;
      }
      DisplayFormat byGuid = findDisplayFormat((IPSGuid) guid);
      if (identityMatchesKey(byGuid, key)) {
        return byGuid;
      }
      return null;
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

  /**
   * Reload after create/update. {@code loadDisplayFormats} can replay the first catalog row
   * (By_Author) for a different GUID/name (#3269 / #3200) — reject that mismatch and fall back
   * to an exact catalog summary copy, then the in-memory saved component.
   */
  private DisplayFormat reload(PSDisplayFormat saved, String name)
      throws PSCmsException, PSUnknownNodeTypeException {
    // Prefer the native we just saved when the name matches. findDisplayFormat
    // can replay By_Author for a newly created key (#3269).
    if (saved != null && namesMatchIgnoreCase(name, saved.getName())) {
      return copyDisplayFormat(saved);
    }
    if (name != null && !name.isBlank()) {
      DisplayFormat byName = findDisplayFormat(name);
      if (identityMatchesKey(byName, name)) {
        return byName;
      }
    }
    if (saved != null && saved.getGUID() != null) {
      DisplayFormat byGuid = findDisplayFormat(saved.getGUID());
      if (byGuid != null
          && (name == null || name.isBlank() || identityMatchesKey(byGuid, name))) {
        return byGuid;
      }
    }
    if (saved != null) {
      return copyDisplayFormat(saved);
    }
    return name == null || name.isBlank() ? null : findExactCatalogCopy(name);
  }

  /**
   * True when {@code df} is the catalog object for {@code key} (internal name or GUID string).
   * Rejects bulk-load replay where a different name (e.g. By_Author / By_Type) is returned for this
   * key. Unnamed stubs match only when the GUID string equals {@code key} — never any key.
   */
  static boolean identityMatchesKey(DisplayFormat df, String key) {
    if (df == null || key == null || key.isBlank()) {
      return false;
    }
    String trimmed = key.trim();
    String loadedName = firstNonBlank(df.getName(), df.getInternalName());
    if (loadedName != null && namesMatchIgnoreCase(trimmed, loadedName)) {
      return true;
    }
    if (trimmed.equalsIgnoreCase(StringUtils.defaultString(df.getGuidString()))) {
      return true;
    }
    Guid g = df.getGuid();
    if (g != null && trimmed.equalsIgnoreCase(StringUtils.defaultString(g.getStringValue()))) {
      return true;
    }
    if (df.getDisplayId() > 0) {
      String asGuid = new PSGuid(PSTypeEnum.DISPLAY_FORMAT, df.getDisplayId()).toString();
      if (trimmed.equalsIgnoreCase(asGuid) || trimmed.equals(String.valueOf(df.getDisplayId()))) {
        return true;
      }
    }
    // Unnamed stub must still match the requested GUID — never By_Author replay.
    return false;
  }

  /**
   * Catalog row whose GUID matches {@code guid}. Prefer this over {@code
   * findDisplayFormat(guid)} so a load that stamps the requested id onto
   * By_Author cannot win after a user format is persisted.
   */
  private DisplayFormat findCatalogCopyByGuid(IPSGuid guid) {
    if (guid == null) {
      return null;
    }
    List<IPSCatalogSummary> summaries = catalogSummaries(null);
    if (summaries == null) {
      return null;
    }
    for (IPSCatalogSummary summary : summaries) {
      if (summary == null || summary.getGUID() == null) {
        continue;
      }
      if (guid.equals(summary.getGUID()) || guid.getUUID() == summary.getGUID().getUUID()) {
        try {
          return copyUniqueSummary(summary);
        } catch (PSCmsException | PSUnknownNodeTypeException e) {
          log.debug("Could not copy display format guid {}: {}", guid, e.toString());
          return copyFromCatalogSummary(summary);
        }
      }
    }
    return null;
  }

  /**
   * Exact INTERNALNAME from catalog summaries, then {@link #copyUniqueSummary} (rejects
   * replayed loads). Used when {@code findDisplayFormat(name)} returns the wrong object.
   */
  private DisplayFormat findExactCatalogCopy(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    List<IPSCatalogSummary> summaries = catalogSummaries(name);
    if (summaries == null) {
      return null;
    }
    for (IPSCatalogSummary summary : summaries) {
      if (summary != null && namesMatchIgnoreCase(name, summary.getName())) {
        try {
          return copyUniqueSummary(summary);
        } catch (PSCmsException | PSUnknownNodeTypeException e) {
          log.debug("Could not copy display format {}: {}", name, e.toString());
          return copyFromCatalogSummary(summary);
        }
      }
    }
    return null;
  }

  private List<IPSCatalogSummary> catalogSummaries(String name) {
    try {
      List<IPSCatalogSummary> byName = designWs.findDisplayFormats(name, null);
      if (byName != null) {
        for (IPSCatalogSummary summary : byName) {
          if (summary != null && namesMatchIgnoreCase(name, summary.getName())) {
            return byName;
          }
        }
      }
      return designWs.findDisplayFormats(null, null);
    } catch (RuntimeException e) {
      log.debug("Could not catalog display formats for {}: {}", name, e.toString());
      return null;
    }
  }

  private void applyWritableFields(PSDisplayFormat nativeDf, DisplayFormat body) {
    if (nativeDf == null || body == null) {
      return;
    }
    String label = firstNonBlank(body.getLabel(), body.getDisplayName());
    if (label != null) {
      nativeDf.setDisplayName(label);
    }
    if (body.getDescription() != null) {
      nativeDf.setDescription(body.getDescription());
    }
    if (body.getColumns() != null) {
      applyColumns(nativeDf, body.getColumns());
      applySortFromColumns(nativeDf, body);
    }
    if (body.getAllowedCommunities() != null) {
      applyAllowedCommunities(nativeDf, body.getAllowedCommunities());
    }
  }

  /**
   * Replace community visibility. Empty map is all communities ({@code addCommunity(null)}).
   * Unknown community is {@link IllegalArgumentException} (HTTP 400).
   */
  void applyAllowedCommunities(PSDisplayFormat nativeDf, List<DisplayFormatCommunity> allowed) {
    if (nativeDf == null || allowed == null) {
      return;
    }
    if (allowed.isEmpty()) {
      nativeDf.addCommunity(null);
      return;
    }
    Map<IPSGuid, String> catalog = communityCatalog.get();
    if (catalog == null) {
      catalog = Map.of();
    }
    List<String> ids = new ArrayList<>();
    for (DisplayFormatCommunity row : allowed) {
      if (row == null) {
        continue;
      }
      if (isAllCommunitiesSentinel(row.getGuid(), row.getName())) {
        continue;
      }
      ids.add(requireKnownCommunityId(row.getGuid(), row.getName(), catalog));
    }
    if (ids.isEmpty()) {
      nativeDf.addCommunity(null);
      return;
    }
    nativeDf.addCommunity(null);
    for (String id : ids) {
      nativeDf.addCommunity(id);
    }
  }

  /**
   * True when the guid/key is the reserved all-communities token {@code -1}. Name is not a
   * sentinel — a community actually named {@code -1} is validated as a specific community.
   */
  static boolean isAllCommunitiesSentinel(String key, String name) {
    String needleKey = key == null ? "" : key.trim();
    return PSDisplayFormat.PROP_COMMUNITY_ALL.equals(needleKey);
  }

  static String requireKnownCommunityId(
      String key, String name, Map<IPSGuid, String> catalog) {
    String needleKey = key == null ? "" : key.trim();
    String needleName = name == null ? "" : name.trim();
    String shown = firstNonBlank(needleKey, needleName);
    if (shown == null) {
      throw new IllegalArgumentException("unknown community");
    }
    if (catalog == null || catalog.isEmpty()) {
      throw new IllegalArgumentException("unknown community: " + shown);
    }
    for (var e : catalog.entrySet()) {
      IPSGuid g = e.getKey();
      String n = e.getValue();
      if (g == null) {
        continue;
      }
      String asString = g.toString();
      if (!needleKey.isEmpty()
          && (needleKey.equalsIgnoreCase(asString)
              || needleKey.equals(String.valueOf(g.getUUID()))
              || needleKey.equals(String.valueOf(g.longValue())))) {
        return String.valueOf(g.longValue());
      }
      if (!needleName.isEmpty() && n != null && needleName.equalsIgnoreCase(n)) {
        return String.valueOf(g.longValue());
      }
      if (!needleKey.isEmpty() && n != null && needleKey.equalsIgnoreCase(n)) {
        return String.valueOf(g.longValue());
      }
    }
    throw new IllegalArgumentException("unknown community: " + shown);
  }

  /**
   * Replace native columns from the REST list. {@code null} columns on the body leave the
   * existing list unchanged (label-only PUT). An empty list becomes sys_title only
   * ({@link PSDisplayFormat#setColumnList}).
   */
  static void applyColumns(PSDisplayFormat nativeDf, DisplayFormatColumnList columns) {
    if (nativeDf == null || columns == null) {
      return;
    }
    PSDFColumns next;
    try {
      next = new PSDFColumns();
    } catch (ClassNotFoundException | PSCmsException e) {
      throw new IllegalStateException("Failed to allocate display format columns", e);
    }
    Set<String> seen = new HashSet<>();
    int displayId = nativeDf.getDisplayId();
    int index = 0;
    for (DisplayFormatColumn dto : columns) {
      if (dto == null) {
        continue;
      }
      String source = requireValidColumnSource(dto.getSource());
      String key = source.toLowerCase(Locale.ROOT);
      if (!seen.add(key)) {
        throw new IllegalArgumentException("duplicate column source: " + source);
      }
      String colLabel = firstNonBlank(dto.getDisplayName(), source);
      String render = firstNonBlank(dto.getRenderType(), PSDisplayColumn.DATATYPE_TEXT);
      String desc = dto.getDescription() == null ? "" : dto.getDescription();
      int grouping =
          dto.isCategorized()
              ? PSDisplayColumn.GROUPING_CATEGORY
              : PSDisplayColumn.GROUPING_FLAT;
      PSKey colKey = PSDisplayColumn.createKey(source, displayId, false);
      PSDisplayColumn col = new PSDisplayColumn(colKey);
      col.setDisplayName(colLabel);
      col.setDescription(desc);
      col.setGroupingType(grouping);
      col.setRenderType(render);
      col.setSortOrder(dto.isAscendingSort());
      col.setPosition(dto.getPosition() >= 0 ? dto.getPosition() : index);
      if (dto.getWidth() > 0) {
        col.setWidth(dto.getWidth());
      }
      next.add(col);
      index++;
    }
    nativeDf.setColumnList(next);
  }

  /**
   * Persist Workbench {@code sortColumn} / {@code sortDirection} from PUT {@code
   * sortedColumnNames} plus the matching column's {@code ascendingSort}. Omitted {@code
   * sortedColumnNames} leaves those properties unchanged.
   */
  static void applySortFromColumns(PSDisplayFormat nativeDf, DisplayFormat body) {
    if (nativeDf == null || body == null || body.getColumns() == null) {
      return;
    }
    String requested = body.getSortedColumnNames();
    if (StringUtils.isBlank(requested)) {
      return;
    }
    String key = requested.trim();
    DisplayFormatColumn chosen = null;
    for (DisplayFormatColumn dto : body.getColumns()) {
      if (dto == null) {
        continue;
      }
      String source = StringUtils.trimToEmpty(dto.getSource());
      if (key.equalsIgnoreCase(source)) {
        chosen = dto;
        break;
      }
    }
    if (chosen == null) {
      throw new IllegalArgumentException("unknown sort column: " + key);
    }
    nativeDf.setProperty(PSDisplayFormat.PROP_SORT_COLUMN, chosen.getSource());
    nativeDf.setProperty(
        PSDisplayFormat.PROP_SORT_DIRECTION,
        chosen.isAscendingSort()
            ? PSDisplayFormat.SORT_ASCENDING
            : PSDisplayFormat.SORT_DESCENDING);
  }

  static String requireValidColumnSource(String raw) {
    if (StringUtils.isBlank(raw)) {
      throw new IllegalArgumentException("column source is required");
    }
    String source = raw.trim();
    if (containsWhitespace(source)) {
      throw new IllegalArgumentException("column source cannot contain whitespace");
    }
    if (source.contains("*") || source.contains("%")) {
      throw new IllegalArgumentException("column source must not contain wildcards");
    }
    if (source.contains("..")
        || source.indexOf('/') >= 0
        || source.indexOf('\\') >= 0
        || source.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("invalid column source");
    }
    if (source.length() > PSDisplayColumn.SOURCE_LENGTH) {
      throw new IllegalArgumentException(
          "column source must not exceed " + PSDisplayColumn.SOURCE_LENGTH + " characters");
    }
    return source;
  }

  private void assertNameUnique(String name) {
    List<IPSCatalogSummary> existing;
    try {
      existing = designWs.findDisplayFormats(name, null);
    } catch (RuntimeException e) {
      log.debug("Could not catalog display formats for uniqueness: {}", e.toString());
      return;
    }
    if (existing == null) {
      return;
    }
    for (IPSCatalogSummary summary : existing) {
      if (summary != null
          && name.equalsIgnoreCase(StringUtils.defaultString(summary.getName()))) {
        throw new WebApplicationException("Display format already exists: " + name, 409);
      }
    }
  }

  private static String requireValidName(String raw) {
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
    if (!isSafeDisplayFormatKey(name)) {
      throw new IllegalArgumentException("invalid name");
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

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String v : values) {
      if (StringUtils.isNotBlank(v)) {
        return v.trim();
      }
    }
    return null;
  }

  static IPSGuid toIpsGuid(Guid guid) {
    if (guid == null) {
      return null;
    }
    String sv = guid.getStringValue();
    if (StringUtils.isNotBlank(sv)) {
      try {
        return new PSGuid(sv.trim());
      } catch (IllegalArgumentException e) {
        log.debug("Invalid display format GUID string: {}", e.getMessage());
      }
    }
    if (guid.getType() > 0 && guid.getUuid() > 0) {
      PSTypeEnum type = PSTypeEnum.valueOf(guid.getType());
      if (type != null) {
        return new PSGuid(type, guid.getUuid());
      }
    }
    if (guid.getLongValue() != 0) {
      return new PSGuid(guid.getLongValue());
    }
    return null;
  }

  private static IPSGuid resolveGuid(DisplayFormat dto) {
    if (dto == null) {
      return null;
    }
    IPSGuid fromGuid = toIpsGuid(dto.getGuid());
    if (fromGuid != null) {
      return fromGuid;
    }
    if (StringUtils.isNotBlank(dto.getGuidString())) {
      try {
        return new PSGuid(dto.getGuidString().trim());
      } catch (IllegalArgumentException e) {
        log.debug("Invalid display format guidString: {}", e.getMessage());
      }
    }
    if (dto.getDisplayId() > 0) {
      return new PSGuid(PSTypeEnum.DISPLAY_FORMAT, dto.getDisplayId());
    }
    return null;
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
          "Request session/user required for display format design write",
          Response.Status.FORBIDDEN);
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

  static boolean isLockError(PSErrorsException e) {
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
          || StringUtils.containsIgnoreCase(msg, "not locked for")
          || StringUtils.containsIgnoreCase(msg, "locked by");
    }
    String text = String.valueOf(err);
    return StringUtils.containsIgnoreCase(text, "is not locked")
        || StringUtils.containsIgnoreCase(text, "locked by");
  }

  private static String errorMessage(Object err) {
    if (err instanceof PSErrorException pe) {
      return StringUtils.defaultIfBlank(pe.getErrorMessage(), pe.getMessage());
    }
    return err != null ? String.valueOf(err) : null;
  }

  private RuntimeException mapSaveOrDeleteFailure(String verb, PSErrorsException e) {
    if (isLockError(e)) {
      return new WebApplicationException(
          "Could not " + verb + " display format; design lock required or held by another user",
          409);
    }
    if (isDependencyError(e)) {
      return new WebApplicationException(
          "Display format has dependents and cannot be deleted", 409);
    }
    log.error("Failed to {} display format via UI design WS", verb, e);
    return new IllegalStateException("Failed to " + verb + " display format", e);
  }
}
