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

package com.percussion.xml;

import java.net.URI;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import org.xml.sax.XMLFilter;

/**
 * Custom TransformerFactory implementation that provides additional functionality while maintaining
 * compatibility with Java 11+.
 */
public class PSTransformerFactoryImpl extends SAXTransformerFactory {

  private final SAXTransformerFactory delegate;

  private void forceResolver() {
    // noop
  }

  /** Constructor TransformerFactoryImpl */
  public PSTransformerFactoryImpl() {
    // Use the SAXTransformerFactory implementation instead of internal APIs
    this.delegate = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    forceResolver();
  }

  /**
   * Get InputSource specification(s) that are associated with the given document specified in the
   * source param, via the xml-stylesheet processing instruction (see <a
   * href="https://www.w3.org/TR/xml-stylesheet/">https://www.w3.org/TR/xml-stylesheet</a>), and
   * that matches the given criteria. Note that it is possible to return several stylesheets that
   * match the criteria, in which case they are applied as if they were a list of imports or
   * cascades.
   *
   * <p>Note that DOM2 has it's own mechanism for discovering stylesheets. Therefore, there isn't a
   * DOM version of this method.
   *
   * @param source The XML source that is to be searched.
   * @param media The media attribute to be matched. May be null, in which case the preferred
   *     templates will be used (i.e. alternate = no).
   * @param title The value of the title attribute to match. May be null.
   * @param charset The value of the charset attribute to match. May be null.
   * @return A Source object capable of being used to create a Templates object.
   * @throws TransformerConfigurationException
   */
  public Source getAssociatedStylesheet(Source source, String media, String title, String charset)
      throws TransformerConfigurationException {
    forceResolver();
    return delegate.getAssociatedStylesheet(source, media, title, charset);
  }

  /**
   * Create a new Transformer object that performs a copy of the source to the result.
   *
   * @return A Transformer object that may be used to perform a transformation in a single thread,
   *     never null.
   * @throws TransformerConfigurationException May throw this during the parse when it is
   *     constructing the Templates object and fails.
   */
  public TemplatesHandler newTemplatesHandler() throws TransformerConfigurationException {
    forceResolver();
    return delegate.newTemplatesHandler();
  }

  /**
   * Set a feature for this <code>TransformerFactory</code> and <code>Transformer</code>s or <code>
   * Template</code>s created by this factory.
   *
   * <p>Feature names are fully qualified {@link URI}s. Implementations may define their own
   * features. An {@link TransformerConfigurationException} is thrown if this <code>
   * TransformerFactory</code> or the <code>Transformer</code>s or <code>Template</code>s it creates
   * cannot support the feature. It is possible for an <code>TransformerFactory</code> to expose a
   * feature value but be unable to change its state.
   *
   * <p>See {@link TransformerFactory} for full documentation of specific features.
   *
   * @param name Feature name.
   * @param value Is feature state <code>true</code> or <code>false</code>.
   * @throws TransformerConfigurationException if this <code>TransformerFactory</code> or the <code>
   *     Transformer</code>s or <code>Template</code>s it creates cannot support this feature.
   * @throws NullPointerException If the <code>name</code> parameter is null.
   */
  public void setFeature(String name, boolean value) throws TransformerConfigurationException {
    delegate.setFeature(name, value);
  }

  /**
   * Look up the value of a feature.
   *
   * <p>The feature name is any fully-qualified URI. It is possible for an TransformerFactory to
   * recognize a feature name but to be unable to return its value; this is especially true in the
   * case of an adapter for a SAX1 Parser, which has no way of knowing whether the underlying parser
   * is validating, for example.
   *
   * @param name The feature name, which is a fully-qualified URI.
   * @return The current state of the feature (true or false).
   */
  public boolean getFeature(String name) {
    return delegate.getFeature(name);
  }

  /**
   * Allows the user to set specific attributes on the underlying implementation.
   *
   * @param name The name of the attribute.
   * @param value The value of the attribute; Boolean or String="true"|"false"
   * @throws IllegalArgumentException thrown if the underlying implementation doesn't recognize the
   *     attribute.
   */
  public void setAttribute(String name, Object value) throws IllegalArgumentException {
    delegate.setAttribute(name, value);
  }

  /**
   * Allows the user to retrieve specific attributes on the underlying implementation.
   *
   * @param name The name of the attribute.
   * @return value The value of the attribute.
   * @throws IllegalArgumentException thrown if the underlying implementation doesn't recognize the
   *     attribute.
   */
  public Object getAttribute(String name) throws IllegalArgumentException {
    return delegate.getAttribute(name);
  }

