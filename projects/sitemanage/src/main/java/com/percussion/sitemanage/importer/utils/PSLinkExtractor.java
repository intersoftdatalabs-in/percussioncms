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
package com.percussion.sitemanage.importer.utils;

import com.percussion.queue.impl.PSSiteQueue;
import com.percussion.services.assembly.impl.PSReplacementFilter;
import com.percussion.sitemanage.dao.impl.PSSiteContentDao;
import com.percussion.sitemanage.data.PSSiteImportConfiguration;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import com.percussion.sitemanage.importer.PSLink;
import com.percussion.sitemanage.importer.PSSiteImporter;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for extracting and processing links and images from HTML documents.
 * Provides methods to extract anchor and image links, generate relative paths, and handle link text.
 */
public final class PSLinkExtractor {

    private static final String DOUBLE_SLASH = "//";
    private static final String DASH = "-";
    private static final String BACK_SLASH = "\\;
    private static final String SLASH = "/";
    private static final String EMPTY = "";
    private static final String QUESTION_MARK = "?";
    private static final String PERIOD = ".";
    private static final String UNKNOWN = "unknown";
    private static final String ABS_HREF = "abs:href";
    public static final String A_HREF = "a[href]";
    public static final String IMG_SOURCE = "img[src]";
    public static final String HREF = "href";
    public static final String SRC = "src";
    public static final String QUERY_STRING_LINK_TEXT_TOKEN = "{{{{{{{PERCUSSION|QUERY|STRING|TOKEN}}}}}}}";
    private static final String QUERY_STRING_PAGE_NAME = "/item-";

    private PSLinkExtractor() {
        // Utility class; prevent instantiation.
    }

    /**
     * Gets the redirected URL for a given site URL.
     *
     * @param siteUrl   The site URL.
     * @param logger    Logger for logging.
     * @param userAgent User agent string.
     * @return The redirected URL, or the original if redirection fails.
     */
    protected String getRedirectedURL(String siteUrl, IPSSiteImportLogger logger, String userAgent) {
        var urlReturn = siteUrl;
        try {
            urlReturn = PSSiteImporter.getRedirectedUrl(siteUrl, logger, userAgent);
        } catch (Exception e) {
            urlReturn = siteUrl;
        }
        return urlReturn;
    }

    /**
     * Gets a list of PSLink objects for anchor tags in a given Document.
     *
     * @param doc      The HTML document.
     * @param log      Logger for logging.
     * @param siteQueue Site queue for caching.
     * @param siteUrl  The site URL.
     * @param config   Import configuration.
     * @return List of PSLink objects.
     */
    public static List<PSLink> getLinksForDocument(final Document doc, final IPSSiteImportLogger log,
                                                   PSSiteQueue siteQueue, String siteUrl, PSSiteImportConfiguration config) {
        var outList = new ArrayList<PSLink>();
        var queryParameter = config.getMapQueryParamToPageName();
        var uniqueListOfLinks = new ArrayList<String>();
        boolean linkCheckPassed = true;
        String paramValue = null;
        final Elements links = doc.select(A_HREF);

        for (var link : links) {
            if ((!removeTrailingSlash(link.attr(ABS_HREF)).equals(getRoot(siteUrl)))
                    && (!link.attr(HREF).startsWith("#"))
                    && (!link.attr(HREF).startsWith("tel"))) {
                var absUrl = link.attr(ABS_HREF);
                // Query parameter check
                if (!StringUtils.isBlank(queryParameter)) {
                    var queryParameters = absUrl.substring(absUrl.indexOf("?") + 1);
                    var queryParametersArr = queryParameters.split("&");
                    for (var s : queryParametersArr) {
                        var parameterNameAndValueArr = s.split("=");
                        if (parameterNameAndValueArr[0].equals(queryParameter)) {
                            paramValue = parameterNameAndValueArr[1];
                            if (paramValue.indexOf("#") != -1) {
                                paramValue = paramValue.substring(0, paramValue.indexOf("#"));
                            }
                            if (uniqueListOfLinks.contains(paramValue)) {
                                linkCheckPassed = false;
                            } else {
                                linkCheckPassed = true;
                                uniqueListOfLinks.add(paramValue);
                            }
                            break;
                        }
                    }
                } else {
                    // Fallback check
                    linkCheckPassed = getRoot(link.attr(ABS_HREF)).equals(getRoot(siteUrl));
                }
                if (!linkCheckPassed) {
                    continue;
                }
                final var absHref = link.attr(ABS_HREF);
                final var aHref = link.attr(HREF);
                if (siteQueue != null && siteQueue.hasLinkBeenProcessed(absHref)) {
                    var cachedLink = siteQueue.getProcessedLink(absHref);
                    cachedLink.setElement(link);
                    outList.add(cachedLink);
                } else {
                    try {
                        PSLink psLink;
                        if (absHref.equals(getRoot(doc.baseUri())) && !absHref.isEmpty()) {
                            psLink = createLink(link, absHref, aHref, PSSiteContentDao.HOME_PAGE_NAME,
                                    getRelativePath(absHref, aHref, log, config));
                        } else {
                            psLink = createLink(link, absHref, aHref, getPageName(absHref, log, config),
                                    getRelativePath(absHref, aHref, log, config));
                        }
                        link.attr(HREF, PSReplacementFilter.filter(psLink.getRelativePathWithFileName()));
                        outList.add(psLink);
                    } catch (Exception e) {
                        log.appendLogMessage(PSLogEntryType.ERROR, "Link Extractor", absHref + " could not be retrieved.");
                        log.appendLogMessage(PSLogEntryType.STATUS, "Link Extractor", absHref
                                + " could not be retrieved due to the following error: " + e.getLocalizedMessage());
                    }
                }
            }
        }
        return outList;
    }

