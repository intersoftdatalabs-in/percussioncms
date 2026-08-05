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

package com.percussion.security.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.InputStream;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Utility class for securing XML parses.
 *
 * <p>Provides constant feature URIs and helpers that hard-disable external-entity resolution and
 * document-type declarations on the various XML parser factories used by Percussion CMS. The
 * defaults defend against XXE (CWE-611) even when callers mistakenly request the legacy {@code
 * enableExternalEntities} options.
 *
 * @author Percussion Software
 */
public class PSSecureXMLUtils {

  private PSSecureXMLUtils() {
    // hidden ctor
  }

  /**
   * JAXP {@code secure-processing} feature URI. Always set to {@code true} by the helpers in this
   * class. See <a href="http://xml.org/sax/features/namespaces">namespaces</a> for the related SAX
   * feature.
   */
  public static final String SECURE_PROCESSING_FEATURE = XMLConstants.FEATURE_SECURE_PROCESSING;

  /**
   * Apache Xerces feature URI used to disallow {@code <!DOCTYPE>} declarations. Set to {@code true}
   * when {@link PSXmlSecurityOptions#isEnableDtdDeclarations()} returns {@code false}.
   */
  public static final String DISALLOW_DOCTYPES_FEATURE =
      "http://apache.org/xml/features/disallow-doctype-decl";

  /**
   * SAX feature URI used to disable resolution of general external entities. Always set to {@code
   * false} as a defense-in-depth measure.
   */
  public static final String SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE =
      "http://xml.org/sax/features/external-general-entities";

  /**
   * Xerces / JDK feature IDs for external entities. Note: these are the real Apache feature URIs —
   * not the xerces.apache.org documentation page URLs that were previously used and always produced
   * "feature is not recognized" noise.
   */
  public static final String X1_GENERAL_EXTERNAL_ENTITIES_FEATURE =
      "http://apache.org/xml/features/external-general-entities";

  /** Alias retained for callers; same URI as {@link #X1_GENERAL_EXTERNAL_ENTITIES_FEATURE}. */
  public static final String X2_GENERAL_EXTERNAL_ENTITIES_FEATURE =
      X1_GENERAL_EXTERNAL_ENTITIES_FEATURE;

  /**
   * Xerces feature URI used to disable resolution of external parameter entities. Always set to
   * {@code false} as a defense-in-depth measure.
   */
  public static final String X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE =
      "http://apache.org/xml/features/external-parameter-entities";

  /** Alias retained for callers; same URI as {@link #X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE}. */
  public static final String X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE =
      X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE;

  /** SAX feature URI used to disable external parameter entities. Always set to {@code false}. */
  public static final String SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE =
      "http://xml.org/sax/features/external-parameter-entities";

  /** Xerces feature URI used to disable loading of external DTDs. Always set to {@code false}. */
  public static final String LOAD_EXTERNAL_DTD =
      "http://apache.org/xml/features/nonvalidating/load-external-dtd";

  /**
   * Default value used for {@link
   * javax.xml.parsers.DocumentBuilderFactory#setXIncludeAware(boolean)}. Set to {@code false} so
   * XInclude processing is disabled.
   */
  public static final boolean XINCLUDE_AWARE = false;

  /**
   * Default value used for {@link
   * javax.xml.parsers.DocumentBuilderFactory#setExpandEntityReferences(boolean)}. Set to {@code
   * false} so entity references are not expanded into text.
   */
  public static final boolean EXPAND_ENTITY_REFERENCES = false;

  private static final Logger log = LogManager.getLogger(PSSecureXMLUtils.class);

  /**
   * Log message template emitted when an XML parser reports that one of the security features
   * requested by this utility is not supported. The single {@code {}} placeholder is filled with
   * the unsupported feature URI.
   */
  public static final String UNSUPPORTED_FEATURE_WARN =
      "enableSecureFeatures exception thrown, XML Feature: {} is not supported by this XML Parser.";

