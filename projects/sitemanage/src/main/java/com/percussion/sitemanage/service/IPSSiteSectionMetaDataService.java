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
package com.percussion.sitemanage.service;

import com.percussion.share.data.IPSFolderPath;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.service.IPSDataService;
import java.util.List;

/**
 * Adds and removes items associated with a folder path.
 * This is a low-level service and should not yet be exposed publicly (WS API, REST API).
 */
public interface IPSSiteSectionMetaDataService {
    String TEMPLATES = "Templates";
    String PAGE_CATALOG = "PageCatalog";
    String SECTION_SYSTEM_FOLDER_NAME = ".system";

    void addItem(IPSFolderPath siteSection, String category, String itemId);

    void removeItem(IPSFolderPath siteSection, String category, String itemId);

    void removeCategory(IPSFolderPath siteSection, String category);

    List<IPSItemSummary> findItems(IPSFolderPath siteSection, String category)
            throws IPSDataService.DataServiceNotFoundException;

    List<IPSFolderPath> findSections(String category, String itemId);

    List<String> findCategories(IPSFolderPath siteSection);

    boolean containCategoryFolder(IPSFolderPath siteSection);

    /**
     * Exception thrown when an unexpected error occurs in this service.
     */
    class PSSiteSectionMetaDataServiceException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public PSSiteSectionMetaDataServiceException(String message) {
            super(message);
        }

        public PSSiteSectionMetaDataServiceException(String message, Throwable cause) {
            super(message, cause);
        }

        public PSSiteSectionMetaDataServiceException(Throwable cause) {
            super(cause);
        }
    }
}
