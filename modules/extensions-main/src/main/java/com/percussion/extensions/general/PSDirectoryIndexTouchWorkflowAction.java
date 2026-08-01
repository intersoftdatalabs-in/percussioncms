/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.extensions.general;

import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.extension.IPSWorkFlowContext;
import com.percussion.extension.IPSWorkflowAction;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.utils.guid.IPSGuid;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Workflow action that ensures Directory Index pages are re-published when {@code percPerson},
 * {@code percDepartment}, or {@code percOrganization} assets are updated.
 *
 * <p>The Directory widget ({@code percDirectory}) assembles its content via JCR queries at assembly
 * time rather than through Active Assembly relationships. This means that when a {@code percPerson}
 * or {@code percDepartment} item is approved, the standard {@code sys_TouchItemsWorkflowAction}
 * cannot find the Directory Index pages via the AA parent chain and therefore does not touch
 * (update the {@code LASTMODIFIEDDATE} of) those pages.
 *
 * <p>This action resolves the problem by finding <em>all</em> {@code percDirectory} content items
 * in the repository and touching them together with their AA parent pages (the Directory Index
 * pages). This causes the incremental-publishing filter to detect the pages as modified and include
 * them in the next publish run.
 *
 * <p>Register this action on every workflow transition that corresponds to content approval/publish
 * for the {@code percPerson}, {@code percDepartment}, and {@code percOrganization} content types
 * (for example the "Approve" and "Quick Approve" transitions in the Default Workflow).
 *
 * @see IPSWorkflowAction
 * @see com.percussion.services.publisher.IPSPublisherService#touchContentTypeItems
 */
public class PSDirectoryIndexTouchWorkflowAction extends PSDefaultExtension
    implements IPSWorkflowAction {
  /** Creates a new PSDirectoryIndexTouchWorkflowAction. */
  public PSDirectoryIndexTouchWorkflowAction() {}

  /** The logger for this class. */
  private static final Logger ms_logger =
      LogManager.getLogger(PSDirectoryIndexTouchWorkflowAction.class);

  /** Name of the Directory widget content type whose items will be touched. */
  private static final String DIRECTORY_CONTENT_TYPE = "percDirectory";

  /**
   * Performs the workflow action.
   *
   * <p>Locates the {@code percDirectory} content type, retrieves all content items of that type,
   * touches them and their Active Assembly parent pages so that the incremental publisher will
   * re-publish every Directory Index page that may be affected by the change to a Person,
   * Department, or Organisation asset.
   *
   * @param context the workflow context, never {@code null}.
   * @param request the request context, never {@code null}.
   * @throws PSExtensionProcessingException if the action cannot be performed.
   */
  @Override
  public void performAction(IPSWorkFlowContext context, IPSRequestContext request)
      throws PSExtensionProcessingException {
    ms_logger.debug(
        "PSDirectoryIndexTouchWorkflowAction: touching percDirectory items "
            + "and their AA parent pages for content item id={}",
        context.getContentID());

    try {
      long directoryTypeId = resolveContentTypeId(DIRECTORY_CONTENT_TYPE);
      if (directoryTypeId < 0) {
        ms_logger.warn(
            "PSDirectoryIndexTouchWorkflowAction: could not resolve content type '{}'. "
                + "Directory Index pages will not be touched automatically.",
            DIRECTORY_CONTENT_TYPE);
        return;
      }

      IPSGuid directoryTypeGuid = new PSGuid(PSTypeEnum.NODEDEF, directoryTypeId);
      Collection<IPSGuid> typeIds = new ArrayList<>();
      typeIds.add(directoryTypeGuid);

      IPSPublisherService pub = PSPublisherServiceLocator.getPublisherService();
      Collection<Integer> touchedIds = pub.touchContentTypeItems(typeIds);

      ms_logger.debug(
          "PSDirectoryIndexTouchWorkflowAction: touched {} items (percDirectory "
              + "assets and their AA parent Directory Index pages).",
          touchedIds.size());

    } catch (Exception e) {
      ms_logger.error(
          "PSDirectoryIndexTouchWorkflowAction: failed to touch Directory Index pages.", e);
      throw new PSExtensionProcessingException(getClass().getName(), e);
    }
  }

  /**
   * Resolves the numeric content type ID for the given content type name using the {@link
   * PSItemDefManager}.
   *
   * @param contentTypeName the name of the content type, never {@code null} or blank.
   * @return the content type ID, or {@code -1} if the name cannot be resolved.
   */
  private long resolveContentTypeId(String contentTypeName) {
    try {
      PSItemDefManager mgr = PSItemDefManager.getInstance();
      return mgr.contentTypeNameToId(contentTypeName);
    } catch (Exception e) {
      ms_logger.error(
          "PSDirectoryIndexTouchWorkflowAction: failed to resolve content type '{}': {}",
          contentTypeName,
          e.getMessage());
      return -1L;
    }
  }
}
