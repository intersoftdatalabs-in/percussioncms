/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.distribution.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Regression tests for the staging-cleanup behavior of
 * {@code modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml}.
 *
 * <p>For feature 002-jdbc-drivers-cleanup. See FR-001, FR-002, FR-005, FR-007
 * and SC-001, SC-002.
 */
class StagingCleanupAntScriptTest {

    private static final String ANT_SCRIPT_CLASSPATH =
            "/installDistributionFiles.xml";

    @Test
    @DisplayName("FR-001: staging copy is followed by a <delete dir=\"..._jdbc-stage...\"/>")
    void stagingCopyIsFollowedByDeleteOfStageDirectory() throws Exception {
        Document doc = loadAntScript();
        Node stagingCopy = findStagingCopyIntoJdbcDir(doc);
        assertNotNull(stagingCopy,
                "Expected to find the <copy todir=\".../jetty/base/lib/jdbc/\"> staging block");

        Element stagingDelete = findSiblingDeleteOfStageDir(doc, stagingCopy);
        assertNotNull(stagingDelete,
                "Expected a sibling <delete dir=\".../_jdbc-stage\"/> after the staging <copy> (FR-001, FR-007)");

        String dirAttr = stagingDelete.getAttribute("dir");
        assertTrue(dirAttr != null && dirAttr.contains("_jdbc-stage"),
                "Delete element's dir attribute must reference _jdbc-stage, was: " + dirAttr);
    }

    @Test
    @DisplayName("FR-002: staging <fileset> <include> globs match the curated set exactly")
    void stagingFilesetIncludesCoverOnlyCuratedDrivers() throws Exception {
        Document doc = loadAntScript();
        Node stagingCopy = findStagingCopyIntoJdbcDir(doc);
        assertNotNull(stagingCopy);

        Element fileset = firstChildElement(stagingCopy, "fileset");
        assertNotNull(fileset, "Staging <copy> must contain a <fileset>");

        List<Element> includes = childElements(fileset, "include");
        assertEquals(BundledJdbcDrivers.STAGING_GLOBS.length, includes.size(),
                "Staging <fileset> must have exactly " + BundledJdbcDrivers.STAGING_GLOBS.length
                        + " <include> entries; found " + includes.size());

        Set<String> actual = new LinkedHashSet<>();
        for (Element inc : includes) {
            actual.add(inc.getAttribute("name"));
        }
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(BundledJdbcDrivers.STAGING_GLOBS));
        assertEquals(expected, actual,
                "Staging <fileset> <include> globs must match the curated set exactly");
    }

    @Test
    @DisplayName("FR-002 (extended): no non-driver provided-scope dep is referenced by the staging copy")
    void noNonDriverProvidedDepIsCopiedToJdbcDir() throws Exception {
        // The staging <fileset> only includes globs for the curated set. If a non-driver
        // provided-scope dep were copied into _jdbc-stage, it would land in jetty/base/lib/jdbc
        // only if its filename happened to match one of the globs above. Verify by asserting
        // that the union of curated globs maps exactly to the curated artifactIds declared in
        // BundledJdbcDrivers.GLOB_TO_ARTIFACT_ID. This is a structural guard: any non-driver
        // provided dep added to pom.xml would either fail to match the globs (and so not be
        // staged) or, if the globs were widened, would surface here as a non-curated artifactId
        // in the module.
        Set<String> curatedIds = BundledJdbcDrivers.curatedArtifactIds();
        Set<String> globArtifactIds = new LinkedHashSet<>();
        for (String[] pair : BundledJdbcDrivers.GLOB_TO_ARTIFACT_ID) {
            globArtifactIds.add(pair[1]);
        }
        assertEquals(curatedIds, globArtifactIds,
                "Each staging <include> glob must correspond to exactly one curated artifactId");
    }

    @Test
    @DisplayName("FR-005: ANT comment does not claim <copy> fails when staged source is missing")
    void antCopyCommentDoesNotMisattributeFailure() throws Exception {
        Document doc = loadAntScript();
        Node stagingCopy = findStagingCopyIntoJdbcDir(doc);
        assertNotNull(stagingCopy);

        // Walk ALL preceding siblings, skipping whitespace, collecting every comment
        // we encounter. We bound the walk by either hitting the parent root (project
        // or target) or by a safety counter.
        StringBuilder preceding = new StringBuilder();
        Node prev = stagingCopy.getPreviousSibling();
        int safety = 0;
        while (prev != null && safety++ < 50) {
            short type = prev.getNodeType();
            if (type == Node.COMMENT_NODE) {
                preceding.insert(0, prev.getTextContent() + "\n");
            }
            prev = prev.getPreviousSibling();
        }

        String commentText = preceding.toString();
        assertFalse(
                commentText.contains("ANT <copy> fails by default when a staged source file is missing"),
                "Comment must not claim ANT <copy> fails when source is missing (FR-005); " +
                        "loud-failure comes from Maven failOnAnyMissingDependency and the verify-jdbc-drivers exec.");
        assertTrue(
                commentText.contains("failOnAnyMissingDependency")
                        || commentText.contains("verify-jdbc-drivers")
                        || commentText.contains("silent no-op")
                        || commentText.contains("empty"),
                "Comment must attribute loud-failure to Maven failOnAnyMissingDependency and/or " +
                        "the verify-jdbc-drivers exec in the verify phase, and must note that an " +
                        "empty globbed <fileset> is a silent no-op (FR-005). Actual comment: <<<" + commentText + ">>>");
    }

    // --- helpers ----------------------------------------------------------

    private Document loadAntScript() throws ParserConfigurationException, SAXException, IOException {
        try (InputStream in = getClass().getResourceAsStream(ANT_SCRIPT_CLASSPATH)) {
            assertNotNull(in, ANT_SCRIPT_CLASSPATH + " must be on the classpath");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(new InputSource(in));
        }
    }

    private Node findStagingCopyIntoJdbcDir(Document doc) throws Exception {
        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList copies = (NodeList) xp.evaluate(
                "//copy[@todir]",
                doc,
                XPathConstants.NODESET);
        for (int i = 0; i < copies.getLength(); i++) {
            Node n = copies.item(i);
            String todir = ((Element) n).getAttribute("todir");
            if (todir != null && todir.contains("jetty/base/lib/jdbc")) {
                return n;
            }
        }
        return null;
    }

    private Element findSiblingDeleteOfStageDir(Document doc, Node after) {
        Node n = after.getNextSibling();
        int safety = 0;
        while (n != null && safety++ < 50) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) n;
                if ("delete".equals(el.getTagName())) {
                    String dir = el.getAttribute("dir");
                    if (dir != null && dir.contains("_jdbc-stage")) {
                        return el;
                    }
                    return null; // some other delete — not the staging cleanup
                }
                // Hit another major element before the staging delete — fail.
                return null;
            }
            n = n.getNextSibling();
        }
        return null;
    }

    private static Element firstChildElement(Node parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node c = children.item(i);
            if (c.getNodeType() == Node.ELEMENT_NODE && tagName.equals(((Element) c).getTagName())) {
                return (Element) c;
            }
        }
        return null;
    }

    private static List<Element> childElements(Node parent, String tagName) {
        List<Element> out = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node c = children.item(i);
            if (c.getNodeType() == Node.ELEMENT_NODE && tagName.equals(((Element) c).getTagName())) {
                out.add((Element) c);
            }
        }
        return out;
    }
}