  private static DocumentBuilderFactory enableDBFFeatures(
      DocumentBuilderFactory dbf, PSXmlSecurityOptions options) {
    dbf.setXIncludeAware(XINCLUDE_AWARE);
    dbf.setExpandEntityReferences(EXPAND_ENTITY_REFERENCES);
    PSXMLEntityResolverWrapper resolver = new PSXMLEntityResolverWrapper();

    // Defense-in-depth: always disable external entities and external parameter
    // entities regardless of caller-supplied options.  This prevents XXE
    // (CWE-611) even when callers accidentally pass enableExternalEntities=true.
    if (options.isEnableExternalEntities()) {
      log.warn(
          "Caller requested enableExternalEntities=true — overriding to false"
              + " to prevent XXE (CWE-611). Update the call site to pass false.");
    }
    if (options.isEnableExternalParameterEntities()) {
      log.warn(
          "Caller requested enableExternalParameterEntities=true — overriding"
              + " to false to prevent XXE (CWE-611). Update the call site to pass false.");
    }

    // Set each feature, logging any unsupported-feature errors as warnings.
    try {
      dbf.setAttribute("http://apache.org/xml/properties/internal/entity-resolver", resolver);
      dbf.setFeature(SECURE_PROCESSING_FEATURE, true);
    } catch (ParserConfigurationException e) {
      log.error(UNSUPPORTED_FEATURE_WARN, SECURE_PROCESSING_FEATURE);
    }

    try {
      dbf.setFeature(DISALLOW_DOCTYPES_FEATURE, !options.isEnableDtdDeclarations());
    } catch (ParserConfigurationException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, DISALLOW_DOCTYPES_FEATURE);
    }

