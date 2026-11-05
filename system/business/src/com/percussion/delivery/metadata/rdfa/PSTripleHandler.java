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

package com.percussion.delivery.metadata.rdfa;

import com.percussion.delivery.metadata.PSMetadataExtractorService;
import com.percussion.delivery.metadata.extractor.data.PSMetadataProperty;
import com.percussion.error.PSExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Custom RDFHandler to collect metadata properties when a file is processed using RDF4J.
 *
 * @author miltonpividori
 *
 */
public class PSTripleHandler implements RDFHandler
{

    private static final Logger log = LogManager.getLogger(PSTripleHandler.class);

    /**
     * Accidental RDFa that will not be included as a metadata property.
     */
    private final String[] accidentalRDFaList =
        new String[] {
            "vocab#stylesheet",
            "#head",
            "#tr"
        };

    private static final String VOCAB_URL = "http://www.w3.org/1999/xhtml/vocab#";

    /**
     * Has every namespace of the page being processed. The key is the URL, the
     * value is the declared namespace. Fox example: {
     * 'http://purl.org/dc/terms/': 'dcterms' }. It used to replace the URL by
     * the declared name when filling PSMetadataProperty.name field.
     */
    private Map<String, String> namespacesByUrl = new HashMap<>();

    /**
     * All the PSMetadataProperty objects that were created from the metadata
     * properties extracted from the page being processed.
     */
    private Set<PSMetadataProperty> properties = new HashSet<>();

    /**
     * The linktext of the PSMetadataEntry.
     */
    private String pageLinktext;

    /**
     * The type of the PSMetadataEntry.
     */
    private String pageType;

    /**
     * Regular expression to separate the URL from the property name in a
     * Triple's predicate.
     */
    private Pattern patternForNamespaceURLExtraction = Pattern.compile("(.+[/#])([^/]+)");

    public PSTripleHandler()
    {
        // Replace XHTML vocab URL by an empty string.
        namespacesByUrl.put(VOCAB_URL, StringUtils.EMPTY);
        namespacesByUrl.put("http://percussion.com/perc/elements/1.0/", "perc");
        namespacesByUrl.put("http://purl.org/dc/terms/", "dcterms");
        namespacesByUrl.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf");
        namespacesByUrl.put("http://ogp.me/ns#", "og");
        namespacesByUrl.put("http://ogp.me/ns/fb#", "fb");
        namespacesByUrl.put("http://vocab.sindice.net/any23#", "any23");
    }

    public Set<PSMetadataProperty> getProperties()
    {
        return properties;
    }

    public String getPageLinktext()
    {
        return pageLinktext;
    }

    public String getPageType()
    {
        if(StringUtils.isNotEmpty(pageType)) {
            return pageType;
        }else{
            HashMap map = new HashMap();
                for(PSMetadataProperty p : getProperties()) {
                    if(p.getName().equals("dcterms:type")) {
                        pageType = p.getValue();
                        return pageType;
                    }
                }
            }

        return "";
    }

    /**
     * RDF4J handler method for receiving namespace declarations.
     */
    @Override
    public void handleNamespace(String prefix, String uri) throws RDFHandlerException
    {
        if (!uri.equals(VOCAB_URL))
            namespacesByUrl.put(uri, prefix);
    }

    /**
     * RDF4J handler method for receiving RDF statements (triples).
     * Here the PSMetadataProperty objects are created with statement information.
     */
    @Override
    public void handleStatement(Statement statement) throws RDFHandlerException
    {
        IRI propertyURL = statement.getPredicate();
        Value propertyValue = statement.getObject();

        // Don't process accidental RDFa, as styles, etc.
        if (accidentalRDFa(propertyURL.toString()))
            return;

        // Process the propertyUrl
        PropertyURLProcessingResult propertyURLProcessingResult = processPropertyUrl(propertyURL.toString());

        // Check the property name. If it's 'alternative', then it maps to
        // PSMetadataEntry.linktext, and
        // 'type' to PSMetadataEntry.type. If none of them, then a
        // PSMetadataProperty is created.
        if (propertyURLProcessingResult.getPropertyNameWithDeclaredNamespace().equals(
                PSMetadataExtractorService.ALTERNATIVE_PROPERTY_NAME))
            pageLinktext = propertyValue.stringValue();
        else if (propertyURLProcessingResult.getPropertyNameWithDeclaredNamespace().equals(
                PSMetadataExtractorService.TYPE_PROPERTY_NAME))
            pageType = propertyValue.stringValue();
        else
        {
            PSMetadataProperty property = getMetadataProperty(propertyURLProcessingResult, propertyValue);

            if (property != null)
                properties.add(property);
        }
    }

    /**
     * Given a PropertyURLProcessingResult object, and the value of the metadata
     * property, it creates the PSMetadataProperty with its value.
     *
     * @param propertyURLProcessingResult Result of processing the property URL.
     *            Should never be <code>null</code>.
     * @param propertyValue Property value. Should never be <code>null</code>.
     * @return PSMetadataProperty object with the metadata property information
     *         given. Can be <code>null</code> if there are errors in parsing
     *         the property value (for instance, a malformed date).
     */
    private PSMetadataProperty getMetadataProperty(PropertyURLProcessingResult propertyURLProcessingResult,
            Value propertyValue)
    {
        String propertyName = propertyURLProcessingResult.getPropertyNameWithDeclaredNamespace();

        String realPropertyValue = propertyValue.stringValue();

        PSMetadataProperty prop = new PSMetadataProperty(propertyName, realPropertyValue);

        return prop;
    }

