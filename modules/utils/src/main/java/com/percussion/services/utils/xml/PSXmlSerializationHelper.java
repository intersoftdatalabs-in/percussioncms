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
import com.percussion.utils.xml.IPSXmlSerialization;
import com.percussion.utils.xml.PSSaxHelper;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.beans.IntrospectionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.betwixt.IntrospectionConfiguration;
import org.apache.commons.betwixt.XMLIntrospector;
import org.apache.commons.betwixt.io.BeanReader;
import org.apache.commons.betwixt.io.BeanWriter;
import org.apache.commons.betwixt.io.read.BeanCreationChain;
import org.apache.commons.betwixt.io.read.BeanCreationList;
import org.apache.commons.betwixt.io.read.ChainedBeanCreator;
import org.apache.commons.betwixt.io.read.ElementMapping;
import org.apache.commons.betwixt.io.read.ReadContext;
import org.apache.commons.betwixt.strategy.HyphenatedNameMapper;
import org.apache.commons.betwixt.strategy.NameMapper;
import org.apache.commons.betwixt.strategy.PropertySuppressionStrategy;
import org.apache.commons.betwixt.strategy.TypeBindingStrategy;
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
 * <p><strong>Production engine (issue #1887 / epic #505):</strong> public {@link #writeToXml},
 * {@link #readFromXML} entry points default to Jackson XML via {@link
 * PSJacksonXmlSerializationHelper}. Naming uses {@link PSXmlElementNameMapper}. Commons Betwixt
 * remains on the classpath and is still available as an emergency rollback only.
 *
 * <p><strong>Rollback flag:</strong> set system property {@value #ENGINE_PROPERTY} to {@value
 * #ENGINE_BETWIXT} to force the legacy Betwixt path. Default is {@value #ENGINE_JACKSON}. Remove
 * this flag when domain slices and #1824 (Betwixt POM removal) complete — do not leave dual engines
 * indefinitely.
 *
 * <p>Public API preserved: {@link #addType}, {@link #readFromXML}, {@link #writeToXml}, {@link
 * #getIdFromXml}, {@link #rewriteLegacyNullRoot}. Methods remain {@code synchronized} until Betwixt
 * is fully removed and concurrency is re-proven.
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
   * System property selecting the XML serialization engine for {@link #writeToXml} / {@link
   * #readFromXML}.
   *
   * <p>Values: {@value #ENGINE_JACKSON} (default) or {@value #ENGINE_BETWIXT} (rollback).
   *
   * <p><strong>Removal plan:</strong> drop this property and Betwixt code paths after domain
   * consumer migration (#1823 slices) and {@code commons-betwixt} removal (#1824).
   */
  public static final String ENGINE_PROPERTY = "com.percussion.xml.serialization.engine";

  /** Default production engine. */
  public static final String ENGINE_JACKSON = "jackson";

  /** Emergency rollback engine (Betwixt). */
  public static final String ENGINE_BETWIXT = "betwixt";

  /** Static used for method lookup */
  static final Class[] NOARGS = new Class[0];

  /**
   * This class dictates a strategy that suppresses the persistence of certain object properties.
   * Except "class", which is directly suppressed, the suppression information is derived from
   * annotation information on the given getter method.
   */
  static class SuppressionStrategy extends PropertySuppressionStrategy {
    @Override
    public boolean suppressProperty(
        Class classContainingTheProperty, Class propertyType, String propertyName) {
      String name = StringUtils.capitalize(propertyName);

      if (name.equalsIgnoreCase("class")) return true;

      try {
        Method m = null;
        try {
          m = classContainingTheProperty.getMethod("get" + name, NOARGS);
        } catch (Exception e) {
          m = classContainingTheProperty.getMethod("is" + name, NOARGS);
        }

        if (m != null) {
          IPSXmlSerialization ann = m.getAnnotation(IPSXmlSerialization.class);
          if (ann != null) {
            return ann.suppress();
          }
        }
        return false;
      } catch (Exception e) {
        return false;
      }
    }
  }

  /**
   * Betwixt {@link NameMapper} that delegates type naming to {@link PSXmlElementNameMapper} so the
   * Jackson helper and Betwixt share one naming strategy (issue #1822 / epic #505).
   *
   * <p>Strips {@code PS}/{@code IPS}, flattens multi-cap runs, then hyphenates (see {@link
   * PSXmlElementNameMapper}). Betwixt uses the same mapper instance for element and attribute name
   * mapping ({@link #createXMLIntrospector()}).
   */
  static class PSNameMapper extends HyphenatedNameMapper {
    @Override
    public String mapTypeToElementName(String name) {
      return PSXmlElementNameMapper.mapTypeToElementName(name);
    }
  }

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

  /** A specific implementation of this, see {@link TypeBindingStrategy} */
  static class PSTypeBindingStrategy extends TypeBindingStrategy {
    @Override
    public BindingType bindingType(Class bindingClass) {
      if (Enum.class.isAssignableFrom(bindingClass))
        return TypeBindingStrategy.BindingType.PRIMITIVE;
      else if (IPSGuid.class.isAssignableFrom(bindingClass))
        return TypeBindingStrategy.BindingType.PRIMITIVE;
      else return TypeBindingStrategy.DEFAULT.bindingType(bindingClass);
    }
  }

  /** Maps element names to implementation classes. Used for deserialization. */
  static Map<String, Class<?>> ms_typeMap = new HashMap<String, Class<?>>();

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
    ms_typeMap.put(elementName, type);
    // Keep Jackson type registry in lock-step (polymorphic / collection items).
    PSJacksonXmlSerializationHelper.addType(elementName, type);
  }

  /**
   * Add a type to the mappings. This method does the default translation of the name to an element
   * name for the registration by using the class {@link PSNameMapper}.
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
   * Whether the public facade uses Jackson (default) or the Betwixt rollback path.
   *
   * @return {@code true} when Jackson is selected (including blank/unknown property values)
   */
  public static boolean isJacksonEngine() {
    String value = System.getProperty(ENGINE_PROPERTY, ENGINE_JACKSON);
    return !ENGINE_BETWIXT.equalsIgnoreCase(StringUtils.trimToEmpty(value));
  }

  /** Holds the suppression strategy singleton. */
  private static SuppressionStrategy ms_supStrategy = null;

  /** Holds the type strategy singleton. */
  private static TypeBindingStrategy ms_typebinder = null;

  /**
   * Get and if necessary create the type binding singleton.
   *
   * @return the singleton, never <code>null</code>
   */
  private static synchronized TypeBindingStrategy getTypeBindingStrategyInstance() {
    if (ms_typebinder == null) {
      ms_typebinder = new PSTypeBindingStrategy();
    }
    return ms_typebinder;
  }

  /**
   * Get and if necessary create the suppression strategy singleton
   *
   * @return the singleton, never <code>null</code>
   */
  private static synchronized SuppressionStrategy getSuppressionStrategyInstance() {
    if (ms_supStrategy == null) {
      ms_supStrategy = new SuppressionStrategy();
    }
    return ms_supStrategy;
  }

  /** Bean creator instance, initialized in the getter */
  private static ChainedBeanCreator ms_beanCreator = null;

  /**
   * The object converter does custom conversions from specific internal Rx classes to string
   * representations.
   */
  private static PSBetwixtObjectConverter ms_converter = new PSBetwixtObjectConverter();

  /**
   * Get the bean creator. The bean creator handles the mapping from an element name to a specific
   * instance class. Uses the name mappings registered with {@link #addType(String, Class)} and
   * {@link #addType(Class)}.
   *
   * @return the creator, never <code>null</code>
   */
  private static synchronized ChainedBeanCreator getBeanCreator() {
    if (ms_beanCreator == null) {
      ms_beanCreator =
          new ChainedBeanCreator() {
            public Object create(
                ElementMapping mapping, ReadContext context, BeanCreationChain next) {
              String name = mapping.getName();
              Class implclass = ms_typeMap.get(name);
              if (implclass != null) {
                try {
                  return implclass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                  log.error(
                      "Could not instantiate, Error: {}", PSExceptionUtils.getMessageForLog(e));

                  log.debug(PSExceptionUtils.getDebugMessageForLog(e));

                  throw new RuntimeException(e);
                }
              } else {
                return next.create(mapping, context);
              }
            }
          };
    }
    return ms_beanCreator;
  }

  /**
   * Set up the standard configuration for writing an object with betwixt
   *
   * @param writer the bean writer, assumed never <code>null</code>
   */
  private static void standardBetwixtConfiguration(BeanWriter writer) {
    writer.setXMLIntrospector(createXMLIntrospector());
    writer.enablePrettyPrint();
    writer.getBindingConfiguration().setObjectStringConverter(ms_converter);
    writer.getBindingConfiguration().getIdMappingStrategy().reset();
  }

  /**
   * Set up the standard configuration for reading an object with betwixt
   *
   * @param reader the bean reader, assumed never <code>null</code>
   * @param clazz the class to be read, may be <code>null</code>
   */
  private static void standardBetwixtConfiguration(BeanReader reader, Class clazz) {
    reader.setXMLIntrospector(createXMLIntrospector());
    reader.getBindingConfiguration().setObjectStringConverter(ms_converter);
    try {
      if (clazz != null) {
        reader.registerBeanClass(clazz);
      } else {
        for (Class c : ms_typeMap.values()) {
          reader.registerBeanClass(c);
        }
      }
    } catch (IntrospectionException e1) {
      throw new RuntimeException(e1);
    }
  }

  /**
   * Creation code cribbed from sample in Betwixt
   *
   * @return the singleton inspector instance
   */
  private static XMLIntrospector createXMLIntrospector() {
    XMLIntrospector introspector = new XMLIntrospector();

    IntrospectionConfiguration config = introspector.getConfiguration();
    NameMapper mapper = new PSNameMapper();
    config.setElementNameMapper(mapper);
    config.setAttributeNameMapper(mapper);
    config.setAttributesForPrimitives(false);
    config.setPropertySuppressionStrategy(getSuppressionStrategyInstance());
    config.setTypeBindingStrategy(getTypeBindingStrategyInstance());

    return introspector;
  }

  /**
   * Write the given object to an XML string. Default engine is Jackson ({@link
   * PSJacksonXmlSerializationHelper}); set {@value #ENGINE_PROPERTY}={@value #ENGINE_BETWIXT} for
   * the legacy Betwixt path.
   *
   * <p>Properties that should not be persisted should have the {@link IPSXmlSerialization}
   * annotation added to their <code>get</code> or <code>is</code> methods.
   *
   * <p>Note: methods stay synchronized until Betwixt is removed and concurrency is re-proven
   * (historical Betwixt / BeanUtils threading issues; keep the gate for the dual-engine period).
   *
   * @param object the object to write, never <code>null</code>
   * @return the XML representation of the object
   * @throws IOException if there's a problem writing the object
   * @throws SAXException if there's a problem writing the object (Betwixt path)
   */
  public static synchronized String writeToXml(Object object) throws IOException, SAXException {
    if (object == null) {
      throw new IllegalArgumentException("object may not be null");
    }
    if (isJacksonEngine()) {
      return PSJacksonXmlSerializationHelper.writeToXml(object);
    }
    return writeToXmlBetwixt(object);
  }

  /** Legacy Betwixt write path (rollback only). */
  private static String writeToXmlBetwixt(Object object) throws IOException, SAXException {
    Writer w = new StringWriter();
    BeanWriter writer = new BeanWriter(w);
    standardBetwixtConfiguration(writer);
    try {
      writer.write(object);
    } catch (IntrospectionException e) {
      throw new SAXException(e);
    } finally {
      writer.close();
      w.close();
    }

    return w.toString();
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
   * Read the object's information from the given XML source. Default engine is Jackson; set {@value
   * #ENGINE_PROPERTY}={@value #ENGINE_BETWIXT} for the legacy Betwixt path. If your object has one
   * or more properties that are expressed as abstract classes or interfaces, you must register the
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

    if (isJacksonEngine()) {
      return readFromXMLJackson(xmlString, clazz);
    }
    return readFromXMLBetwixt(xmlString, clazz);
  }

  /**
   * Jackson deserialize path for the public facade.
   *
   * @param xmlString never blank
   * @param clazz target type, may be {@code null} (resolved from root via {@link #ms_typeMap})
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

  /** Legacy Betwixt read path (rollback only). */
  private static Object readFromXMLBetwixt(String xmlString, Class clazz)
      throws IOException, SAXException {
    // Rewrite legacy Betwixt root <null>…</null> to the mapped type element name
    // before parsing. Package payloads (keywords, etc.) were often written with a
    // root of "null"; Betwixt 0.7 only allows one registered name per class, and
    // registerBeanClass(Class) binds the mapped name (e.g. "keyword"), so parse()
    // of a <null> root returns null → "No bean found".
    String parseXml = rewriteLegacyNullRoot(xmlString, clazz);

    Document doc = PSXmlDocumentBuilder.createXmlDocument(new StringReader(parseXml), false);
    String rootName =
        (doc != null && doc.getDocumentElement() != null)
            ? doc.getDocumentElement().getTagName()
            : null;

    SAXParser parser = null;
    Object restored = null;
    try {
      parser = PSSaxHelper.newSAXParser(null);

      BeanReader reader = new BeanReader(parser);

      standardBetwixtConfiguration(reader, clazz);
      Reader r = new StringReader(parseXml);

      BeanCreationList chain = BeanCreationList.createStandardChain();
      chain.insertBeanCreator(1, getBeanCreator());
      reader.getReadConfiguration().setBeanCreationChain(chain);

      restored = reader.parse(r);
    } catch (ParserConfigurationException e) {
      throw new SAXException(
          "No bean found (parser configuration failed"
              + (clazz != null ? " for " + clazz.getName() : "")
              + (rootName != null ? ", root='" + rootName + "'" : "")
              + "): "
              + e.getMessage(),
          e);
    }
    if (restored == null) {
      throw new SAXException(
          "No bean found"
              + (clazz != null ? " for " + clazz.getName() : "")
              + (rootName != null ? ", root element '" + rootName + "'" : "")
              + ". Check that the root element is registered for Betwixt.");
    }
    return restored;
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
    return ms_typeMap.get(rootName);
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
   * is {@code <null id="…">} rather than {@code <keyword>}. Left unchanged, neither Betwixt nor
   * Jackson can bind the root and {@link #readFromXML(String, Class)} fails with "No bean found".
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
