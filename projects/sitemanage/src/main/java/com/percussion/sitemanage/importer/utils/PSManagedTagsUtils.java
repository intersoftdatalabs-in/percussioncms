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
package com.percussion.sitemanage.importer.utils;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.Validate.notNull;

import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;

/**
 * @author Santiago M. Murchio
 * 
 */
public class PSManagedTagsUtils
{
    private static final String HTTP_EQUIV = "http-equiv";

    private static final String PROPERTY = "property";

    private static final String NAME = "name";
    
    private static final String COMMENT_START = "<!--";

    private static final String COMMENT_END = "-->";

    private static final List<String> HTTP_EQUIV_TO_EXCLUDE = asList(new String[]
    {"content-type"});

    private static final String DESCRIPTION_META_NAME = "description";
    
    private static final List<String> META_NAMES_TO_EXCLUDE = asList(new String[]
    {"generator", "robots", DESCRIPTION_META_NAME});
    
    

    private static final List<String> META_PROPERTIES_TO_EXCLUDE = asList(new String[]
    {"dcterms:author", "dcterms:type", "dcterms:source", "dcterms:created", "dcterms:alternative", "perc:tags",
            "perc:tags", "perc:category", "perc:calendar", "perc:start_date", "perc:end_date"});

    private static final String SRC = "src";

    private static final String[] MANAGED_JS_FILENAMES = new String[]
    {"jquery.js", "jquery.min.js", "jquery.ui.core.js", "jquery.tools.min.js", "jquery-latest.js", "jquery-ui.min.js",
            "jquery.ui.js"};

    private static final String[] MANAGED_JS_PATTERN = new String[]{"jquery-[\\d].*", "jquery-ui-[\\d].*"};
    
    private static final String SCRIPT = "script";

    /**
     * Verifies if the given tag is a managed reference to a jquery or
     * javascript file.
     * 
     * @param tag {@link Element} to verify. Must not be <code>null</code>.
     * @return <code>true</code> if the given tag references a managed js file.
     *         <code>false</code> otherwise.
     */
    public static boolean isManagedJSReference(Element tag)
    {
        notNull(tag);
        
        if(!equalsIgnoreCase(tag.tagName(), SCRIPT))
        {
            return false;
        }

        String srcAttr = tag.attr(SRC);
        String filename = getFilenameFromSrcAttribute(srcAttr);

        if(matchByfilename(filename))
        {
            return true;
        }
        
        if(matchByPattern(filename))
        {
            return true;
        }
        
        return false;
    }

    /**
     * Checks if the given tag is managed by CM1.
     * <p>
     * Managed tags are:
     * <li>meta http-equiv="content-type"
     * <li>meta name="generator"
     * <li>meta name="robots"
     * <li>meta name="description"
     * <li>meta property="dcterms:author"
     * <li>meta property="dcterms:type"
     * <li>meta property="dcterms:source"
     * <li>meta property="dcterms:created"
     * <li>meta property="dcterms:alternative"
     * <li>meta property="perc:tags"
     * <li>meta property="perc:category"
     * <li>meta property="perc:calendar"
     * <li>meta property="perc:start_date"
     * <li>meta property="perc:end_date"
     * 
     * @param metaTag {@link Element} with the given tag to inspect, must not be
     *            <code>null</code>.
     * @return <code>true</code> if the given tag is managed by CM1. <code>false
     *         </code> if not.
     */
    public static boolean isManagedMetadataTag(Element metaTag)
    {
        notNull(metaTag);
        
        String httpEquivAttr = metaTag.attr(HTTP_EQUIV);
        if (isNotBlank(httpEquivAttr) && HTTP_EQUIV_TO_EXCLUDE.contains(httpEquivAttr.toLowerCase()))
        {
            return true;
        }

        String nameAttr = metaTag.attr(NAME);
        if (isNotBlank(nameAttr) && META_NAMES_TO_EXCLUDE.contains(nameAttr.toLowerCase()))
        {
            return true;
        }

        String propertyAttr = metaTag.attr(PROPERTY);
        if (isNotBlank(propertyAttr) && META_PROPERTIES_TO_EXCLUDE.contains(propertyAttr.toLowerCase()))
        {
            return true;
        }

        return false;
    }

