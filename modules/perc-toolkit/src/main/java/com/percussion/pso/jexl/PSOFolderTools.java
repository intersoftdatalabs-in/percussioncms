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
package com.percussion.pso.jexl;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSFolderProperty;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSJexlExpression;
import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.IPSJexlParam;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSJexlUtilBase;
import com.percussion.pso.utils.PSOItemFolderUtilities;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.data.PSAssemblyWorkItem;
import com.percussion.services.assembly.impl.nav.PSNavHelper;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.contentmgr.IPSNode;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * JEXL functions for folder manipulation.
 *
 * @author DavidBenua
 * @author AdamGent
 */
public class PSOFolderTools extends PSJexlUtilBase implements IPSJexlExpression {
  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(PSOFolderTools.class);

  private IPSContentWs contentWs;
  private IPSGuidManager guidManager;

  /**
   * Extensions manager will use this constructor.
   * Creates a new PSOFolderTools.
   *
   */
  public PSOFolderTools() {
    super();
  }

  /**
   * Preferred Constructor for programatic use outside jexl.
   *
   * @param contentWs Content web service.
   * @param guidManager Guid Manager.
   */
  public PSOFolderTools(IPSContentWs contentWs, IPSGuidManager guidManager) {
    super();
    init(contentWs, guidManager);
  }

  /**
   * init operation.
   *
   * @param contentWs the content ws
   * @param guidManager the guid manager
   */
  protected final void init(IPSContentWs contentWs, IPSGuidManager guidManager) {
    this.contentWs = contentWs;
    this.guidManager = guidManager;
  }

  /**
   * Get the folder path for an item.
   *
   * @param itemId the GUID for the item
   * @return the parent folder path. If there are multiple paths, the first one will be returned.
   *     Will be null if the item is not in any folders.
   * @throws PSErrorException if an error occurs
   * @throws PSCmsException if a CMS error occurs
   */
  @IPSJexlMethod(
      description = "get the folder path for this item",
      params = {@IPSJexlParam(name = "itemId", description = "the item GUID")})
  /**
   * Returns the parent folder path.
   *
   * @param itemId the item id
   * @return the result
   * @throws PSErrorException if an error occurs
   * @throws PSCmsException if an error occurs
   */
  public String getParentFolderPath(IPSGuid itemId) throws PSErrorException, PSCmsException {
    String errmsg;

    List<String> paths = getParentFolderPaths(itemId);
    if (paths.isEmpty()) {
      errmsg = "no paths returned for " + itemId;
      log.info(errmsg);
      return null;
    }
    if (paths.size() == 1) {
      log.debug("found path {}", paths.get(0));
      return paths.get(0);
    }
    log.warn("multiple paths found for item " + itemId);
    return paths.get(0);
  }

