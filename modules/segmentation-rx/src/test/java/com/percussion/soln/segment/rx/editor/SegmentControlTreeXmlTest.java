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

package com.percussion.soln.segment.rx.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.custommonkey.xmlunit.XMLAssert.*;
import static com.percussion.soln.segment.rx.editor.XMLTestHelper.*;

import org.custommonkey.xmlunit.XMLUnit;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.percussion.soln.segment.ISegmentNode;
import com.percussion.soln.segment.ISegmentTree;
import com.percussion.soln.segment.rx.editor.SegmentControlTreeXml;

public class SegmentControlTreeXmlTest {
    SegmentControlTreeXml segTreeXml;
    Mockery context;
    SegmentMocks segMocks;
    
    @BeforeAll
    public static void setUpXML() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        context = new JUnit4Mockery();
        segTreeXml = new SegmentControlTreeXml();
        segMocks = new SegmentMocks(context);
    }
    
    @Test
    public void shouldReturnBlankXml() throws Exception {
        ISegmentNode root = null;
        ISegmentTree tree = segMocks.makeTreeStub(root);
        Document doc = segTreeXml.segmentTreeToXml(tree);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", xmlToString(doc).trim() );
    }
    
    @Test
    public void shouldReturnJustTreeElement() throws Exception {
        ISegmentNode root = segMocks.makeRootSegmentStub("root");
        segMocks.noChildren(root);
        ISegmentTree tree = segMocks.makeTreeStub(root);
        Document doc = segTreeXml.segmentTreeToXml(tree);
        assertEquals(normalizeXml("<tree label=\"root\"/>"), normalizeXml(xmlToString(doc)));
    }
    
    @Test
    public void shouldReturnJustTreeElementWithDoubleSlashRootLabel() throws Exception {
        ISegmentNode root = segMocks.makeRootSegmentStub("//");
        segMocks.noChildren(root);
        ISegmentTree tree = segMocks.makeTreeStub(root);
        Document doc = segTreeXml.segmentTreeToXml(tree);
        assertEquals(normalizeXml("<tree label=\"//\"/>"), normalizeXml(xmlToString(doc)));
    }

    @Test
    public void shouldReturnTreeElementWithTwoChildNodes() throws Exception {
        ISegmentNode root = segMocks.makeRootSegmentStub("root");
        ISegmentTree tree = segMocks.makeTreeStub(root);
        ISegmentNode a = segMocks.makeSegmentStub(2, "a", "//rootf/af", false);
        ISegmentNode b = segMocks.makeSegmentStub(3, "b", "//rootf/bf", true);
        segMocks.noChildren(a); segMocks.noChildren(b);
        segMocks.addChildren(root, a,b);
        String expected = "<tree label=\"root\">" +
                            "<node id=\"2\" label=\"a\" selectable=\"no\"/>" +
                            "<node id=\"3\" label=\"b\" selectable=\"yes\"/>" +
                          "</tree>";
        Document doc = segTreeXml.segmentTreeToXml(tree);
        assertEquals(normalizeXml(expected), normalizeXml(xmlToString(doc)));
    }

    @Test
    public void shouldEscapeLabels() throws Exception {
        ISegmentNode root = segMocks.makeRootSegmentStub("root");
        ISegmentTree tree = segMocks.makeTreeStub(root);
        ISegmentNode a = segMocks.makeSegmentStub(2, "a & a", "//rootf/af", false);
        ISegmentNode b = segMocks.makeSegmentStub(3, "b ' b", "//rootf/bf", true);
        segMocks.noChildren(a); segMocks.noChildren(b);
        segMocks.addChildren(root, a,b);
        Document doc = segTreeXml.segmentTreeToXml(tree);

        // Semantic contract: DOM attribute values (independent of serializer entity style).
        NodeList nodes = doc.getDocumentElement().getElementsByTagName("node");
        assertEquals(2, nodes.getLength());
        assertEquals("a & a", ((Element) nodes.item(0)).getAttribute("label"));
        assertEquals("b ' b", ((Element) nodes.item(1)).getAttribute("label"));

        // Serialized form: '&' must be &amp;. Apostrophe may be literal in double-quoted
        // attribute values per XML 1.0 5th ed. §2.3 / §2.4 (AttValue); JDK 21 Xerces does so.
        String expected = "<tree label=\"root\">" +
                            "<node id=\"2\" label=\"a &amp; a\" selectable=\"no\"/>" +
                            "<node id=\"3\" label=\"b ' b\" selectable=\"yes\"/>" +
                          "</tree>";
        assertEquals(normalizeXml(expected), normalizeXml(xmlToString(doc)));
    }
    
    @Test
    public void shouldReturnWithThreeLevelsDeep() throws Exception {
        ISegmentNode root = segMocks.makeRootSegmentStub("root");
        ISegmentTree tree = segMocks.makeTreeStub(root);
        ISegmentNode a = segMocks.makeSegmentStub(2, "a", "//rootf/af", false);
        ISegmentNode b = segMocks.makeSegmentStub(3, "b", "//rootf/af/bf", true);
        ISegmentNode c = segMocks.makeSegmentStub(4, "c", "//rootf/af/bf/cf", true);
        segMocks.addChildren(root, a);
        segMocks.addChildren(a, b);
        segMocks.addChildren(b, c);
        segMocks.noChildren(c);
        Document doc = segTreeXml.segmentTreeToXml(tree);
        String expected = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            +"<tree label=\"root\">"
            +"<node id=\"2\" label=\"a\" selectable=\"no\">"
              +"<node id=\"3\" label=\"b\" selectable=\"yes\">"
                +"<node id=\"4\" label=\"c\" selectable=\"yes\"/>"
              +"</node>"
            +"</node>"
          +"</tree>";
        assertEquals(normalizeXml(expected), normalizeXml(xmlToString(doc)));
    }
}
