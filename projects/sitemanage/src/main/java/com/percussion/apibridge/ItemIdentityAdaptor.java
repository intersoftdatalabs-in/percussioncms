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

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.data.PSInternalRequestCallException;
import com.percussion.itemmanagement.data.PSItemUserInfo;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService.PSItemWorkflowServiceException;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.rest.explorer.IItemIdentityAdaptor;
import com.percussion.rest.explorer.ItemIdentity;
import com.percussion.server.PSRequest;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.system.utils.PSSiteManageBean;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Explorer row visibility identity (#6783 / parent #4530). Resolves the checkout-status token plus
 * numeric content / object / workflow / community ids the shell evaluator needs for Workbench
 * hide-on-match rules — facts that are not on {@code PSPathItem} rows (#6618).
 *
 * <p>Per-item failures (unknown id, unauthorized, or outside the caller's communities)
 * omit that row instead of failing the batch: the client stays permissive for anything
 * missing. Only a missing/empty batch is a 400. Unexpected service failures propagate to
 * the resource's HTTP 500 mapping (they must not masquerade as "not found"). Assignment
 * type and publishable type stay {@code null} until a server follow-on resolves
 * them (residual on #6783).
 */
@PSSiteManageBean
public class ItemIdentityAdaptor implements IItemIdentityAdaptor {

  private static final Logger log = LogManager.getLogger(ItemIdentityAdaptor.class);

  private final IPSItemWorkflowService workflow;
  private final IPSWorkflowHelper workflowHelper;

  @Autowired
  public ItemIdentityAdaptor(
      IPSItemWorkflowService workflow, IPSWorkflowHelper workflowHelper) {
    this.workflow = workflow;
    this.workflowHelper = workflowHelper;
  }

  @Override
  public List<ItemIdentity> lookupBatch(URI baseUri, List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      throw new WebApplicationException("ids are required", Response.Status.BAD_REQUEST);
    }
    List<ItemIdentity> out = new ArrayList<>();
    for (String raw : ids) {
      String id = StringUtils.trimToEmpty(raw);
      if (id.isEmpty()) {
        continue;
      }
      ItemIdentity identity = lookupOne(id);
      if (identity != null) {
        out.add(identity);
      }
    }
    return out;
  }

  private ItemIdentity lookupOne(String id) {
    try {
      PSItemUserInfo owner = workflow.lookupCheckoutOwner(id);
      PSComponentSummary summary = workflowHelper.getComponentSummary(id);
      if (owner == null || summary == null) {
        return null;
      }
      if (!isVisibleToCaller(summary)) {
        log.debug("identity row omitted for id {}: outside caller communities", id);
        return null;
      }
      ItemIdentity identity = new ItemIdentity(id);
      identity.setCheckoutStatus(toCheckoutStatus(owner.getCheckOutUser(), owner.getCurrentUser()));
      identity.setContentTypeId(toPositiveLong(summary.getContentTypeId()));
      identity.setObjectType(toPositiveInt(summary.getObjectType()));
      identity.setWorkflowId(toPositiveInt(summary.getWorkflowAppId()));
      identity.setCommunityId(toPositiveInt(summary.getCommunityId()));
      return identity;
    } catch (PSItemWorkflowServiceException | PSValidationException e) {
      // Unknown id or unauthorized: omit the row, keep the batch permissive.
      log.debug("identity row omitted for id {}: {}", id, e.getMessage());
      return null;
    } catch (RuntimeException e) {
      // Unexpected failure (DB/service outage): must not masquerade as "not found",
      // which would silently fall back to permissive visibility. Propagate so the
      // resource maps it to HTTP 500 (review #6814).
      log.warn("identity lookup failed for id {}", id, e);
      throw e;
    }
  }

  /**
   * Community visibility guard (review #6814). The summary loaders perform no caller-AuthZ
   * check (raw {@code loadComponentSummary}), so without this gate any authenticated caller
   * could enumerate content-type / workflow / community ids and checkout owners of items
   * outside their communities. Rows outside the caller's session communities are omitted
   * like unknown ids. Non-community-scoped rows (community id {@code <= 0}: folders /
   * all-community) stay visible; sessions without community restrictions stay unrestricted.
   * Finer folder-ACL parity remains with the row access-band evaluator client-side.
   */
  static boolean isVisibleToCaller(PSComponentSummary summary) {
    int communityId = summary.getCommunityId();
    if (communityId <= 0) {
      return true;
    }
    try {
      PSRequest req = PSSecurityFilter.getCurrentRequest();
      if (req == null || req.getUserSession() == null) {
        return true;
      }
      List<String> communities = req.getUserSession().getUserCommunities(req);
      if (communities == null || communities.isEmpty()) {
        return true;
      }
      return communities.contains(String.valueOf(communityId));
    } catch (RuntimeException | PSInternalRequestCallException e) {
      // AuthZ uncertainty fails closed: omit rather than disclose (review #6814).
      log.debug("identity community check failed, omitting row: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Checkout token the shell evaluator compares hide-on-match. Mirrors {@code
   * PSActionVisibilityChecker} checkout-status values ({@code "Checked Out By Me"} /
   * {@code "Checked Out"}); blank when nobody holds the lock.
   */
  static String toCheckoutStatus(String checkoutUser, String currentUser) {
    String lock = StringUtils.trimToEmpty(checkoutUser);
    if (lock.isEmpty()) {
      return "";
    }
    String session = StringUtils.trimToEmpty(currentUser);
    if (!session.isEmpty() && lock.equalsIgnoreCase(session)) {
      return "Checked Out By Me";
    }
    return "Checked Out";
  }

  private static Long toPositiveLong(long value) {
    return value > 0 ? value : null;
  }

  private static Integer toPositiveInt(int value) {
    return value > 0 ? value : null;
  }
}
