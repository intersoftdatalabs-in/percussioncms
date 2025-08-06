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
package com.percussion.webui.gadget.servlets;

import com.percussion.error.PSExceptionUtils;
import com.percussion.server.PSServer;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * This servlet is used to retrieve a listing of gadgets from the gadget repository.  For each gadget, the following
 * information returned will be returned: name, description, url, icon url.
 */
public class GadgetRepositoryListingServlet extends HttpServlet {
    private static final Logger log = LogManager.getLogger(GadgetRepositoryListingServlet.class);

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(
     *    javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        var type = req.getParameter("type");
        if (type == null || type.isBlank()) type = "All";
        resp.setContentType("application/json");
        try (var out = resp.getWriter()) {
            var gadgets = new JSONArray();
            var root = new File(gadgetsRoot.getPath());
            var gadgetFiles = root.listFiles();
            if (gadgetFiles != null) {
                for (var gadgetFile : gadgetFiles) {
                    if (!gadgetFile.isDirectory()) continue;
                    JSONObject gadget = null;
                    var gadgetConfigFiles = gadgetFile.listFiles();
                    for (var gadgetConfigFile : gadgetConfigFiles) {
                        if (gadgetConfigFile.isDirectory()) continue;
                        if (gadgetConfigFile.getName().endsWith(".xml")) {
                            gadget = loadGadget(gadgetConfigFile);
                            if (gadget != null) break;
                        }
                    }
                    if (gadget != null && (type.equalsIgnoreCase("All") || type.equalsIgnoreCase(gadget.get("type").toString()))) {
                        gadgets.add(gadget);
                    }
                }
            }
            Collections.sort(gadgets, gComp);
            var gadgetListing = new JSONObject();
            gadgetListing.put("Gadget", gadgets);
            out.println(gadgetListing.toString());
        } catch (IOException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            resp.setStatus(500);
        }
    }

    /**
     * Loads a gadget from the specified configuration file.
     *
     * @param config the gadget configuration file, assumed not <code>null</code>.
     *
     * @return the gadget as a <code>JSONObject</code> object.  May be <code>null</code> if the gadget could not be
     * loaded.
     *
     * <p>
     * The format of the returned object is as follows:
     * <p>
     * {"name":"The gadget name",
     *  "description":"The gadget description.",
     *  "url":"/cm/gadgets/repository/MyGadget/MyGadget.xml",
     *  "iconUrl":"/cm/gadgets/repository/MyGadget/images/MyGadgetIcon.png"}
     */
    @SuppressWarnings("unchecked")
    private JSONObject loadGadget(File config) {
        JSONObject gadget = null;
        try (var fin = new FileInputStream(config)) {
            var doc = PSXmlDocumentBuilder.createXmlDocument(fin, false);
            var modulePrefs = doc.getElementsByTagName("ModulePrefs");
            if (modulePrefs.getLength() > 0) {
                var modulePref = (Element) modulePrefs.item(0);
                gadget = new JSONObject();
                gadget.put("name", modulePref.getAttribute("title"));
                gadget.put("type", getGadgetType(modulePref.getAttribute("title")));
                gadget.put("category", modulePref.getAttribute("category"));
                var adminOnly = modulePref.getAttribute("adminOnly");
                gadget.put("adminOnly", adminOnly != null && (adminOnly.equalsIgnoreCase("true") || adminOnly.equalsIgnoreCase("yes")));
                gadget.put("description", modulePref.getAttribute("description"));
                var path = config.getCanonicalPath().replace("\\", "/");
                var absRootPath = gadgetsRoot.getCanonicalPath().replace("\\", "/");
                var url = path.replace(absRootPath + "/", "");
                gadget.put("url", GADGETS_BASE_URL + url);
                var configParentPath = config.getParentFile().getCanonicalPath().replace("\\", "/");
                var iconBaseUrl = configParentPath.replace(absRootPath + "/", "");
                gadget.put("iconUrl", GADGETS_BASE_URL + iconBaseUrl + '/' + modulePref.getAttribute("thumbnail"));
            }
        } catch (IOException | SAXException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return gadget;
    }

    /**
     * Used for sorting json representations of gadgets.  Gadgets will be sorted alphabetically by name, case-sensitive.
     * It is assumed that each json respresentation will have a name field.
     */
    public static class GadgetComparator implements Comparator<JSONObject> {
        @Override
        public int compare(JSONObject obj1, JSONObject obj2) {
            return ((String) obj1.get("name")).compareTo((String) obj2.get("name"));
        }
    }

    /**
     * Helper method to get the gadget type for the supplied gadget name. If the
     * gadgetTypeMap is <code>null</code>, then initializes it by loading
     * GadgetRegistry.xml. If the supplied gadget is not a registered gadget
     * then returns the type as "Custom".
     *
     * @param gadgetName The name of the gadget for which the type needs to be
     *            found, assumed not blank.
     * @return The gadget type, never <code>null</code>, will be "Custom" if the
     *         gadget is not found in the registry.
     */
    private String getGadgetType(String gadgetName) {
        if (gadgetTypeMap == null) gadgetTypeMap = loadGadgetTypeMap();
        var gadgetType = gadgetTypeMap.get(gadgetName);
        if (gadgetType == null) gadgetType = "Custom";
        return gadgetType;
    }

    /**
     * Helper method that loads the GadgetRegistry.xml and creates a map of gadget name as key and
     * gadget type as value.
     * @return Map of gadget name and type, never <code>null</code> may be empty.
     */
    protected Map<String, String> loadGadgetTypeMap() {
        var gadTypeMap = new HashMap<String, String>();
        try (var in = this.getClass().getClassLoader().getResourceAsStream("com/percussion/webui/gadget/servlets/GadgetRegistry.xml")) {
            if (in == null) {
                log.error("Gadget registry file is missing from WEB-INF/classes/{}", "com/percussion/webui/gadget/servlets/GadgetRegistry.xml");
                return gadTypeMap;
            }
            var doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
            var groupElems = doc.getElementsByTagName("group");
            for (int i = 0; i < groupElems.getLength(); i++) {
                var groupElem = (Element) groupElems.item(i);
                var groupName = groupElem.getAttribute("name");
                var gadgetElems = groupElem.getElementsByTagName("gadget");
                for (int j = 0; j < gadgetElems.getLength(); j++) {
                    var gadgetElem = (Element) gadgetElems.item(j);
                    var gdgName = gadgetElem.getAttribute("name");
                    gadTypeMap.put(gdgName, groupName);
                }
            }
        } catch (IOException | SAXException e) {
            log.error("Failed to load gadget registry file : {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return gadTypeMap;
    }

    /**
     * The base url for all gadgets.
     */
    private static final String GADGETS_BASE_URL = "/cm/gadgets/repository/";

    /**
     * Used for sorting gadgets.
     */
    private final GadgetComparator gComp = new GadgetComparator();

    /**
     * The root directory of all gadgets (i.e., the gadget repository).  Never <code>null</code>.
     */
    private final File gadgetsRoot = new File(PSServer.getRxDir() + "/cm/gadgets/repository");

    //Private data variable initialized in getGadgetType method.
    private Map<String,String> gadgetTypeMap = null;
}
