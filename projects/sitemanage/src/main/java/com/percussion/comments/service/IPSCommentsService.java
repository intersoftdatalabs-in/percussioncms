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
package com.percussion.comments.service;

import com.percussion.comments.data.PSComment;
import com.percussion.comments.data.PSCommentModeration;
import com.percussion.comments.data.PSCommentsSummary;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSValidationException;

import java.util.List;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Service for managing comments in Percussion CMS.
 *
 * <p>Provides APIs for retrieving, moderating, and summarizing comments.
 * All methods are required to be backward compatible.
 *
 * <p>Example usage:
 * <pre>{@code
 * List<PSCommentsSummary> summaries = commentsService.getPagesWithComments("siteA", 10, 0);
 * }</pre>
 *
 * @author davidpardini
 */
public interface IPSCommentsService {
    /**
     * Provides a list of all pages with comments for the given site.
     *
     * @param site the site name, must not be blank
     * @param max maximum number of comments per delivery server (for paging)
     * @param start index to start returning comments (for paging)
     * @return list of page summaries, never {@code null}, may be empty
     */
    List<PSCommentsSummary> getPagesWithComments(
            @PathParam("site") String site,
            @QueryParam("max") Integer max,
            @QueryParam("start") Integer start);

    /**
     * Provides a summary of the comment information for the given page.
     * The count information is combined from all delivery servers.
     *
     * @param id page id, never blank
     * @return comment summary for the page, never {@code null}
     */
    PSCommentsSummary getCommentsSummary(String id)
            throws IPSDataService.DataServiceLoadException,
                   IPSDataService.DataServiceNotFoundException,
                   PSValidationException;

    /**
     * Provides a list of count info only for all pages with comments for the given site.
     *
     * @param siteName the site name, not {@code null} or empty
     * @return list of summaries (counts and path only), never {@code null}, may be empty
     */
    List<PSCommentsSummary> getCommentCountsForSite(String siteName);

    /**
     * Gets all comments on the requested page.
     *
     * @param site the site name
     * @param pagePath the page path (e.g., /Sites/sitename/.../page.html)
     * @param max unused
     * @param start unused
     * @return all comments on the page, never {@code null}
     */
    List<PSComment> getCommentsOnPage(
            @PathParam("site") String site,
            @PathParam("url") String pagePath,
            @QueryParam("max") Integer max,
            @QueryParam("start") Integer start);

    /**
     * Approves or rejects comments according to the PSCommentModeration object.
     *
     * @param site the site name
     * @param commentModeration moderation info, must not be {@code null}
     */
    void moderate(String site, PSCommentModeration commentModeration);

    /**
     * Sets the license override for underlying calls (for unit testing).
     *
     * @param licenseId the license id
     */
    void setLicenseOverride(String licenseId);

    /**
     * Returns the current license override if any.
     *
     * @return license id or empty string, never {@code null}
     */
    String getLicenseOverride();
}