    /**
     * Replace HTML tags that are typically self closing tags (like
     * <code>BR</code>), that were converted to a pair of tags by Any23. Write
     * those tags as self closing tags again (E.G. <BR/>
     * ) to avoid issues with browsers.
     *
     * @param xhtmlPropertyValue XHTML that was generated by Any23 library.
     * @return xhtml property with all pairs of BR tags converted to self
     *         closing tags.
     */
    private String replaceSelfClosingTags(String xhtmlPropertyValue)
    {
        // This pattern matches <BR> tag pairs and content between them. (Case
        // insensitive).
        Pattern brTagsPattern = Pattern.compile("<\\s*BR[^>]*>(.*?)<\\s*/\\s*BR>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = brTagsPattern.matcher(xhtmlPropertyValue);
        xhtmlPropertyValue = matcher.replaceAll("<BR/>");
        return xhtmlPropertyValue;
    }

    /**
     * Check if a property URL is accidental RDFa.
     *
     * @param propertyUrl Property URL to check. Should never be
     *            <code>null</code>.
     * @return 'true' if the property URL is accidetal RDFa. 'false' otherwise.
     */
    private boolean accidentalRDFa(String propertyUrl)
    {
        for (String accidentalRDFa : accidentalRDFaList)
            if (propertyUrl.endsWith(accidentalRDFa))
                return true;

        // we take out the property url if it has more than 100
        // characters because it isn't a valid property name
        if(propertyUrl.length() > 100){
            return true;
        }

        return false;
    }

    public String getNamespace(String completePropertyUrl){
        if(!org.springframework.util.StringUtils.isEmpty(completePropertyUrl)) {
            if(completePropertyUrl.indexOf("#") > 0) {
                return completePropertyUrl.substring(0, completePropertyUrl.indexOf("#")+1);
            }else if(completePropertyUrl.lastIndexOf("/") >0){
                return completePropertyUrl.substring(0, completePropertyUrl.lastIndexOf("/")+1);
            } else{
                return "";
            }
        }
        return completePropertyUrl;
    }

    public String getPlainPropertyName(String completePropertyUrl){
        if(!org.springframework.util.StringUtils.isEmpty(completePropertyUrl)) {
            if(completePropertyUrl.indexOf("#") > 0) {
                return completePropertyUrl.substring(completePropertyUrl.indexOf("#") + 1);
            }else if(completePropertyUrl.lastIndexOf("/") >0){
                return completePropertyUrl.substring(completePropertyUrl.lastIndexOf("/") + 1);
            }
        }
        return completePropertyUrl;
    }

    /**
     * Given a complete property URL, it makes some processing on it, replacing
     * the URL by the declared namespace, or deleting it if it's the case of the
     * default namespace (vocab attribute).
     *
     * @param completePropertyUrl The complete property url extracted. Should
     *            never be <code>null</code>.
     * @return A PropertyURLProcessingResult with information about processing.
     *         Never <code>null</code>.
     */
    private PropertyURLProcessingResult processPropertyUrl(String completePropertyUrl)
    {
        try {

            String namespaceUrl = getNamespace(completePropertyUrl);
            String plainPropertyName =  getPlainPropertyName(completePropertyUrl);
            String propertyName = completePropertyUrl;

            // Replace the URL by the declared namespace, or delete it if it's
            // the case of the default one.
            if (namespacesByUrl.containsKey(namespaceUrl)) {
                String replacement = namespacesByUrl.get(namespaceUrl);

                if (!StringUtils.isEmpty(replacement))
                    propertyName = completePropertyUrl.replace(namespaceUrl, replacement + ":");
                else
                    propertyName = completePropertyUrl.replace(namespaceUrl, replacement);
            }

            return new PropertyURLProcessingResult(plainPropertyName, propertyName);
        }catch(IllegalStateException e){
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return new PropertyURLProcessingResult(completePropertyUrl, completePropertyUrl);
    }

    @Override
    public void startRDF() throws RDFHandlerException
    {
        // no-op
    }

    @Override
    public void endRDF() throws RDFHandlerException
    {
        // no-op
    }

    @Override
    public void handleComment(String comment) throws RDFHandlerException
    {
        // no-op
    }

    /**
     * Represent the result of processing a property URL. It contains the plain
     * property name (for example, "title") and the property name with declared
     * namespace (for example, "dcterms:title").
     *
     * @author miltonpividori
     *
     */
    class PropertyURLProcessingResult
    {
        /**
         * The plain property name. For example, "title".
         */
        private String plainPropertyName;

        /**
         * The property name along with the declared namespace. For example,
         * "dcterms:title".
         */
        private String propertyNameWithDeclaredNamespace;

        private static final String PROPERTY_ABSTRACT = "abstract";

        public PropertyURLProcessingResult(String plainPropertyName, String propertyNameWithDeclaredNamespace)
        {
            this.plainPropertyName = plainPropertyName;
            this.propertyNameWithDeclaredNamespace = propertyNameWithDeclaredNamespace;
        }

        /**
         * @return the plainPropertyName
         */
        public String getPlainPropertyName()
        {
            return plainPropertyName;
        }

        /**
         * @return the propertyNameWithDeclaredNamespace
         */
        public String getPropertyNameWithDeclaredNamespace()
        {
            return propertyNameWithDeclaredNamespace;
        }
    }
}
