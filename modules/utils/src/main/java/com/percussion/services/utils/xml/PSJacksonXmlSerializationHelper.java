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

import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.std.FromStringDeserializer;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlWriteFeature;

/**
 * Jackson XML serialization engine used by {@link PSXmlSerializationHelper} (production default as
 * of issue #1887 / epic #505 slice 1).
 *
 * <p>Call sites should continue to use the {@link PSXmlSerializationHelper} facade. This helper may
 * also be invoked directly by tests and dual-path diagnostics. Betwixt remains on the classpath for
 * emergency rollback via {@link PSXmlSerializationHelper#ENGINE_PROPERTY}.
 *
 * <p>Naming strategy (see {@link PSXmlElementNameMapper}):
 *
 * <ul>
 *   <li>Root elements: {@link PSXmlElementNameMapper#mapTypeToElementName(String)} (PS/IPS strip +
 *       multi-cap flatten + hyphenation)
 *   <li>Property elements: kebab-case ({@link PropertyNamingStrategies#KEBAB_CASE}) matching
 *       historical Betwixt {@code HyphenatedNameMapper} for properties
 *   <li>Collections: wrapper element enabled by default (Betwixt-style {@code
 *       <choices><choice/>…</choices>})
 * </ul>
 *
 * <p>Legacy package payloads with root {@code <null>} are rewritten via {@link
 * PSXmlSerializationHelper#rewriteLegacyNullRoot(String, Class)} before deserialize.
 *
 * <p><strong>Approved XML deviations vs historical Betwixt writes:</strong> Jackson does not emit
 * Betwixt graph-identity {@code id="…"} attributes on complex elements (values live in child
 * elements).
 *
 * <p>{@link IPSGuid} / {@link PSGuid} use string form (same as historical Betwixt {@code
 * PSBetwixtObjectConverter}) via a registered module (issue #1888 / #1890 / #1891 / epic #505).
 */
public final class PSJacksonXmlSerializationHelper {

  private static final Class<?>[] NOARGS = new Class<?>[0];

  /** Element name → implementation class (mirrors Betwixt type registry for polymorphic items). */
  private static final Map<String, Class<?>> TYPE_MAP = new ConcurrentHashMap<>();

  private static final XmlMapper MAPPER = createMapper();

  private PSJacksonXmlSerializationHelper() {
    // utility
  }

  /**
   * Register an element name to implementation class for polymorphic / collection item binding.
   *
   * @param elementName XML element name, never blank
   * @param type implementation class, never {@code null}
   */
  public static void addType(String elementName, Class<?> type) {
    if (StringUtils.isBlank(elementName)) {
      throw new IllegalArgumentException("elementName may not be null or empty");
    }
    if (type == null) {
      throw new IllegalArgumentException("type may not be null");
    }
    TYPE_MAP.put(elementName, type);
  }

  /**
   * Register a type under its default {@link PSXmlElementNameMapper} element name.
   *
   * @param type class, never {@code null}
   */
  public static void addType(Class<?> type) {
    if (type == null) {
      throw new IllegalArgumentException("type may not be null");
    }
    addType(PSXmlElementNameMapper.mapTypeToElementName(type.getSimpleName()), type);
  }

  /**
   * Serialize {@code object} to XML using Jackson {@link XmlMapper}. Root element name is derived
   * with {@link PSXmlElementNameMapper} (PS/IPS strip + hyphenation).
   *
   * @param object never {@code null}
   * @return XML string, never {@code null}
   * @throws IOException on write failure
   */
  public static String writeToXml(Object object) throws IOException {
    if (object == null) {
      throw new IllegalArgumentException("object may not be null");
    }
    String rootName =
        PSXmlElementNameMapper.mapTypeToElementName(object.getClass().getSimpleName());
    return MAPPER.writer().withRootName(rootName).writeValueAsString(object);
  }

