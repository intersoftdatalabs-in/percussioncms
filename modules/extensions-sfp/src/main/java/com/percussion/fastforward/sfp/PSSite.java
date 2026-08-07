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
package com.percussion.fastforward.sfp;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.data.PSInternalRequestCallException;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.server.IPSInternalRequest;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.xml.PSXmlTreeWalker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class defines some useful static methods related a Site.
 *
 * @author James Schultz
 */
public class PSSite {

  /** Default constructor for PSSite. */
  public PSSite() {
    // default constructor
  }

  /**
   * Uses the site id parameter to lookup the site folder root path stored in the RXSITES table.
   *
   * @param siteid Id of the site whose site folder root will be returned. if provided null, null
   *     will be returned
   * @param request the current request context, never <code>null</code>
   * @return the site folder root of the supplied site, may be null or empty.
   */
  public static String lookupFolderRootForSite(String siteid, IPSRequestContext request) {
    String folderRoot = null;

    if (siteid != null) {
      // build and execute an interal request
      Map<String, Object> lookupParams = new HashMap<>(1);
      lookupParams.put(IPSHtmlParameters.SYS_SITEID, siteid);
      IPSInternalRequest lookupRequest =
          request.getInternalRequest(LOOKUP_SITE_FOLDER_ROOT, lookupParams, false);
      if (lookupRequest == null) {
        request.printTraceMessage("ERROR: cannot find query resource: " + LOOKUP_SITE_FOLDER_ROOT);
      } else {
        try {
          Document results = lookupRequest.getResultDoc();
          folderRoot = parseFolderRootForSite(results);
        } catch (PSInternalRequestCallException e) {
          request.printTraceMessage(
              "ERROR: while making internal request to " + LOOKUP_SITE_FOLDER_ROOT);
          request.printTraceMessage(e.getMessage());
        }
      }
    }
    return folderRoot;
  }

  /**
   * Parses XML document to extract the site folder root from the following structure:<br>
   *
   * <pre><code>
   * &lt;!ELEMENT lookupSiteFolderRoot (folderPath?)>
   * &lt;!ELEMENT folderPath (#PCDATA)>
   * </code></pre>
   *
   * @param resultXml the XML document to be parsed
   * @return the site folder root, may be null or empty
   */
  public static String parseFolderRootForSite(Document resultXml) {
    String folderRoot = null;
    if (resultXml != null) {
      Element root = resultXml.getDocumentElement();
      if (root != null) {
        NodeList path = root.getElementsByTagName("folderPath");
        Node n = path.item(0);
        if (n != null) folderRoot = PSXmlTreeWalker.getElementData(n);
      }
    }
    return folderRoot;
  }

  /**
   * Gets the published filename for the supplied folder locator. If the folder property named
   * {@link PSFolder#PROPERTY_PUB_FILE_NAME} is present, its value will be used as the file name for
   * this folder. If this property is not defined, the folder name is returned.
   *
   * @param locator the locator for the folder, must not be null.
   * @return the file name for this folder as described above, never null or empty.
   * @throws PSCmsException if an error occurs.
   */
  public static String getFolderFileName(PSLocator locator) throws PSCmsException {
    if (locator == null) {
      throw new IllegalArgumentException("locator must not be null");
    }

    return PSServerFolderProcessor.getInstance().getPubFileName(locator.getId());
  }

