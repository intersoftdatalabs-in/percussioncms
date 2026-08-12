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
package com.percussion.sitemanage.service.impl;

import static java.text.MessageFormat.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.IPSFolderPath;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.service.IPSDataService;
import com.percussion.sitemanage.service.IPSSiteSectionMetaDataService;
import com.percussion.webservices.content.IPSContentWs;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("siteSectionMetaDataService")
@Lazy
public class PSSiteSectionMetaDataService implements IPSSiteSectionMetaDataService {

  private IPSFolderHelper folderHelper;
  private IPSContentWs contentWs;

  @Autowired
  public PSSiteSectionMetaDataService(IPSFolderHelper folderHelper, IPSContentWs contentWs) {
    super();
    notNull(folderHelper);
    this.folderHelper = folderHelper;
    this.contentWs = contentWs;
  }

  public void addItem(IPSFolderPath section, String category, String itemId) {
    String path = sectionToPath(section, category);
    validateItemId(itemId);
    try {
      folderHelper.addItem(path, itemId);
    } catch (Exception e) {
      handleException("add", e, section, category, itemId);
    }
  }

  /**
   * Gets the actual folder path of the category of the specified section.
   *
   * @param section the path of the section, not blank.
   * @param category the category of the section.
   * @return the folder path, not blank.
   */
  protected String sectionToPath(IPSFolderPath section, String category) {
    String sectionPath = section.getFolderPath();
    notNull(sectionPath);
    validateSection(sectionPath);
    validateCategory(category);
    return folderHelper.concatPath(sectionPath, SECTION_SYSTEM_FOLDER_NAME, category);
  }

  public boolean containCategoryFolder(IPSFolderPath section) {
    notNull(section, "section");
    String sectionPath = section.getFolderPath();
    if (sectionPath == null) {
      return false;
    }
    validateSection(sectionPath);
    String categoryPath = folderHelper.concatPath(sectionPath, SECTION_SYSTEM_FOLDER_NAME);
    return contentWs.getIdByPath(categoryPath) != null;
  }

  public List<String> findCategories(IPSFolderPath section) {
    notNull(section, "section");
    var sectionPath = section.getFolderPath();
    validateSection(sectionPath);
    var systemSectionPath = folderHelper.concatPath(sectionPath, SECTION_SYSTEM_FOLDER_NAME);
    List<String> catPaths = new ArrayList<>();
    try {
      catPaths = folderHelper.findChildren(systemSectionPath);
    } catch (Exception e) {
      log.error("failed to find children for path: {}", systemSectionPath);
    }
    // Java 11: Use Stream to map category paths to names
    return catPaths.stream().map(folderHelper::name).toList();
  }

  public List<IPSItemSummary> findItems(IPSFolderPath section, String category)
      throws IPSDataService.DataServiceNotFoundException {
    var path = sectionToPath(section, category);
    var p = folderHelper.pathTarget(path);
    if (p.isToNothing()) {
      return emptyList();
    }
    try {
      return folderHelper.findItems(path);
    } catch (Exception e) {
      throw new PSSiteSectionMetaDataServiceException(
          format(
              "Error happened while finding items for section: {0} for category: {1}",
              section, category),
          e);
    }
  }

  public void removeItem(IPSFolderPath section, String category, String itemId) {
    String path = sectionToPath(section, category);
    validateItemId(itemId);
    try {
      folderHelper.removeItem(path, itemId, false);
    } catch (Exception e) {
      handleException("remove", e, section, category, itemId);
    }
  }

  public List<IPSFolderPath> findSections(String category, String itemId) {
    try {
      var sep = folderHelper.pathSeparator();
      String matchPathRaw = sep + folderHelper.concatPath(SECTION_SYSTEM_FOLDER_NAME, category);
      final String matchPath = removeEnd(matchPathRaw, sep);
      var paths = folderHelper.findPaths(itemId);

      if (log.isDebugEnabled()) {
        log.debug(
            format(
                "findSections - category:{3}, itemId:{2}, matchPath:{0}, paths:{1}",
                matchPath, paths, itemId, category));
      }

      // Java 11: Use Stream to filter and map to SectionPath (implements IPSFolderPath)
      List<IPSFolderPath> sections =
          paths.stream()
              .filter(p -> endsWith(p, matchPath))
              .map(
                  p -> {
                    var path = removeEnd(p, matchPath);
                    isTrue(isNotBlank(path), "The section path should not be empty.");
                    return (IPSFolderPath) new SectionPath(path);
                  })
              .toList();

      if (log.isDebugEnabled()) {
        log.debug("findSections - sections: {}", sections);
      }
      return sections;
    } catch (Exception e) {
      throw new PSSiteSectionMetaDataServiceException(
          format(
              "Error occurred trying to find sections/sites for category: {0} and item id: {1}",
              category, itemId),
          e);
    }
  }

  public void removeCategory(IPSFolderPath siteSection, String category) {
    String path = sectionToPath(siteSection, category);
    try {
      folderHelper.deleteFolder(path);
    } catch (Exception e) {
      throw new PSSiteSectionMetaDataServiceException(
          format(
              "Failed to remove category: {0} from section path: {1}",
              category, siteSection.getFolderPath()));
    }
  }

  public static class SectionPath implements IPSFolderPath {

    private String folderPath;

    public SectionPath(String folderPath) {
      super();
      this.folderPath = folderPath;
    }

    public String getFolderPath() {
      return folderPath;
    }

    public void setFolderPath(String folderPath) {
      this.folderPath = folderPath;
    }

    @Override
    public boolean equals(Object o) {
      return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
      return folderPath == null ? 0 : folderPath.hashCode();
    }

    @Override
    public String toString() {
      final StringBuffer sb = new StringBuffer("SectionPath{");
      sb.append("folderPath='").append(folderPath).append('\'');
      sb.append('}');
      return sb.toString();
    }
  }

  private void handleException(
      String action, Exception e, IPSFolderPath section, String category, String itemId) {
    String error =
        format(
            "Failed to {3} item id: {0} to section path: {1}, category: {2}",
            itemId, section, category, action);
    throw new PSSiteSectionMetaDataServiceException(error, e);
  }

  protected void validateItemId(String itemId) {
    isTrue(isNotBlank(itemId), "Item id cannot be blank");
  }

  protected void validateSection(String section) {
    isTrue(startsWith(section, "//"), "Section paths must begin with //");
  }

  protected void validateCategory(String category) {
    isTrue(
        !contains(category, folderHelper.pathSeparator()),
        "Category must not contain a '" + folderHelper.pathSeparator() + "'");
  }

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Logger log = LogManager.getLogger(PSSiteSectionMetaDataService.class);
}
