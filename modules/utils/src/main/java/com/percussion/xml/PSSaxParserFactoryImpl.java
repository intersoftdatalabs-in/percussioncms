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
package com.percussion.xml;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Create a parser for use in Percussion. This parser has the entity resolver set to an instance of
 * our entity resolver. This class is currently configured to work with Xerces.
 *
 * <p>Installed as the JVM default via {@code javax.xml.parsers.SAXParserFactory}. Callers such as
 * Commons Digester3 use {@link SAXParserFactory#newInstance()} and then {@link #newSAXParser()}.
 * Digester treats any failure in {@code newSAXParser()} as a null parser (swallowed exception), so
 * this factory must never leave a null underlying factory in the ThreadLocal cache.
 *
 * @author dougrand
 */
public class PSSaxParserFactoryImpl extends SAXParserFactory {

  private static final Logger log = LogManager.getLogger(PSSaxParserFactoryImpl.class);

  /** Preferred Xerces factory class name (matches product security defaults). */
  static final String XERCES_SAX_FACTORY = "org.apache.xerces.jaxp.SAXParserFactoryImpl";

  /**
   * Per-thread secured Xerces factory. Never stores {@code null}: a failed secure init falls back
   * to a plain Xerces/JDK factory so Digester/Velocity Tools do not permanently poison the thread.
   */
  private static final ThreadLocal<SAXParserFactory> factoryThreadLocal =
      ThreadLocal.withInitial(PSSaxParserFactoryImpl::createUnderlyingFactory);

  /**
   * Build the underlying factory used for {@link #newSAXParser()}.
   *
   * @return never {@code null}
   */
  static SAXParserFactory createUnderlyingFactory() {
    try {
      SAXParserFactory factory =
          PSSecureXMLUtils.getSecuredSaxParserFactory(
              XERCES_SAX_FACTORY,
              null,
              PSXmlSecurityOptions.secureWithDtd());
      configureDefaults(factory);
      return factory;
    } catch (Exception e) {
      log.warn(
          "Secure SAXParserFactory init failed ({}); falling back to plain Xerces/JDK factory. {}",
          e.toString(),
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return createFallbackFactory();
    }
  }

  /**
   * Non-secured but usable factory when secure init fails (missing class, bad TCCL, etc.).
   *
   * @return never {@code null}
   */
  static SAXParserFactory createFallbackFactory() {
    try {
      // Explicit class + current TCCL (null CL means context CL in JAXP)
      SAXParserFactory factory = SAXParserFactory.newInstance(XERCES_SAX_FACTORY, null);
      configureDefaults(factory);
      return factory;
    } catch (Exception xercesMissing) {
      log.warn(
          "Xerces SAXParserFactory unavailable ({}); using JAXP platform default.",
          xercesMissing.toString());
      try {
        // Temporarily clear system property so newInstance() does not recurse into this class
        String previous = System.getProperty("javax.xml.parsers.SAXParserFactory");
        try {
          System.clearProperty("javax.xml.parsers.SAXParserFactory");
          SAXParserFactory factory = SAXParserFactory.newInstance();
          configureDefaults(factory);
          return factory;
        } finally {
          if (previous != null) {
            System.setProperty("javax.xml.parsers.SAXParserFactory", previous);
          }
        }
      } catch (Exception platformFail) {
        // Last resort: still must not return null (Digester caches a dead thread otherwise)
        throw new IllegalStateException(
            "Unable to create any SAXParserFactory for PSSaxParserFactoryImpl", platformFail);
      }
    }
  }

  private static void configureDefaults(SAXParserFactory factory) throws Exception {
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    try {
      factory.setFeature("http://xml.org/sax/features/namespaces", true);
      factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
    } catch (SAXNotRecognizedException | SAXNotSupportedException | ParserConfigurationException e) {
      log.debug("Optional SAX feature not supported: {}", e.toString());
    }
  }

  private static SAXParserFactory requireFactory() {
    SAXParserFactory factory = factoryThreadLocal.get();
    if (factory == null) {
      // Defensive: never previously returned null; recover if ThreadLocal was cleared wrongly
      factory = createUnderlyingFactory();
      factoryThreadLocal.set(factory);
    }
    return factory;
  }

  @Override
  public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
    return requireFactory().newSAXParser();
  }

  @Override
  public void setFeature(String name, boolean value)
      throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
    try {
      requireFactory().setFeature(name, value);
    } catch (SAXNotRecognizedException | SAXNotSupportedException e1) {
      // Optional / implementation-specific features are common; keep console clean.
      log.debug("SAX feature not supported by underlying factory: {} — {}", name, e1.getMessage());
    }
  }

  @Override
  public boolean getFeature(String name)
      throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
    try {
      return requireFactory().getFeature(name);
    } catch (SAXException e) {
      log.warn(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getMessageForLog(e), e);
      throw new SAXNotSupportedException(e.getMessage());
    }
  }

  @Override
  public void setNamespaceAware(boolean awareness) {
    try {
      requireFactory().setNamespaceAware(awareness);
    } catch (java.lang.UnsupportedOperationException e) {
      log.warn(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getMessageForLog(e), e);
    }
  }

  @Override
  public void setValidating(boolean validating) {
    try {
      requireFactory().setValidating(validating);
    } catch (java.lang.UnsupportedOperationException e) {
      log.warn(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getMessageForLog(e), e);
    }
  }

  @Override
  public void setXIncludeAware(boolean state) {
    try {
      requireFactory().setXIncludeAware(state);
    } catch (java.lang.UnsupportedOperationException e) {
      log.warn(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getMessageForLog(e), e);
    }
  }
}
