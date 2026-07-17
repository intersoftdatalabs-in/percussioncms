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
package com.percussion.pathmanagement.service.impl;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.springframework.util.StringUtils.trimLeadingCharacter;
import static org.springframework.util.StringUtils.trimTrailingCharacter;

import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.designmanagement.service.IPSFileSystemService;
import com.percussion.designmanagement.service.IPSFileSystemService.PSExistingFolderException;
import com.percussion.designmanagement.service.IPSFileSystemService.PSFolderNameLengthLimitException;
import com.percussion.designmanagement.service.IPSFileSystemService.PSFolderOperationException;
import com.percussion.designmanagement.service.IPSFileSystemService.PSInvalidCharacterInFolderNameException;
import com.percussion.designmanagement.service.IPSFileSystemService.PSInvalidFolderNameException;
import com.percussion.pathmanagement.data.PSDeleteFolderCriteria;
import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.pathmanagement.data.PSItemByWfStateRequest;
import com.percussion.pathmanagement.data.PSMoveFolderItem;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.data.PSRenameFolderItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.security.io.PSPathInjectionGuard;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.PSDateUtils;
import com.percussion.share.data.IPSItemSummary.Category;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.data.PSNoContent;
import com.percussion.share.service.exception.PSBeanValidationUtils;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.ui.service.IPSListViewHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link IPSPathService} implementation that handles requests to URL that maps to the file system.
 */
public abstract class PSFileSystemPathItemService implements IPSPathService {

  public static final String FILE_SYSTEM_FILE_TYPE = "FSFile";
  public static final String FILE_SYSTEM_FOLDER_TYPE = "FSFolder";
  public static final String VALIDATE_SUCCESS = "Success";

  private static final Logger log = LogManager.getLogger(PSFileSystemPathItemService.class);

  // FIXME We should not use this class directly.
  private final PSItemDefManager itemDefManager = PSItemDefManager.getInstance();

  protected IPSListViewHelper listViewHelper;
  protected IPSFileSystemService fileSystemService;
  protected String rootName;
  protected IPSFolderHelper folderHelper;

  public PSFileSystemPathItemService(
      IPSFolderHelper folderHelper,
      IPSFileSystemService fileSystemManagerService,
      IPSListViewHelper listViewHelper) {
    this.folderHelper = folderHelper;
    this.fileSystemService = fileSystemManagerService;
    this.listViewHelper = listViewHelper;
  }

  @Override
  public IPSListViewHelper getListViewHelper() {
    return listViewHelper;
  }

  public void setListViewHelper(IPSListViewHelper listViewHelper) {
    this.listViewHelper = listViewHelper;
  }

  public String getRootName() {
    return rootName;
  }

  public void setRootName(String rootName) {
    this.rootName = rootName;
  }

  @Override
  public List<String> getRolesAllowed() {
    // By default, any role is allowed access URL served by this IPSPathService implementation.
    return List.of();
  }

  private List<File> getChildren(String path) {
    try {
      return fileSystemService.getChildren(path);
    } catch (FileNotFoundException e) {
      return Collections.emptyList();
    }
  }

  @Override
  public PSPathItem find(String path)
      throws PSPathNotFoundServiceException, PSPathServiceException {
    log.debug("Find root of path: {}", path);
    if ("/".equals(path)) {
      return findRoot();
    }
    return findItem(path);
  }

  protected PSPathItem findItem(String path) throws PSPathNotFoundServiceException {
    notEmpty(path);

    var file = fileSystemService.getFile(path);

    if (!file.exists()) {
      throw new PSPathNotFoundServiceException("The path doesn't exist: " + path);
    }

    String parentPath;
    if ("/".equals(path)) {
      parentPath = "/";
    } else {
      if (path.endsWith("/")) {
        parentPath = "/" + FilenameUtils.getPath(path.substring(0, path.length() - 1));
      } else {
        parentPath = "/" + FilenameUtils.getPath(path);
      }
    }

    if (!parentPath.endsWith("/")) {
      parentPath += "/";
    }

    var item = getPathItemFromFile(parentPath, file);

    // Always add a slash at the end of the path, regardless of folder or file.
    if (!item.getPath().endsWith("/")) {
      item.setPath(item.getPath() + "/");
    }

    return item;
  }