    /**
     * Gets a list of PSLink objects for image tags in a given Document.
     *
     * @param doc The HTML document.
     * @param log Logger for logging.
     * @return List of PSLink objects for images.
     */
    public static List<PSLink> getImagesForDocument(final Document doc, final IPSSiteImportLogger log) {
        var outList = new ArrayList<PSLink>();
        final Elements images = doc.select(IMG_SOURCE);
        for (var image : images) {
            final var absHref = image.attr(ABS_HREF);
            final var imgSrc = image.attr(SRC);
            try {
                var psImage = createLink(image, "", imgSrc, "", "");
                outList.add(psImage);
            } catch (Exception e) {
                log.appendLogMessage(PSLogEntryType.ERROR, "Link Extractor", absHref + " could not be retrieved.");
                log.appendLogMessage(PSLogEntryType.STATUS, "Link Extractor", absHref
                        + " could not be retrieved due to the following error: " + e.getLocalizedMessage());
            }
        }
        return outList;
    }

    protected static PSLink createLink(Element link, final String absHref, final String aHref, final String pageName,
                                       final String relativePath) throws UnsupportedEncodingException {
        try {
            return PSLink.createLink(PSReplacementFilter.filter(relativePath),
                    URLDecoder.decode(getLinkText(aHref, link), "UTF-8"), absHref,
                    PSReplacementFilter.filter(pageName), link);
        } catch (UnsupportedEncodingException e) {
            throw e;
        }
    }

    /**
     * Extracts link text for a given anchor.
     *
     * @param absHref The absolute HREF.
     * @param link    The anchor element.
     * @return The link text.
     */
    protected static String getLinkText(final String absHref, Element link) {
        String linkText = "";
        if (absHref != null && !absHref.isEmpty() && removeTrailingSlash(absHref).equals(getRoot(absHref))) {
            return "Home";
        }
        if (link.hasAttr("title") && !link.attr("title").isEmpty()) {
            if (!PSLinkBadKeywords.isStringInFilterList(link.attr("title")))
                return PSLinkBadKeywords.filterLinkTextString(link.attr("title"));
        }
        if (link.text() != null && !link.text().isEmpty()) {
            if (!PSLinkBadKeywords.isStringInFilterList(link.text()))
                return PSLinkBadKeywords.filterLinkTextString(link.text());
        }
        try {
            var doc = Jsoup.connect(absHref).get();
            var h1 = doc.select("h1");
            if (!h1.isEmpty()) {
                var h1Text = h1.get(0).text();
                if (h1Text != null && !linkText.isEmpty()) {
                    if (!PSLinkBadKeywords.isStringInFilterList(h1Text)) {
                        return h1Text;
                    }
                }
            }
            return doc.title();
        } catch (Exception e) {
            // Ignore bad links.
        }
        if (absHref != null) {
            linkText = absHref;
            if (absHref.contains(QUESTION_MARK)) {
                linkText = QUERY_STRING_LINK_TEXT_TOKEN;
            } else {
                linkText = getLastElementInPath(linkText);
                if (linkText.contains(PERIOD)) {
                    linkText = linkText.substring(0, linkText.indexOf(PERIOD));
                }
                if (linkText.isEmpty()) {
                    linkText = UNKNOWN;
                }
            }
        } else {
            linkText = UNKNOWN;
        }
        return linkText;
    }

    /**
     * Gets the last item in a path. Does not handle query strings.
     */
    private static String getLastElementInPath(final String linkText) {
        var linkTextMod = removeTrailingSlash(linkText);
        if (linkTextMod.contains(SLASH)) {
            linkTextMod = linkTextMod.substring(linkTextMod.lastIndexOf(SLASH) + 1);
        }
        return linkTextMod;
    }

