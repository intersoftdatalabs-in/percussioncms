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
package com.percussion.pso.preview;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.Iterator;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Like PSAction but without all the PSDbComponent baggage
 *
 * @author DavidBenua
 */
public class PSOAction implements Comparable<PSOAction> {

  private String handler;
  private String label;
  private String name;
  private String url;
  private String type;
  private int sortrank;
  private String description;
  private Properties properties;

  /**
   * Default constructor
   * Creates a new PSOAction.
   *
   */
  public PSOAction() {}

  /**
   * See referenced member.
   * @see Comparable#compareTo(Object)
   * @param other the other
   * @return the result
   */
  public int compareTo(PSOAction other) {
    if (this == other) return 0;
    return this.label.compareTo(other.label);
  }

  /**
   * See referenced member.
   * @see Object#equals(Object)
   * @param obj the obj
   * @return the result
   */
  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  /**
   * hashCode operation.
   *
   * @return the result
   */
  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * toXml operation.
   *
   * @param doc the doc
   * @return the result
   */
  @SuppressWarnings("unused")
  public Element toXml(Document doc) {
    Element root = doc.createElement("Action");
    if (doc == null) throw new IllegalArgumentException("doc may not be null.");

    root.setAttribute(NAME_ATTR, name);
    root.setAttribute(LABEL_ATTR, label);
    root.setAttribute(TYPE_ATTR, type);

    root.setAttribute(URL_ATTR, url);
    root.setAttribute(HANDLER_ATTR, handler);
    root.setAttribute(SORTRANK_ATTR, String.valueOf(sortrank));
    if (StringUtils.isNotBlank(description)) {
      PSXmlDocumentBuilder.addElement(doc, root, "Description", description);
    }

    if (properties != null && (!properties.isEmpty())) {
      Element props = PSXmlDocumentBuilder.addEmptyElement(doc, root, "Props");
      Iterator<?> propitr = properties.keySet().iterator();
      while (propitr.hasNext()) {
        String key = (String) propitr.next();
        String value = properties.getProperty(key);
        Element prop = PSXmlDocumentBuilder.addElement(doc, props, "Prop", value);
        prop.setAttribute("propid", "0");
        prop.setAttribute("name", key);
      }
    }

    return root;
  }

  /**
   * Returns the handler.
   * @return the handler
   */
  public String getHandler() {
    return handler;
  }

  /**
   * Sets the handler.
   * @param handler the handler to set
   */
  public void setHandler(String handler) {
    this.handler = handler;
  }

  /**
   * Returns the label.
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Sets the label.
   * @param label the label to set
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Returns the name.
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the url.
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the url.
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Returns the type.
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Returns the sortrank.
   * @return the sortrank
   */
  public int getSortrank() {
    return sortrank;
  }

  /**
   * Sets the sortrank.
   * @param sortrank the sortrank to set
   */
  public void setSortrank(int sortrank) {
    this.sortrank = sortrank;
  }

  /**
   * Returns the description.
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the properties.
   * @return the properties
   */
  public Properties getProperties() {
    return properties;
  }

  /**
   * Sets the properties.
   * @param properties the properties to set
   */
  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  private static final String LABEL_ATTR = "label";
  private static final String NAME_ATTR = "name";
  private static final String TYPE_ATTR = "type";
  private static final String URL_ATTR = "url";
  private static final String HANDLER_ATTR = "handler";
  private static final String SORTRANK_ATTR = "sortrank";
}
