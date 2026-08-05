/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.services.utils.xml;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Helper methods for handling serialization to and from XML.
 *
 * <p><strong>Production engine (issue #1887 / epic #505 / #2062):</strong> public {@link
 * #writeToXml}, {@link #readFromXML} entry points use Jackson XML exclusively via {@link
 * PSJacksonXmlSerializationHelper}. Naming uses {@link PSXmlElementNameMapper}. Apache Commons
 * Betwixt has been removed from the classpath (#2062 / #1824 Child B).
 *
 * <p>Public API preserved: {@link #addType}, {@link #readFromXML}, {@link #writeToXml}, {@link
 * #getIdFromXml}, {@link #rewriteLegacyNullRoot}. Methods remain {@code synchronized} until
 * concurrency is re-proven without the historical dual-engine gate.
 *
 * <p><strong>Approved XML deviations vs historical Betwixt writes:</strong>
 *
 * <ul>
 *   <li>Betwixt graph-identity {@code id="…"} attributes on complex elements are not emitted by
 *       Jackson (property values live in child elements).
 *   <li>Historical {@code idref} graph stubs are expanded on Jackson <em>read</em> via {@link
 *       PSBetwixtIdrefExpander} (product decision expand-on-read, #1899) so package ACL permissions
 *       are not silently dropped.
 *   <li>Property / collection item element names for unannotated domain beans may differ until
 *       domain migration slices add Jackson annotations or shared mix-ins (#1888+).
 * </ul>
 *
 * @author dougrand
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PSXmlSerializationHelper {
  /** Static for logging */
  private static final Logger log = LogManager.getLogger(PSXmlSerializationHelper.class);

  /**
   * A content handler that searches for the id of the object, which is used to extract the id from
   * the xml. This is contained in the first attribute named "guid" or the first element named
   * "guid" that is found.
   */
  static class FindIdAttribute extends DefaultHandler {
    /** The id, <code>null</code> until the handler has found the id. */
    String m_id = null;

    /**
     * Set to <code>true</code> if a guid element is found. Then the next text found will be grabbed
     * for the id.
     */
    boolean m_nextText = false;

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      m_id = attributes.getValue("guid");
      if (!StringUtils.isBlank(m_id)) {
        throw new SAXException("Done");
      }
      if (qName.equals("guid")) {
        m_nextText = true;
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (m_nextText) {
        m_id = new String(ch, start, length);
        throw new SAXException("Done");
      } else {
        super.characters(ch, start, length);
      }
    }

    /**
     * Get the id
     *
     * @return the id
     */
    public String getId() {
      return m_id;
    }
  }

  /**
   * Add a type and element name to the mappings
   *
   * @param elementName the element name, never <code>null</code> or empty
   * @param type the class, never <code>null</code>
   */
  public static synchronized void addType(String elementName, Class<?> type) {
    if (elementName == null || StringUtils.isBlank(elementName)) {
      throw new IllegalArgumentException("elementName may not be null or empty");
    }
    if (type == null) {
      throw new IllegalArgumentException("type may not be null");
    }
    // Keep Jackson type registry in lock-step (polymorphic / collection items).
    PSJacksonXmlSerializationHelper.addType(elementName, type);
  }

  /**
   * Add a type to the mappings. This method does the default translation of the name to an element
   * name for the registration by using {@link PSXmlElementNameMapper}.
   *
   * @param type the class, never <code>null</code>
   */
  public static synchronized void addType(Class<?> type) {
    if (type == null) {
      throw new IllegalArgumentException("type may not be null");
    }
    String name = PSXmlElementNameMapper.mapTypeToElementName(type.getSimpleName());
    addType(name, type);
  }

  /**
   * Write the given object to an XML string via Jackson ({@link PSJacksonXmlSerializationHelper}).
   *
   * <p>Properties that should not be persisted should have the {@link
   * com.percussion.utils.xml.IPSXmlSerialization} annotation added to their <code>get</code> or
   * <code>is</code> methods.
   *
   * <p>Note: methods stay synchronized until concurrency is re-proven (historical Betwixt /
   * BeanUtils threading issues; keep the gate for the Jackson-only period).
   *
   * @param object the object to write, never <code>null</code>
   * @return the XML representation of the object
   * @throws IOException if there's a problem writing the object
   * @throws SAXException retained on signature for API compatibility (Jackson path throws {@link
   *     IOException} only)
   */
  public static synchronized String writeToXml(Object object) throws IOException, SAXException {
    if (object == null) {
      throw new IllegalArgumentException("object may not be null");
    }
    return PSJacksonXmlSerializationHelper.writeToXml(object);
  }

  /**
   * Extract the guid from the "guid" attribute.
   *
   * @param type the type, never <code>null</code>
   * @param xmlsource the xml source document, never <code>null</code> or empty
   * @return the guid, never <code>null</code>
   */
  public static IPSGuid getIdFromXml(PSTypeEnum type, String xmlsource) {
    if (type == null) {
      throw new IllegalArgumentException("type may not be null");
    }
    if (StringUtils.isBlank(xmlsource)) {
      throw new IllegalArgumentException("xmlsource may not be null or empty");
    }
    FindIdAttribute fia = new FindIdAttribute();
    SAXParserFactory fact =
        PSSecureXMLUtils.getSecuredSaxParserFactory(PSXmlSecurityOptions.secureWithDtd());

    try {
      SAXParser parser = fact.newSAXParser();
      parser.parse(new ByteArrayInputStream(xmlsource.getBytes()), fia);
    } catch (SAXException e) {
      // Ignore, expected
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (!StringUtils.isBlank(fia.getId())) {
      PSGuid rval = new PSGuid(type, fia.getId(), false);
      return rval;
    } else return null;
  }

  /**
   * Read the object's information from the given XML source. If your object has one or more
   * properties that are expressed as abstract classes or interfaces, you must register the needed
   * classes by calling {@link #addType(String, Class)}.
   *
   * @param xmlsource the xml source, never <code>null</code> or empty
   * @param object the object to read, never <code>null</code>
   * @return the object created from the given XML source, never <code>null</code>.
   * @throws IOException
   * @throws SAXException
   */
  public static Object readFromXML(String xmlsource, Object object)
      throws IOException, SAXException {
    if (object == null) {
      throw new IllegalArgumentException("object may not be null");
    }
    Object restored = readFromXML(xmlsource, object.getClass());
    try {
      BeanUtils.copyProperties(object, restored);
      return object;
    } catch (Exception e) {
      // Find underlying cause Exception.
      if (e.getCause() != null) {
        log.error("Cause= {}, Error: {}", e.getCause(), PSExceptionUtils.getMessageForLog(e));
      }
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new RuntimeException("Error copying bean properties", e);
    }
  }

  /**
   * Read the object's information from the given XML source via Jackson. If your object has one or
   * more properties that are expressed as abstract classes or interfaces, you must register the
   * needed classes by calling {@link #addType(String, Class)}.
   *
   * @param xmlString the xml source, never <code>null</code> or empty
   * @param clazz the class to read, may be <code>null</code> (resolved from root element via type
   *     map when possible)
   * @return restored object, never {@code null} on success
   * @throws IOException
   * @throws SAXException
   */
  public static synchronized Object readFromXML(String xmlString, Class clazz)
      throws IOException, SAXException {
    if (StringUtils.isBlank(xmlString)) {
      throw new IllegalArgumentException("xmlString may not be null or empty");
    }
    return readFromXMLJackson(xmlString, clazz);
  }

  /**
   * Jackson deserialize path for the public facade.
   *
   * @param xmlString never blank
   * @param clazz target type, may be {@code null} (resolved from root via type map)
   */
  private static Object readFromXMLJackson(String xmlString, Class clazz)
      throws IOException, SAXException {
    Class<?> target = clazz;
    String parseXml = rewriteLegacyNullRoot(xmlString, target);

    if (target == null) {
      target = resolveClassFromRootElement(parseXml);
      if (target == null) {
        String rootName = peekRootElementName(parseXml);
        throw new SAXException(
            "No bean found"
                + (rootName != null ? ", root element '" + rootName + "'" : "")
                + ". Register the root type with addType or pass an explicit Class.");
      }
      // If root was still legacy <null>, rewrite now that type is known
      parseXml = rewriteLegacyNullRoot(parseXml, target);
    }

    try {
      return PSJacksonXmlSerializationHelper.readFromXml(parseXml, target);
    } catch (IOException e) {
      String rootName = peekRootElementName(parseXml);
      throw new SAXException(
          "No bean found"
              + " for "
              + target.getName()
              + (rootName != null ? ", root element '" + rootName + "'" : "")
              + ": "
              + e.getMessage(),
          e);
    }
  }

  /**
   * Resolve a registered implementation class from the document root element name.
   *
   * @param xmlString XML to peek, never blank
   * @return registered class or {@code null}
   */
  private static Class<?> resolveClassFromRootElement(String xmlString) {
    String rootName = peekRootElementName(xmlString);
    if (StringUtils.isBlank(rootName)) {
      return null;
    }
    return PSJacksonXmlSerializationHelper.typeMapView().get(rootName);
  }

  /**
   * Best-effort root element local name (no full schema validation).
   *
   * @param xmlString never blank
   * @return root tag name or {@code null}
   */
  private static String peekRootElementName(String xmlString) {
    try {
      Document doc = PSXmlDocumentBuilder.createXmlDocument(new StringReader(xmlString), false);
      if (doc != null && doc.getDocumentElement() != null) {
        return doc.getDocumentElement().getTagName();
      }
    } catch (Exception e) {
      log.debug("Could not peek root element name: {}", PSExceptionUtils.getMessageForLog(e));
    }
    return null;
  }

  /**
   * When {@code clazz} is provided and the document root is the legacy element name {@code null},
   * rewrite open/close root tags to the mapped type name (via {@link PSXmlElementNameMapper}).
   *
   * <p>Package archives under {@code modules/perc-packages} commonly contain keyword XML whose root
   * is {@code <null id="…">} rather than {@code <keyword>}. Left unchanged, Jackson cannot bind the
   * root and {@link #readFromXML(String, Class)} fails with "No bean found".
   *
   * @param xmlString original XML, never blank
   * @param clazz target type, may be {@code null} (no rewrite)
   * @return XML ready for deserialize (possibly unchanged)
   */
  public static String rewriteLegacyNullRoot(String xmlString, Class<?> clazz) {
    if (clazz == null || StringUtils.isBlank(xmlString)) {
      return xmlString;
    }
    // Fast path: most modern payloads are already correctly named (case-sensitive)
    if (!xmlString.contains("<null") && !xmlString.contains("</null>")) {
      return xmlString;
    }

    String mapped = PSXmlElementNameMapper.mapTypeToElementName(clazz.getSimpleName());
    if (StringUtils.isBlank(mapped) || "null".equals(mapped)) {
      return xmlString;
    }

    // Case-sensitive: only legacy Betwixt root <null>, not <Null>/<NULL> or other types.
    // Optional XML declaration, then root open tag. DOTALL for multiline prolog only.
    java.util.regex.Pattern openPat =
        java.util.regex.Pattern.compile(
            "^(\\s*<\\?xml\\b[^?]*\\?>\\s*)?<null(\\s|>)", java.util.regex.Pattern.DOTALL);
    java.util.regex.Matcher openM = openPat.matcher(xmlString);
    if (!openM.find()) {
      return xmlString;
    }
    String decl = openM.group(1) != null ? openM.group(1) : "";
    String afterName = openM.group(2);
    StringBuffer openBuf = new StringBuffer();
    // quoteReplacement so mapped names with $ or \ never act as backreferences
    openM.appendReplacement(
        openBuf, java.util.regex.Matcher.quoteReplacement(decl + "<" + mapped + afterName));
    openM.appendTail(openBuf);
    String openReplaced = openBuf.toString();

    // Root close is the last </null> (package keyword XML has no nested <null>).
    // Do not require end-of-string so trailing comments/whitespace remain valid.
    final String closeLegacy = "</null>";
    int closeAt = openReplaced.lastIndexOf(closeLegacy);
    if (closeAt < 0) {
      return openReplaced;
    }
    return openReplaced.substring(0, closeAt)
        + "</"
        + mapped
        + ">"
        + openReplaced.substring(closeAt + closeLegacy.length());
  }

  /**
   * Read object from string, returning reconstituted object, calls {@link #readFromXML(String,
   * Class)}
   *
   * @param xmlString xml string, never <code>null</code> or empty
   * @return the read object, could be <code>null</code>
   * @throws SAXException
   * @throws IOException
   */
  public static Object readFromXML(String xmlString)
      throws IOException, SAXException, ParserConfigurationException {
    return readFromXML(xmlString, null);
  }
}
