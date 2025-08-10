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
package com.percussion.inlinelinkconverter;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSDbComponent;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSRelationshipProcessorProxy;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Extended inline link converter that can filter content IDs based on site root folder.
 * This class extends PSInlineLinkConverter to provide additional functionality for
 * filtering content items that belong to a specific site folder hierarchy.
 *
 * @author Percussion Software
 * @since 1.0
 */
public class PSInlineLinkClearAttribs extends PSInlineLinkConverter {

    private static final Logger log = LogManager.getLogger(PSInlineLinkClearAttribs.class);

    /**
     * Constructs a new PSInlineLinkClearAttribs instance with the specified properties and XSL document.
     *
     * @param props the properties of the conversion, must not be null
     * @param xslDoc the XSL document for content transformation, must not be null
     * @throws IllegalArgumentException if props or xslDoc is null
     */
    public PSInlineLinkClearAttribs(Properties props, Document xslDoc) {
        super(props, xslDoc);
    }

    /**
     * Retrieves content IDs for the specified content type, optionally filtered by site root.
     * If a site root is configured in properties, only content items within that folder
     * hierarchy will be returned.
     *
     * @param contentType the content type to retrieve IDs for, must not be null or empty
     * @return list of ContentKey objects representing the filtered content IDs
     * @throws PSCmsException if an error occurs during content retrieval
     */
    @Override
    @SuppressWarnings("unchecked")
    protected List<ContentKey> getContentIds(String contentType) throws PSCmsException {
        var siteRoot = Optional.ofNullable(m_props.getProperty("siteRoot"))
                .filter(root -> !root.trim().isEmpty());

        if (siteRoot.isEmpty()) {
            return (List<ContentKey>) super.getContentIds(contentType);
        }

        var resultList = new ArrayList<ContentKey>();

        try {
            var relProxy = getRemoteRelationshipProxy();
            var folderType = PSDbComponent.getComponentType(PSFolder.class);
            List<ContentKey> cidList = (List<ContentKey>) super.getContentIds(contentType);

            for (var contentKey : cidList) {
                if (isContentInSiteRoot(contentKey, relProxy, folderType, siteRoot.get())) {
                    resultList.add(contentKey);
                }
            }
        } catch (Exception e) {
            log.error("Error filtering content IDs by site root: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug("Full exception details: ", e);
        }

        return resultList;
    }

    /**
     * Checks if the given content item belongs to the specified site root folder hierarchy.
     *
     * @param contentKey the content key to check
     * @param relProxy the relationship processor proxy
     * @param folderType the folder type string
     * @param siteRoot the site root path to match against
     * @return true if the content is within the site root, false otherwise
     * @throws Exception if an error occurs during path resolution
     */
    private boolean isContentInSiteRoot(ContentKey contentKey, PSRelationshipProcessorProxy relProxy,
            String folderType, String siteRoot) throws Exception {

        var cid = contentKey.getContentId();
        var rev = contentKey.getRevision();
        var loc = new PSLocator(Integer.parseInt(cid), Integer.parseInt(rev));

        var paths = relProxy.getRelationshipOwnerPaths(folderType, loc,
                PSRelationshipConfig.TYPE_FOLDER_CONTENT);

        return Arrays.stream(paths)
                .anyMatch(path -> path.startsWith(siteRoot));
    }

    /**
     * Creates and returns a relationship processor proxy for remote operations.
     *
     * @return the remote relationship processor proxy, never null
     * @throws Exception if unable to create the proxy
     */
    private PSRelationshipProcessorProxy getRemoteRelationshipProxy() throws Exception {
        var requester = m_rtAgent.getRemoteRequester();
        return new PSRelationshipProcessorProxy(
                PSRelationshipProcessorProxy.PROCTYPE_REMOTE, requester);
    }

    /**
     * Main entry point for the inline link clear attributes tool.
     * Loads configuration and starts the conversion process.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            var propsFile = "InlineLinkClearAttribs.properties";
            var xslFile = "InlineLinkClearAttribs.xsl";

            var props = loadProperties(propsFile);
            var xslDoc = loadXslDocument(xslFile);

            var converter = new PSInlineLinkClearAttribs(props, xslDoc);
            converter.doConvert();

        } catch (Exception e) {
            log.error("Failed to run inline link clear attributes conversion: {}",
                    PSExceptionUtils.getMessageForLog(e));
            log.debug("Full exception details: ", e);
            System.err.println("Conversion failed: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Loads properties from the specified file.
     *
     * @param filename the properties file name
     * @return loaded properties
     * @throws IOException if file cannot be read
     */
    private static Properties loadProperties(String filename) throws IOException {
        var props = new Properties();
        try (var fis = new FileInputStream(filename)) {
            props.load(fis);
        }
        return props;
    }

    /**
     * Loads XSL document from the specified file.
     *
     * @param filename the XSL file name
     * @return loaded XML document
     * @throws Exception if file cannot be parsed
     */
    private static Document loadXslDocument(String filename) throws Exception {
        try (var fis = new FileInputStream(filename)) {
            return PSXmlDocumentBuilder.createXmlDocument(fis, false);
        }
    }
}