  /**
   * Deserialize XML into a new instance of {@code clazz}. Accepts legacy root {@code <null>} when
   * {@code clazz} is non-null (same rewrite as Betwixt path).
   *
   * @param xmlString never blank
   * @param clazz target type, never {@code null}
   * @return restored instance, never {@code null}
   * @throws IOException on parse failure
   */
  public static <T> T readFromXml(String xmlString, Class<T> clazz) throws IOException {
    if (StringUtils.isBlank(xmlString)) {
      throw new IllegalArgumentException("xmlString may not be null or empty");
    }
    if (clazz == null) {
      throw new IllegalArgumentException("clazz may not be null");
    }
    String parseXml = PSXmlSerializationHelper.rewriteLegacyNullRoot(xmlString, clazz);
    return MAPPER.readValue(parseXml, clazz);
  }

  /**
   * Deserialize into {@code target} by reading a new instance of its class and copying properties
   * (BeanUtils), matching {@link PSXmlSerializationHelper#readFromXML(String, Object)}.
   *
   * @param xmlString never blank
   * @param target never {@code null}
   * @return {@code target} after property copy
   * @throws IOException on parse failure
   */
  public static Object readFromXml(String xmlString, Object target) throws IOException {
    if (target == null) {
      throw new IllegalArgumentException("target may not be null");
    }
    Object restored = readFromXml(xmlString, target.getClass());
    try {
      BeanUtils.copyProperties(target, restored);
      return target;
    } catch (Exception e) {
      throw new IOException("Error copying bean properties after Jackson XML read", e);
    }
  }

  /**
   * Shared mapper for tests that need advanced configuration without forking product defaults.
   *
   * @return the process-wide {@link XmlMapper}, never {@code null}
   */
  public static XmlMapper getMapper() {
    return MAPPER;
  }

  /**
   * Snapshot of registered element→type entries (for tests and diagnostics).
   *
   * @return unmodifiable copy, never {@code null}
   */
  public static Map<String, Class<?>> typeMapView() {
    return Map.copyOf(TYPE_MAP);
  }

  private static XmlMapper createMapper() {
    SimpleModule suppressModule = new SimpleModule("ps-ips-xml-serialization-suppress");
    suppressModule.setSerializerModifier(new IpsXmlSuppressionModifier());

    SimpleModule guidModule = new SimpleModule("ps-ips-guid-string");
    // Match Betwixt PSBetwixtObjectConverter: IPSGuid ↔ toString / new PSGuid(String)
    guidModule.addSerializer(IPSGuid.class, new ToStringSerializer(IPSGuid.class));
    guidModule.addSerializer(PSGuid.class, new ToStringSerializer(PSGuid.class));
    guidModule.addDeserializer(IPSGuid.class, new IpsGuidFromStringDeserializer());
    guidModule.addDeserializer(PSGuid.class, new PsGuidFromStringDeserializer());

    return XmlMapper.builder()
        .defaultUseWrapper(true)
        .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
        .annotationIntrospector(new PsJacksonXmlAnnotationIntrospector())
        .addModule(suppressModule)
        .addModule(guidModule)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        // Betwixt pretty-prints; keep Jackson pretty for readable golden comparison
        .enable(SerializationFeature.INDENT_OUTPUT)
        // Avoid empty XML declaration quirks when comparing to Betwixt body fragments
        .disable(XmlWriteFeature.WRITE_XML_DECLARATION)
        .build();
  }

  /**
   * Deserializes package/design {@code <guid>} string values into {@link IPSGuid}, matching {@link
   * PSBetwixtObjectConverter#stringToObject}.
   */
  static final class IpsGuidFromStringDeserializer extends FromStringDeserializer<IPSGuid> {
    private static final long serialVersionUID = 1L;

    IpsGuidFromStringDeserializer() {
      super(IPSGuid.class);
    }

    @Override
    protected IPSGuid _deserialize(String value, DeserializationContext ctxt)
        throws JacksonException {
      if (StringUtils.isBlank(value)) {
        return null;
      }
      try {
        return new PSGuid(value.trim());
      } catch (RuntimeException ex) {
        return (IPSGuid)
            ctxt.handleWeirdStringValue(
                IPSGuid.class, value, "not a valid PSGuid string: %s", ex.getMessage());
      }
    }
  }

