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
package com.percussion.deployer.objectstore;

/**
 * Implemented by classes that are to be notified of changes to the
 * {@link PSDependencyTreeContext} in case the UI representing that context
 * needs to be updated.
 */
public interface IPSDependencyTreeCtxListener {
  /**
   * Informs the listener that the supplied context has been modified and that
   * any nodes represented by that context may need to be updated in the UI.
   *
   * @param ctx The modified context, never <code>null</code>.
   */
  void ctxChanged(PSDependencyContext ctx);

  /**
   * Determine if the listener is listening for changes on the supplied
   * package. Used to remove listeners when a package's tree is removed.
   *
   * @param pkg The package to check, never <code>null</code>.
   *
   * @return <code>true</code> if the listener is listening for changes to the
   * supplied package's tree, <code>false</code> if not.
   */
  boolean listensForChanges(PSDeployableElement pkg);
}
