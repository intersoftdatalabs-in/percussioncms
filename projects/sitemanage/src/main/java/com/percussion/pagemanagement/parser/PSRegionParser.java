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
package com.percussion.pagemanagement.parser;

import static org.apache.commons.lang.Validate.notNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.Tag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.percussion.pagemanagement.data.PSAbstractRegion;
import com.percussion.pagemanagement.data.PSRegionCode;
import com.percussion.pagemanagement.parser.IPSRegionParser.IPSRegionParserRegionFactory;

/**
 * Walks the HTML tree, pulling out regions and code, transforming them into our Abstract Syntax Tree: {@link PSParsedRegionTree}.
 * <p>
 * A region is an HTML element that uses the {@link #REGION_CLASS}. Everything else is HTML/velocity code.
 * Implementation is a simple stack-based FSM parser.
 *
 * @param <REGION> region type
 * @param <CODE> code type
 * @author adamgent, Sunny Sal
 */
public class PSRegionParser<REGION extends PSAbstractRegion, CODE extends PSRegionCode> {

    private static final Logger LOG = LogManager.getLogger(PSRegionParser.class);
    private static final String REGION_CLASS = "perc-region";
    private static final String REGION_ID_ATTR = "id";
    private static final String CLASS_ATTR = "class";

    private final IPSRegionParserRegionFactory<REGION, CODE> regionFactory;

    public PSRegionParser(IPSRegionParserRegionFactory<REGION, CODE> regionFactory) {
        this.regionFactory = regionFactory;
    }

    private class RegionToken {
        REGION region;
        Element element;

        RegionToken(Element element, REGION region) {
            this.element = element;
            this.region = region;
        }
    }

    /**
     * Parses HTML to turn into an Abstract Syntax Tree.
     * @param text valid HTML, never null or empty.
     * @return an AST of regions and code.
     */
    public PSParsedRegionTree<REGION, CODE> parse(String text) {
        var src = new Source(text);
        var it = src.iterator();
        var regionStack = new Stack<RegionToken>();
        var tree = new PSParsedRegionTree<REGION, CODE>(regionFactory);
        var root = tree.getRootNode();
        var rootToken = new RegionToken(null, root);
        regionStack.push(rootToken);
        boolean inCode = false;
        CODE code = null;
        while (it.hasNext()) {
            var element = regionStack.peek().element;
            var current = regionStack.peek().region;
            var seg = it.next();
            if (current.getChildren() == null) {
                current.setChildren(new ArrayList<>());
            }
            if (isRegionStart(seg)) {
                inCode = false;
                var st = (StartTag) seg;
                var e = st.getElement();
                var r = createRegion(e);
                current.getChildren().add(r);
                // Use internal mutable map for population
                tree.getMutableRegions().put(r.getRegionId(), r);
                var rt = new RegionToken(e, r);
                if (e.getEndTag() != null) {
                    regionStack.push(rt);
                }
            } else if (element != null && seg.equals(element.getEndTag())) {
                inCode = false;
                regionStack.pop();
            } else {
                if (inCode) {
                    var html = code.getTemplateCode();
                    html = html == null ? "" : html;
                    code.setTemplateCode(html + seg.toString());
                } else {
                    code = regionFactory.createRegionCode();
                    code.setTemplateCode(seg.toString());
                    current.getChildren().add(code);
                    inCode = true;
                }
            }
        }
        return tree;
    }

    private boolean isRegionStart(Segment seg) {
        if (seg instanceof StartTag) {
            var st = (StartTag) seg;
            var divClass = st.getAttributeValue(CLASS_ATTR);
            return divClass != null && divClass.contains(REGION_CLASS);
        }
        return false;
    }

    private REGION createRegion(Element elem) {
        if (elem == null) {
            throw new IllegalArgumentException("elem may not be null");
        }
        var regionId = elem.getAttributeValue(REGION_ID_ATTR);
        notNull(regionId);
        var region = regionFactory.createRegion(regionId);
        region.setRegionId(regionId);
        region.setStartTag(elem.getStartTag().toString());
        Tag end = elem.getEndTag();
        if (end != null) {
            region.setEndTag(end.toString());
        }
        return region;
    }
}
