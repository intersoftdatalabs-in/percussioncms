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

import com.percussion.itemmanagement.data.PSItemUserInfo;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService.PSItemWorkflowServiceException;
import com.percussion.rest.editor.EditorItemLockInfo;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.rest.editor.IEditorItemLockAdaptor;
import com.percussion.rest.errors.NotAuthorizedException;
import com.percussion.system.utils.PSSiteManageBean;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * EditorHost checkout / check-in (#4644). Maps missing assignment to HTTP 403 and lock conflicts to
 * HTTP 409. Does not treat a 200 {@code ItemUserInfo} held by another user as success.
 */
@PSSiteManageBean
public class EditorItemLockAdaptor implements IEditorItemLockAdaptor {

  private final IPSItemWorkflowService workflow;

  @Autowired
  public EditorItemLockAdaptor(IPSItemWorkflowService workflow) {
    this.workflow = workflow;
  }

  @Override
  public EditorItemLockInfo checkout(URI baseUri, String itemId) {
    requireId(itemId);
    try {
      if (!workflow.isModifiableByUser(itemId)) {
        throw new NotAuthorizedException();
      }
      PSItemUserInfo info = workflow.checkOut(itemId);
      EditorItemLockInfo mapped = map(info);
      if (heldByOther(mapped)) {
        throw conflict("Item is checked out to another user");
      }
      return mapped;
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSItemWorkflowServiceException | PSValidationException e) {
      throw forbiddenOrConflict(e);
    }
  }

  @Override
  public EditorItemLockInfo checkin(URI baseUri, String itemId) {
    requireId(itemId);
    try {
      if (!workflow.isModifiableByUser(itemId)) {
        throw new NotAuthorizedException();
      }
      workflow.checkIn(itemId);
      return new EditorItemLockInfo("", "", "", "");
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSItemWorkflowServiceException | PSValidationException e) {
      throw conflict(e.getMessage() == null ? "Check-in conflict" : e.getMessage());
    }
  }

  static boolean heldByOther(EditorItemLockInfo info) {
    if (info == null) {
      return false;
    }
    String lock = StringUtils.trimToEmpty(info.getCheckOutUser());
    String session = StringUtils.trimToEmpty(info.getCurrentUser());
    if (lock.isEmpty() || session.isEmpty()) {
      return false;
    }
    return !lock.equalsIgnoreCase(session);
  }

  private static EditorItemLockInfo map(PSItemUserInfo info) {
    if (info == null) {
      return new EditorItemLockInfo("", "", "", "");
    }
    return new EditorItemLockInfo(
        StringUtils.defaultString(info.getItemName()),
        StringUtils.defaultString(info.getCheckOutUser()),
        StringUtils.defaultString(info.getCurrentUser()),
        StringUtils.defaultString(info.getAssignmentType()));
  }

  private static void requireId(String itemId) {
    if (StringUtils.isBlank(itemId)) {
      throw new WebApplicationException("item id is required", Response.Status.BAD_REQUEST);
    }
  }

  private static WebApplicationException conflict(String message) {
    return new WebApplicationException(message, Response.Status.CONFLICT);
  }

  private static WebApplicationException forbiddenOrConflict(Exception e) {
    String msg = e.getMessage() == null ? "" : e.getMessage();
    if (StringUtils.containsIgnoreCase(msg, "not allowed")
        || StringUtils.containsIgnoreCase(msg, "permission")
        || StringUtils.containsIgnoreCase(msg, "forbidden")) {
      return new NotAuthorizedException();
    }
    return conflict(msg.isEmpty() ? "Checkout conflict" : msg);
  }
}
