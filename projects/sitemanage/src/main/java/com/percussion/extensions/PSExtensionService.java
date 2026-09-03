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

package com.percussion.extensions;

import com.percussion.error.PSNonUniqueException;
import com.percussion.error.PSNotFoundException;
import com.percussion.extension.*;
import com.percussion.server.PSServer;
import java.io.File;
import java.net.URL;
import java.util.Iterator;

// REFACTORED: CP-JAVA11
/**
 * Spring-facing facade over {@link PSServer#getExtensionManager}. Resolves the
 * manager lazily: Spring often constructs this bean before {@code PSServer.init}
 * installs the extension manager (H2 QA / Jetty), so constructor capture of a
 * null manager caused catalog NPEs on {@code GET /services/extensions/catalog}.
 */
public class PSExtensionService implements IPSExtensionService {

  /** Cached after first successful resolve; never store a null from early Spring init. */
  private volatile PSExtensionManager manager;

  public PSExtensionService() {
    // Eager attempt is fine when the server is already up (unit tests / late beans).
    IPSExtensionManager current = PSServer.getExtensionManager(null);
    if (current instanceof PSExtensionManager) {
      this.manager = (PSExtensionManager) current;
    }
  }

  /**
   * Returns the live server extension manager, resolving and caching on first use when
   * the constructor ran before {@code PSServer} initialized extensions.
   *
   * @return non-null manager
   * @throws IllegalStateException if the server has not installed an extension manager yet
   */
  PSExtensionManager resolveManager() {
    PSExtensionManager local = manager;
    if (local != null) {
      return local;
    }
    synchronized (this) {
      local = manager;
      if (local != null) {
        return local;
      }
      IPSExtensionManager current = PSServer.getExtensionManager(null);
      if (!(current instanceof PSExtensionManager)) {
        throw new IllegalStateException(
            "PSServer extension manager is not initialized (Spring constructed"
                + " PSExtensionService before PSServer.init)");
      }
      local = (PSExtensionManager) current;
      manager = local;
      return local;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<PSExtensionRef> getExtensionHandlerNames() {
    return resolveManager().getExtensionHandlerNames();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<PSExtensionRef> getExtensionNames(
      String handlerNamePattern,
      String context,
      String interfacePattern,
      String extensionNamePattern)
      throws PSExtensionException {
    return resolveManager()
        .getExtensionNames(handlerNamePattern, context, interfacePattern, extensionNamePattern);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<URL> getExtensionFiles(PSExtensionRef ref)
      throws PSNotFoundException, PSExtensionException {
    return resolveManager().getExtensionFiles(ref);
  }

  @Override
  public boolean exists(PSExtensionRef ref) throws PSExtensionException {
    return resolveManager().exists(ref);
  }

  @Override
  public IPSExtensionDef getExtensionDef(PSExtensionRef ref)
      throws PSExtensionException, PSNotFoundException {
    return resolveManager().getExtensionDef(ref);
  }

  @Override
  public void startExtensionHandler(String handlerName)
      throws PSExtensionException, PSNotFoundException {
    resolveManager().startExtensionHandler(handlerName);
  }

  @Override
  public void stopExtensionHandler(String handlerName)
      throws PSExtensionException, PSNotFoundException {
    resolveManager().stopExtensionHandler(handlerName);
  }

  @Override
  public void installExtension(IPSExtensionDef def, Iterator<?> resources)
      throws PSExtensionException, PSNotFoundException, PSNonUniqueException {
    resolveManager().installExtension(def, resources);
  }

  @Override
  public void installExtension(
      IPSExtensionDef def, Iterator<?> resources, IPSExtensionListener listener)
      throws PSExtensionException, PSNotFoundException, PSNonUniqueException {
    resolveManager().installExtension(def, resources, listener);
  }

  @Override
  public void removeExtension(PSExtensionRef ref) throws PSNotFoundException, PSExtensionException {
    resolveManager().removeExtension(ref);
  }

  @Override
  public void updateExtension(IPSExtensionDef def, Iterator<?> resources)
      throws PSExtensionException, PSNotFoundException {
    resolveManager().updateExtension(def, resources);
  }

  @Override
  public IPSExtension prepareExtension(PSExtensionRef ref, IPSExtensionListener listener)
      throws PSNotFoundException, PSExtensionException {
    return resolveManager().prepareExtension(ref, listener);
  }

  @Override
  public void unregisterListener(PSExtensionRef ref, IPSExtensionListener listener) {
    resolveManager().unregisterListener(ref, listener);
  }

  @Override
  public File getCodeBase(IPSExtensionDef def) throws PSNotFoundException, PSExtensionException {
    return resolveManager().getCodeBase(def);
  }

  @Override
  public void notifyAdd(PSExtensionRef ref) {
    resolveManager().notifyAdd(ref);
  }

  @Override
  public void registerListener(PSExtensionRef ref, IPSExtensionListener listener) {
    resolveManager().registerListener(ref, listener);
  }
}
