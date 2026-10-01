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
package com.percussion.share.relationship.service.impl;

import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.share.relationship.data.PSExplorerRelationshipEdge;
import com.percussion.share.relationship.service.ExplorerRelationshipAction;
import com.percussion.share.relationship.service.IPSExplorerRelationshipRemoveService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Loads relationships owned by one content item and deletes a single non-folder row.
 *
 * <p>Active Assembly rows use {@link IPSContentWs#deleteContentRelations}. Other categories use
 * {@link IPSSystemWs#deleteRelationships}. Folder relationships are refused with conflict so a
 * folder-only selection cannot be reported as a removed item relationship.
 */
@PSSiteManageBean("explorerRelationshipRemoveService")
public class PSExplorerRelationshipRemoveService implements IPSExplorerRelationshipRemoveService {

  private final IPSIdMapper idMapper;
  private final IPSSystemWs systemWs;
  private final IPSContentWs contentWs;
  private final RelationshipGuidFactory guids;

  @Autowired
  public PSExplorerRelationshipRemoveService(
      IPSIdMapper idMapper, IPSSystemWs systemWs, IPSContentWs contentWs) {
    this(idMapper, systemWs, contentWs, PSExplorerRelationshipRemoveService::defaultRelationshipGuid);
  }

  PSExplorerRelationshipRemoveService(
      IPSIdMapper idMapper,
      IPSSystemWs systemWs,
      IPSContentWs contentWs,
      RelationshipGuidFactory guids) {
    this.idMapper = idMapper;
    this.systemWs = systemWs;
    this.contentWs = contentWs;
    this.guids = guids;
  }

  @FunctionalInterface
  interface RelationshipGuidFactory {
    IPSGuid relationship(int id);
  }

  @Override
  public ExplorerRelationshipAction listOwned(String itemId) {
    Resolved resolved = resolve(itemId);
    if (resolved.failure != null) {
      return resolved.failure;
    }
    List<PSExplorerRelationshipEdge> edges = new ArrayList<>();
    for (PSRelationship rel : loadOwned(resolved.contentId)) {
      if (rel == null || isFolder(rel)) {
        continue;
      }
      edges.add(toEdge(rel));
    }
    return ExplorerRelationshipAction.listed(edges);
  }

  @Override
  public ExplorerRelationshipAction removeOwned(String itemId, int relationshipId) {
    if (relationshipId <= 0) {
      return ExplorerRelationshipAction.of(
          ExplorerRelationshipAction.Status.BAD_REQUEST, "relationshipId is required");
    }
    Resolved resolved = resolve(itemId);
    if (resolved.failure != null) {
      return resolved.failure;
    }
    PSRelationship rel = findById(relationshipId);
    if (rel == null) {
      return ExplorerRelationshipAction.of(
          ExplorerRelationshipAction.Status.NOT_FOUND, "Relationship was not found");
    }
    PSLocator owner = rel.getOwner();
    if (owner == null || owner.getId() != resolved.contentId) {
      return ExplorerRelationshipAction.of(
          ExplorerRelationshipAction.Status.CONFLICT,
          "Relationship does not belong to the selected item");
    }
    if (isFolder(rel)) {
      return ExplorerRelationshipAction.of(
          ExplorerRelationshipAction.Status.CONFLICT,
          "Folder relationships cannot be removed from this panel");
    }
    try {
      IPSGuid guid = guids.relationship(relationshipId);
      if (isActiveAssembly(rel)) {
        contentWs.deleteContentRelations(Collections.singletonList(guid));
      } else {
        systemWs.deleteRelationships(Collections.singletonList(guid));
      }
    } catch (PSErrorsException | PSErrorException e) {
      return ExplorerRelationshipAction.of(
          ExplorerRelationshipAction.Status.CONFLICT, safeMessage(e, "The relationship could not be removed"));
    } catch (RuntimeException e) {
      if (isAccessDenied(e)) {
        return ExplorerRelationshipAction.of(
            ExplorerRelationshipAction.Status.FORBIDDEN, "You do not have permission to remove this relationship");
      }
      throw e;
    } catch (Exception e) {
      if (isAccessDenied(e)) {
        return ExplorerRelationshipAction.of(
            ExplorerRelationshipAction.Status.FORBIDDEN, "You do not have permission to remove this relationship");
      }
      return ExplorerRelationshipAction.of(
          ExplorerRelationshipAction.Status.CONFLICT, safeMessage(e, "The relationship could not be removed"));
    }
    return ExplorerRelationshipAction.removed();
  }

  @Override
  public ExplorerRelationshipAction addOwned(String itemId, String targetItemId, String configName) {
    if (configName == null || configName.isBlank()) {
      return ExplorerRelationshipAction.of(
          ExplorerRelationshipAction.Status.BAD_REQUEST, "configName is required");
    }
    if (isFolderConfigName(configName)) {
      return ExplorerRelationshipAction.of(
          ExplorerRelationshipAction.Status.CONFLICT,
          "Folder relationships cannot be added from this panel");
    }
    if (targetItemId == null || targetItemId.isBlank()) {
      return ExplorerRelationshipAction.of(
          ExplorerRelationshipAction.Status.BAD_REQUEST, "targetItemId is required");
    }
    Resolved owner = resolve(itemId);
    if (owner.failure != null) {
      return owner.failure;
    }
    Resolved target = resolve(targetItemId.trim());
    if (target.failure != null) {
      return target.failure;
    }
    if (owner.contentId == target.contentId) {
      return ExplorerRelationshipAction.of(
          ExplorerRelationshipAction.Status.CONFLICT, "An item cannot own a relationship to itself");
    }
    try {
      PSRelationship created =
          systemWs.createRelationship(configName.trim(), owner.guid, target.guid);
      if (created == null || isFolder(created)) {
        return ExplorerRelationshipAction.of(
            ExplorerRelationshipAction.Status.CONFLICT,
            "Folder relationships cannot be added from this panel");
      }
      if (isActiveAssembly(created)) {
        return ExplorerRelationshipAction.of(
            ExplorerRelationshipAction.Status.CONFLICT,
            "Active Assembly relationships cannot be added from this panel");
      }
      systemWs.saveRelationships(Collections.singletonList(created));
      return ExplorerRelationshipAction.created(toEdge(created));
    } catch (PSErrorsException | PSErrorException e) {
      if (isAccessDenied(e)) {
        return ExplorerRelationshipAction.of(
            ExplorerRelationshipAction.Status.FORBIDDEN,
            "You do not have permission to add this relationship");
      }
      return ExplorerRelationshipAction.of(
          ExplorerRelationshipAction.Status.CONFLICT,
          safeMessage(e, "The relationship could not be added"));
    } catch (RuntimeException e) {
      if (isAccessDenied(e)) {
        return ExplorerRelationshipAction.of(
            ExplorerRelationshipAction.Status.FORBIDDEN,
            "You do not have permission to add this relationship");
      }
      throw e;
    } catch (Exception e) {
      if (isAccessDenied(e)) {
        return ExplorerRelationshipAction.of(
            ExplorerRelationshipAction.Status.FORBIDDEN,
            "You do not have permission to add this relationship");
      }
      return ExplorerRelationshipAction.of(
          ExplorerRelationshipAction.Status.CONFLICT,
          safeMessage(e, "The relationship could not be added"));
    }
  }

  static boolean isFolderConfigName(String name) {
    String normalized = name.trim().toLowerCase(Locale.ROOT);
    return normalized.equals(PSRelationshipConfig.CATEGORY_FOLDER)
        || normalized.equals("folder")
        || normalized.equals("folder content");
  }

  private List<PSRelationship> loadOwned(int contentId) {
    PSRelationshipFilter filter = new PSRelationshipFilter();
    filter.setOwnerId(contentId);
    try {
      List<PSRelationship> loaded = systemWs.loadRelationships(filter);
      return loaded == null ? List.of() : loaded;
    } catch (PSErrorException e) {
      throw new IllegalStateException(e);
    }
  }

  private PSRelationship findById(int relationshipId) {
    PSRelationshipFilter filter = new PSRelationshipFilter();
    filter.setRelationshipId(relationshipId);
    List<PSRelationship> loaded;
    try {
      loaded = systemWs.loadRelationships(filter);
    } catch (PSErrorException e) {
      throw new IllegalStateException(e);
    }
    if (loaded == null) {
      return null;
    }
    for (PSRelationship rel : loaded) {
      if (rel != null && rel.getId() == relationshipId) {
        return rel;
      }
    }
    return loaded.isEmpty() ? null : loaded.get(0);
  }

  private static PSExplorerRelationshipEdge toEdge(PSRelationship rel) {
    String name = "";
    String category = "";
    PSRelationshipConfig config = rel.getConfig();
    if (config != null) {
      name = config.getName() == null ? "" : config.getName();
      category = config.getCategory() == null ? "" : config.getCategory();
    }
    int dependentId = rel.getDependent() == null ? 0 : rel.getDependent().getId();
    String label = name.isEmpty() ? ("Relationship " + rel.getId()) : (name + " -> " + dependentId);
    return new PSExplorerRelationshipEdge(rel.getId(), name, category, dependentId, label);
  }

  private static boolean isFolder(PSRelationship rel) {
    PSRelationshipConfig config = rel.getConfig();
    return config != null && PSRelationshipConfig.CATEGORY_FOLDER.equals(config.getCategory());
  }

  private static boolean isActiveAssembly(PSRelationship rel) {
    PSRelationshipConfig config = rel.getConfig();
    return config != null && config.isActiveAssemblyRelationship();
  }

  private Resolved resolve(String itemId) {
    if (itemId == null || itemId.isBlank()) {
      return Resolved.failed(
          ExplorerRelationshipAction.of(
              ExplorerRelationshipAction.Status.BAD_REQUEST, "itemId is required"));
    }
    try {
      IPSGuid guid = idMapper.getGuid(itemId.trim());
      if (guid == null) {
        return Resolved.failed(
            ExplorerRelationshipAction.of(
                ExplorerRelationshipAction.Status.NOT_FOUND, "Item was not found"));
      }
      return Resolved.ok(guid);
    } catch (RuntimeException e) {
      if (isAccessDenied(e)) {
        return Resolved.failed(
            ExplorerRelationshipAction.of(
                ExplorerRelationshipAction.Status.FORBIDDEN,
                "You do not have permission to read this item"));
      }
      return Resolved.failed(
          ExplorerRelationshipAction.of(
              ExplorerRelationshipAction.Status.NOT_FOUND, "Item was not found"));
    }
  }

  static boolean isAccessDenied(Throwable error) {
    for (Throwable current = error; current != null; current = current.getCause()) {
      String simple = current.getClass().getSimpleName().toLowerCase(Locale.ROOT);
      if (simple.contains("auth")
          || simple.contains("forbidden")
          || simple.contains("permission")
          || simple.contains("accessdenied")) {
        return true;
      }
      String message = current.getMessage();
      if (message != null) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("access denied")
            || lower.contains("not authorized")
            || lower.contains("forbidden")
            || lower.contains("permission denied")) {
          return true;
        }
      }
    }
    return false;
  }

  private static String safeMessage(Exception e, String fallback) {
    String message = e.getMessage();
    if (message == null || message.isBlank()) {
      return fallback;
    }
    return message;
  }

  private static IPSGuid defaultRelationshipGuid(int id) {
    return com.percussion.services.guidmgr.PSGuidManagerLocator.getGuidMgr()
        .makeGuid(id, com.percussion.services.catalog.PSTypeEnum.RELATIONSHIP);
  }

  private static final class Resolved {
    final int contentId;
    final IPSGuid guid;
    final ExplorerRelationshipAction failure;

    private Resolved(int contentId, IPSGuid guid, ExplorerRelationshipAction failure) {
      this.contentId = contentId;
      this.guid = guid;
      this.failure = failure;
    }

    static Resolved ok(IPSGuid guid) {
      return new Resolved(guid.getUUID(), guid, null);
    }

    static Resolved failed(ExplorerRelationshipAction failure) {
      return new Resolved(0, null, failure);
    }
  }
}