    /**
     * Removes a trailing slash from link text.
     */
    private static String removeTrailingSlash(final String linkText) {
        var linkTextMod = linkText;
        if (!linkTextMod.isEmpty() && hasTrailingSlash(linkTextMod)) {
            linkTextMod = linkTextMod.substring(0, linkTextMod.length() - 1);
        }
        return linkTextMod;
    }

    /**
     * Checks text for a trailing slash.
     */
    public static boolean hasTrailingSlash(final String linkText) {
        return !linkText.isEmpty() && linkText.substring(linkText.length() - 1).equals(SLASH);
    }

    /**
     * Handles query string in a URL.
     */
    private static String handleQueryString(final String stringForStrip) {
        return stringForStrip.replace("/?", QUERY_STRING_PAGE_NAME);
    }

    /**
     * Gets the base path without root and query string.
     */
    private static String getBasePath(final String absHref) {
        var relativePath = absHref.replace(BACK_SLASH, SLASH);
        relativePath = absHref.replace(getRoot(absHref), "");
        relativePath = handleQueryString(relativePath);
        return relativePath;
    }

    /**
     * Extracts the relative path for a given absolute HREF.
     */
    protected static String getRelativePath(final String absHref, String aHref, final IPSSiteImportLogger log, PSSiteImportConfiguration config) {
        var relativePath = getBasePath(absHref.replace(BACK_SLASH, SLASH));
        if (relativePath.isEmpty()) {
            if (getRoot(absHref).equals(absHref)) {
                return SLASH;
            }
            var drPath = aHref.replace(BACK_SLASH, SLASH);
            relativePath = drPath.replace(getPageName(drPath, log, config), EMPTY);
        } else {
            if (!hasTrailingSlash(relativePath)) {
                relativePath = relativePath.substring(0, relativePath.lastIndexOf(SLASH) + 1);
            }
        }
        if (log != null) {
            log.appendLogMessage(PSLogEntryType.STATUS, "Link Extractor", "Changed Relative Path : " + relativePath
                    + " to " + PSReplacementFilter.filter(relativePath));
        }
        return PSReplacementFilter.filter(relativePath);
    }

    /**
     * Gets the page name (file name) from an absHref.
     */
    protected static String getPageName(final String absHref, final IPSSiteImportLogger log, PSSiteImportConfiguration config) {
        String queryParameter = null;
        if (config != null) {
            queryParameter = config.getMapQueryParamToPageName();
        }
        if (!StringUtils.isBlank(queryParameter) && absHref.indexOf("?") != -1) {
            var queryParameters = absHref.substring(absHref.indexOf("?") + 1);
            var queryParametersArr = queryParameters.split("&");
            String pageName = null;
            String paramValue = null;
            for (var s : queryParametersArr) {
                var parameterNameAndValueArr = s.split("=");
                if (parameterNameAndValueArr[0].equals(queryParameter)) {
                    paramValue = parameterNameAndValueArr[1];
                    if (paramValue.indexOf("#") != -1) {
                        paramValue = paramValue.substring(0, paramValue.indexOf("#"));
                    }
                    pageName = paramValue + "." + config.getSite().getDefaultFileExtention();
                    break;
                }
            }
            return pageName;
        } else {
            var endPartString = PSSiteContentDao.HOME_PAGE_NAME;
            var cleanAbsHref = absHref.replace(BACK_SLASH, SLASH);
            if (!(cleanAbsHref != null && !cleanAbsHref.isEmpty() && removeTrailingSlash(cleanAbsHref).equals(
                    getRoot(cleanAbsHref)))) {
                final var basePath = getBasePath(cleanAbsHref);
                if (!hasTrailingSlash(cleanAbsHref) || getLastElementInPath(basePath).contains(PERIOD)) {
                    endPartString = getLastElementInPath(basePath);
                }
            }
            if (!PSReplacementFilter.filter(endPartString).equals(endPartString)) {
                if (log != null) {
                    log.appendLogMessage(PSLogEntryType.STATUS, "Link Extractor", "Changed Page Name: " + endPartString
                            + " to " + PSReplacementFilter.filter(endPartString));
                }
            }
            if (endPartString.contains(PERIOD) && endPartString.contains(QUESTION_MARK)) {
                endPartString = endPartString.replace(PERIOD, DASH);
            }
            return PSReplacementFilter.filter(endPartString).replace("#", "-");
        }
    }

    /**
     * Gets the site root for a path.
     */
    protected static String getRoot(final String path) {
        var builtRoot = "";
        if (path != null && path.contains(DOUBLE_SLASH)) {
            final var leftPart = path.substring(0, path.indexOf(DOUBLE_SLASH) + 2);
            var rightPart = path.replace(leftPart, EMPTY);
            if (rightPart.contains(SLASH)) {
                rightPart = rightPart.substring(0, rightPart.indexOf(SLASH));
                builtRoot = leftPart + rightPart;
            } else {
                builtRoot = path;
            }
        }
        return builtRoot;
    }
}
