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

import com.percussion.rest.Guid;
import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.rest.workflows.IWorkflowsAdaptor;
import com.percussion.rest.workflows.WorkflowContentTypesDesignLockException;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSNodeDefinition;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.contentmgr.data.PSContentTypeWorkflow;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.PSContentWsLocator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * SY-06 workflow → content-type associations for public REST.
 *
 * <p>GET lists content types linked to a workflow via {@link
 * IPSContentMgr#findNodeDefinitionsByWorkflow}. PUT full-replaces that set using {@link
 * IPSContentDesignWs#loadAssociatedWorkflows} / {@link
 * IPSContentDesignWs#saveAssociatedWorkflows} (same association API Workbench uses when deleting a
 * workflow). Unlike CD-08 ({@code PUT .../contenttypes/{id}/allowedWorkflows}), this path acquires
 * a design lock on each affected content type and releases it on save.
 */
@PSSiteManageBean
public class WorkflowsAdaptor implements IWorkflowsAdaptor {

  private static final Logger log = LogManager.getLogger(WorkflowsAdaptor.class);

  static final String ADMIN_REQUIRED = "Admin role required to read or write workflow associations";

  private final IPSContentDesignWs designWs;
  private final IPSWorkflowService workflowService;
  private final IPSContentMgr contentMgr;
  private final BooleanSupplier adminChecker;

  @Autowired(required = false)
  private IPSUserService userService;

  public WorkflowsAdaptor() {
    this(
        PSContentWsLocator.getContentDesignWebservice(),
        PSWorkflowServiceLocator.getWorkflowService(),
        PSContentMgrLocator.getContentMgr(),
        null);
  }

