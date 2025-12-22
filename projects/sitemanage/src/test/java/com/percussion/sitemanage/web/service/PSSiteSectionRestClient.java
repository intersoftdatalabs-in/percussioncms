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

package com.percussion.sitemanage.web.service;

import com.percussion.share.test.PSDataServiceRestClient;
import com.percussion.sitemanage.data.PSCreateExternalLinkSection;
import com.percussion.sitemanage.data.PSCreateSiteSection;
import com.percussion.sitemanage.data.PSMoveSiteSection;
import com.percussion.sitemanage.data.PSReplaceLandingPage;
import com.percussion.sitemanage.data.PSSectionNode;
import com.percussion.sitemanage.data.PSSiteBlogPosts;
import com.percussion.sitemanage.data.PSSiteBlogProperties;
import com.percussion.sitemanage.data.PSSiteSection;
import com.percussion.sitemanage.data.PSSiteSectionProperties;
import com.percussion.sitemanage.data.PSUpdateSectionLink;
import java.util.List;

/**
 * REST client for site section operations. // REFACTORED: CP-JAVA11 Used by {@link
 * PSSiteTemplateServiceTest} for testing all site section operations.
 *
 * @author yubingchen (modernized by Sunny Sal)
 */
public class PSSiteSectionRestClient extends PSDataServiceRestClient<PSSiteSection> {

  public PSSiteSectionRestClient(String url) {
    super(PSSiteSection.class, url, "/Rhythmyx/services/sitemanage/section/");
  }

  public PSSiteSection create(PSCreateSiteSection req) {
    var resp = postObjectToPath(getPath() + "create", req);
    return objectFromResponseBody(resp, PSSiteSection.class);
  }

  public PSSiteSection createSectionLink(String targetSectionGuid, String parentSectionGuid) {
    return getObjectFromPath(
        getPath() + "createSectionLink/" + targetSectionGuid + "/" + parentSectionGuid,
        PSSiteSection.class);
  }

  public PSSiteSection createExternalLinkSection(PSCreateExternalLinkSection req) {
    var resp = postObjectToPath(getPath() + "createExternalLinkSection", req);
    return objectFromResponseBody(resp, PSSiteSection.class);
  }

  public PSSiteSectionProperties getSectionProperties(String id) {
    return getObjectFromPath(getPath() + "properties/" + id, PSSiteSectionProperties.class);
  }

  public PSSiteSection update(PSSiteSectionProperties req) {
    var resp = postObjectToPath(getPath() + "update", req);
    return objectFromResponseBody(resp, PSSiteSection.class);
  }

  public PSSiteSection move(PSMoveSiteSection req) {
    var resp = postObjectToPath(getPath() + "move", req);
    return objectFromResponseBody(resp, PSSiteSection.class);
  }

  public PSSiteSection loadRoot(String siteName) {
    return getObjectFromPath(getPath() + "root/" + siteName);
  }

  public PSSectionNode loadTree(String siteName) {
    return objectFromResponseBody(GET(getPath() + "tree/" + siteName), PSSectionNode.class);
  }

  public List<PSSiteSection> loadChildSections(PSSiteSection section) {
    var resp = postObjectToPath(getPath() + "childSections", section);
    return objectsFromResponseBody(resp, PSSiteSection.class);
  }

  public PSReplaceLandingPage replaceLandingPage(PSReplaceLandingPage req) {
    var resp = postObjectToPath(getPath() + "replaceLandingPage", req);
    return objectFromResponseBody(resp, PSReplaceLandingPage.class);
  }

  public List<PSSiteBlogProperties> getBlogsForSite(String siteName) {
    return getObjectsFromPath(concatPath(getPath(), "blogs", siteName), PSSiteBlogProperties.class);
  }

  public List<PSSiteBlogProperties> getAllBlogs() {
    return getObjectsFromPath(concatPath(getPath(), "allBlogs"), PSSiteBlogProperties.class);
  }

  public PSSiteBlogPosts getBlogPosts(String blogId) {
    return getObjectFromPath(concatPath(getPath(), "blogPosts", blogId), PSSiteBlogPosts.class);
  }

  /**
   * Posts a request to update a section link and returns its response.
   *
   * @param updateRequest {@link PSUpdateSectionLink} request, assumed not null.
   * @return {@link PSSiteSection} object, never null.
   */
  public PSSiteSection updateSectionLink(PSUpdateSectionLink updateRequest) {
    var resp = postObjectToPath(getPath() + "updateSectionLink", updateRequest);
    return objectFromResponseBody(resp, PSSiteSection.class);
  }
}