  /**
   * Create an XMLFilter that uses the given source as the transformation instructions.
   *
   * @param src The source of the transformation instructions.
   * @return An XMLFilter object, or null if this feature is not supported.
   * @throws TransformerConfigurationException
   */
  public XMLFilter newXMLFilter(Source src) throws TransformerConfigurationException {
    return delegate.newXMLFilter(src);
  }

  /**
   * Create an XMLFilter that uses the given source as the transformation instructions.
   *
   * @param templates non-null reference to Templates object.
   * @return An XMLFilter object, or null if this feature is not supported.
   * @throws TransformerConfigurationException
   */
  public XMLFilter newXMLFilter(Templates templates) throws TransformerConfigurationException {
    return delegate.newXMLFilter(templates);
  }

  /**
   * Get a TransformerHandler object that can process SAX ContentHandler events into a Result, based
   * on the transformation instructions specified by the argument.
   *
   * @param src The source of the transformation instructions.
   * @return TransformerHandler ready to transform SAX events.
   * @throws TransformerConfigurationException
   */
  public TransformerHandler newTransformerHandler(Source src)
      throws TransformerConfigurationException {
    return delegate.newTransformerHandler(src);
  }

  /**
   * Get a TransformerHandler object that can process SAX ContentHandler events into a Result, based
   * on the Templates argument.
   *
   * @param templates The source of the transformation instructions.
   * @return TransformerHandler ready to transform SAX events.
   * @throws TransformerConfigurationException
   */
  public TransformerHandler newTransformerHandler(Templates templates)
      throws TransformerConfigurationException {
    return delegate.newTransformerHandler(templates);
  }

  /**
   * Get a TransformerHandler object that can process SAX ContentHandler events into a Result.
   *
   * @return TransformerHandler ready to transform SAX events.
   * @throws TransformerConfigurationException
   */
  public TransformerHandler newTransformerHandler() throws TransformerConfigurationException {
    return delegate.newTransformerHandler();
  }

  /**
   * Process the source into a Transformer object. Care must be given to know that this object can
   * not be used concurrently in multiple threads.
   *
   * @param source An object that holds a URL, input stream, etc.
   * @return A Transformer object capable of being used for transformation purposes in a single
   *     thread.
   * @throws TransformerConfigurationException May throw this during the parse when it is
   *     constructing the Templates object and fails.
   */
  public Transformer newTransformer(Source source) throws TransformerConfigurationException {
    return delegate.newTransformer(source);
  }

  /**
   * Create a new Transformer object that performs a copy of the source to the result.
   *
   * @return A Transformer object capable of being used for transformation purposes in a single
   *     thread.
   * @throws TransformerConfigurationException May throw this during the parse when it is
   *     constructing the Templates object and it fails.
   */
  public Transformer newTransformer() throws TransformerConfigurationException {

    return delegate.newTransformer();
  }

  /**
   * Process the source into a Templates object, which is likely a compiled representation of the
   * source. This Templates object may then be used concurrently across multiple threads. Creating a
   * Templates object allows the TransformerFactory to do detailed performance optimization of
   * transformation instructions, without penalizing runtime transformation.
   *
   * @param source An object that holds a URL, input stream, etc.
   * @return A Templates object capable of being used for transformation purposes.
   * @throws TransformerConfigurationException May throw this during the parse when it is
   *     constructing the Templates object and fails.
   */
  public Templates newTemplates(Source source) throws TransformerConfigurationException {
    forceResolver();
    return delegate.newTemplates(source);
  }

  /**
   * Set an object that will be used to resolve URIs used in xsl:import, etc. This will be used as
   * the default for the transformation.
   *
   * @param resolver An object that implements the URIResolver interface, or null.
   */
  public void setURIResolver(URIResolver resolver) {
    delegate.setURIResolver(resolver);
  }

  /**
   * Get the object that will be used to resolve URIs used in xsl:import, etc. This will be used as
   * the default for the transformation.
   *
   * @return The URIResolver that was set with setURIResolver.
   */
  public URIResolver getURIResolver() {
    return delegate.getURIResolver();
  }

  /**
   * Get the error listener in effect for the TransformerFactory.
   *
   * @return A non-null reference to an error listener.
   */
  public ErrorListener getErrorListener() {
    return delegate.getErrorListener();
  }

  /**
   * Set an error listener for the TransformerFactory.
   *
   * @param listener Must be a non-null reference to an ErrorListener.
   * @throws IllegalArgumentException if the listener argument is null.
   */
  public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {
    delegate.setErrorListener(listener);
  }
}