  /** Package-visible for unit tests. {@code null} adminChecker uses {@link #isCurrentUserAdmin()}. */
  WorkflowsAdaptor(
      IPSContentDesignWs designWs,
      IPSWorkflowService workflowService,
      IPSContentMgr contentMgr,
      BooleanSupplier adminChecker) {
    this.designWs = designWs;
    this.workflowService = workflowService;
    this.contentMgr = contentMgr;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  @Override
  public List<NamedObjectRef> getAllowedContentTypes(URI baseUri, String idOrName) {
    requireAdmin();
    PSWorkflow workflow = resolveWorkflow(idOrName);
    if (workflow == null) {
      return null;
    }
    return listAssociatedContentTypes(workflow.getGUID());
  }

  @Override
  public List<NamedObjectRef> setAllowedContentTypes(
      URI baseUri, String idOrName, List<NamedObjectRef> allowedContentTypes) {
    requireAdmin();
    requireSessionUserForWrite();
    if (allowedContentTypes == null) {
      throw new IllegalArgumentException("allowedContentTypes is required");
    }
    PSWorkflow workflow = resolveWorkflow(idOrName);
    if (workflow == null) {
      return null;
    }
    IPSGuid wfGuid = workflow.getGUID();
    Set<Long> desiredCtUuids = resolveDesiredContentTypeUuids(allowedContentTypes);
    Set<Long> currentCtUuids = currentAssociatedContentTypeUuids(wfGuid);

    Set<Long> toAdd = new LinkedHashSet<>(desiredCtUuids);
    toAdd.removeAll(currentCtUuids);
    Set<Long> toRemove = new LinkedHashSet<>(currentCtUuids);
    toRemove.removeAll(desiredCtUuids);

    for (Long ctUuid : toAdd) {
      addWorkflowAssociation(ctUuid, wfGuid);
    }
    for (Long ctUuid : toRemove) {
      removeWorkflowAssociation(ctUuid, wfGuid);
    }
    return listAssociatedContentTypes(wfGuid);
  }

  private List<NamedObjectRef> listAssociatedContentTypes(IPSGuid wfGuid) {
    List<NamedObjectRef> out = new ArrayList<>();
    try {
      List<IPSNodeDefinition> defs = contentMgr.findNodeDefinitionsByWorkflow(wfGuid);
      if (defs != null) {
        for (IPSNodeDefinition def : defs) {
          if (def == null || def.getGUID() == null) {
            continue;
          }
          out.add(toContentTypeRef(def));
        }
      }
    } catch (RepositoryException e) {
      log.error(
          "Failed to list content types for workflow {}: {}", wfGuid, e.getMessage(), e);
      throw new IllegalStateException("Failed to list workflow content-type associations", e);
    }
    out.sort(
        Comparator.comparing(
            r -> r.getLabel() != null ? r.getLabel() : "", String.CASE_INSENSITIVE_ORDER));
    return out;
  }

  private Set<Long> currentAssociatedContentTypeUuids(IPSGuid wfGuid) {
    Set<Long> out = new HashSet<>();
    try {
      List<IPSNodeDefinition> defs = contentMgr.findNodeDefinitionsByWorkflow(wfGuid);
      if (defs != null) {
        for (IPSNodeDefinition def : defs) {
          if (def != null && def.getGUID() != null) {
            out.add((long) def.getGUID().getUUID());
          }
        }
      }
    } catch (RepositoryException e) {
      throw new IllegalStateException("Failed to load current workflow content-type associations", e);
    }
    return out;
  }

  private void addWorkflowAssociation(long ctUuid, IPSGuid wfGuid) {
    IPSGuid ctGuid = new PSGuid(PSTypeEnum.NODEDEF, ctUuid);
    List<IPSGuid> workflows = loadWorkflowGuidsLocked(ctGuid);
    if (containsGuid(workflows, wfGuid)) {
      // Already associated; release the lock we just acquired.
      saveWorkflowGuids(ctGuid, workflows);
      return;
    }
    List<IPSGuid> next = new ArrayList<>(workflows);
    next.add(wfGuid);
    saveWorkflowGuids(ctGuid, next);
  }

  private void removeWorkflowAssociation(long ctUuid, IPSGuid wfGuid) {
    IPSGuid ctGuid = new PSGuid(PSTypeEnum.NODEDEF, ctUuid);
    List<IPSGuid> workflows = loadWorkflowGuidsLocked(ctGuid);
    List<IPSGuid> next = new ArrayList<>();
    for (IPSGuid g : workflows) {
      if (g != null && !sameWorkflow(g, wfGuid)) {
        next.add(g);
      }
    }
    saveWorkflowGuids(ctGuid, next);
  }

  private List<IPSGuid> loadWorkflowGuidsLocked(IPSGuid ctGuid) {
    try {
      // overrideLock=true steals leftover same-user locks (H2 / crashed PUT).
      List<PSContentTypeWorkflow> rels = designWs.loadAssociatedWorkflows(ctGuid, true, true);
      List<IPSGuid> out = new ArrayList<>();
      if (rels != null) {
        for (PSContentTypeWorkflow rel : rels) {
          if (rel != null && rel.getWorkflowId() != null) {
            out.add(rel.getWorkflowId());
          }
        }
      }
      return out;
    } catch (PSErrorResultsException e) {
      throw mapLoadLockConflict(e, ctGuid);
    }
  }

  private void saveWorkflowGuids(IPSGuid ctGuid, List<IPSGuid> workflowIds) {
    try {
      designWs.saveAssociatedWorkflows(ctGuid, workflowIds, true);
    } catch (PSErrorsException e) {
      throw mapSaveLockConflict(e, ctGuid);
    }
  }

  private Set<Long> resolveDesiredContentTypeUuids(List<NamedObjectRef> refs) {
    Set<Long> out = new LinkedHashSet<>();
    int i = 0;
    for (NamedObjectRef ref : refs) {
      if (ref == null) {
        throw new IllegalArgumentException("allowedContentTypes[" + i + "] is null");
      }
      out.add((long) resolveContentTypeUuid(ref, "allowedContentTypes[" + i + "]"));
      i++;
    }
    return out;
  }

  private int resolveContentTypeUuid(NamedObjectRef ref, String field) {
    if (ref.getGuid() != null) {
      int fromGuid = uuidFromRestGuid(ref.getGuid(), PSTypeEnum.NODEDEF, field);
      if (fromGuid > 0) {
        requireContentTypeExists(fromGuid, field);
        return fromGuid;
      }
    }
    if (StringUtils.isNotBlank(ref.getName())) {
      String want = ref.getName().trim();
      if (want.contains("*")) {
        throw new IllegalArgumentException(field + " must not contain wildcards");
      }
      List<IPSCatalogSummary> found = designWs.findContentTypes(want);
      if (found != null) {
        for (IPSCatalogSummary sum : found) {
          if (sum != null
              && sum.getGUID() != null
              && want.equalsIgnoreCase(StringUtils.defaultString(sum.getName()))) {
            return sum.getGUID().getUUID();
          }
        }
      }
      throw new IllegalArgumentException(field + " content type not found: " + want);
    }
    throw new IllegalArgumentException(field + " requires name or guid");
  }

  private void requireContentTypeExists(int ctUuid, String field) {
    List<IPSCatalogSummary> found = designWs.findContentTypes("*");
    if (found != null) {
      for (IPSCatalogSummary sum : found) {
        if (sum != null && sum.getGUID() != null && sum.getGUID().getUUID() == ctUuid) {
          return;
        }
      }
    }
    throw new IllegalArgumentException(field + " content type not found: " + ctUuid);
  }

  private PSWorkflow resolveWorkflow(String idOrName) {
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    String trimmed = idOrName.trim();
    if (trimmed.contains("*")) {
      throw new IllegalArgumentException("idOrName must not contain wildcards");
    }
    // Numeric uuid
    if (StringUtils.isNumeric(trimmed)) {
      int uuid = Integer.parseInt(trimmed);
      Optional<PSWorkflow> byId =
          workflowService.findWorkflow(new PSGuid(PSTypeEnum.WORKFLOW, uuid));
      return byId.orElse(null);
    }
    // Rest guid string e.g. 0-23-4
    if (trimmed.contains("-")) {
      try {
        IPSGuid g = new PSGuid(trimmed);
        if (g.getType() == PSTypeEnum.WORKFLOW.getOrdinal() || g.getType() == 0) {
          IPSGuid typed =
              g.getType() == 0 ? new PSGuid(PSTypeEnum.WORKFLOW, g.getUUID()) : g;
          Optional<PSWorkflow> byGuid = workflowService.findWorkflow(typed);
          if (byGuid.isPresent()) {
            return byGuid.get();
          }
        }
      } catch (RuntimeException e) {
        log.debug("idOrName not a workflow guid: {}", trimmed);
      }
    }
    List<PSWorkflow> byName = workflowService.findWorkflowsByName(trimmed);
    if (byName == null || byName.isEmpty()) {
      return null;
    }
    return byName.get(0);
  }

  private static NamedObjectRef toContentTypeRef(IPSNodeDefinition def) {
    NamedObjectRef ref = new NamedObjectRef();
    ref.setGuid(ApiUtils.convertGuid(def.getGUID()));
    String name = def.getName();
    ref.setName(name);
    ref.setLabel(StringUtils.defaultIfBlank(def.getLabel(), name));
    return ref;
  }

  private static int uuidFromRestGuid(Guid guid, PSTypeEnum expectedType, String field) {
    if (guid == null) {
      return 0;
    }
    String sv = guid.getStringValue();
    if (StringUtils.isNotBlank(sv)) {
      try {
        IPSGuid g = ApiUtils.convertGuid(guid);
        if (g == null) {
          return 0;
        }
        if (g.getType() == 0) {
          g = new PSGuid(expectedType, g.getUUID());
        }
        if (g.getType() != expectedType.getOrdinal()) {
          throw new IllegalArgumentException(
              field + " guid type mismatch: expected " + expectedType + " got " + g.getType());
        }
        return g.getUUID();
      } catch (IllegalArgumentException e) {
        throw e;
      } catch (RuntimeException e) {
        throw new IllegalArgumentException(field + " invalid guid: " + sv, e);
      }
    }
    int uuid = guid.getUuid();
    if (uuid > 0
        && guid.getType() != 0
        && guid.getType() != expectedType.getOrdinal()) {
      throw new IllegalArgumentException(
          field + " guid type mismatch: expected " + expectedType + " got " + guid.getType());
    }
    return uuid > 0 ? uuid : 0;
  }

  private static boolean containsGuid(List<IPSGuid> guids, IPSGuid want) {
    for (IPSGuid g : guids) {
      if (sameWorkflow(g, want)) {
        return true;
      }
    }
    return false;
  }

  private static boolean sameWorkflow(IPSGuid a, IPSGuid b) {
    if (a == null || b == null) {
      return false;
    }
    return a.getUUID() == b.getUUID();
  }

  private static WorkflowContentTypesDesignLockException mapLoadLockConflict(
      PSErrorResultsException e, IPSGuid ctGuid) {
    String msg =
        "Could not update content type workflow associations; design lock conflict on content"
            + " type "
            + (ctGuid != null ? ctGuid.getUUID() : "?");
    return new WorkflowContentTypesDesignLockException(msg, e);
  }

  private static WorkflowContentTypesDesignLockException mapSaveLockConflict(
      PSErrorsException e, IPSGuid ctGuid) {
    String msg =
        "Could not save content type workflow associations; design lock conflict on content type "
            + (ctGuid != null ? ctGuid.getUUID() : "?");
    return new WorkflowContentTypesDesignLockException(msg, e);
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
          "Request session/user required for workflow association write",
          Response.Status.FORBIDDEN);
    }
  }

  private static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  private static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }
}
