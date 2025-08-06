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

package com.percussion.category.web.service;

import com.percussion.category.data.PSCategory;
import com.percussion.category.data.PSCategoryNode;
import com.percussion.category.service.IPSCategoryService;
import com.percussion.share.service.exception.PSDataServiceException;

/**
 * REST client for category management, used in integration tests.
 * Uses Jersey client to interact with the Percussion CMS category REST API.
 */
public class PSCategoryServiceRestClient extends PSJerseyRestClient implements IPSCategoryService {

    private String path = "/services/categorymanagement/category";

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public PSCategory getCategoryList(String siteName) throws PSDataServiceException {
        login("Admin", "demo");
        return getData(concatPath(getPath(), "all", siteName));
    }

    @Override
    public PSCategory updateCategories(PSCategory category, String siteName) {
        return postData(concatPath(getPath(), "update", siteName), category);
    }

    @Override
    public String getLockInfo() {
        return null;
    }

    @Override
    public void lockCategoryTab(String date) {
        // Not implemented for test client
    }

    @Override
    public void removeCategoryTabLock() {
        // Not implemented for test client
    }

    @Override
    public void updateCategoryInDTS(String sitename, String deliveryserver) {
        // Not implemented for test client
    }

    @Override
    public PSCategory getCategoryTreeForSite(String sitename, String rootPath, boolean includeDeleted, boolean includeSelectable) {
        return null;
    }

    @Override
    public PSCategoryNode findCategoryNode(String siteName, String rootPath, boolean includeDeleted, boolean includeNotSelectable) {
        return null;
    }
}
