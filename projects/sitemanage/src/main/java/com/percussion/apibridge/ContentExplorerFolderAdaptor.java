/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.apibridge;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSFolderProperty;
import com.percussion.cms.objectstore.PSObjectPermissions;
import com.percussion.rest.contentexplorer.folders.AddFolderRequest;
import com.percussion.rest.contentexplorer.folders.AddFolderTreeRequest;
import com.percussion.rest.contentexplorer.folders.FolderChildrenRequest;
import com.percussion.rest.contentexplorer.folders.IContentExplorerFolderAdaptor;
import com.percussion.rest.contentexplorer.folders.RxFolder;
import com.percussion.rest.contentexplorer.folders.RxFolderChildList;
import com.percussion.rest.contentexplorer.folders.RxFolderProperty;
import com.percussion.rest.contentexplorer.folders.RxFolderSummary;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Content-explorer folders REST façade (#3073 / parent #3054).
 *
 * <p>Thin apibridge over {@link IPSContentWs} folder methods — the same domain path classic Content
 * Explorer / SOAP content folder ops use. Does not implement CM1 site-section semantics (see {@link
 * FolderAdaptor}).
 */
@PSSiteManageBean
public class ContentExplorerFolderAdaptor implements IContentExplorerFolderAdaptor {

  private static final Logger log = LogManager.getLogger(ContentExplorerFolderAdaptor.class);

  /** Safe id: digits, letters, hyphen, underscore, colon, period, slash (guid forms). */
  private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9_.:\\-/]+$");

  private final IPSContentWs contentWs;
  private final IPSIdMapper idMapper;
  private final Function<String, IPSGuid> guidResolver;

  @Autowired
  public ContentExplorerFolderAdaptor(IPSIdMapper idMapper) {
    this(PSContentWsLocator.getContentWebservice(), idMapper, idMapper::getGuid);
  }

  /** Package-visible for unit tests. */
  ContentExplorerFolderAdaptor(
      IPSContentWs contentWs, IPSIdMapper idMapper, Function<String, IPSGuid> guidResolver) {
    this.contentWs = contentWs;
    this.idMapper = idMapper;
    this.guidResolver = guidResolver;
  }

  // -------------------------------------------------------------------------
  // Public adaptor API
  // -------------------------------------------------------------------------

  @Override
  public RxFolder loadByPath(URI baseUri, String path) {
    String normalized = normalizeRxPath(path);
    if (normalized == null) {
      throw new IllegalArgumentException("path is required");
    }
    try {
      List<PSFolder> folders = contentWs.loadFolders(new String[] {normalized});
      if (folders == null || folders.isEmpty() || folders.get(0) == null) {
        return null;
      }
      RxFolder dto = toDto(folders.get(0));
      if (dto.getPath() == null) {
        dto.setPath(normalized);
      }
      return dto;
    } catch (PSErrorResultsException e) {
      log.debug("loadFolders path {} failed: {}", normalized, e.getAllErrorString());
      return null;
    } catch (PSErrorException e) {
      log.debug("loadFolders path {} error: {}", normalized, e.getMessage());
      return null;
    } catch (RuntimeException e) {
      throw mapRuntime(e, "load folder by path");
    }
  }

  @Override
  public RxFolder loadById(URI baseUri, String id) {
    IPSGuid guid = resolveGuid(id);
    try {
      PSFolder folder = contentWs.loadFolder(guid, true);
      if (folder == null) {
        return null;
      }
      return toDto(folder);
    } catch (PSErrorException e) {
      log.debug("loadFolder id {} failed: {}", id, e.getMessage());
      return null;
    } catch (RuntimeException e) {
      throw mapRuntime(e, "load folder by id");
    }
  }

  @Override
  public RxFolderChildList findChildrenById(URI baseUri, String id) {
    IPSGuid guid = resolveGuid(id);
    try {
      List<PSItemSummary> children = contentWs.findFolderChildren(guid, false);
      RxFolderChildList out = new RxFolderChildList(mapSummaries(children));
      out.setParentId(idMapper.getString(guid));
      return out;
    } catch (PSErrorException e) {
      throw notFoundOrServer(e, "Folder not found or cannot list children");
    } catch (RuntimeException e) {
      throw mapRuntime(e, "list children by id");
    }
  }

  @Override
  public RxFolderChildList findChildrenByPath(URI baseUri, String path) {
    String normalized = normalizeRxPath(path);
    if (normalized == null) {
      throw new IllegalArgumentException("path is required");
    }
    try {
      List<PSItemSummary> children = contentWs.findFolderChildren(normalized, false);
      RxFolderChildList out = new RxFolderChildList(mapSummaries(children));
      out.setParentPath(normalized);
      return out;
    } catch (PSErrorException e) {
      throw notFoundOrServer(e, "Folder not found or cannot list children");
    } catch (RuntimeException e) {
      throw mapRuntime(e, "list children by path");
    }
  }

  @Override
  public RxFolderChildList findChildFoldersById(URI baseUri, String id) {
    IPSGuid guid = resolveGuid(id);
    try {
      List<PSItemSummary> children = contentWs.findChildFolders(guid);
      RxFolderChildList out = new RxFolderChildList(mapSummaries(children));
      out.setParentId(idMapper.getString(guid));
      return out;
    } catch (PSErrorException e) {
      throw notFoundOrServer(e, "Folder not found or cannot list child folders");
    } catch (RuntimeException e) {
      throw mapRuntime(e, "list child folders by id");
    }
  }

  @Override
  public RxFolderChildList findChildFoldersByPath(URI baseUri, String path) {
    String normalized = normalizeRxPath(path);
    if (normalized == null) {
      throw new IllegalArgumentException("path is required");
    }
    // findChildFolders is id-only on IPSContentWs — resolve path first.
    if ("/".equals(normalized)) {
      // Root: children of "/" that are folders (Folders + Sites).
      return findChildrenByPath(baseUri, "/");
    }
    RxFolder parent = loadByPath(baseUri, normalized);
    if (parent == null || parent.getId() == null) {
      throw new WebApplicationException(
          Response.status(Response.Status.NOT_FOUND).entity("Folder not found").build());
    }
    RxFolderChildList out = findChildFoldersById(baseUri, parent.getId());
    out.setParentPath(normalized);
    return out;
  }

  @Override
  public RxFolder addFolder(URI baseUri, AddFolderRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    if (StringUtils.isBlank(request.getName())) {
      throw new IllegalArgumentException("name is required");
    }
    if (StringUtils.isBlank(request.getParentPath())) {
      throw new IllegalArgumentException("parentPath is required");
    }
    String parent = normalizeRxPath(request.getParentPath());
    if (parent == null || "/".equals(parent)) {
      throw new IllegalArgumentException("parentPath must be an existing folder path");
    }
    try {
      PSFolder created;
      if (StringUtils.isNotBlank(request.getSourcePath())) {
        String src = normalizeRxPath(request.getSourcePath());
        created = contentWs.addFolder(request.getName().trim(), parent, src, true);
      } else {
        created = contentWs.addFolder(request.getName().trim(), parent, true);
      }
      return toDto(created);
    } catch (PSErrorException e) {
      log.error("addFolder failed: {}", e.getMessage(), e);
      throw new WebApplicationException(
          "Failed to add folder: " + safeMessage(e), e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (RuntimeException e) {
      throw mapRuntime(e, "add folder");
    }
  }

  @Override
  public List<RxFolder> addFolderTree(URI baseUri, AddFolderTreeRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    String path = normalizeRxPath(request.getPath());
    if (path == null || "/".equals(path)) {
      throw new IllegalArgumentException("path is required and must not be root alone");
    }
    try {
      List<PSFolder> created = contentWs.addFolderTree(path, true);
      List<RxFolder> out = new ArrayList<>();
      if (created != null) {
        for (PSFolder f : created) {
          if (f != null) {
            out.add(toDto(f));
          }
        }
      }
      return out;
    } catch (PSErrorResultsException e) {
      log.error("addFolderTree partial error: {}", e.getAllErrorString());
      throw new WebApplicationException(
          "Failed to add folder tree: " + e.getAllErrorString(),
          e,
          Response.Status.INTERNAL_SERVER_ERROR);
    } catch (PSErrorException e) {
      log.error("addFolderTree failed: {}", e.getMessage(), e);
      throw new WebApplicationException(
          "Failed to add folder tree: " + safeMessage(e), e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (RuntimeException e) {
      throw mapRuntime(e, "add folder tree");
    }
  }

  @Override
  public RxFolder saveFolder(URI baseUri, String id, RxFolder body) {
    if (body == null) {
      throw new IllegalArgumentException("folder body is required");
    }
    IPSGuid guid = resolveGuid(id);
    try {
      PSFolder existing = contentWs.loadFolder(guid, true);
      if (existing == null) {
        throw new WebApplicationException(
            Response.status(Response.Status.NOT_FOUND).entity("Folder not found").build());
      }
      applyUpdates(existing, body);
      PSFolder saved = contentWs.saveFolder(existing);
      return toDto(saved != null ? saved : existing);
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSErrorException e) {
      log.error("saveFolder failed: {}", e.getMessage(), e);
      throw new WebApplicationException(
          "Failed to save folder: " + safeMessage(e), e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (RuntimeException e) {
      throw mapRuntime(e, "save folder");
    }
  }

  @Override
  public void moveChildren(URI baseUri, FolderChildrenRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    String sourcePath = firstNonBlank(request.getSourcePath());
    String targetPath = firstNonBlank(request.getTargetPath(), request.getParentPath());
    String sourceId = firstNonBlank(request.getSourceId());
    String targetId = firstNonBlank(request.getTargetId(), request.getParentId());

    // null child list = move all children (WS contract); empty after resolve also → null
    List<IPSGuid> childGuids = resolveChildGuids(request.getChildIds());
    if (childGuids.isEmpty()) {
      childGuids = null;
    }

    try {
      if (StringUtils.isNotBlank(sourcePath) && StringUtils.isNotBlank(targetPath)) {
        contentWs.moveFolderChildren(
            normalizeRxPath(sourcePath), normalizeRxPath(targetPath), childGuids);
        return;
      }
      if (StringUtils.isNotBlank(sourceId) && StringUtils.isNotBlank(targetId)) {
        IPSGuid src = resolveGuid(sourceId);
        IPSGuid tgt = resolveGuid(targetId);
        boolean check =
            request.getCheckFolderPermission() == null || request.getCheckFolderPermission();
        contentWs.moveFolderChildren(src, tgt, childGuids, check);
        return;
      }
      throw new IllegalArgumentException(
          "move-children requires sourcePath+targetPath or sourceId+targetId");
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (PSErrorException e) {
      log.error("moveFolderChildren failed: {}", e.getMessage(), e);
      throw new WebApplicationException(
          "Failed to move children: " + safeMessage(e), e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (RuntimeException e) {
      throw mapRuntime(e, "move children");
    }
  }

  @Override
  public void addChildren(URI baseUri, FolderChildrenRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    String parentPath = firstNonBlank(request.getParentPath(), request.getTargetPath());
    String parentId = firstNonBlank(request.getParentId(), request.getTargetId());
    List<IPSGuid> childGuids = resolveChildGuids(request.getChildIds());
    if (childGuids.isEmpty()) {
      throw new IllegalArgumentException("childIds is required and must not be empty");
    }
    try {
      if (StringUtils.isNotBlank(parentPath)) {
        contentWs.addFolderChildren(normalizeRxPath(parentPath), childGuids);
        return;
      }
      if (StringUtils.isNotBlank(parentId)) {
        contentWs.addFolderChildren(resolveGuid(parentId), childGuids);
        return;
      }
      throw new IllegalArgumentException("add-children requires parentPath or parentId");
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (PSErrorException e) {
      log.error("addFolderChildren failed: {}", e.getMessage(), e);
      throw new WebApplicationException(
          "Failed to add children: " + safeMessage(e), e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (RuntimeException e) {
      throw mapRuntime(e, "add children");
    }
  }

  @Override
  public void removeChildren(URI baseUri, FolderChildrenRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    String parentPath = firstNonBlank(request.getParentPath(), request.getTargetPath());
    String parentId = firstNonBlank(request.getParentId(), request.getTargetId());
    List<IPSGuid> childGuids = resolveChildGuids(request.getChildIds());
    // null = remove all children (WS contract)
    if (childGuids.isEmpty()) {
      childGuids = null;
    }
    boolean purge = Boolean.TRUE.equals(request.getPurgeItems());
    try {
      if (StringUtils.isNotBlank(parentPath)) {
        contentWs.removeFolderChildren(normalizeRxPath(parentPath), childGuids, purge);
        return;
      }
      if (StringUtils.isNotBlank(parentId)) {
        contentWs.removeFolderChildren(resolveGuid(parentId), childGuids, purge);
        return;
      }
      throw new IllegalArgumentException("remove-children requires parentPath or parentId");
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (PSErrorsException | PSErrorException e) {
      log.error("removeFolderChildren failed: {}", e.getMessage(), e);
      throw new WebApplicationException(
          "Failed to remove children: " + safeMessage(e), e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (RuntimeException e) {
      throw mapRuntime(e, "remove children");
    }
  }

  @Override
  public void deleteFolder(URI baseUri, String id, boolean purgeItems) {
    IPSGuid guid = resolveGuid(id);
    try {
      contentWs.deleteFolders(List.of(guid), purgeItems, true);
    } catch (PSErrorsException e) {
      log.error("deleteFolders failed: {}", e.getMessage(), e);
      throw new WebApplicationException(
          "Failed to delete folder: " + safeMessage(e), e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (RuntimeException e) {
      throw mapRuntime(e, "delete folder");
    }
  }

  // -------------------------------------------------------------------------
  // Path normalize (package-visible for unit tests)
  // -------------------------------------------------------------------------

  /**
   * Normalize client path forms to repository RX paths expected by {@link IPSContentWs}.
   *
   * <ul>
   *   <li>{@code /} → root listing path
   *   <li>{@code //Folders/...} / {@code //Sites/...} kept (trailing slash stripped)
   *   <li>{@code /Folders/...} / {@code /Sites/...} → double-slash form
   *   <li>{@code Folders/...} / {@code Sites/...} → double-slash form
   *   <li>Backslashes → forward slashes; drive-letter prefix stripped
   * </ul>
   *
   * @return normalized path, or {@code null} when input is blank
   */
  static String normalizeRxPath(String path) {
    if (path == null) {
      return null;
    }
    String p = path.trim().replace('\\', '/');
    if (p.isEmpty()) {
      return null;
    }
    // Strip Windows drive letter if a client accidentally sent one.
    if (p.length() >= 2 && Character.isLetter(p.charAt(0)) && p.charAt(1) == ':') {
      p = p.substring(2);
    }
    if (p.isEmpty()) {
      return null;
    }

    // Collapse accidental 3+ leading slashes to exactly two when repository-like.
    while (p.startsWith("///")) {
      p = p.substring(1);
    }

    boolean repoForm = p.startsWith("//");
    if (repoForm) {
      // Keep // prefix; collapse internal // after prefix (linear helpers; CodeQL #1977).
      String rest = collapseDuplicateSlashes(p.substring(2)); // linear, no regex (CodeQL #1977)
      p = "//" + rest;
    } else {
      p = collapseDuplicateSlashes(p); // linear, no regex (CodeQL #1977)
      if (!p.startsWith("/")) {
        p = "/" + p;
      }
      // Promote known roots to repository form.
      String lower = p.toLowerCase(Locale.ROOT);
      if (lower.equals("/folders")
          || lower.startsWith("/folders/")
          || lower.equals("/sites")
          || lower.startsWith("/sites/")) {
        p = "/" + p; // /Folders → //Folders
      }
    }

    // Strip trailing slash except pure root forms (linear helper; CodeQL #1977).
    if (p.length() > 1 && p.endsWith("/")) {
      p = stripTrailingSlashes(p); // linear, no regex (CodeQL #1977)
      if (p.equals("/") || p.equals("//")) {
        return "/";
      }
      // If we stripped to empty after //, treat as root.
      if (p.equals("//")) {
        return "/";
      }
    }
    if ("//".equals(p) || p.isEmpty()) {
      return "/";
    }
    return p;
  }


  /**
   * Collapse runs of {@code /} to a single slash in linear time (no regex). Avoids CodeQL
   * {@code java/polynomial-redos} on user-supplied folder paths (alert #1977).
   */
  private static String collapseDuplicateSlashes(String s) {
    if (s == null || s.length() < 2) {
      return s;
    }
    StringBuilder out = new StringBuilder(s.length());
    char prev = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '/' && prev == '/') {
        continue;
      }
      out.append(c);
      prev = c;
    }
    return out.toString();
  }

  /**
   * Strip trailing {@code /} characters in linear time (no regex). Same CodeQL motivation as
   * {@link #collapseDuplicateSlashes(String)}.
   */
  private static String stripTrailingSlashes(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    int end = s.length();
    while (end > 0 && s.charAt(end - 1) == '/') {
      end--;
    }
    return end == s.length() ? s : s.substring(0, end);
  }

  // -------------------------------------------------------------------------
  // Mapping helpers
  // -------------------------------------------------------------------------

  private void applyUpdates(PSFolder existing, RxFolder body) {
    if (StringUtils.isNotBlank(body.getName())) {
      existing.setName(body.getName().trim());
    }
    if (body.getDescription() != null) {
      existing.setDescription(body.getDescription());
    }
    if (body.getCommunityId() != null) {
      existing.setCommunityId(body.getCommunityId());
    }
    if (StringUtils.isNotBlank(body.getLocale())) {
      existing.setLocale(body.getLocale().trim());
    }
    if (body.getProperties() != null) {
      for (RxFolderProperty prop : body.getProperties()) {
        if (prop == null || StringUtils.isBlank(prop.getName())) {
          continue;
        }
        String value = prop.getValue() != null ? prop.getValue() : "";
        String desc = prop.getDescription() != null ? prop.getDescription() : "";
        existing.setProperty(prop.getName().trim(), value, desc);
      }
    }
  }

  private RxFolder toDto(PSFolder folder) {
    RxFolder dto = new RxFolder();
    if (folder.getGuid() != null) {
      dto.setId(idMapper.getString(folder.getGuid()));
    } else if (folder.getLocator() != null) {
      dto.setId(idMapper.getString(folder.getLocator()));
      dto.setContentId((long) folder.getLocator().getId());
    }
    if (folder.getLocator() != null) {
      dto.setContentId((long) folder.getLocator().getId());
    }
    dto.setName(folder.getName());
    dto.setDescription(folder.getDescription());
    dto.setCommunityId(folder.getCommunityId());
    dto.setCommunityName(folder.getCommunityName());
    dto.setLocale(folder.getLocale());
    dto.setDisplayFormatName(folder.getDisplayFormatName());
    dto.setPath(folder.getFolderPath());
    PSObjectPermissions perms = folder.getPermissions();
    if (perms != null) {
      dto.setPermissions(perms.getPermissions());
    }
    List<RxFolderProperty> props = new ArrayList<>();
    Iterator<PSFolderProperty> it = folder.getProperties();
    while (it != null && it.hasNext()) {
      PSFolderProperty p = it.next();
      if (p == null) {
        continue;
      }
      props.add(new RxFolderProperty(p.getName(), p.getValue(), p.getDescription()));
    }
    dto.setProperties(props);
    return dto;
  }

  private List<RxFolderSummary> mapSummaries(List<PSItemSummary> children) {
    List<RxFolderSummary> out = new ArrayList<>();
    if (children == null) {
      return out;
    }
    for (PSItemSummary s : children) {
      if (s == null) {
        continue;
      }
      RxFolderSummary row = new RxFolderSummary();
      IPSGuid g = s.getGUID();
      if (g instanceof PSLegacyGuid) {
        row.setContentId((long) ((PSLegacyGuid) g).getContentId());
      }
      if (g != null) {
        row.setId(idMapper.getString(g));
      }
      row.setName(s.getName());
      if (s.getObjectType() != null) {
        row.setObjectType(s.getObjectType().name());
      }
      row.setContentTypeName(s.getContentTypeName());
      row.setContentTypeId(s.getContentTypeId());
      out.add(row);
    }
    return out;
  }

  private IPSGuid resolveGuid(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("id is required");
    }
    String trimmed = id.trim();
    if (!SAFE_ID.matcher(trimmed).matches()) {
      throw new IllegalArgumentException("id contains invalid characters");
    }
    try {
      // Numeric content id → legacy folder guid
      if (trimmed.matches("\\d+")) {
        long n = Long.parseLong(trimmed);
        if (n <= 0 || n > Integer.MAX_VALUE) {
          throw new IllegalArgumentException("id out of range: " + trimmed);
        }
        return new PSLegacyGuid((int) n, -1);
      }
      IPSGuid g = guidResolver.apply(trimmed);
      if (g == null) {
        throw new IllegalArgumentException("Unable to resolve id: " + trimmed);
      }
      return g;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Unable to resolve id: " + trimmed, e);
    }
  }

  private List<IPSGuid> resolveChildGuids(List<String> childIds) {
    List<IPSGuid> out = new ArrayList<>();
    if (childIds == null) {
      return out;
    }
    for (String c : childIds) {
      if (StringUtils.isBlank(c)) {
        continue;
      }
      out.add(resolveGuid(c));
    }
    return out;
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String v : values) {
      if (StringUtils.isNotBlank(v)) {
        return v;
      }
    }
    return null;
  }

  private static String safeMessage(Throwable e) {
    if (e == null || e.getMessage() == null) {
      return "unknown error";
    }
    String m = e.getMessage();
    return m.length() > 400 ? m.substring(0, 400) : m;
  }

  private static WebApplicationException notFoundOrServer(PSErrorException e, String clientMsg) {
    String msg = e != null ? e.getMessage() : null;
    // WS often signals missing folder as error; map to 404 when message suggests not found.
    if (msg != null) {
      String lower = msg.toLowerCase(Locale.ROOT);
      if (lower.contains("not found")
          || lower.contains("does not exist")
          || lower.contains("invalid path")
          || lower.contains("no folder")) {
        return new WebApplicationException(
            e, Response.status(Response.Status.NOT_FOUND).entity(clientMsg).build());
      }
    }
    return new WebApplicationException(
        e, Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(clientMsg).build());
  }

  private static RuntimeException mapRuntime(RuntimeException e, String op) {
    if (e instanceof WebApplicationException || e instanceof IllegalArgumentException) {
      return e;
    }
    log.error("Unexpected failure during {}: {}", op, e.getMessage(), e);
    return e;
  }
}
