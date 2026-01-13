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
package com.percussion.servlets.taglib;

import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import jakarta.faces.application.Application;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.webapp.UIComponentTag;
import org.apache.commons.lang3.StringUtils;

/**
 * Base class for our JSF tags, provides basic implementations inspired by the core jsf book.
 *
 * @author dougrand
 */
public abstract class PSJSFBaseTag extends UIComponentTag {
  /** The label for the component. */
  private String m_label;

  @Override
  public String getRendererType() {
    return null;
  }

  /* (non-Javadoc)
   * @see jakarta.faces.webapp.UIComponentTag#setProperties(jakarta.faces.component.UIComponent)
   */
  @Override
  protected void setProperties(UIComponent comp) {
    super.setProperties(comp);
    setValueBinding(comp, "label", m_label);
  }

  /**
   * Process the value binding, and set the property on the component.
   *
   * @param comp the component
   * @param name the name of the property, never <code>null</code> or empty
   * @param value the value, may be <code>null</code> or empty
   */
  @SuppressWarnings("unchecked")
  protected void setValueBinding(UIComponent comp, String name, String value) {
    if (comp == null) {
      throw new IllegalArgumentException("comp may not be null");
    }
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be null or empty");
    }
    if (StringUtils.isBlank(value)) {
      return;
    }
    if (!isValueReference(value)) {
      comp.getAttributes().put(name, value);
    } else {
      FacesContext ctx = FacesContext.getCurrentInstance();
      Application app = ctx.getApplication();
      ValueExpression ve = app.getExpressionFactory().createValueExpression(
            ctx.getELContext(), value, Object.class);
      comp.setValueExpression(name, ve);
    }
  }

  /**
   * Create a method binding.
   *
   * @param comp the component, never <code>null</code>
   * @param name the name of the property, never <code>null</code> or empty.
   * @param value the value, may be <code>null</code> or empty.
   * @param params the parameter classes used by the called method, or <code>null</code>
   */
  @SuppressWarnings("unchecked")
  protected void setMethodBinding(UIComponent comp, String name, String value, Class[] params) {
    if (comp == null) {
      throw new IllegalArgumentException("comp may not be null");
    }
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be null or empty");
    }
    if (StringUtils.isBlank(value)) {
      return;
    }
    FacesContext ctx = FacesContext.getCurrentInstance();
    Application app = ctx.getApplication();
    MethodExpression me = app.getExpressionFactory().createMethodExpression(
          ctx.getELContext(), value, Object.class, params);
    comp.getAttributes().put(name, me);
  }

  /**
   * @return the label
   */
  public String getLabel() {
    return m_label;
  }

  /**
   * @param label the label to set
   */
  public void setLabel(String label) {
    m_label = label;
  }
}
