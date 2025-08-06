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

import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;

/**
 * Service for importing sites and cataloged pages.
 */
public interface IPSSiteImportService {

    /**
     * Imports a site from a URL.
     *
     * @param site the site to import, not null.
     * @param userAgent the user agent string, not null.
     * @return the import context, not null.
     * @throws PSSiteImportException if an error occurs.
     */
    PSSiteImportCtx importSiteFromUrl(PSSite site, String userAgent) throws PSSiteImportException;

    /**
     * Imports a cataloged page.
     *
     * @param site the imported site, not null.
     * @param pageId the cataloged page, not blank.
     * @param userAgent the user agent used to import the site, not blank.
     * @param context the import context, not null.
     * @return the import context, not null.
     * @throws PSSiteImportException if an error occurs.
     */
    PSSiteImportCtx importCatalogedPage(PSSite site, String pageId, String userAgent, PSSiteImportCtx context)
            throws PSSiteImportException;
}
