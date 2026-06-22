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

package com.percussion.rest.pages;

import com.percussion.rest.errors.FolderNotFoundException;
import com.percussion.rest.errors.PageNotFoundException;
import com.percussion.rest.errors.SiteNotFoundException;
import com.percussion.rest.util.Examples;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class PageTestAdaptor implements IPageAdaptor {

  @Override
  public Page getPage(URI baseUri, String id) {
    var page = Examples.SAMPLE_PAGE;
    page.setId(id);
    if ("invalidId".equals(id)) {
      throw new PageNotFoundException();
    }
    return page;
  }

  @Override
  public Page getPage(URI baseUri, String siteName, String path, String pageName) {
    if ("Sites".equals(siteName)) {
      throw new IllegalArgumentException("siteName cannot be Sites");
    }

    var page = Examples.SAMPLE_PAGE;
    page.setName(pageName);
    page.setFolderPath(path);
    page.setSiteName(siteName);

    if ("testNotFound".equals(siteName)) {
      throw new SiteNotFoundException();
    }
    if (path.contains("testNotFound")) {
      throw new FolderNotFoundException();
    }
    if ("testNotFound".equals(pageName)) {
      throw new PageNotFoundException();
    }
    return page;
  }

  @Override
  public Page updatePage(URI baseUri, Page page) {
    return page;
  }

  @Override
  public void deletePage(URI baseUri, String siteName, String path, String pageName) {
    if ("Sites".equals(siteName)) {
      throw new IllegalArgumentException("siteName cannot be Sites");
    }
    if ("testNotFound".equals(pageName)) {
      throw new PageNotFoundException();
    }
  }

  @Override
  public Page renamePage(URI baseURI, String siteName, String path, String pageName, String name) {
    if ("Sites".equals(siteName)) {
      throw new IllegalArgumentException("siteName cannot be Sites");
    }
    var p = new Page();
    p.setName(name);
    return p;
  }

  @Override
  public int approveAllPages(URI baseURI, String folderPath) {
    return 0;
  }

  @Override
  public Page changePageTemplate(URI baseUri, Page p) {
    return null;
  }

  @Override
  public List<String> allPagesReport(URI baseUri, String siteFolderPath) {
    return null;
  }

  @Override
  public int archiveAllPages(URI baseUri, String folderPath) {
    return 0;
  }

  @Override
  public int submitForReviewAllPages(URI baseUri, String folderPath) {
    return 0;
  }
}
