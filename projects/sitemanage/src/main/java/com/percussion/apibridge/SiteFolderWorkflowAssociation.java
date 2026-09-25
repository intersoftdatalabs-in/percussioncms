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

import com.percussion.pathmanagement.data.PSFolderProperties;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSWorkflow;
import java.util.List;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Site-folder workflow association ({@code //Sites/…} {@code sys_workflowid}).
 *
 * <p>Prepare validates the workflow name and that the site folder exists without writing. Commit
 * stores the id. Unknown names are {@code 400} and must be rejected before the site row is saved.
 */
@PSSiteManageBean
@Lazy
public class SiteFolderWorkflowAssociation {

  private static final Logger log = LogManager.getLogger(SiteFolderWorkflowAssociation.class);

  private final IPSContentWs contentWs;
  private final IPSFolderHelper folderHelper;
  private final IPSIdMapper idMapper;
  private final IPSWorkflowService workflowService;

  @Autowired
  public SiteFolderWorkflowAssociation(
      IPSContentWs contentWs,
      IPSFolderHelper folderHelper,
      IPSIdMapper idMapper,
      IPSWorkflowService workflowService) {
    this.contentWs = contentWs;
    this.folderHelper = folderHelper;
    this.idMapper = idMapper;
    this.workflowService = workflowService;
  }

  /** Resolved write that has not been persisted yet. */
  public static final class Assignment {
    private final String folderId;
    private final int workflowId;
    private final String workflowName;

    Assignment(String folderId, int workflowId, String workflowName) {
      this.folderId = folderId;
      this.workflowId = workflowId;
      this.workflowName = workflowName;
    }

    public String workflowName() {
      return workflowName;
    }
  }

  /**
   * Current folder workflow name, or {@code null} when the folder or workflow cannot be read.
   */
  public String readName(IPSSite site) {
    try {
      String path = folderPath(site);
      log.debug("readName site={} path={}", site == null ? null : site.getName(), path);
      if (path == null) {
        return null;
      }
      IPSGuid folderGuid = contentWs.getIdByPath(path);
      if (folderGuid == null) {
        return null;
      }
      PSFolderProperties props = folderHelper.findFolderProperties(idMapper.getString(folderGuid));
      int workflowId = folderHelper.getValidWorkflowId(props);
      if (workflowId <= 0) {
        log.warn("Site '{}' folder '{}' has no workflow id", site.getName(), path);
        return null;
      }
      PSWorkflow workflow = workflowService.loadWorkflow(new PSGuid(PSTypeEnum.WORKFLOW, workflowId));
      if (workflow == null || StringUtils.isBlank(workflow.getName())) {
        List<PSWorkflow> all = workflowService.findWorkflowsByName("%");
        if (all != null) {
          for (PSWorkflow candidate : all) {
            if (candidate != null
                && candidate.getGUID() != null
                && candidate.getGUID().getUUID() == workflowId
                && StringUtils.isNotBlank(candidate.getName())) {
              return candidate.getName();
            }
          }
        }
        log.warn("Site '{}' workflow id {} did not resolve to a name", site.getName(), workflowId);
        return null;
      }
      return workflow.getName();
    } catch (RuntimeException | PSValidationException ex) {
      log.warn("Could not read site workflow for '{}': {}", site == null ? null : site.getName(), ex.toString());
      return null;
    }
  }

  /**
   * Validate an existing workflow and the site folder. Does not write.
   *
   * @throws WebApplicationException 400 unknown workflow, 404 missing site folder
   */
  public Assignment prepare(IPSSite site, String workflowName) {
    String name = workflowName == null ? "" : workflowName.trim();
    if (name.isEmpty()) {
      throw new WebApplicationException("Workflow name is required", Response.Status.BAD_REQUEST);
    }
    // Call the concrete service method (transactional proxy). The interface default
    // findWorkflowByExactName self-invokes and can run without a Hibernate session.
    List<PSWorkflow> matches = workflowService.findWorkflowsByName(name);
    PSWorkflow found = null;
    if (matches != null) {
      for (PSWorkflow workflow : matches) {
        if (workflow != null && name.equalsIgnoreCase(workflow.getName()) && workflow.getGUID() != null) {
          if (found != null) {
            throw new WebApplicationException(
                "Unknown workflow: " + name, Response.Status.BAD_REQUEST);
          }
          found = workflow;
        }
      }
    }
    if (found == null) {
      throw new WebApplicationException("Unknown workflow: " + name, Response.Status.BAD_REQUEST);
    }
    String folderId = requireFolderId(site);
    return new Assignment(folderId, found.getGUID().getUUID(), found.getName());
  }

  /** Persist a prepared assignment onto the site folder. */
  public void commit(Assignment assignment) {
    if (assignment == null) {
      throw new WebApplicationException(
          "Workflow assignment is required", Response.Status.BAD_REQUEST);
    }
    try {
      PSFolderProperties props = folderHelper.findFolderProperties(assignment.folderId);
      if (props == null) {
        throw new WebApplicationException("Site folder not found", Response.Status.NOT_FOUND);
      }
      props.setWorkflowId(assignment.workflowId);
      folderHelper.saveFolderProperties(props);
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (PSValidationException | RuntimeException ex) {
      throw new WebApplicationException(
          "Could not save site workflow", Response.Status.BAD_REQUEST);
    }
  }

  private String requireFolderId(IPSSite site) {
    String path = folderPath(site);
    if (path == null) {
      throw new WebApplicationException("Site folder not found", Response.Status.NOT_FOUND);
    }
    try {
      IPSGuid folderGuid = contentWs.getIdByPath(path);
      if (folderGuid == null) {
        throw new WebApplicationException("Site folder not found", Response.Status.NOT_FOUND);
      }
      String id = idMapper.getString(folderGuid);
      if (StringUtils.isBlank(id)) {
        throw new WebApplicationException("Site folder not found", Response.Status.NOT_FOUND);
      }
      return id;
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new WebApplicationException("Site folder not found", Response.Status.NOT_FOUND);
    }
  }

  /**
   * Logical CMS folder path (always {@code /}, not an OS path). Prefers {@link
   * IPSSite#getFolderRoot()}.
   */
  static String folderPath(IPSSite site) {
    if (site == null) {
      return null;
    }
    if (StringUtils.isNotBlank(site.getFolderRoot())) {
      String root = site.getFolderRoot().trim().replace('\\', '/');
      if (root.startsWith("//")) {
        return root;
      }
      if (root.startsWith("/")) {
        return "/" + root;
      }
      return "//" + root;
    }
    if (StringUtils.isBlank(site.getName())) {
      return null;
    }
    return "//Sites/" + site.getName().trim();
  }
}
