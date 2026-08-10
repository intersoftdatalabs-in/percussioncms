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

package com.percussion.cms.objectstore;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The PSComponentSummaries is a container class which contains a set of PSComponentSummary objects
 */
public final class PSComponentSummaries extends PSDbComponentSet<PSComponentSummary> {
  /** Default constructor. */
  public PSComponentSummaries() {
    super(PSComponentSummary.class);
  }

  /**
   * Ctor that takes an array of PSComponentSummary objects.
   *
   * @param compArray array of PSComponentSummary objects, never <code>null</code> may be <code>
   *     empty</code>.
   */
  public PSComponentSummaries(PSComponentSummary[] compArray) {
    super(PSComponentSummary.class);

    if (compArray == null) throw new IllegalArgumentException("compArray may not be null");

    addAll(Arrays.asList(compArray));
  }

  /**
   * Creates an instance from a an array of Elements that was created by a sequence of calls to
   * PSComponentSummary.toXML();
   *
   * @param source A valid array of elements that meet the dtd defined in the PSComponentSummary of
   *     {@link PSComponentSummary#toXml(Document)}, never <code>null</code>.
   * @throws PSUnknownNodeTypeException If the supplied source element does not conform to the dtd
   *     defined in the <code>fromXml</code> method.
   */
  public PSComponentSummaries(Element[] source) throws PSUnknownNodeTypeException {
    super(PSComponentSummary.class);

    for (int i = 0; i < source.length; i++) super.add(new PSComponentSummary(source[i]));
  }

  /**
   * Creates an instance from a list of Elements that was created by a sequence of calls to
   * PSComponentSummary.toXML();
   *
   * @param source A valid list of {@link Element} objects that meet the dtd defined in the
   *     PSComponentSummary of {@link PSComponentSummary#toXml(Document)}, never <code>null</code>.
   * @throws PSUnknownNodeTypeException If the supplied source element does not conform to the dtd
   *     defined in the <code>fromXml</code> method.
   */
  public PSComponentSummaries(List<?> source) throws PSUnknownNodeTypeException {
    super(PSComponentSummary.class);

    for (Object elem : source) {
      if (!(elem instanceof Element))
        throw new IllegalArgumentException("source must contain a list of Element objects");

      try {
        super.add(new PSComponentSummary((Element) elem));
      } catch (Exception e) {
        throw new PSUnknownNodeTypeException(0, e.toString());
      }
    }
  }

  /**
   * Creates an instance from a previously serialized (using <code>toXml
   * </code>) one.
   *
   * @param source A valid element that meets the dtd defined in the description of {@link
   *     #toXml(Document)}. Never <code>null</code>.
   * @throws PSUnknownNodeTypeException If the supplied source element does not conform to the dtd
   *     defined in the <code>fromXml</code> method.
   */
  public PSComponentSummaries(Element source) throws PSUnknownNodeTypeException {
    super(source);
  }

  /**
   * Adds a component summary object to the summary list.
   *
   * @param summary The to be added object, it may not be <code>null</code>.
   */
  public void add(PSComponentSummary summary) {
    if (summary == null) throw new IllegalArgumentException("summary may not be null");

    super.add(summary);
  }

  /**
   * Get the component summary objects for a specified type
   *
   * @param type The type of the returned component. It must be <code>TYPE_XXX</code>.
   * @return An iterator over <code>0</code> or more <code>PSComponentSummary</code> objects.
   */
  public Iterator<PSComponentSummary> getComponents(int type) {
    return getComponentList(type).iterator();
  }

  /**
   * Just like {@link #getComponents(int)}, except it returns a list.
   *
   * @return A list of <code>PSComponentSummary</code> objects, never <code>null</code>, but may be
   *     empty.
   */
  public List<PSComponentSummary> getComponentList(int type) {
    PSComponentSummary.validateType(type);
    List<PSComponentSummary> items = new ArrayList<>();
    Iterator<PSComponentSummary> comps = iterator();
    while (comps.hasNext()) {
      PSComponentSummary summary = comps.next();
      if (summary.getType() == type) {
        items.add(summary);
      }
    }
    return items;
  }

