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
public class PSExtensionService implements IPSExtensionService {

  private final PSExtensionManager manager;

  public PSExtensionService() {
    this.manager = (PSExtensionManager) PSServer.getExtensionManager(null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<PSExtensionRef> getExtensionHandlerNames() {
    return manager.getExtensionHandlerNames();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<PSExtensionRef> getExtensionNames(
      String handlerNamePattern,
      String context,
      String interfacePattern,
      String extensionNamePattern)
      throws PSExtensionException {
    return manager.getExtensionNames(
        handlerNamePattern, context, interfacePattern, extensionNamePattern);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<URL> getExtensionFiles(PSExtensionRef ref)
      throws PSNotFoundException, PSExtensionException {
    return manager.getExtensionFiles(ref);
  }

  @Override
  public boolean exists(PSExtensionRef ref) throws PSExtensionException {
    return manager.exists(ref);
  }

  @Override
  public IPSExtensionDef getExtensionDef(PSExtensionRef ref)
      throws PSExtensionException, PSNotFoundException {
    return manager.getExtensionDef(ref);
  }

  @Override
  public void startExtensionHandler(String handlerName)
      throws PSExtensionException, PSNotFoundException {
    manager.startExtensionHandler(handlerName);
  }

  @Override
  public void stopExtensionHandler(String handlerName)
      throws PSExtensionException, PSNotFoundException {
    manager.stopExtensionHandler(handlerName);
  }

  @Override
  public void installExtension(IPSExtensionDef def, Iterator<?> resources)
      throws PSExtensionException, PSNotFoundException, PSNonUniqueException {
    manager.installExtension(def, resources);
  }

  @Override
  public void installExtension(
      IPSExtensionDef def, Iterator<?> resources, IPSExtensionListener listener)
      throws PSExtensionException, PSNotFoundException, PSNonUniqueException {
    manager.installExtension(def, resources, listener);
  }

  @Override
  public void removeExtension(PSExtensionRef ref) throws PSNotFoundException, PSExtensionException {
    manager.removeExtension(ref);
  }

  @Override
  public void updateExtension(IPSExtensionDef def, Iterator<?> resources)
      throws PSExtensionException, PSNotFoundException {
    manager.updateExtension(def, resources);
  }

  @Override
  public IPSExtension prepareExtension(PSExtensionRef ref, IPSExtensionListener listener)
      throws PSNotFoundException, PSExtensionException {
    return manager.prepareExtension(ref, listener);
  }

  @Override
  public void unregisterListener(PSExtensionRef ref, IPSExtensionListener listener) {
    manager.unregisterListener(ref, listener);
  }

  @Override
  public File getCodeBase(IPSExtensionDef def) throws PSNotFoundException, PSExtensionException {
    return manager.getCodeBase(def);
  }

  @Override
  public void notifyAdd(PSExtensionRef ref) {
    manager.notifyAdd(ref);
  }

  @Override
  public void registerListener(PSExtensionRef ref, IPSExtensionListener listener) {
    manager.registerListener(ref, listener);
  }
}