  /**
   * Builds the folder path walking backwards from the selected site folder to the site root. An
   * empty list is returned if the locator is not a descendent of the rootLoc.
   *
   * @param rootId the root folder id
   * @param locator the locator of an item or folder under the site folder to build path list. Must
   *     not be null.
   * @param addLocator true if add the locator to the returned path; false don't add the locator to
   *     the returned path. This is because the locator may not be a locator of a folder.
   * @return a list of PSLocator that represent the path. The 2nd element is the sub-folder of the
   *     1st element, the 3nd element is the sub-folder of the 2nd element, and so on and so forth,
   *     the last element is the locator. It may be empty if the locator is not a descendent of
   *     rootLoc. It never null.
   * @throws PSCmsException if an error occurs.
   */
  public static List<PSLocator> buildFolderPathList(
      int rootId, PSLocator locator, boolean addLocator) throws PSCmsException {

    if (rootId <= 0) throw new IllegalArgumentException("rootid must not be > 0");
    if (locator == null) throw new IllegalArgumentException("locator must not be null");

    PSServerFolderProcessor fldProcessor = PSServerFolderProcessor.getInstance();
    List<List<PSLocator>> paths = fldProcessor.getFolderLocatorPaths(locator);

    for (List<PSLocator> onePath : paths) {
      ListIterator<PSLocator> walkPath = onePath.listIterator();
      // collect the locators while walking the path from bottom up
      List<PSLocator> path = new ArrayList<>();
      while (walkPath.hasNext()) {
        PSLocator tmpLoc = walkPath.next();
        if (tmpLoc.getId() == rootId) {
          Collections.reverse(path);
          if (addLocator) path.add(locator);
          return path;
        }

        path.add(tmpLoc);
      }
    }

    return Collections.emptyList();
  }

  /**
   * Renders the site folder path as a String. The path will always begin and end with a {@link
   * #SITE_PATH_SEPARATOR Separator}. If the list of folders is empty, the returned path will
   * consist of a single Separator.
   *
   * @param siteFolderList a list of PSFolders that represent the path, must not be null, may be
   *     empty.
   * @return the site folder path. Never null.
   * @throws PSCmsException if an error occurs.
   * @deprecated use {@link #renderSiteFolderPathLocators(List)} instead.
   */
  @Deprecated
  public static String renderSiteFolderPath(List<PSFolder> siteFolderList) throws PSCmsException {
    StringBuilder path = new StringBuilder();
    for (PSFolder folder : siteFolderList) {
      PSLocator loc = folder.getLocator();
      path.append(SITE_PATH_SEPARATOR);
      path.append(getFolderFileName(loc));
    }
    path.append(SITE_PATH_SEPARATOR);
    return path.toString();
  }

  /**
   * Renders the site folder path as a String. The path will always begin and end with a {@link
   * #SITE_PATH_SEPARATOR Separator}. If the list of folders is empty, the returned path will
   * consist of a single Separator.
   *
   * @param siteFolderList a list of {@link PSLocator} that represent the path, must not be null ,
   *     may be empty. The 2nd element is the sub-folder of the 1st element, the 3nd element is the
   *     sub-folder of the 2nd element, and so on and so forth.
   * @return the site folder path. Never null.
   * @throws PSCmsException if an error occurs.
   */
  public static String renderSiteFolderPathLocators(List<? extends PSLocator> siteFolderList)
      throws PSCmsException {
    if (siteFolderList == null) {
      throw new IllegalArgumentException("siteFolderList must not be null");
    }
    StringBuilder path = new StringBuilder();
    for (PSLocator loc : siteFolderList) {
      path.append(SITE_PATH_SEPARATOR);
      path.append(getFolderFileName(loc));
    }
    path.append(SITE_PATH_SEPARATOR);
    return path.toString();
  }

  /**
   * Name of the Rhythmyx internal resource used to query the site folder root for a given site id.
   */
  private static final String LOOKUP_SITE_FOLDER_ROOT =
      "rx_supportSiteFolderContentList/lookupSiteFolderRoot.xml";

  /**
   * Name of the request private object key that indicates to site folder assembly that the path
   * generation should be suppressed.
   */
  public static final String SUPPRESS_SITE_PATH_KEY = "ff-suppress-site-path-key";

  /** String constant for the key to store the folder path as session object. */
  public static final String SITE_PATH_NAME = "com.percussion.fastforward.sfp.path";

  /** String constant for path separator string used while building location path. */
  public static final String SITE_PATH_SEPARATOR = "/";

  /** Reference to Log4j singleton object used to log any errors or debug info. */
  private static final Logger ms_log = LogManager.getLogger(PSSite.class);
}