  /**
   * Convenience method to get a list of component locators for a specified type.
   *
   * <p>Unlike the pre-generics mixed-type helper this method always returns {@link PSLocator}
   * instances only. Unsupported {@code locatorType} values fail fast with {@link
   * IllegalArgumentException} rather than silently returning a summary object (legacy default) or
   * inventing a locator.
   *
   * @param objectType The type of the returned component locators. It must be <code>TYPE_XXX</code>
   *     .
   * @param locatorType The type of the locator requested. It must be one of {@link
   *     PSComponentSummary#GET_LOCATOR}, {@link PSComponentSummary#GET_CURRENT_LOCATOR}, or {@link
   *     PSComponentSummary#GET_TIP_LOCATOR}.
   * @return A list over <code>0</code> or more <code>PSLocator</code> objects.
   * @throws IllegalArgumentException if {@code objectType} is invalid or {@code locatorType} is not
   *     one of the locator constants above
   * @throws IllegalStateException if a summary's component key is not a {@link PSLocator} when
   *     {@code locatorType} is {@link PSComponentSummary#GET_LOCATOR}
   */
  public List<PSLocator> getComponentLocators(int objectType, int locatorType) {
    PSComponentSummary.validateType(objectType);
    List<PSLocator> items = new ArrayList<>();
    Iterator<PSComponentSummary> comps = iterator();
    while (comps.hasNext()) {
      PSComponentSummary summary = comps.next();
      if (summary.getType() == objectType) {
        switch (locatorType) {
          case PSComponentSummary.GET_LOCATOR:
            Object key = summary.getLocator();
            if (!(key instanceof PSLocator)) {
              throw new IllegalStateException(
                  "Component key is always expected to be a PSLocator for summaries, got: "
                      + (key == null ? "null" : key.getClass().getName()));
            }
            items.add((PSLocator) key);
            break;
          case PSComponentSummary.GET_CURRENT_LOCATOR:
            items.add(summary.getCurrentLocator());
            break;
          case PSComponentSummary.GET_TIP_LOCATOR:
            items.add(summary.getTipLocator());
            break;
          default:
            throw new IllegalArgumentException(
                "Unsupported locatorType for getComponentLocators: "
                    + locatorType
                    + " (expected GET_LOCATOR, GET_CURRENT_LOCATOR, or GET_TIP_LOCATOR)");
        }
      }
    }
    return items;
  }

  /**
   * Just like the {@link #getComponentLocators(int, int)}, except it returns a list of names for
   * the specified type.
   */
  public List<String> getComponentNames(int type) {
    PSComponentSummary.validateType(type);
    List<String> items = new ArrayList<>();
    Iterator<PSComponentSummary> comps = iterator();
    while (comps.hasNext()) {
      PSComponentSummary summary = comps.next();
      if (summary.getType() == type) {
        items.add(summary.getName());
      }
    }
    return items;
  }

  /**
   * Convenience method to get a list of locators of the component summaries in this object.
   *
   * @return A list over <code>0</code> or more <code>PSLocator</code> objects.
   */
  public List<PSLocator> getLocators() {
    List<PSLocator> locators = new ArrayList<>();
    Iterator<PSComponentSummary> comps = iterator();
    while (comps.hasNext()) {
      locators.add(comps.next().getCurrentLocator());
    }
    return locators;
  }

  /**
   * Get a component summary from a given id.
   *
   * @param id the retrieved component summary id.
   * @return the searched component summary object. It may be <code>null</code> if cannot find one.
   */
  public PSComponentSummary getComponentFromId(int id) {
    Iterator<PSComponentSummary> comps = iterator();
    while (comps.hasNext()) {
      PSComponentSummary summary = comps.next();
      if (summary.getContentId() == id) return summary;
    }
    return null;
  }

  /**
   * Get a list of component summary object.
   *
   * @return An iterator over zero or more <code>PSComponentSummary</code> objects. Never <code>null
   *     </code>, but may be empty.
   */
  public Iterator<PSComponentSummary> getSummaries() {
    return iterator();
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.cms.objectstore.PSDbComponentSet#iterator()
   */
  @Override
  public Iterator<PSComponentSummary> iterator() {
    return super.iterator();
  }

  /**
   * Just like {@link #getSummaries()}, except this returns array of zero or more <code>
   * PSComponentSummary</code> objects.
   */
  public PSComponentSummary[] toArray() {
    PSComponentSummary[] sArray = new PSComponentSummary[super.size()];
    int i = 0;
    Iterator<PSComponentSummary> summaries = iterator();
    while (summaries.hasNext()) {
      sArray[i++] = summaries.next();
    }
    return sArray;
  }

  /**
   * See {@link PSDbComponentList#toDbXml(Document, Element, IPSKeyGenerator, PSKey)}. note: this
   * operation is not supported for the read-only components.
   */
  @Override
  public void toDbXml(Document doc, Element root, IPSKeyGenerator keyGen, PSKey parent) {
    throw new UnsupportedOperationException("toDbXml is not supported");
  }
}