  @Override
  public List<PSPathItem> findChildren(String path)
      throws PSPathNotFoundServiceException, PSPathServiceException {
    var children = getChildren(path);
    var folderPathItems = new ArrayList<PSPathItem>();
    var filePathItems = new ArrayList<PSPathItem>();

    for (var child : children) {
      if (child.isDirectory()) {
        folderPathItems.add(getPathItemFromFile(path, child));
      } else if (!PSPathOptions.folderChildrenOnly()) {
        filePathItems.add(getPathItemFromFile(path, child));
      }
    }

    folderPathItems.sort(PSPathItemComparator.getInstance());
    filePathItems.sort(PSPathItemComparator.getInstance());

    folderPathItems.addAll(filePathItems);

    return folderPathItems;
  }

  private PSPathItem getPathItemFromFile(String parentPath, File child)
      throws PSPathNotFoundServiceException {
    // CWE-22/CWE-23 defense (T043): child is a File produced upstream by
    // PSFileSystemService.getChildren, which calls validatePath on the
    // user-supplied path; however CodeQL does not yet model that custom
    // sanitizer. Apply the canonical PSPathInjectionGuard.requireSafeFileName
    // on child.getName() here as defense-in-depth — the single-segment
    // name check rejects traversal and NUL-byte payloads BEFORE any
    // child.isDirectory() / child.getName() / child.getPath() calls below
    // are reached. See #1053 (CodeQL java/path-injection).
    PSPathInjectionGuard.requireSafeFileName(child.getName());
    // parent path should be a folder
    if (fileSystemService.getFile(parentPath).isFile()) {
      parentPath = fileSystemService.getParentFolder(parentPath);
    }

    var item = new PSPathItem();
    item.setName(fileSystemService.getNameFromFile(child));
    item.setId(generatePathItemId(child));
    item.setType(child.isDirectory() ? FILE_SYSTEM_FOLDER_TYPE : FILE_SYSTEM_FILE_TYPE);
    item.setIcon(getIcon(child));

    var itemPath = parentPath + child.getName();
    if (!itemPath.endsWith("/") && child.isDirectory()) {
      itemPath += "/";
    }

    item.setPath(itemPath);
    item.setFolderPath(folderHelper.concatPath(getFullFolderPath(parentPath), item.getName()));
    item.setFolderPaths(List.of(FilenameUtils.getFullPathNoEndSeparator(item.getFolderPath())));
    // Avoid compile-time dependency on the SYSTEM constant which might not exist in older
    // versions of the enum used on the classpath.  Use valueOf(String) instead, which will
    // compile as long as Category is an enum.
    item.setCategory(Category.valueOf("SYSTEM"));
    item.setRevisionable(false);
    item.setLeaf(!child.isDirectory());
    item.setAccessLevel(PSFolderPermission.Access.ADMIN);
    item.setRelatedObject(child);

    return item;
  }

  private String generatePathItemId(File file) {
    return Integer.toString(file.getPath().hashCode());
  }

  private String getIcon(File file) {
    if (file.isDirectory()) {
      return "/Rhythmyx/sys_resources/images/finderFolder.png";
    }

    var rxProps = itemDefManager.getRxFileIconProperties();
    var sysProps = itemDefManager.getSysFileIconProperties();

    var fileExtension = FilenameUtils.getExtension(file.getName()).toLowerCase();

    var iconFn = rxProps.getProperty(fileExtension);
    iconFn = getIconPath(iconFn, false);

    // Get it from system properties if it is blank
    if (StringUtils.isBlank(iconFn)) {
      iconFn = sysProps.getProperty(fileExtension);
      iconFn = getIconPath(iconFn, true);
    }

    return iconFn;
  }

  private String getIconPath(String fileExtension, boolean isSys) {
    var iconPath = itemDefManager.getFullIconPath(fileExtension, isSys);

    if (iconPath == null) {
      return StringUtils.EMPTY;
    }

    // FIXME This shouldn't be hardcoded
    return "/Rhythmyx" + iconPath.substring(2);
  }

