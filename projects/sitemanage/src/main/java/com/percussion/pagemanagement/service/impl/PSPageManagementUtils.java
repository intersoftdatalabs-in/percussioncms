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

// REFACTORED: CP-JAVA11
package com.percussion.pagemanagement.service.impl;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import com.percussion.sitemanage.importer.helpers.IPSImportHelper;
import com.percussion.sitemanage.importer.utils.PSManagedTagsUtils;
import com.percussion.utils.types.PSPair;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PSPageManagementUtils {
    private static final String CONTENT_ATTR_NAME = "content";
    private static final String NAME_SEPARATOR = "-";
    public static final String TEMPLATE_NAME = "Template";
    public static final String PAGE_NAME = "Page";
    public static final String UNASSIGNED_WIDGET_NAME = "perc-unassigned.widget";

    /**
     * Takes name and appends count at the end if necessary. Uses a name separator.
     */
    public static String getNameForCount(String name, int count) {
        var nameForCount = name;
        if (count != 0) {
            nameForCount += NAME_SEPARATOR + count;
        }
        return nameForCount;
    }

    /**
     * Create a Raw HTML widget item, and its corresponding asset, with the given widget slot id (or widget id).
     */
    public static PSWidgetItem createRawHtmlWidgetItem(String slotid) {
        var widget = new PSWidgetItem();
        widget.setDefinitionId("percRawHtml");
        widget.setName(PSPageManagementUtils.UNASSIGNED_WIDGET_NAME);
        widget.setId(slotid);
        return widget;
    }

    /**
     * Extracts scripts after body start and before body end from the document body in memory and sets them to afterBodyStart and beforeBodyClose in pageContent.
     * Removes title from the head of imported document in memory and updates headContent in pageContent.
     */
    public static void extractMetadata(PSPageContent pageContent, IPSSiteImportLogger logger) {
        var headContent = commentOutManagedTags(pageContent, logger);
        pageContent.setHeadContent(StringEscapeUtils.unescapeHtml4(headContent));

        var docBody = pageContent.getSourceDocument().body();
        var bodyElems = docBody.children();

        var afterBodyStart = extractAfterBodyStartContent(bodyElems, logger);
        var beforeBodyClose = extractBeforeBodyClose(bodyElems, logger);

        pageContent.setAfterBodyStart(StringEscapeUtils.unescapeHtml4(afterBodyStart.toString()));
        pageContent.setBeforeBodyClose(StringEscapeUtils.unescapeHtml4(beforeBodyClose.toString()));
        pageContent.setBodyContent(StringEscapeUtils.unescapeHtml4(docBody.html()));
    }

    private static StringBuilder extractBeforeBodyClose(Elements bodyElems, IPSSiteImportLogger logger) {
        var beforeBodyCloseElems = new Elements();
        for (int i = bodyElems.size(); i > 0; i--) {
            var element = bodyElems.get(i - 1);
            if (!element.tagName().equalsIgnoreCase("script"))
                break;
            beforeBodyCloseElems.add(element);
        }

        var beforeBodyClose = new StringBuilder();
        for (int j = beforeBodyCloseElems.size(); j > 0; j--) {
            var element = beforeBodyCloseElems.get(j - 1);

            if (PSManagedTagsUtils.isManagedJSReference(element)) {
                beforeBodyClose.append(PSManagedTagsUtils.commentTagText(element.outerHtml()));
                logger.appendLogMessage(PSLogEntryType.STATUS, IPSImportHelper.COMMENTED_JS_REFERENCE_FROM_BODY, element.toString());
            } else {
                beforeBodyClose.append(element.outerHtml());
            }
            element.remove();
        }
        return beforeBodyClose;
    }

    private static StringBuilder extractAfterBodyStartContent(Elements bodyElems, IPSSiteImportLogger logger) {
        var afterBodyStart = new StringBuilder();
        for (var element : bodyElems) {
            if (!element.tagName().equalsIgnoreCase("script"))
                break;

            if (PSManagedTagsUtils.isManagedJSReference(element)) {
                afterBodyStart.append(PSManagedTagsUtils.commentTagText(element.outerHtml()));
                logger.appendLogMessage(PSLogEntryType.STATUS, IPSImportHelper.COMMENTED_JS_REFERENCE_FROM_BODY, element.toString());
            } else {
                afterBodyStart.append(element.outerHtml());
            }
            element.remove();
        }
        return afterBodyStart;
    }

    private static String commentOutManagedTags(PSPageContent pageContent, IPSSiteImportLogger logger) {
        var docHead = pageContent.getSourceDocument().head();

        // first comment the title tag
        for (var title : docHead.select("title")) {
            logger.appendLogMessage(PSLogEntryType.STATUS, IPSImportHelper.COMMENTED_OUT_ELEMENT, title.toString());
            PSManagedTagsUtils.commentTag(docHead, title);
        }

        commentMetadataTags(pageContent, docHead, logger);
        commentManagedJSReferences(docHead, logger);
        return docHead.html();
    }

    private static void commentManagedJSReferences(Element element, IPSSiteImportLogger logger) {
        var scriptTags = element.select("script");
        for (var scriptTag : scriptTags) {
            if (PSManagedTagsUtils.isManagedJSReference(scriptTag)) {
                logger.appendLogMessage(PSLogEntryType.STATUS, IPSImportHelper.COMMENTED_JS_REFERENCE_FROM_HEAD, scriptTag.toString());
                PSManagedTagsUtils.commentTag(element, scriptTag);
            }
        }
    }

    private static void commentMetadataTags(PSPageContent pageContent, Element docHead, IPSSiteImportLogger logger) {
        var metaTags = docHead.select("meta");
        for (var metaTag : metaTags) {
            if (PSManagedTagsUtils.isManagedMetadataTag(metaTag)) {
                if (PSManagedTagsUtils.isDescriptionMetaTag(metaTag))
                    pageContent.setDescription(metaTag.attr(CONTENT_ATTR_NAME));

                logger.appendLogMessage(PSLogEntryType.STATUS, IPSImportHelper.COMMENTED_OUT_ELEMENT, metaTag.toString());
                PSManagedTagsUtils.commentTag(docHead, metaTag);
            }
        }
    }
}
