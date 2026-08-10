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

import com.percussion.cms.IPSCmsErrors;
import com.percussion.cms.PSCmsException;
import com.percussion.design.objectstore.PSContentEditorMapper;
import com.percussion.design.objectstore.PSContentEditorPipe;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSUIDefinition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class extracts definition data for <code>PSCoreItem</code> object population. It parses a
 * <code>PSContentEditor</code> object and populates a <code>PSCoreItem</code> object from that
 * definition. This cannot be instantiated explicitly by callers.
 *
 * <p>Definition extraction for construction uses {@link #extractDefinition(PSItemDefinition)} so
 * {@link PSCoreItem} does not pass {@code this} to an external type while subclasses are still
 * initializing ({@code -Xlint:this-escape}). The visitor path remains for populating fully
 * constructed {@link PSItemChildEntry} templates.
 */
public class PSItemDefExtractor implements IPSVisitor {
  /**
   * Cannot be instantiated by outsiders. Builds field/child lists from the item definition without
   * holding a {@link PSCoreItem} reference.
   *
   * @param itemDefinition assumed not <code>null</code>
   * @throws PSCmsException if an error occurs extracting the definition
   */
  private PSItemDefExtractor(PSItemDefinition itemDefinition) throws PSCmsException {
    m_itemDefinition = itemDefinition;
    m_fields = new ArrayList<>();
    m_children = new ArrayList<>();
    processFieldSet(getFieldSet(), null, false);
  }

  /**
   * Populates the <code>PSCoreItem</code> with its definition. Prefer construction via {@link
   * PSCoreItem}'s private extract path which uses {@link #extractDefinition(PSItemDefinition)}.
   *
   * @param coreItem must not be <code>null</code>.
   * @throws PSCmsException if an error occurs populating the <code>coreItem</code>
   */
  public static void populateItemDefinition(PSCoreItem coreItem) throws PSCmsException {
    if (coreItem == null) throw new IllegalArgumentException("coreItem must not be null");

    DefinitionParts parts = extractDefinition(coreItem.getItemDefinition());
    coreItem.applyExtractedDefinition(parts);
  }

  /**
   * Extracts field and child definitions from an item definition without a live {@link PSCoreItem}.
   * Used by {@link PSCoreItem} construction to avoid this-escape.
   *
   * @param itemDefinition must not be <code>null</code>
   * @return extracted parts, never <code>null</code>
   * @throws PSCmsException if an error occurs extracting the definition
   */
  public static DefinitionParts extractDefinition(PSItemDefinition itemDefinition)
      throws PSCmsException {
    if (itemDefinition == null) throw new IllegalArgumentException("itemDefinition must not be null");

    PSItemDefExtractor xtr = new PSItemDefExtractor(itemDefinition);
    return new DefinitionParts(xtr.m_fields, xtr.m_children);
  }

  /**
   * Result of definition extraction: parent-level fields and complex children (with template entry
   * fields already populated).
   */
  public static final class DefinitionParts {
    private final List<PSItemField> fields;
    private final List<PSItemChild> children;

    DefinitionParts(List<PSItemField> fields, List<PSItemChild> children) {
      this.fields = fields;
      this.children = children;
    }

    public List<PSItemField> getFields() {
      return fields;
    }

    public List<PSItemChild> getChildren() {
      return children;
    }
  }

  /**
   * This is the method that does the work. Given a <code>PSFieldSet</code> it will create <code>
   * PSItemFields</code>. If an element of the <code>PSFieldSet</code> is another <code>PSFieldSet
   * </code> and is of type complex child it creates a <code>PSItemChild</code> and <code>
   * PSItemChildEntry</code> and then recursively calls itself to add the <code>PSItemFields</code>
   * to the <code>PSItemChildEntry</code>.
   *
   * <p>When {@code itemAccessor} is {@code null}, parent-level fields and children are accumulated
   * into {@link #m_fields} / {@link #m_children} (construction-safe path). When non-null (child
   * entry population), the visitor {@link IPSItemAccessor#accept} path is used — the accessor is a
   * fully constructed {@link PSItemChildEntry}.
   *
   * @param fieldSet the fieldset to parse - assumed not <code>null</code>
   * @param itemAccessor the item on which to add the elements, or {@code null} for parent-level
   *     accumulation
   * @param isMultiValue <code>true</code> if it is, otherwise <code>false</code>.
   */
  private void processFieldSet(
      PSFieldSet fieldSet, IPSItemAccessor itemAccessor, boolean isMultiValue) {
    Iterator it = fieldSet.getAll();
    PSField field = null;
    PSDisplayMapping mapping = null;
    while (it.hasNext()) {
      Object o = it.next();
      if (o instanceof PSFieldSet) {
        PSFieldSet childSet = (PSFieldSet) o;

        // Is it multiPropertySimpleChild
        if (childSet.getType() == PSFieldSet.TYPE_MULTI_PROPERTY_SIMPLE_CHILD)
          // just add the fields to the parent level
          processFieldSet(childSet, itemAccessor, false);

        // Is it  simpleChild
        else if (childSet.getType() == PSFieldSet.TYPE_SIMPLE_CHILD)
          // this is a multivalue field, add to parent:
          processFieldSet(childSet, itemAccessor, true);
        else if (childSet.getType() == PSFieldSet.TYPE_COMPLEX_CHILD) {
          mapping = getDisplayMapping(childSet.getName());
          if (mapping == null) continue; // ignore non-mapped fields

          // create child
          PSItemChild child = new PSItemChild(childSet, mapping);

          // create entry (which is item accessor) and populate template fields
          PSItemChildEntry entry = child.createChildEntry();

          // TODO: SUPPORT CHILDREN OF CHILDREN???
          if (itemAccessor == null) {
            m_children.add(child);
          } else {
            m_object = child;
            itemAccessor.accept(this);
            m_object = null;
          }

          // populate template entry fields (entry is fully constructed)
          processFieldSet(childSet, entry, false);
        }
      } else if (o instanceof PSField) {
        // handle fields:
        field = (PSField) o;
        // get ui set:
        mapping = getDisplayMapping(field.getSubmitName());
        if (mapping == null) continue; // ignore non-mapped fields

        PSItemField itemField = new PSItemField(field, mapping.getUISet(), isMultiValue);
        if (itemAccessor == null) {
          m_fields.add(itemField);
        } else {
          m_object = itemField;
          itemAccessor.accept(this);
          m_object = null;
        }
      }
    }
  }

  /**
   * Returns the <code>PSDisplayMapping</code> for the specified field name. This depends on <code>
   * getFieldSet()</code> being called first. Which is called by the ctor.
   *
   * @param fieldName - assumed not <code>null</code> or empty, is case sensitive
   * @return may be <code>null</code> as fields are not required to have a <code>PSDisplayMapping
   *     </code>
   */
  private PSDisplayMapping getDisplayMapping(String fieldName) {
    // get ui definition for the label:
    PSUIDefinition ceUiDef = m_parentMapper.getUIDefinition();

    // get the display mapper:
    PSDisplayMapper disMpr = ceUiDef.getDisplayMapper();

    // get the display mapping:
    PSDisplayMapping disMapping = disMpr.getMapping(fieldName);

    return disMapping;
  }

  /**
   * Returns an <code>Object</code>. This is called by a <code>IPSItemAccessor</code> when parsing a
   * definition.
   *
   * @return Object - may be <code>null</code>
   */
  public Object getObject() {
    return m_object;
  }

  /**
   * Gets the field set from the content editor, called by the ctor.
   *
   * @return the parent field set, never <code>null</code>.
   */
  private PSFieldSet getFieldSet() throws PSCmsException {
    // get pipe:
    PSContentEditorPipe cePipe =
        (PSContentEditorPipe) m_itemDefinition.getContentEditor().getPipe();

    if (cePipe == null) throw new PSCmsException(IPSCmsErrors.DATA_EXTRACTION_ERROR_NULL_DATAPIPE);

    // get Mapper:
    PSContentEditorMapper ceMapper = cePipe.getMapper();

    // get field set:
    PSFieldSet ceFieldSet = ceMapper.getFieldSet();

    m_parentMapper = ceMapper;

    return ceFieldSet;
  }

  /**
   * The Parent Mapper. The top most mapper of the <code>PSContentEditor</code>, set by <code>
   * getFieldSet()</code>, never <code>null</code>.
   */
  private PSContentEditorMapper m_parentMapper;

  /** Item definition being extracted, set by the ctor, never <code>null</code>. */
  private final PSItemDefinition m_itemDefinition;

  /** Parent-level fields accumulated during extraction. */
  private final List<PSItemField> m_fields;

  /** Complex children accumulated during extraction. */
  private final List<PSItemChild> m_children;

  /** Temporary field. Mostly <code>null</code>. Used in creation of objects. */
  private Object m_object;
}
