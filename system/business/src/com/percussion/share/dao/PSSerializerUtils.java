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
package com.percussion.share.dao;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Various serializing/marshalling static methods.
 * <p>
 * Also has utilities for data conversion and copying of data
 * from one object to another.
 *
 * @author adamgent
 *
 */
public class PSSerializerUtils
{

    /**
     * The standard marshalling of objects is currently done with JAXB.
     * @param <T> object type
     * @param object  never <code>null</code>.
     * @return never <code>null</code>.
     */
    public static <T> String marshal(T object) {
        var sw = new StringWriter();
        try {
            PSJaxbContext.createMarshaller(object.getClass()).marshal(object, sw);
            return sw.toString();
        } catch (JAXBException e) {
            log.error("Unable to marshall JAXB object: {} Error: {}", object, PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return null;
        }
    }

    /**
     * See {@link #marshal(Object)}.
     * This should be used lightly as it does not have validation.
     * @param <T>
     * @param dataField never <code>null</code>.
     * @param type never <code>null</code>.
     * @return never <code>null</code>.
     */
    public static <T> T unmarshal(String dataField, Class<T> type) {
        try {
            var inputStream = new ByteArrayInputStream(dataField.getBytes(StandardCharsets.UTF_8));
            var unmarshaller = Objects.requireNonNull(PSJaxbContext.createUnmarshaller(type));

            // The SAXSource is built from a secured SAXParserFactory AND a
            // secured XMLReader (see PSSecureXMLUtils.getSecuredSaxSource,
            // which sets both the factory features AND explicitly re-sets
            // the same features on the XMLReader via setFeatureSafe for
            // defense-in-depth). Both disallow-doctype-decl=true and all
            // external-entity features are disabled. External entity
            // references in the input are rejected at the parser level
            // before they reach the unmarshaller.
            //
            // The inline // codeql[java/xxe] suppression at the END of the
            // next line (the unmarshaller.unmarshal call) is required
            // because CodeQL's data-flow analysis still flags that line
            // as a taint sink even though the source IS sanitized. This
            // is a documented CodeQL false positive per contracts/C2.
            // The matching row in suppressions.md (alert_id=2) tracks this
            // exception. See specs/004-zero-code-scanning-alerts/tasks.md
            // T039 and GitHub code-scanning advisory #1709.
            Source source = PSSecureXMLUtils.getSecuredSaxSource(inputStream);
            @SuppressWarnings("unchecked")
            T object = (T) unmarshaller.unmarshal(source); // codeql[java/xxe] false positive: XMLReader has disallow-doctype-decl=true and all external-entity features disabled via PSSecureXMLUtils.setFeatureSafe; see T039 and advisory #1709
            return object;
        } catch (JAXBException e) {
            log.error("Unable to load XML file. Check for syntax problems. Error: {}, Data: {}", PSExceptionUtils.getMessageForLog(e), dataField);
            return null;
        } catch (RuntimeException e) {
            // Re-throw unchecked exceptions (NPE, IllegalArgumentException,
            // ClassCastException, etc.) so that misconfigurations like a
            // null PSJaxbContext (from Objects.requireNonNull at line 89)
            // or a missing JAXB context factory surface to the caller
            // instead of being silently swallowed as a null return. Per
            // the review on PR #1199.
            throw e;
        } catch (Exception e) {
            // The SAX parser construction can fail in restricted environments
            // (ParserConfigurationException, SAXException, etc.); surface
            // as a parse failure (null return) rather than propagating.
            log.error("Unable to construct secured SAX parser for unmarshal. Error: {}, Data: {}", PSExceptionUtils.getMessageForLog(e), dataField);
            return null;
        }
    }


    /**
     * Unmarshal an XML stream into an Object validating against its schema.
     * <p>
     * The schema is assumed to be in the same java class package as the type parameter
     * with the same name but ending in <code>.xsd</code>
     * <p>
     * <b>Example:</b>
     * <p>
     * <b>Class:</b> <code>com.percussion.Stuff.class</code><p><b>Schema:</b> <code>com.percussion.Stuff.xsd</code>
     * <p>
     * Note: This requires the schema file to be put into the jar when deployed.
     * @param <T> type to unmarshal
     * @param stream never <code>null</code>.
     * @param type never <code>null</code>.
     * @return never <code>null</code>.
     * @throws Exception
     */
    public static <T> T unmarshalWithValidation(InputStream stream, Class<T> type) throws Exception {
        notNull(stream, "stream");
        notNull(type, "type");
        var schemaFactory = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        var source = new StreamSource(type.getResourceAsStream(type.getSimpleName() + ".xsd"));
        var schema = schemaFactory.newSchema(source);
        var unmarshaller = PSJaxbContext.createUnmarshaller(type);
        unmarshaller.setSchema(schema);

        // See the long justification in unmarshal() above.
        Source secureSource = PSSecureXMLUtils.getSecuredSaxSource(stream);
        @SuppressWarnings("unchecked")
        T result = (T) unmarshaller.unmarshal(secureSource); // codeql[java/xxe] false positive: XMLReader has disallow-doctype-decl=true and all external-entity features disabled via PSSecureXMLUtils.setFeatureSafe; see T039 and advisory #1709
        return result;
    }

    /**
     * Will convert an object into an XML representation but in JSON format.
     * This is useful for determining what the REST services JSON output of an object is.
     * <p>
     * This is <strong>not</strong> a object->JSON conversion but rather a
     * object->XML->JSON conversion.
     *
     * @param <T> object type.
     * @param object
     * @return a JSON object representing an XML document.
     * @throws Exception Cannot marshal the object.
     */
    public static <T> String getJsonXmlFromObject(T object) throws Exception {
        var sw = new StringWriter();
        var context = JAXBContext.newInstance(object.getClass());
        var marshaller = context.createMarshaller();
        var convention = new MappedNamespaceConvention();
        var xw = new MappedXMLStreamWriter(convention, sw);
        marshaller.marshal(object, xw);
        return sw.getBuffer().toString();
    }

    public static <T> List<T> copyFullToSummaries(List<? extends T> froms, Class<T> type) {
        var newList = new ArrayList<T>();
        for (var from : froms) {
            T sum;
            try {
                sum = type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            copyFullToSummary(from, sum);
            newList.add(sum);
        }
        return newList;
    }

    public static <T> void copyFullToSummary(T from, T to) {
        try {
            BeanUtils.copyProperties(to, from);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Does a clone of an object using bean reflection.
     * <p>
     * The object should following the java bean standards and
     * have a empty constructor.
     * @param <T> Type of object.
     * @param from object to clone from. never <code>null</code>.
     * @return never <code>null</code>.
     */
    public static <T> T clone(T from) {
        var err = "Cannot clone bean";
        try {

            T result = (T) BeanUtils.cloneBean(from);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(err, e);
        }
    }

    /**
     * Parses a simple JSON string turning it into a native Java object.
     * Example valid JSON:
     * <pre>
     * "a" // string
     *  1 // number
     * { "a" : 1, "b" : 2 } // object
     * ['a','b'] //list
     * </pre>
     * <strong>Blank and empty strings will return null</strong>
     * <em>
     * This should be used for simple JSON values and not complex objects.
     * Use either JAXB or jackson for complex values.
     * </em>
     * @param json either a JSON string, number, array or object.
     * @return either a list, number, string, map or <code>null</code>.
     */
    public static Object getObjectFromJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return null;
            }
            var mapper = JsonMapper.builder().build();
            String wrappedJson = '[' + json + ']';
            var array = mapper.readValue(wrappedJson, List.class);
            if (array.isEmpty()) {
                return null;
            }
            var first = array.get(0);
            if (first instanceof List<?>) {
                return first;
            } else if (first instanceof java.util.Map<?, ?>) {
                return first;
            } else if (first == null) {
                return null;
            } else {
                return first;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Bad json string: {}", json);
            }
            return json;
        }
    }

    /**
     * The inverse of {@link #getObjectFromJson(String)}.
     * @param obj a bean, list, number, or string, never <code>null</code>.
     * @return never <code>null</code>.
     */
    public static String getJsonFromObject(Object obj) {
        try {
            var mapper = JsonMapper.builder().build();
            var wrappedList = Collections.singletonList(obj);
            String jsonList = mapper.writeValueAsString(wrappedList);
            // Remove surrounding [ and ]
            String data = jsonList.substring(1, jsonList.length() - 1);
            return data;
        } catch (Exception e) {
            log.error("Error serializing object to JSON: {}", PSExceptionUtils.getMessageForLog(e));
            // Fallback to toString
            return obj.toString();
        }
    }

    /**
     * The log instance to use for this class, never <code>null</code>.
     */
    private static final Logger log = LogManager.getLogger(PSSerializerUtils.class);

}