    /**
     * Check if the given tag is the description meta tag
     * 
     * @param metaTag The tag to check, not <code>null</code>.
     * 
     * @return <code>true</code> if it is the description tag, <code>false</code> if not.
     */
    public static boolean isDescriptionMetaTag(Element metaTag)
    {
        notNull(metaTag);
        
        return (DESCRIPTION_META_NAME.equalsIgnoreCase(metaTag.attr(NAME)));
    }
    
    /**
     * Removes the given tag from the dom and then adds a comment with the text
     * that the tag had. For example, for this tag
     * <code>&lt;meta name="robots" content="noindex" /&gt;</code> it will add
     * <code>&lt;!--&lt;meta name="robots" content="noindex" /&gt;--&gt;</code>
     * 
     * @param docHead {@link Element} with the DOM to modify. Must not
     *            <code>null</code>.
     * @param tag {@link Element} with the tag to comment. Must not
     *            <code>null</code>.
     */
    public static void commentTag(Element docHead, Element tag)
    {
        notNull(docHead);
        notNull(tag);
        
        Comment commentedTag = new Comment(tag.toString());
        docHead.appendChild(commentedTag);
        tag.remove();
    }

    /**
     * Comments the given tag text. For example if the tag text is:
     * 
     * <pre>
     * &lt;script src="jquery.js" type="text/javascript"&gt;&lt;/script&gt;
     * </pre>
     * 
     * it will return
     * 
     * <pre>
     * &lt;!--&lt;script src="jquery.js" type="text/javascript"&gt;&lt;/script&gt;--&gt;
     * </pre>
     * 
     * @param tagText {@link String} with the text of the tag to comment, must
     *            not be <code>null</code> but may be empty.
     * @return {@link String} never <code>null</code> nor empty.
     */
    public static String commentTagText(String tagText)
    {
        notNull(tagText);
        return COMMENT_START + tagText + COMMENT_END;
    }
    
    /**
     * Verifies if the given filename matches with the regular expressions that
     * represent the managed js references. 
     * 
     * @param filename {@link String}, may be <code>null</code> or empty, in
     *            which case it will return <code>false</code>.
     * @return <code>true</code> if the source references a managed js file.
     *         <code>false</code> otherwise.
     */
    private static boolean matchByPattern(String filename)
    {
        if (isBlank(filename))
        {
            return false;
        }
        
        for(String regex : MANAGED_JS_PATTERN)
        {
            Pattern pattern = Pattern.compile(regex);
            if(pattern.matcher(filename).matches())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifies if the given filename matches with the filenames that represent
     * the managed js references.
     * 
     * @param filename {@link String}, may be <code>null</code> or empty, in
     *            which case it will return <code>false</code>.
     * @return <code>true</code> if the source references a managed js file.
     *         <code>false</code> otherwise.
     */
    private static boolean matchByfilename(String filename)
    {
        if (isBlank(filename))
        {
            return false;
        }
        
        for (String managedJSFilename : MANAGED_JS_FILENAMES)
        {
            if (equalsIgnoreCase(filename, managedJSFilename))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the filename from a source reference. For example, if the source is
     * this:
     * <code>http://ajax.aspnetcdn.com/ajax/jQuery/jquery-1.7.1.min.js?b=111</code>
     * this method will return <code>jquery-1.7.1.min.js</code>
     * 
     * @param srcAttr {@link String} with the source reference, assumed not
     *            <code>null</code>.
     * @return {@link String} never <code>null</code> but may be empty.
     */
    private static String getFilenameFromSrcAttribute(String srcAttr)
    {
        
        String pathParts[] = srcAttr.split("/");
        String filename = pathParts[pathParts.length - 1];
        
        if(filename.indexOf('?') >= 0)
        {
            return filename.substring(0, filename.indexOf('?'));
        }
        return filename;
    }

}
