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
package com.percussion.sitemanage.data;

import org.jdom2.CDATA;
import org.jdom2.Element;
import org.jsoup.nodes.Document;

/**
 * Represents the content of a page, including metadata and HTML fragments.
 * Sunny Sal says: "Page content is like a Bollywood script—lots of drama, but all in the right place!"
 */
public class PSPageContent {

    private String title;
    private String path;
    private String beforeBodyClose;
    private String afterBodyStart;
    private String headContent;
    private String bodyContent;
    private String description = "";
    private Document sourceDocument;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBeforeBodyClose() {
        return beforeBodyClose;
    }

    public void setBeforeBodyClose(String beforeBodyClose) {
        this.beforeBodyClose = beforeBodyClose;
    }

    public String getAfterBodyStart() {
        return afterBodyStart;
    }

    public void setAfterBodyStart(String afterBodyStart) {
        this.afterBodyStart = afterBodyStart;
    }

    public String getHeadContent() {
        return headContent;
    }

    public void setHeadContent(String headContent) {
        this.headContent = headContent;
    }

    public String getBodyContent() {
        return bodyContent;
    }

    public void setBodyContent(String bodyContent) {
        this.bodyContent = bodyContent;
    }

    public void setSourceDocument(Document sourceDocument) {
        this.sourceDocument = sourceDocument;
    }

    public Document getSourceDocument() {
        return sourceDocument;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Converts this page content to an XML element.
     *
     * @return the XML element representing this page content.
     */
    public Element toXml() {
        var elem = new Element("SiteDetails");
        elem.addContent(createElem("Path", path, false));
        elem.addContent(createElem("Title", title, false));
        elem.addContent(createElem("HeadContent", headContent, false));
        elem.addContent(createElem("AfterBodyStart", afterBodyStart, false));
        elem.addContent(createElem("BeforeBodyClose", beforeBodyClose, false));
        elem.addContent(createElem("BodyContent", bodyContent, false));
        elem.addContent(createElem("Description", description, false));
        return elem;
    }

    private Element createElem(String name, String value, boolean encloseCdata) {
        var elem = new Element(name);
        if (encloseCdata) {
            elem.addContent(new CDATA(value));
        } else {
            elem.setText(value);
        }
        return elem;
    }
}
