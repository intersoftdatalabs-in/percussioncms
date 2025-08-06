// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.sitemanage.service;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.data.PSPagedItemList;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.data.PSPublishingAction;
import com.percussion.sitemanage.data.PSSitePublishResponse;

import java.util.List;

/**
 * Service for site publishing operations.
 */
public interface IPSSitePublishService {

    String FULL_EDITION_SUFFIX = "_FULL";

    /**
     * The publishing action type. Indicates which edition will be invoked.
     */
    enum PubType {
        FULL,
        FULL_NONBINARY,
        INCREMENTAL,
        STAGING_INCREMENTAL,
        PUBLISH_NOW,
        TAKEDOWN_NOW,
        STAGE_NOW,
        REMOVE_FROM_STAGING_NOW;

        /**
         * Lookup value by ordinal.
         *
         * @param ordinal the ordinal.
         * @return the matching enum value, or FULL as a default.
         */
        public static PubType valueOf(int ordinal) {
            for (var t : values()) {
                if (t.ordinal() == ordinal) {
                    return t;
                }
            }
            return FULL;
        }
    }

    PSSitePublishResponse publishIncremental(String siteName, String id, boolean isResource, String server)
            throws PSDataServiceException, IPSPubServerService.PSPubServerServiceException,
            IPSItemWorkflowService.PSItemWorkflowServiceException, IPSItemService.PSItemServiceException, PSNotFoundException;

    PSSitePublishResponse publishIncrementalWithApproval(String siteName, String id, boolean isResource, String server, String itemsToApprove)
            throws PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException,
            IPSPubServerService.PSPubServerServiceException, IPSItemService.PSItemServiceException, PSNotFoundException;

    PSSitePublishResponse publish(String siteName, PubType type, String id, boolean isResource, String server)
            throws PSDataServiceException, IPSPubServerService.PSPubServerServiceException,
            IPSItemWorkflowService.PSItemWorkflowServiceException, IPSItemService.PSItemServiceException, PSNotFoundException;

    List<PSPublishingAction> getPublishingActions(String id)
            throws PSDataServiceException, PSNotFoundException;

    PSPagedItemList getQueuedIncrementalContent(String siteName, String serverName, int startIndex, int pageSize)
            throws PSSitePublishException;

    PSPagedItemList getQueuedIncrementalRelatedContent(String siteName, String serverName, int startIndex, int pageSize)
            throws PSSitePublishException;

    /**
     * Exception thrown when an error occurs attempting to publish a site.
     */
    class PSSitePublishException extends PSDataServiceException {
        private static final long serialVersionUID = 1L;

        public PSSitePublishException() {
            super();
        }

        public PSSitePublishException(String message, Throwable cause) {
            super(message, cause);
        }

        public PSSitePublishException(String message) {
            super(message);
        }

        public PSSitePublishException(Throwable cause) {
            super(cause);
        }
    }
}