  /** Concrete {@link PSGuid} string deserializer (same wire form as {@link IPSGuid}). */
  static final class PsGuidFromStringDeserializer extends FromStringDeserializer<PSGuid> {
    private static final long serialVersionUID = 1L;

    PsGuidFromStringDeserializer() {
      super(PSGuid.class);
    }

    @Override
    protected PSGuid _deserialize(String value, DeserializationContext ctxt)
        throws JacksonException {
      if (StringUtils.isBlank(value)) {
        return null;
      }
      try {
        return new PSGuid(value.trim());
      } catch (RuntimeException ex) {
        return (PSGuid)
            ctxt.handleWeirdStringValue(
                PSGuid.class, value, "not a valid PSGuid string: %s", ex.getMessage());
      }
    }
  }

  /**
   * Jackson XML annotation introspector (keeps {@code @JacksonXml*} working) that also honors
   * {@link IPSXmlSerialization#suppress()} on getters / {@code is} methods and ignores the
   * JavaBeans {@code class} property.
   */
  static final class PsJacksonXmlAnnotationIntrospector extends JacksonXmlAnnotationIntrospector {

    private static final long serialVersionUID = 1L;

    PsJacksonXmlAnnotationIntrospector() {
      super(true); // defaultUseWrapper = true (Betwixt-style collection wrappers)
    }

    @Override
    public boolean hasIgnoreMarker(MapperConfig<?> config, AnnotatedMember m) {
      if (super.hasIgnoreMarker(config, m)) {
        return true;
      }
      String name = m.getName();
      if ("class".equalsIgnoreCase(name) || "getClass".equals(name)) {
        return true;
      }
      Method method = extractMethod(m);
      if (method != null) {
        IPSXmlSerialization ann = method.getAnnotation(IPSXmlSerialization.class);
        if (ann != null && ann.suppress()) {
          return true;
        }
      }
      return false;
    }

    private static Method extractMethod(AnnotatedMember m) {
      try {
        Object raw = m.getMember();
        if (raw instanceof Method method) {
          return method;
        }
      } catch (Exception ignored) {
        // fall through
      }
      return null;
    }
  }

  /**
   * Serializer modifier backup for suppress annotations discovered only on getter methods when the
   * introspector path does not see them (defensive; primary path is the introspector).
   */
  static final class IpsXmlSuppressionModifier extends ValueSerializerModifier {
    private static final long serialVersionUID = 1L;

    @Override
    public List<BeanPropertyWriter> changeProperties(
        SerializationConfig config,
        BeanDescription.Supplier beanDesc,
        List<BeanPropertyWriter> beanProperties) {
      beanProperties.removeIf(
          writer -> {
            String prop = writer.getName();
            if ("class".equalsIgnoreCase(prop)) {
              return true;
            }
            AnnotatedMember member = writer.getMember();
            Method method = null;
            if (member != null) {
              Object raw = member.getMember();
              if (raw instanceof Method m) {
                method = m;
              }
            }
            if (method == null) {
              method = findGetter(beanDesc.getBeanClass(), prop);
            }
            if (method != null) {
              IPSXmlSerialization ann = method.getAnnotation(IPSXmlSerialization.class);
              return ann != null && ann.suppress();
            }
            return false;
          });
      return beanProperties;
    }

    private static Method findGetter(Class<?> beanClass, String prop) {
      if (beanClass == null || StringUtils.isBlank(prop)) {
        return null;
      }
      String camel = prop.contains("-") ? kebabToCamel(prop) : prop;
      String capCamel = StringUtils.capitalize(camel);
      for (String name : new String[] {"get" + capCamel, "is" + capCamel}) {
        try {
          return beanClass.getMethod(name, NOARGS);
        } catch (NoSuchMethodException ignored) {
          // try next
        }
      }
      return null;
    }

    private static String kebabToCamel(String kebab) {
      StringBuilder b = new StringBuilder();
      boolean up = false;
      for (int i = 0; i < kebab.length(); i++) {
        char ch = kebab.charAt(i);
        if (ch == '-') {
          up = true;
        } else if (up) {
          b.append(Character.toUpperCase(ch));
          up = false;
        } else {
          b.append(ch);
        }
      }
      return b.toString();
    }
  }
}
