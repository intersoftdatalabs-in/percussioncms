/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.sitemanage.importer;

import org.jsoup.nodes.Element;

/**
 * Represents a link extracted from a site import.
 */
public final class PSLink {
    private static final String SLASH = "/";

    /** The user-viewable text for the link. */
    private String linkText;

    /** The relative path for the link. */
    private String linkPath;

    /** The absolute HREF for the link. */
    private String absoluteLink;

    /** The page name (file name). */
    private String pageName;

    /** The actual Jsoup element. */
    private Element element;

    /**
     * Gets the page name (file name).
     *
     * @return the file name.
     */
    public String getPageName() {
        return pageName;
    }

    /**
     * Sets the page name (file name).
     *
     * @param pageName the file name.
     */
    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    /**
     * Gets the absolute HREF for the link.
     *
     * @return the absolute HREF.
     */
    public String getAbsoluteLink() {
        return absoluteLink;
    }

    /**
     * Sets the absolute HREF for the link.
     *
     * @param absLink the absolute HREF.
     */
    public void setAbsoluteLink(final String absLink) {
        this.absoluteLink = absLink;
    }

    /**
     * Gets the user-viewable text for the link.
     *
     * @return the link text.
     */
    public String getLinkText() {
        return linkText;
    }

    /**
     * Sets the user-viewable text for the link.
     *
     * @param linkText the link text.
     */
    public void setLinkText(final String linkText) {
        this.linkText = linkText;
    }

    /**
     * Gets the relative path for the link.
     *
     * @return the relative path.
     */
    public String getLinkPath() {
        return linkPath;
    }

    /**
     * Sets the relative path for the link.
     *
     * @param linkPath the relative path.
     */
    public void setLinkPath(final String linkPath) {
        this.linkPath = linkPath;
    }

    /**
     * Sets the actual JSoup element.
     *
     * @param element the element.
     */
    public void setElement(Element element) {
        this.element = element;
    }

    /**
     * Gets the Jsoup element.
     *
     * @return the Jsoup element.
     */
    public Element getElement() {
        return element;
    }

    /**
     * Factory for PSLink.
     *
     * @param linkPath     the relative path for the link.
     * @param linkText     the text to display to the user.
     * @param absoluteHref the absolute HREF for the link.
     * @param pageName     the page name.
     * @param element      the Jsoup element.
     * @return a new PSLink instance.
     */
    public static PSLink createLink(
            final String linkPath,
            final String linkText,
            final String absoluteHref,
            final String pageName,
            Element element) {
        var link = new PSLink();
        link.setLinkPath(linkPath);
        link.setLinkText(linkText);
        link.setAbsoluteLink(absoluteHref);
        link.setPageName(pageName);
        link.setElement(element);
        return link;
    }

    /**
     * Factory for PSLink without element reference.
     *
     * @param linkPath     the relative path for the link.
     * @param linkText     the text to display to the user.
     * @param absoluteHref the absolute HREF for the link.
     * @param pageName     the page name.
     * @return a new PSLink instance.
     */
    public static PSLink createLinkWithoutElementReference(
            final String linkPath,
            final String linkText,
            final String absoluteHref,
            final String pageName) {
        var link = new PSLink();
        link.setLinkPath(linkPath);
        link.setLinkText(linkText);
        link.setAbsoluteLink(absoluteHref);
        link.setPageName(pageName);
        return link;
    }

    /**
     * Gets the relative path with the file name.
     *
     * @return the relative path with the file name.
     */
    public String getRelativePathWithFileName() {
        var out = this.getLinkPath();
        if (!hasTrailingSlash(out)) {
            out = out.concat(SLASH);
        }
        out = this.getLinkPath() + this.getPageName();
        return out;
    }

    @Override
    public String toString() {
        return "Link Path: " + this.getLinkPath()
                + " Link Text: " + this.getLinkText()
                + " Page Name: " + this.getPageName()
                + " HREF: " + this.getAbsoluteLink();
    }

    /**
     * Checks if the given text has a trailing slash.
     *
     * @param linkText the link text for evaluation.
     * @return true if there is a trailing slash, false otherwise.
     */
    public static boolean hasTrailingSlash(final String linkText) {
        return !linkText.isEmpty() && linkText.substring(linkText.length() - 1).equals(SLASH);
    }

    /** Private constructor for factory methods. */
    public PSLink() {
        // Intentionally empty.
    }
}
