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

package com.percussion.delivery.metadata;

import java.util.Set;

/**
 * A single indexed metadata entry representing one published page in the DTS metadata indexer. An
 * entry exposes the identifying fields of the page (name, folder, pagepath, link text, type and
 * site) along with a collection of {@link IPSMetadataProperty} values that capture the indexed
 * Dublin Core and Percussion metadata.
 */
public interface IPSMetadataEntry {

  /**
   * Returns the page name.
   *
   * @return the page name, may be <code>null</code>.
   */
  public String getName();

  /**
   * Sets the page name.
   *
   * @param name the page name to set; may be <code>null</code>.
   */
  public void setName(String name);

  /**
   * Returns the folder the page lives under.
   *
   * @return the folder path, may be <code>null</code>.
   */
  public String getFolder();

  /**
   * Sets the folder the page lives under.
   *
   * @param folder the folder path to set; may be <code>null</code>.
   */
  public void setFolder(String folder);

  /**
   * Returns the page path.
   *
   * @return the page path, may be <code>null</code>.
   */
  public String getPagepath();

  /**
   * Sets the pagepath.
   *
   * @param path the pagepath to set; may be <code>null</code>.
   */
  public void setPagepath(String path);

  /**
   * Returns the link text associated with the page.
   *
   * @return the link text, may be <code>null</code>.
   */
  public String getLinktext();

  /**
   * Sets the link text associated with the page.
   *
   * @param linktext the link text to set; may be <code>null</code>.
   */
  public void setLinktext(String linktext);

  /**
   * Returns the content type of the page.
   *
   * @return the type, may be <code>null</code>.
   */
  public String getType();

  /**
   * Sets the content type of the page.
   *
   * @param type the content type to set; may be <code>null</code>.
   */
  public void setType(String type);

  /**
   * Returns the site the page belongs to.
   *
   * @return the site name, may be <code>null</code>.
   */
  public String getSite();

  /**
   * Sets the site the page belongs to.
   *
   * @param site the site name to set; may be <code>null</code>.
   */
  public void setSite(String site);

  /**
   * Returns the properties attached to this entry. This returns a cloned set of properties changing
   * the value of these directly will not affect the property values in the entry. To change
   * property values on the entry you must passed the properties back to the entries {@link
   * #setProperties(Set)} method.
   *
   * @return the properties, never <code>null</code>, may be empty.
   */
  public Set<IPSMetadataProperty> getProperties();

  /**
   * Replaces the properties attached to this entry.
   *
   * @param properties the properties to set; may be <code>null</code>.
   */
  public void setProperties(Set<IPSMetadataProperty> properties);

  /**
   * Adds a single property to the entry's property set.
   *
   * @param prop the property to add; may be <code>null</code>.
   */
  public void addProperty(IPSMetadataProperty prop);

  /** Clears all properties from the entry. */
  public void clearProperties();
}
