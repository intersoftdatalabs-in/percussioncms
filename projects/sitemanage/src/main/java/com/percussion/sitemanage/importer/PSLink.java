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

package com.percussion.sitemanage.importer;

import org.jsoup.nodes.Element;

public final class PSLink
{    
    private static final String SLASH = "/";
    /**
     * The user viewable text for the link
     */
    private String LINK_TEXT = null;
    /**
     * The relative path for the link
     */
    private String LINK_PATH = null;

    /**
     * The Absolute Href for the link
     */
    private String ABS_Link = null;

    /**
     * The Page Name... as in file name
     */
    private String PAGE_NAME = null;

    /**
     * The actual Jsoup element
     */
    private Element ELEMENT = null;
    
    /**
     * Gets the page name (file name)
     * 
     * @return a name for the file
     */
    public String getPageName() {
        return PAGE_NAME;
    }

    /**
     * Sets the page name (file name)
     * 
     * @param pageName
     *            the file name
     */
    public void setPageName(String pageName) {
        PAGE_NAME = pageName;
    }

    /**
     * Gets the Absolute HREF link for the link
     * 
     * @return the Absolute HREF for the link
     */
    public String getAbsoluteLink() {
        return ABS_Link;
    }

    /**
     * Sets the Absolute HREF for the link
     * 
     * @param absLink
     *            the Absolute HREF for the link
     */
    public void setAbsoluteLink(final String absLink) {
        ABS_Link = absLink;
    }

    /**
     * Gets the user viewable text for the link
     * 
     * @return the user viewable text for the link
     */
    public String getLinkText() {
        return LINK_TEXT;
    }

    /**
     * Sets the user viewable text for the link
     * 
     * @param lINK_TEXT
     *            the user viewable text for the link
     */
    public void setLinkText(final String linkText) {
        LINK_TEXT = linkText;
    }

    /**
     * Gets the relative path for the link
     * 
     * @return the relative path for the link
     */
    public String getLinkPath() {
        return LINK_PATH;
    }

    /**
     * Sets the relative path for the link
     * 
     * @param linkPath
     *            the relative path for the link
     */
    public void setLinkPath(final String linkPath) {
        LINK_PATH = linkPath;
    }

    /**
     * Sets the actual JSoup element
     * @param element the element
     */
    public void setElement(Element element) {
        ELEMENT = element;
    }
    
    /**
     * Gets the element
     * @return the Jsoup element for 
     */
    public Element getElement(){
        return ELEMENT;
    }
    

    /**
     * Factory for PSLink
     * 
     * @param linkPath
     *            the relative path for the link
     * @param linkText
     *            the text to display to the user
     * 
     * @param absoluteHref
     *            the absolute HREF for the link
     */
    public static PSLink createLink(final String linkPath,
            final String linkText, final String absoluteHref,
            final String pageName, Element element) {
        final PSLink link = new PSLink();
        link.setLinkPath(linkPath);
        link.setLinkText(linkText);
        link.setAbsoluteLink(absoluteHref);
        link.setPageName(pageName);
        link.setElement(element);
        return link;
    }
    
    public static PSLink createLinkWithoutElementReference(final String linkPath,
            final String linkText, final String absoluteHref,
            final String pageName)
    {
    	 final PSLink link = new PSLink();
         link.setLinkPath(linkPath);
         link.setLinkText(linkText);
         link.setAbsoluteLink(absoluteHref);
         link.setPageName(pageName);
         return link;
    }

    /**
     * Gets the relative path with the file name
     * 
     * @return the relative path with the file name
     */
    public String getRelativePathWithFileName()
    {
        String out = this.getLinkPath();
        if (!hasTrailingSlash(out)){
            out = out.concat("/");
        }
        out = this.getLinkPath() + this.getPageName();
        return out;
    }
    
    public String toString() {
        return "Link Path: " + this.getLinkPath() + " Link Text: "
                + this.getLinkText() + " Page Name: " + this.getPageName()
                + " HREF: " + this.getAbsoluteLink();
    }

    /**
     * Checks text for a trailing slash
     * 
     * @param linkText the link text for evaluation
     * @return a boolean indicating the presence or lack of a trailing slash
     */
    public static boolean hasTrailingSlash(final String linkText)
    {
        boolean hasTrailingSlash = false;
        if (!linkText.isEmpty())
        {
            hasTrailingSlash = linkText.substring(linkText.length() - 1).equals(SLASH);
        }
        return hasTrailingSlash;
    }
    /**
     * Private constructor
     */
    public PSLink() {

    }

    
}