  @Override
  public PSItemProperties findItemProperties(String path) {
    var file = fileSystemService.getFile(path);

    var itemProperties = new PSItemProperties();
    itemProperties.setId(file.getName());
    itemProperties.setName(file.getName());
    itemProperties.setSize(String.valueOf(file.length()));
    itemProperties.setLastModifiedDate(PSDateUtils.getDateToString(new Date(file.lastModified())));
    itemProperties.setPath(path);

    return itemProperties;
  }

  @Override
  public List<PSItemProperties> findItemProperties(PSItemByWfStateRequest request)
      throws PSPathNotFoundServiceException, PSPathServiceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public PSPathItem addFolder(String path)
      throws PSPathNotFoundServiceException, PSPathServiceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public PSPathItem addNewFolder(String path)
      throws PSPathNotFoundServiceException, PSPathServiceException {
    try {
      var newFolder = fileSystemService.addFolder(path);
      return getPathItemFromFile(path, newFolder);
    } catch (IOException e) {
      throw new PSPathServiceException(e);
    }
  }

  @Override
  public PSPathItem renameFolder(PSRenameFolderItem item)
      throws PSSpringValidationException, PSPathNotFoundServiceException {
    var errors = PSBeanValidationUtils.validate(item);
    errors.throwIfInvalid();

    try {
      var newFolder = fileSystemService.renameFolder(item.getPath(), item.getName());
      return getPathItemFromFile(fileSystemService.getParentFolder(item.getPath()), newFolder);
    } catch (PSFolderNameLengthLimitException e) {
      errors.rejectValue(
          "name",
          "renameFolderItem.longName",
          "Cannot rename folder '<old_name>' to '<new_name>' because that name exceeds character"
              + " limit.");
      throw errors;
    } catch (PSInvalidFolderNameException e) {
      errors.rejectValue(
          "name",
          "renameFolderItem.reservedName",
          "Cannot rename folder '<old_name>' to '<new_name>' because that is a reserved folder"
              + " name.");
      throw errors;
    } catch (PSExistingFolderException e) {
      errors.rejectValue(
          "name",
          "renameFolderItem.duplicatedName",
          "Cannot rename folder '<old_name>' to '<new_name>' because there is another folder or"
              + " file with that name.");
      throw errors;
    } catch (PSInvalidCharacterInFolderNameException e) {
      errors.rejectValue(
          "name",
          "renameFolderItem.invalidCharInName",
          "Cannot rename folder '<old_name>' to '<new_name>' because folder names cannot contain"
              + " the following characters: "
              + e.getInvalidChars());
      throw errors;
    } catch (PSFolderOperationException e) {
      // TODO Fix the message here
      errors.rejectValue(
          "name", "renameFolderItem.unknownCause", "Unknown problem when renaming the folder.");
      throw errors;
    }
  }

  @Override
  public PSNoContent moveItem(PSMoveFolderItem request) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int deleteFolder(PSDeleteFolderCriteria criteria) throws PSPathServiceException {
    try {
      fileSystemService.deleteFolder(criteria.getPath());
      return 0;
    } catch (IOException e) {
      throw new PSPathServiceException(
          "An error occurred when deleting the folder '"
              + getFolderName(criteria)
              + "'. Some files or folders may not have been deleted.");
    }
  }

  private String getFolderName(PSDeleteFolderCriteria criteria) {
    var paths = criteria.getPath().split("/");
    if (!paths[paths.length - 1].isEmpty()) {
      return paths[paths.length - 1];
    } else {
      return paths[paths.length - 2];
    }
  }

  @Override
  public String validateFolderDelete(String path) throws PSPathServiceException {
    notEmpty(path, "path");

    var response = "";

    // validate that the folder we are about to delete is below the 'themes' folder
    var auxPath = trimLeadingCharacter(trimTrailingCharacter(path, '/'), '/');
    var paths = auxPath.split("/");
    if (paths.length < 2) {
      // it means that we only have the themes folder, so it can't be deleted
      response = "VALIDATE_ERROR_NOT_UNDER_THEMES";
    }

    return StringUtils.isEmpty(response) ? VALIDATE_SUCCESS : response;
  }

  @Override
  public String findLastExistingPath(String path) {
    throw new UnsupportedOperationException();
  }

  protected abstract PSPathItem findRoot() throws PSPathNotFoundServiceException;

  protected abstract String getFullFolderPath(String path) throws PSPathNotFoundServiceException;
}
