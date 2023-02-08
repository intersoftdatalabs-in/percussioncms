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

package com.percussion.security.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import java.io.StringReader;

/**
 * Utility class for securing XML parses.
 */
public class PSSecureXMLUtils {

    private PSSecureXMLUtils(){
        //hidden ctor
    }
    //  http://xml.org/sax/features/namespaces
    // Set to true
    public static final String SECURE_PROCESSING_FEATURE = XMLConstants.FEATURE_SECURE_PROCESSING;

    // Set to true based on param
    public static final String DISALLOW_DOCTYPES_FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";

    // Set to false
    public static final String SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE="http://xml.org/sax/features/external-general-entities";

    // Set to true
    public static final String X1_GENERAL_EXTERNAL_ENTITIES_FEATURE = "http://xerces.apache.org/xerces-j/features.html#external-general-entities";

   //Set to true
   public static final String X2_GENERAL_EXTERNAL_ENTITIES_FEATURE="http://xerces.apache.org/xerces2-j/features.html#external-general-entities";

    //false
    public static final String X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE="http://xerces.apache.org/xerces-j/features.html#external-parameter-entities";

    public static final String X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE="http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities";

    public static final String SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE="http://xml.org/sax/features/external-parameter-entities";

    public static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    public static final boolean XINCLUDE_AWARE=false;
    public static final boolean EXPAND_ENTITY_REFERENCES=false;

    private static final Logger log = LogManager.getLogger(PSSecureXMLUtils.class);

    public static final String UNSUPPORTED_FEATURE_WARN="enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.";


    private static DocumentBuilderFactory enableDBFFeatures(DocumentBuilderFactory dbf, PSXmlSecurityOptions options){
        dbf.setXIncludeAware(XINCLUDE_AWARE);
        dbf.setExpandEntityReferences(EXPAND_ENTITY_REFERENCES);
        PSXMLEntityResolverWrapper resolver = new PSXMLEntityResolverWrapper();

        //Set each feature logging any errors as warnings for unsupported features.
        try{
            dbf.setAttribute("http://apache.org/xml/properties/internal/entity-resolver",resolver);
            dbf.setFeature(SECURE_PROCESSING_FEATURE,options.isEnableSecureProcessing());
        } catch (ParserConfigurationException e) {
            log.error(UNSUPPORTED_FEATURE_WARN,
                    SECURE_PROCESSING_FEATURE);
        }

        try{
            dbf.setFeature(DISALLOW_DOCTYPES_FEATURE,!options.isEnableDtdDeclarations());
        } catch (ParserConfigurationException e) {
            log.debug(UNSUPPORTED_FEATURE_WARN,
                    DISALLOW_DOCTYPES_FEATURE);
        }

        try{
            dbf.setFeature(SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE, options.isEnableExternalEntities());
        } catch (ParserConfigurationException e) {
            log.debug(UNSUPPORTED_FEATURE_WARN,
                    SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE);
        }

        try{
            dbf.setFeature(X1_GENERAL_EXTERNAL_ENTITIES_FEATURE, options.isEnableExternalEntities());
        } catch (ParserConfigurationException e) {
            log.debug(UNSUPPORTED_FEATURE_WARN,
                    X1_GENERAL_EXTERNAL_ENTITIES_FEATURE);
        }

        try{
            dbf.setFeature(X2_GENERAL_EXTERNAL_ENTITIES_FEATURE, options.isEnableExternalEntities());
        } catch (ParserConfigurationException e) {
            log.debug(UNSUPPORTED_FEATURE_WARN,
                    X2_GENERAL_EXTERNAL_ENTITIES_FEATURE);
        }

        try{
            dbf.setFeature(X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE,options.isEnableExternalParameterEntities());
        } catch (ParserConfigurationException e) {
            log.debug(UNSUPPORTED_FEATURE_WARN,
                    X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
        }

        try{
            dbf.setFeature(X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE,options.isEnableExternalParameterEntities());
        } catch (ParserConfigurationException e) {
            log.debug(UNSUPPORTED_FEATURE_WARN,
                    X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
        }

        try{
            dbf.setFeature(SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE, options.isEnableExternalEntities());
        } catch (ParserConfigurationException e) {
            log.debug(UNSUPPORTED_FEATURE_WARN,
                    SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
        }

        try{
            dbf.setFeature(LOAD_EXTERNAL_DTD,options.isEnableExternalDtdReferences());
        } catch (ParserConfigurationException e) {
            log.debug(UNSUPPORTED_FEATURE_WARN,
                    LOAD_EXTERNAL_DTD);
        }

        return dbf;
    }

    public static DocumentBuilderFactory getSecuredDocumentBuilderFactory(String className, PSXmlSecurityOptions options) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return enableDBFFeatures((DocumentBuilderFactory) Class.forName(
                className)
                .newInstance(), options);
    }

    /**
     * Will return a Document DocumentBuilderFactory initialized with security features enabled.
     * The default settings follow OWASP guidelines for protecting against
     * XML eXternal Entity injection (XXE) vulnerabilities.:
     *
     * https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
     *
     * As an XML application server / middleware that relies heavily on DTD's. the disable all DTD
     * feature is optional.
     *
     * @param options The security options to enable for this parser factory.
     * @return The DocumentBuilderFactory with the secure features enabled.
     */
    public static DocumentBuilderFactory getSecuredDocumentBuilderFactory(PSXmlSecurityOptions options){

        return enableDBFFeatures(DocumentBuilderFactory.newInstance(),options);
    }

    /**
     * Secures XMLInputFactory instances.  External entities are disabled and DTD's are turned on
     * or off based on the caller.
     *
     * @param options Options for secure processing
     * @return
     */
    public static XMLInputFactory getSecuredXMLInputFactory(PSXmlSecurityOptions options){

        XMLInputFactory xif = XMLInputFactory.newInstance();

        // This enables / disables DTDs entirely for that factory
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, options.isEnableDtdDeclarations());

        // disable / enable external entities
        xif.setProperty("javax.xml.stream.isSupportingExternalEntities", options.isEnableExternalEntities());

        return xif;
    }


    public static XStream getSecuredXStream(){
        XStream xs = new XStream(new DomDriver());
        // TODO: 01-04-2022   whitelist specific classes
        xs.allowTypesByWildcard(new String[] {
                "com.percussion.**"
        });
        return xs;
    }

    public static SAXParserFactory getSecuredSaxParserFactory(String className, ClassLoader classLoader, PSXmlSecurityOptions options){

        SAXParserFactory spf = SAXParserFactory.newInstance(className,classLoader);

        //Set each feature logging any errors as warnings for unsupported features.
        try{
            spf.setFeature(SECURE_PROCESSING_FEATURE,options.isEnableSecureProcessing());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    SECURE_PROCESSING_FEATURE);
        }

        try{
            spf.setFeature(DISALLOW_DOCTYPES_FEATURE,!options.isEnableDtdDeclarations());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    DISALLOW_DOCTYPES_FEATURE);
        }