  /**
   * Returns the folder.
   *
   * @param path the path
   * @return the result
   */
  public PSFolder getFolder(String path) {
    PSFolder folder = null;
    try {
      if (path == null) {
        throw new RuntimeException("Path parameter cannot be null");
      }

      folder = getContentWs().loadFolders(new String[] {path}).get(0);

      if (folder == null) {
        throw new RuntimeException();
      }

    } catch (PSErrorResultsException e) {
      log.error(
          "Could not locate Folder for: {} Error: {}", path, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new RuntimeException(e);
    } catch (Exception e) {
      log.error(
          "An unexpected exception occurred while retrieving Folder for: {} Error: {}",
          path,
          e.getMessage());
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return folder;
  }

  /**
   * Gets the path of a folder containing this item.
   *
   * @param assemblyItem the assembly item whose parent folder will be fetched.
   * @return the folder path of the containing folder.
   * @throws PSErrorResultsException if an error occurs
   * @throws PSExtensionProcessingException if an error occurs
   * @throws PSErrorException if an error occurs
   * @throws PSCmsException if an error occurs
   */
  @IPSJexlMethod(
      description = "get the folder path for this item",
      params = {@IPSJexlParam(name = "assemblyItem", description = "$sys.assemblyItem")},
      returns = "the path of the folder that contains this item")
  /**
   * Returns the parent folder path.
   *
   * @param assemblyItem the assembly item
   * @return the result
   * @throws PSErrorResultsException if an error occurs
   * @throws PSExtensionProcessingException if an error occurs
   * @throws PSErrorException if an error occurs
   * @throws PSCmsException if an error occurs
   */
  public String getParentFolderPath(IPSAssemblyItem assemblyItem)
      throws PSErrorResultsException,
          PSExtensionProcessingException,
          PSErrorException,
          PSCmsException {
    int id = assemblyItem.getFolderId();
    String path = null;
    /*
     * If there is no folder id associated with the assembly item
     * (ie sys_folderid was not passed as a parameter) then we are going
     * to have to lookup the folder using the same process that managed nav
     * does.
     * Unfortunately this process is tightly coupled to Nav so we have
     * to instantiate the PSNavHelper class instead of using a service.
     * TODO Dave should look over this.
     */
    if (id <= 0) {
      log.debug("Assembly Item does not have a folder id.");
      if (assemblyItem instanceof PSAssemblyWorkItem) {
        PSAssemblyWorkItem awi = (PSAssemblyWorkItem) assemblyItem;
        PSNavHelper helper = awi.getNavHelper();
        if (helper != null) {
          log.debug("Using NavHelper to find folder id.");
          String errMesg = "Tried to use NavHelper to get parent folder path but failed!";
          try {
            IPSNode navNode = (IPSNode) helper.findNavNode(assemblyItem);
            if (navNode != null) {
              path = getParentFolderPath(navNode.getGuid());
            } else {
              log.warn(
                  "Tried to use NavHelper to getParentFolderPath "
                      + "but no navon could be found.");
              path = null;
            }
          } catch (PSCmsException e) {
            log.error(errMesg, e);
            throw new RuntimeException(e);
          } catch (PSFilterException e) {
            log.error(errMesg, e);
            throw new RuntimeException(e);
          } catch (RepositoryException e) {
            log.warn("Could not find folder using NavHelper: ", e);
            path = null;
          }
        } else {
          log.debug(
              "Could not use NavHelper to find folderid because the"
                  + " provided assembly item did not have one. (getNavHelper() == null)");
          path = null;
        }
      }
    } else {
      log.debug("Using AssemblyItem's folderid = {}", id);
      path = getFolderPath(id);
      if (path == null) {
        log.debug("Could not get folder path for id {}", id);
      }
    }
    return path;
  }

  @IPSJexlMethod(
      description = "Gets the folder properties of a folder.",
      params = {@IPSJexlParam(name = "path", description = "folder path")},
      returns = "The folder properties (Map)")
  /**
   * Returns the folder properties.
   *
   * @param path the path
   * @return the result
   */
  @SuppressWarnings("unchecked")
  public Map<String, String> getFolderProperties(String path) {
    try {
      PSFolder folder = getContentWs().loadFolders(new String[] {path}).get(0);
      Map<String, String> props = new HashMap<String, String>();
      Iterator<PSFolderProperty> it = folder.getProperties();
      while (it.hasNext()) {
        PSFolderProperty prop = it.next();
        props.put(prop.getName(), prop.getValue());
      }
      return props;
    } catch (PSErrorResultsException e) {
      log.error("Could not get folder properties for: " + path, e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the folder paths Given a FolderID.
   *
   * @param id the id of the folder
   * @return the folder path.
   * @throws PSCmsException if an error occurs
   */
  @IPSJexlMethod(
      description = "Gets the folder path given a Folder ID",
      params = {@IPSJexlParam(name = "id", description = "folder id")},
      returns = "The folder path (String)")
  /**
   * Returns the folder path.
   *
   * @param id the id
   * @return the result
   * @throws PSCmsException if an error occurs
   */
  public String getFolderPath(int id) throws PSCmsException {
    try {
      return PSOItemFolderUtilities.getFolderPath(id);
    } catch (PSNotFoundException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new PSCmsException(e);
    }
  }

  /**
   * Get the folder paths for an item.
   *
   * @param guid the GUID for the item
   * @return the parent folder path. If there are multiple paths, the first one will be returned.
   *     Will be return empty list if the item is not in any folders.
   * @throws PSCmsException if an error occurs
   */
  @IPSJexlMethod(
      description = "get the folder path for this item",
      params = {@IPSJexlParam(name = "itemId", description = "the item GUID")})
  /**
   * Returns the parent folder paths.
   *
   * @param guid the guid
   * @return the result
   * @throws PSCmsException if an error occurs
   */
  public List<String> getParentFolderPaths(IPSGuid guid) throws PSCmsException {
    return PSOItemFolderUtilities.getFolderPathsForItem(guid);
  }

  /***
   * Returns a lightweight list of the child items and folders of this item.
   */
  @IPSJexlMethod(
      description = "get the child folders & items for this item",
      params = {@IPSJexlParam(name = "folderId", description = "the folderid")})
  /**
   * Returns the child folders.
   *
   * @param folderId the folder id
   * @return the result
   * @throws PSCmsException if an error occurs
   * @throws PSErrorException if an error occurs
   */
  public List<PSItemSummary> getChildFolders(int folderId) throws PSCmsException, PSErrorException {

    List<PSItemSummary> ret = new ArrayList<PSItemSummary>();

    try {
      ret =
          contentWs.findFolderChildren(
              guidManager.makeGuid(folderId, PSTypeEnum.LEGACY_CONTENT), false);
    } catch (PSErrorException psex) {
      log.error(psex.getLocalizedMessage());
    } catch (Exception e) {
      log.error(e.getLocalizedMessage());
    }
    return ret;
  }

  /**
   * Get the folder id for a folder path.
   *
   * @param path the path for the item
   * @return content item id for the folder
   * @throws PSCmsException Exception if an error occurred
   */
  @IPSJexlMethod(
      description = "get the folder id for this folder path",
      params = {@IPSJexlParam(name = "path", description = "The path to get the id for")})
  /**
   * Returns the id for path.
   *
   * @param path the path
   * @return the result
   * @throws PSCmsException if an error occurs
   */
  public int getIdForPath(String path) throws PSCmsException {
    PSServerFolderProcessor folderproc = PSServerFolderProcessor.getInstance();
    return folderproc.getIdByPath(path);
  }

  /**
   * Get the folder id for a item id
   *
   * @param itemId the id to find the parent folder id for
   * @return content item id for the folder
   * @throws PSCmsException Exception if an error occurred
   */
  @IPSJexlMethod(
      description = "get the parent folder id for this item id",
      params = {
        @IPSJexlParam(name = "itemId", description = "The item id to find the folder id for")
      })
  /**
   * Returns the parent folder id.
   *
   * @param itemId the item id
   * @return the result
   * @throws PSCmsException if an error occurs
   */
  public int getParentFolderId(int itemId) throws PSCmsException {
    try {
      return PSOItemFolderUtilities.getParentFolderId(itemId);
    } catch (PSNotFoundException e) {
      throw new PSCmsException(e);
    }
  }

  /**
   * init operation.
   *
   * @param def the def
   * @param codeRoot the code root
   * @throws PSExtensionException if an error occurs
   */
  @Override
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
    super.init(def, codeRoot);
    IPSContentWs contentWs = PSContentWsLocator.getContentWebservice();
    IPSGuidManager guidManager = PSGuidManagerLocator.getGuidMgr();
    init(contentWs, guidManager);
  }

  /**
   * Returns the content ws.
   *
   * @return the result
   */
  public IPSContentWs getContentWs() {
    return contentWs;
  }

  /**
   * Sets the content ws.
   *
   * @param contentWs the content ws
   */
  public void setContentWs(IPSContentWs contentWs) {
    this.contentWs = contentWs;
  }

  /**
   * Returns the guid manager.
   *
   * @return the result
   */
  public IPSGuidManager getGuidManager() {
    return guidManager;
  }

  /**
   * Sets the guid manager.
   *
   * @param guidManager the guid manager
   */
  public void setGuidManager(IPSGuidManager guidManager) {
    this.guidManager = guidManager;
  }

  @IPSJexlMethod(
      description = "Add a folder tree for the fully qualified path. ",
      params = {@IPSJexlParam(name = "path", description = "folder path")},
      returns = "The newly added folders")
  /**
   * addFolderTree operation.
   *
   * @param path the path
   * @return the result
   */
  public PSFolder addFolderTree(String path) {
    PSFolder folder = null;
    String[] stringArr = new String[1];
    stringArr[0] = path;
    try {
      folder = contentWs.loadFolders(stringArr).get(0);
    } catch (PSErrorResultsException e1) {
      log.info("Folder does not exist, creating new FolderTree for path: " + path, e1);
      try {
        folder = contentWs.addFolderTree(path).get(0);
      } catch (PSErrorResultsException e) {
        log.error("Could not generate new folder at path: " + path, e);
        throw new RuntimeException(e);
      } catch (PSErrorException e) {
        log.error("Could not generate new folder at path: " + path, e);
        throw new RuntimeException(e);
      }
    }

    return folder;
  }

  @IPSJexlMethod(
      description = "Add a folder below the specified Parent folder",
      params = {
        @IPSJexlParam(name = "folderName", description = "Name of the folder to create"),
        @IPSJexlParam(name = "parent", description = "folder path to create new folder in")
      },
      returns = "The newly added folder")
  /**
   * addFolder operation.
   *
   * @param folderName the folder name
   * @param parent the parent
   * @return the result
   */
  public PSFolder addFolder(String folderName, String parent) {
    PSFolder folder = null;

    try {
      folder = contentWs.addFolder(folderName, parent);
    } catch (PSErrorException e) {
      log.error("Could not generate new folder '" + folderName + "' at path: " + parent, e);
      throw new RuntimeException(e);
    }
    return folder;
  }
}
