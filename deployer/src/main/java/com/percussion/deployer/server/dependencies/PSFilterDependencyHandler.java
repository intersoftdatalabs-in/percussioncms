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
package com.percussion.deployer.server.dependencies;

import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import java.util.Iterator;
import java.util.List;

/**
 * Class to handle packaging and deploying a Filter deployable element.
 *
 * @author vamsinukala
 */
public class PSFilterDependencyHandler extends PSElementDependencyHandler {

  /**
   * Construct the dependency handler.
   *
   * @param def The def for the type supported by this handler. May not be <code>null</code> and
   *     must be of the type supported by this class. See {@link #getType()} for more info.
   * @param dependencyMap The full dependency map. May not be <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   */
  public PSFilterDependencyHandler(PSDependencyDef def, PSDependencyMap dependencyMap) {
    super(def, dependencyMap);
  }

  // see base class
  @Override
  protected PSDependencyHandler getChildHandler() {
    if (m_childHandler == null) {
      m_childHandler = getDependencyHandler(PSFilterDefDependencyHandler.DEPENDENCY_TYPE);
    }
    return m_childHandler;
  }

  // see base class
  @Override
  public Iterator<String> getChildTypes() {
    return ms_childTypes.iterator();
  }

  // see base class
  @Override
  public String getType() {
    return DEPENDENCY_TYPE;
  }

  /** Constant for this handler's supported type */
  static final String DEPENDENCY_TYPE = "Filter";

  /**
   * The content list definition handler, initialized by <code>getChildHandler()</code> if it is
   * <code>null</code>, will never be <code>null</code> after that.
   */
  private PSDependencyHandler m_childHandler = null;

  /** List of child types supported by this handler, it will never be <code>null</code> or empty. */
  private static final List<String> ms_childTypes =
      List.of(PSFilterDefDependencyHandler.DEPENDENCY_TYPE);
}
