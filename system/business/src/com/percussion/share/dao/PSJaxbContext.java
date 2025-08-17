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

import com.percussion.security.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java 11 refactored: Thread-safe singleton/context manager for JAXB marshallers and unmarshallers.
 * <p>
 * Uses thread-local storage for marshaller/unmarshaller instances and a concurrent map for singleton context per class.
 * <p>
 * All methods are static and thread-safe. Callers must use the static get() method for instance retrieval.
 */
public class PSJaxbContext {
    private static final Logger log = LogManager.getLogger(PSJaxbContext.class);

    // Singleton pattern: one instance per class (Java 11 generics)
    private static final Map<Class<?>, PSJaxbContext> singletonMap = new ConcurrentHashMap<>();

    private final Class<?> clazz;

    // Thread-local pattern: one marshaller/unmarshaller instance per thread
    private final ThreadLocal<Marshaller> marshallerThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<Unmarshaller> unmarshallerThreadLocal = new ThreadLocal<>();

    /**
     * Returns the singleton PSJaxbContext for the given class.
     * Thread-safe and uses Java 11 features.
     */
    public static PSJaxbContext get(Class<?> clazz) {
        return singletonMap.computeIfAbsent(clazz, k -> new PSJaxbContext(k));
    }

    // Private constructor: use get() for instance creation
    private PSJaxbContext(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * Creates a new JAXB Marshaller for the given class.
     * @param aClass the class to marshal
     * @return a new Marshaller instance, or null if creation fails
     */
    public static Marshaller createMarshaller(Class<?> aClass) {
        try {
            var m = get(aClass).createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            return m;
        } catch (JAXBException e) {
            log.error("FATAL... Unable to create JAXB Marshaller: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return null;
        }
    }

    /**
     * Creates a new JAXB Unmarshaller for the given class.
     * @param aClass the class to unmarshal
     * @return a new Unmarshaller instance, or null if creation fails
     */
    public static Unmarshaller createUnmarshaller(Class<?> aClass) {
        try {
            return get(aClass).createUnmarshaller();
        } catch (JAXBException e) {
            log.error("FATAL... Unable to create JAXB Unmarshaller: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return null;
        }
    }

    /**
     * Gets/Creates a marshaller (thread-safe)
     * @throws JAXBException if marshaller creation fails
     */
    public Marshaller createMarshaller() throws JAXBException {
        var m = marshallerThreadLocal.get();
        if (m == null) {
            var jc = JAXBContext.newInstance(clazz);
            m = jc.createMarshaller();
            marshallerThreadLocal.set(m);
        }
        return m;
    }

    /**
     * Gets/Creates an unmarshaller (thread-safe)
     * @throws JAXBException if unmarshaller creation fails
     */
    public Unmarshaller createUnmarshaller() throws JAXBException {
        var um = unmarshallerThreadLocal.get();
        if (um == null) {
            var jc = JAXBContext.newInstance(clazz);
            um = jc.createUnmarshaller();
            unmarshallerThreadLocal.set(um);
        }
        return um;
    }
}