    // Hard-disable all external-entity features (XXE prevention)
    try {
      dbf.setFeature(SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE, false);
    } catch (ParserConfigurationException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE);
    }

    try {
      dbf.setFeature(X1_GENERAL_EXTERNAL_ENTITIES_FEATURE, false);
    } catch (ParserConfigurationException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, X1_GENERAL_EXTERNAL_ENTITIES_FEATURE);
    }

    try {
      dbf.setFeature(X2_GENERAL_EXTERNAL_ENTITIES_FEATURE, false);
    } catch (ParserConfigurationException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, X2_GENERAL_EXTERNAL_ENTITIES_FEATURE);
    }

    try {
      dbf.setFeature(X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
    } catch (ParserConfigurationException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
    }

    try {
      dbf.setFeature(X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
    } catch (ParserConfigurationException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
    }

    try {
      dbf.setFeature(SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
    } catch (ParserConfigurationException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
    }

    try {
      dbf.setFeature(LOAD_EXTERNAL_DTD, options.isEnableExternalDtdReferences());
    } catch (ParserConfigurationException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, LOAD_EXTERNAL_DTD);
    }

    return dbf;
  }

  /**
   * Returns a secured DocumentBuilderFactory for the given class name. Uses reflection to
   * instantiate the factory, then applies OWASP XXE-prevention features.
   *
   * @param className the fully-qualified DocumentBuilderFactory implementation class name
   * @param options the security options to apply
   * @return the secured DocumentBuilderFactory
   * @throws ClassNotFoundException if the class cannot be found
   * @throws ReflectiveOperationException if the class cannot be instantiated
   */
  public static DocumentBuilderFactory getSecuredDocumentBuilderFactory(
      String className, PSXmlSecurityOptions options)
      throws ClassNotFoundException, ReflectiveOperationException {
    var clazz = Class.forName(className);
    var instance = (DocumentBuilderFactory) clazz.getDeclaredConstructor().newInstance();
    return enableDBFFeatures(instance, options);
  }

  /**
   * Will return a Document DocumentBuilderFactory initialized with security features enabled. The
   * default settings follow OWASP guidelines for protecting against XML eXternal Entity injection
   * (XXE) vulnerabilities.:
   *
   * <p>https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
   *
   * <p>As an XML application server / middleware that relies heavily on DTD's. the disable all DTD
   * feature is optional.
   *
   * @param options The security options to enable for this parser factory.
   * @return The DocumentBuilderFactory with the secure features enabled.
   */
  public static DocumentBuilderFactory getSecuredDocumentBuilderFactory(
      PSXmlSecurityOptions options) {

    return enableDBFFeatures(DocumentBuilderFactory.newInstance(), options);
  }

  /**
   * Secures {@link XMLInputFactory} instances. External entities are hard-disabled and DTD support
   * is enabled or disabled based on the caller-supplied options.
   *
   * @param options the security options to apply; assumed not {@code null}
   * @return a secured {@link XMLInputFactory} instance with external entities disabled
   */
  public static XMLInputFactory getSecuredXMLInputFactory(PSXmlSecurityOptions options) {

    XMLInputFactory xif = XMLInputFactory.newInstance();

    // This enables / disables DTDs entirely for that factory
    xif.setProperty(XMLInputFactory.SUPPORT_DTD, options.isEnableDtdDeclarations());

    // Hard-disable external entities regardless of caller options (XXE prevention)
    if (options.isEnableExternalEntities()) {
      log.warn(
          "Caller requested enableExternalEntities=true for XMLInputFactory"
              + " — overriding to false to prevent XXE (CWE-611).");
    }
    xif.setProperty("javax.xml.stream.isSupportingExternalEntities", false);

    return xif;
  }

  /**
   * Returns an XStream instance configured to (de)serialize only classes under the {@code
   * com.percussion.**} package and to use the DOM driver for XML I/O.
   *
   * <p>Note: the type whitelist is intentionally broad so that all Percussion CMS payload classes
   * can be processed; tighten it for any deployment that handles untrusted input.
   *
   * @return a non-null, configured {@link XStream} instance
   */
  public static XStream getSecuredXStream() {
    XStream xs = new XStream(new DomDriver());
    // TODO: 01-04-2022   whitelist specific classes
    xs.allowTypesByWildcard(new String[] {"com.percussion.**"});
    return xs;
  }

  /**
   * Returns a TransformerFactory with OWASP-recommended security attributes. External DTD and
   * stylesheet access are disabled to prevent XXE and SSRF via XSLT processing.
   *
   * @return a secured TransformerFactory instance
   */
  public static TransformerFactory getSecuredTransformerFactory() {
    // Explicitly request the JDK implementation to avoid picking up legacy
    // Saxon 6.5.3 via SPI — its TransformerFactoryImpl is too old and does
    // not implement JAXP 1.5+ methods like setFeature().
    TransformerFactory tf =
        TransformerFactory.newInstance(
            "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", null);
    return enableTFFeatures(tf);
  }

  /**
   * Returns a TransformerFactory for the given implementation class name with OWASP-recommended
   * security attributes. External DTD and stylesheet access are disabled to prevent XXE and SSRF.
   *
   * @param className the fully-qualified TransformerFactory implementation class name
   * @param classLoader the class loader to use, may be {@code null}
   * @return a secured TransformerFactory instance
   */
  public static TransformerFactory getSecuredTransformerFactory(
      String className, ClassLoader classLoader) {
    TransformerFactory tf = TransformerFactory.newInstance(className, classLoader);
    return enableTFFeatures(tf);
  }

  /**
   * Applies security features to a TransformerFactory instance. Disables external DTD and
   * stylesheet access to prevent XXE (CWE-611) and SSRF via XSLT processing.
   *
   * @param tf the TransformerFactory to secure, assumed not {@code null}
   * @return the secured TransformerFactory
   */
  private static TransformerFactory enableTFFeatures(TransformerFactory tf) {
    try {
      tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    } catch (IllegalArgumentException | AbstractMethodError e) {
      log.debug("TransformerFactory does not support ACCESS_EXTERNAL_DTD attribute");
    }
    try {
      tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    } catch (IllegalArgumentException | AbstractMethodError e) {
      log.debug("TransformerFactory does not support ACCESS_EXTERNAL_STYLESHEET attribute");
    }
    try {
      tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    } catch (javax.xml.transform.TransformerConfigurationException | AbstractMethodError e) {
      log.debug("TransformerFactory does not support FEATURE_SECURE_PROCESSING");
    }
    return tf;
  }

  /**
   * Returns a {@link SAXParserFactory} for the given implementation class name with
   * OWASP-recommended security attributes. External entities and external parameter entities are
   * hard-disabled regardless of caller-supplied options to prevent XXE (CWE-611).
   *
   * @param className the fully-qualified SAXParserFactory implementation class name
   * @param classLoader the class loader to use, may be {@code null}
   * @param options the security options to apply; assumed not {@code null}
   * @return a secured {@link SAXParserFactory} instance
   */
  public static SAXParserFactory getSecuredSaxParserFactory(
      String className, ClassLoader classLoader, PSXmlSecurityOptions options) {

    SAXParserFactory spf = SAXParserFactory.newInstance(className, classLoader);
    return enableSPFFeatures(spf, options);
  }

  /**
   * Returns a {@link SAXParserFactory} configured with OWASP-recommended security attributes.
   * External entities and external parameter entities are hard-disabled regardless of
   * caller-supplied options to prevent XXE (CWE-611).
   *
   * @param options the security options to apply; assumed not {@code null}
   * @return a secured {@link SAXParserFactory} instance
   */
  public static SAXParserFactory getSecuredSaxParserFactory(PSXmlSecurityOptions options) {

    SAXParserFactory spf = SAXParserFactory.newInstance();
    return enableSPFFeatures(spf, options);
  }

  /**
   * Applies security features to a SAXParserFactory instance. External entities and external
   * parameter entities are always hard-disabled regardless of caller-supplied options to prevent
   * XXE (CWE-611).
   *
   * @param spf the SAXParserFactory to secure, assumed not {@code null}
   * @param options the security options to apply
   * @return the secured SAXParserFactory
   */
  private static SAXParserFactory enableSPFFeatures(
      SAXParserFactory spf, PSXmlSecurityOptions options) {

    // Defense-in-depth: always disable external entities regardless of caller options.
    if (options.isEnableExternalEntities()) {
      log.warn(
          "Caller requested enableExternalEntities=true for SAXParserFactory"
              + " — overriding to false to prevent XXE (CWE-611).");
    }
    if (options.isEnableExternalParameterEntities()) {
      log.warn(
          "Caller requested enableExternalParameterEntities=true for SAXParserFactory"
              + " — overriding to false to prevent XXE (CWE-611).");
    }

    // Set each feature, logging unsupported features at debug level.
    try {
      spf.setFeature(SECURE_PROCESSING_FEATURE, true);
    } catch (java.lang.UnsupportedOperationException
        | ParserConfigurationException
        | SAXNotRecognizedException
        | SAXNotSupportedException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, SECURE_PROCESSING_FEATURE);
    }

    try {
      spf.setFeature(DISALLOW_DOCTYPES_FEATURE, !options.isEnableDtdDeclarations());
    } catch (java.lang.UnsupportedOperationException
        | ParserConfigurationException
        | SAXNotRecognizedException
        | SAXNotSupportedException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, DISALLOW_DOCTYPES_FEATURE);
    }

    // Hard-disable all external-entity features (XXE prevention)
    try {
      spf.setFeature(SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE, false);
    } catch (java.lang.UnsupportedOperationException
        | ParserConfigurationException
        | SAXNotRecognizedException
        | SAXNotSupportedException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE);
    }

    try {
      spf.setFeature(X1_GENERAL_EXTERNAL_ENTITIES_FEATURE, false);
    } catch (java.lang.UnsupportedOperationException
        | ParserConfigurationException
        | SAXNotRecognizedException
        | SAXNotSupportedException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, X1_GENERAL_EXTERNAL_ENTITIES_FEATURE);
    }

    try {
      spf.setFeature(X2_GENERAL_EXTERNAL_ENTITIES_FEATURE, false);
    } catch (java.lang.UnsupportedOperationException
        | ParserConfigurationException
        | SAXNotRecognizedException
        | SAXNotSupportedException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, X2_GENERAL_EXTERNAL_ENTITIES_FEATURE);
    }

    try {
      spf.setFeature(X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
    } catch (java.lang.UnsupportedOperationException
        | ParserConfigurationException
        | SAXNotRecognizedException
        | SAXNotSupportedException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
    }

    try {
      spf.setFeature(X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
    } catch (java.lang.UnsupportedOperationException
        | ParserConfigurationException
        | SAXNotRecognizedException
        | SAXNotSupportedException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
    }

    try {
      spf.setFeature(SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
    } catch (java.lang.UnsupportedOperationException
        | ParserConfigurationException
        | SAXNotRecognizedException
        | SAXNotSupportedException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE);
    }

    try {
      spf.setFeature(LOAD_EXTERNAL_DTD, options.isEnableExternalDtdReferences());
    } catch (java.lang.UnsupportedOperationException
        | ParserConfigurationException
        | SAXNotRecognizedException
        | SAXNotSupportedException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, LOAD_EXTERNAL_DTD);
    }

    return spf;
  }

  /**
   * Used to effectively block calls to external entities by the underlying parser.
   *
   * <p>Should be called if local catalog resolution fails to return a result.
   *
   * @return A new InputSource based on an empty string reader
   */
  public static InputSource getNoOpSource() {
    return new InputSource(new StringReader(""));
  }

  /**
   * Returns a {@link Source} backed by a secured SAX parser for use with JAXB {@code
   * Unmarshaller.unmarshal(Source)}. External entity declarations are rejected and external DTDs
   * are disabled at the parser level, preventing XXE (CWE-611) regardless of the JAXB default
   * configuration.
   *
   * <p>Default security options (from {@link PSXmlSecurityOptions#secure()}): DTDs disabled,
   * external entities disabled, external DTD references disabled, secure processing on. Callers
   * that need DTDs (e.g., legacy XSD imports) can pass a custom {@link PSXmlSecurityOptions} to the
   * overload below.
   *
   * @param inputStream the XML input stream; assumed not {@code null}
   * @return a SAXSource backed by a secured XMLReader
   * @throws Exception if the secured SAX parser cannot be constructed
   */
  public static Source getSecuredSaxSource(InputStream inputStream) throws Exception {
    return getSecuredSaxSource(inputStream, PSXmlSecurityOptions.secure());
  }

  /**
   * Returns a {@link Source} backed by a secured SAX parser for use with JAXB {@code
   * Unmarshaller.unmarshal(Source)}.
   *
   * @param inputStream the XML input stream; assumed not {@code null}
   * @param options the security options to apply
   * @return a SAXSource backed by a secured XMLReader
   * @throws Exception if the secured SAX parser cannot be constructed
   */
  public static Source getSecuredSaxSource(InputStream inputStream, PSXmlSecurityOptions options)
      throws Exception {
    SAXParserFactory spf = getSecuredSaxParserFactory(options);
    XMLReader xmlReader = spf.newSAXParser().getXMLReader();

    // Defense-in-depth: explicitly set the XXE-prevention features on the
    // XMLReader itself, in addition to the SAXParserFactory. CodeQL's
    // taint analysis does not always recognize the feature propagation
    // from SAXParserFactory -> SAXParser -> XMLReader; setting the
    // features directly on the reader makes the security guarantees
    // visible on the very object that JAXB will use. Each feature is
    // wrapped in try/catch for the same "unsupported feature is logged
    // at DEBUG" semantics as the factory.
    setFeatureSafe(xmlReader, DISALLOW_DOCTYPES_FEATURE, true /* always disallow */);
    setFeatureSafe(xmlReader, SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE, false);
    setFeatureSafe(xmlReader, X1_GENERAL_EXTERNAL_ENTITIES_FEATURE, false);
    setFeatureSafe(xmlReader, X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
    setFeatureSafe(xmlReader, X2_EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
    setFeatureSafe(xmlReader, SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
    setFeatureSafe(xmlReader, LOAD_EXTERNAL_DTD, options.isEnableExternalDtdReferences());

    InputSource inputSource = new InputSource(inputStream);
    return new SAXSource(xmlReader, inputSource);
  }

  /**
   * Safely set a SAX feature on an {@link XMLReader}, logging (but otherwise ignoring) any
   * unrecognized/unsupported feature per the existing factory-style handling.
   */
  private static void setFeatureSafe(XMLReader xmlReader, String feature, boolean value) {
    try {
      xmlReader.setFeature(feature, value);
    } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
      log.debug(UNSUPPORTED_FEATURE_WARN, feature);
    }
  }

  /**
   * Initialize JAXP properties to use specific parsers / transformers. Generally used by unit tests
   * that do not have access to the jvm.ini settings when running.
   */
  public static void setupJAXPDefaults() {
    System.setProperty(
        "javax.xml.transform.TransformerFactory",
        "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
    System.setProperty(
        "javax.xml.parsers.SAXParserFactory",
        "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
    System.setProperty(
        "javax.xml.datatype.DatatypeFactory",
        "com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl");
    System.setProperty(
        "javax.xml.parsers.DocumentBuilderFactory",
        "com.percussion.xml.PSDocumentBuilderFactoryImpl");
  }
}
