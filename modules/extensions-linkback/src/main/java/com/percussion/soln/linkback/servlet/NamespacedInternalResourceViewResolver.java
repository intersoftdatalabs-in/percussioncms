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

package com.percussion.soln.linkback.servlet;

import java.util.Locale;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * Resolves views only when their names start with a configured namespace.
 *
 * <p>Creates a view resolver with no namespace configured; callers must set the namespace before
 * use.
 */
public class NamespacedInternalResourceViewResolver extends InternalResourceViewResolver {

  /** Creates a view resolver with no namespace configured. */
  public NamespacedInternalResourceViewResolver() {}

  private String m_namespace;

  /**
   * Loads a view whose name is prefixed with the configured namespace.
   *
   * @param viewName the view name to resolve
   * @param locale the locale to associate with the view
   * @return the resolved view, or {@code null} when the view name does not start with the
   *     configured namespace
   * @throws IllegalStateException if no namespace has been assigned
   */
  @Override
  protected View loadView(String viewName, Locale locale) throws Exception {
    if (m_namespace == null) throw new IllegalStateException("namespace must be assigned");

    // only handle requests whose view name is prefixed with a specific
    // namespace
    if (viewName.startsWith(m_namespace)) {
      return super.loadView(viewName.substring(m_namespace.length()), locale);
    }
    return null;
  }

  /**
   * Returns the namespace required for a view to be resolved.
   *
   * @return the configured namespace
   */
  public String getNamespace() {
    return m_namespace;
  }

  /**
   * Sets the namespace required for a view to be resolved.
   *
   * @param namespace the namespace to set
   */
  public void setNamespace(String namespace) {
    m_namespace = namespace;
  }
}
