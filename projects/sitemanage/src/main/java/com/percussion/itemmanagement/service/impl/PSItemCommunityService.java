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
package com.percussion.itemmanagement.service.impl;

import static com.percussion.share.service.exception.PSParameterValidationUtils.rejectIfBlank;
import static com.percussion.webservices.PSWebserviceUtils.getUserName;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.itemmanagement.community.ItemCommunityAssignmentRules;
import com.percussion.itemmanagement.community.ItemCommunityAssignmentRules.Reason;
import com.percussion.itemmanagement.data.PSItemCommunityChoice;
import com.percussion.itemmanagement.data.PSItemCommunityChoices;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.exceptions.PSORMException;
import com.percussion.webservices.security.IPSSecurityWs;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Content Explorer assign-community for one page or asset (#5077 / parent #4530).
 *
 * <p>Writes {@code CONTENTSTATUS.COMMUNITYID}. This is not the login community switch and not
 * folder ACL community. Folders, a blank or unknown id, an unchanged id, and a community that is
 * not in the catalog are not success. An item checked out to someone else is HTTP 409.
 */
@Path("/item/community")
@PSSiteManageBean("itemCommunityRestService")
@Lazy
public class PSItemCommunityService {

  private static final Logger log = LogManager.getLogger(PSItemCommunityService.class);

  private final IPSWorkflowHelper workflowHelper;
  private final IPSSecurityWs securityWs;
  /** When null, the CMS object manager locator supplies the writer. Tests pass a fake. */
  private final IPSCmsObjectMgr objectMgr;

  @Autowired
  public PSItemCommunityService(
      @Lazy IPSWorkflowHelper workflowHelper, IPSSecurityWs securityWs) {
    this(workflowHelper, securityWs, null);
  }

  /** Package-visible so unit tests can write through a fake manager instead of the locator. */
  PSItemCommunityService(
      IPSWorkflowHelper workflowHelper, IPSSecurityWs securityWs, IPSCmsObjectMgr objectMgr) {
    this.workflowHelper = workflowHelper;
    this.securityWs = securityWs;
    this.objectMgr = objectMgr;
  }

  @GET
  @Path("allowed/{id}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSItemCommunityChoices allowed(@PathParam("id") String id) {
    try {
      rejectIfBlank("allowedCommunities", "id", id);
      PSComponentSummary sum = loadItem(id);
      if (sum.isFolder()) {
        throw new WebApplicationException(
            "Folders are not assigned a community", Response.Status.BAD_REQUEST);
      }
      int current = readCommunityId(sum);
      List<PSItemCommunityChoice> choices = loadChoices();
      if (current > 0 && !containsId(choices, current)) {
        PSItemCommunityChoice currentChoice = new PSItemCommunityChoice();
        currentChoice.setId(Integer.toString(current));
        currentChoice.setName(Integer.toString(current));
        choices.add(currentChoice);
      }
      PSItemCommunityChoices out = new PSItemCommunityChoices();
      out.setItemId(id);
      out.setCurrentCommunityId(current > 0 ? Integer.toString(current) : "");
      out.setChoices(choices);
      return out;
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    } catch (RuntimeException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    }
  }

  @POST
  @Path("change/{id}/{communityId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSItemCommunityChoices change(
      @PathParam("id") String id, @PathParam("communityId") String communityId) {
    try {
      rejectIfBlank("changeCommunity", "id", id);
      PSItemCommunityChoices catalog = allowed(id);
      int current = parseCurrent(catalog.getCurrentCommunityId());
      Set<Integer> allowedIds = new HashSet<>();
      if (catalog.getChoices() != null) {
        for (PSItemCommunityChoice choice : catalog.getChoices()) {
          if (choice == null || choice.getId() == null) {
            continue;
          }
          try {
            int parsed = Integer.parseInt(choice.getId().trim());
            if (parsed > 0) {
              allowedIds.add(parsed);
            }
          } catch (NumberFormatException ignored) {
            // skip a catalog row that is not an id
          }
        }
      }
      Reason reason = ItemCommunityAssignmentRules.decide(communityId, current, allowedIds);
      if (reason != Reason.OK) {
        Response.Status status =
            reason == Reason.FORBIDDEN || reason == Reason.UNCHANGED
                ? Response.Status.FORBIDDEN
                : Response.Status.BAD_REQUEST;
        throw new WebApplicationException("Community change rejected: " + reason.name(), status);
      }
      int targetId = Integer.parseInt(communityId.trim());
      PSComponentSummary sum = loadItem(id);
      String holder = sum.getCheckoutUserName();
      if (holder != null && !holder.isBlank()) {
        String me = getUserName();
        if (me == null || !holder.equals(me)) {
          throw new WebApplicationException(
              "Item is checked out to another user", Response.Status.CONFLICT);
        }
      }
      sum.setCommunityId(targetId);
      IPSCmsObjectMgr objMgr = cmsObjectMgr();
      objMgr.saveComponentSummaries(List.of(sum));
      objMgr.evictComponentSummaries(List.of(sum.getContentId()));
      PSItemCommunityChoices out = new PSItemCommunityChoices();
      out.setItemId(id);
      out.setCurrentCommunityId(Integer.toString(targetId));
      out.setChoices(catalog.getChoices());
      return out;
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSORMException | PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.Status.CONFLICT);
    } catch (RuntimeException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.Status.CONFLICT);
    }
  }

  private PSComponentSummary loadItem(String id) throws PSValidationException {
    PSComponentSummary sum = workflowHelper.getComponentSummary(id);
    if (sum == null) {
      throw new WebApplicationException("Item was not found", Response.Status.BAD_REQUEST);
    }
    return sum;
  }

  private IPSCmsObjectMgr cmsObjectMgr() {
    return objectMgr != null ? objectMgr : PSCmsObjectMgrLocator.getObjectManager();
  }

  private List<PSItemCommunityChoice> loadChoices() {
    if (securityWs == null) {
      throw new WebApplicationException(
          "Community catalog is not available", Response.Status.INTERNAL_SERVER_ERROR);
    }
    List<PSCommunity> rows = securityWs.loadCommunities("*");
    List<PSItemCommunityChoice> choices = new ArrayList<>();
    if (rows == null) {
      return choices;
    }
    for (PSCommunity row : rows) {
      if (row == null || row.getId() <= 0) {
        continue;
      }
      PSItemCommunityChoice choice = new PSItemCommunityChoice();
      choice.setId(Long.toString(row.getId()));
      String name = row.getName();
      choice.setName(name == null || name.isBlank() ? choice.getId() : name);
      choices.add(choice);
    }
    return choices;
  }

  private static boolean containsId(List<PSItemCommunityChoice> choices, int id) {
    String key = Integer.toString(id);
    for (PSItemCommunityChoice choice : choices) {
      if (choice != null && key.equals(choice.getId())) {
        return true;
      }
    }
    return false;
  }

  private static int parseCurrent(String raw) {
    if (raw == null || raw.isBlank()) {
      return -1;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException ex) {
      return -1;
    }
  }

  /**
   * {@link PSComponentSummary#getCommunityId()} unboxes a nullable column. A missing id is "none",
   * not a crash.
   */
  static int readCommunityId(PSComponentSummary sum) {
    if (sum == null) {
      return -1;
    }
    try {
      return sum.getCommunityId();
    } catch (NullPointerException ex) {
      return -1;
    }
  }
}