        try{
            spf.setFeature(SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE,options.isEnableExternalEntities());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE);
        }

        try{
            spf.setFeature(X1_GENERAL_EXTERNAL_ENTITIES_FEATURE,options.isEnableExternalEntities());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    X1_GENERAL_EXTERNAL_ENTITIES_FEATURE);
        }

        try{
            spf.setFeature(X2_GENERAL_EXTERNAL_ENTITIES_FEATURE,options.isEnableExternalEntities());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    X2_GENERAL_EXTERNAL_ENTITIES_FEATURE);
        }

        try{
            spf.setFeature(X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE,options.isEnableExternalParameterEntities());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
        }

        try{
            spf.setFeature(X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE,options.isEnableExternalParameterEntities());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
        }

        try{
            spf.setFeature(SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE,options.isEnableExternalParameterEntities());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
        }

        try{
            spf.setFeature(LOAD_EXTERNAL_DTD,options.isEnableExternalDtdReferences());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    LOAD_EXTERNAL_DTD);
        }

        return spf;
    }
    public static SAXParserFactory getSecuredSaxParserFactory(PSXmlSecurityOptions options){

        SAXParserFactory spf = SAXParserFactory.newInstance();

        //Set each feature logging any errors as warnings for unsupported features.
        try{
            spf.setFeature(SECURE_PROCESSING_FEATURE,options.isEnableSecureProcessing());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    SECURE_PROCESSING_FEATURE);
        }

        try{
            spf.setFeature(DISALLOW_DOCTYPES_FEATURE,!options.isEnableDtdDeclarations());
        } catch (java.lang.UnsupportedOperationException |ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    DISALLOW_DOCTYPES_FEATURE);
        }

        try{
            spf.setFeature(SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE,options.isEnableExternalEntities());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE);
        }

        try{
            spf.setFeature(X1_GENERAL_EXTERNAL_ENTITIES_FEATURE,options.isEnableExternalEntities());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    X1_GENERAL_EXTERNAL_ENTITIES_FEATURE);
        }

        try{
            spf.setFeature(X2_GENERAL_EXTERNAL_ENTITIES_FEATURE,options.isEnableExternalEntities());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    X2_GENERAL_EXTERNAL_ENTITIES_FEATURE);
        }

        try{
            spf.setFeature(X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE,options.isEnableExternalParameterEntities());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
        }

        try{
            spf.setFeature(X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE,options.isEnableExternalParameterEntities());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
        }

        try{
            spf.setFeature(SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE,options.isEnableExternalParameterEntities());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
        }

        try{
            spf.setFeature(LOAD_EXTERNAL_DTD,options.isEnableExternalDtdReferences());
        } catch (java.lang.UnsupportedOperationException | ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            log.debug("enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.",
                    LOAD_EXTERNAL_DTD);
        }

        return spf;
    }

    /**
     * Used to effectively block calls to external entities by the underlying parser.
     *
     * Should be called if local catalog resolution fails to return a result.
     *
     * @return A new InputSource based on an empty string reader
     */
    public  static InputSource getNoOpSource(){
        return new InputSource(new StringReader(""));
    }

    /**
     * Initialize JAXP properties to use specific parsers / transformers.  Generally used by unit tests
     * that do not have access to the jvm.ini settings when running.
     */
    public static void setupJAXPDefaults(){
        System.setProperty("javax.xml.transform.TransformerFactory","com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        System.setProperty("javax.xml.parsers.SAXParserFactory","com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
        System.setProperty("javax.xml.datatype.DatatypeFactory","com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl");
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory","com.percussion.xml.PSDocumentBuilderFactoryImpl");
    }
}
