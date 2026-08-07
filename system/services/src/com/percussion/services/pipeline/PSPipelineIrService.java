/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.services.pipeline;

import com.percussion.design.objectstore.PSApplication;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Default {@link IPSPipelineIrService}: classic objectstore import + JSON IR + file store.
 *
 * <p>Does not depend on a running server or {@code PSServerXmlObjectStore} singleton so unit tests
 * and design tooling can use the service with an injected base directory.
 */
public class PSPipelineIrService implements IPSPipelineIrService {

  private final PSPipelineIrFileStore store;

  /**
   * @param storeDir directory for {@code *.pipeline.json} native IR documents
   */
  public PSPipelineIrService(Path storeDir) {
    this(new PSPipelineIrFileStore(storeDir));
  }

  /** Package-visible for tests that inject a store. */
  PSPipelineIrService(PSPipelineIrFileStore store) {
    this.store = Objects.requireNonNull(store, "store");
  }

  @Override
  public PipelineIrDocument importClassicXml(InputStream classicXml) throws PSPipelineIrException {
    return PSClassicPipelineImporter.importFromXml(classicXml);
  }

  @Override
  public PipelineIrDocument importClassicApplication(PSApplication application)
      throws PSPipelineIrException {
    Objects.requireNonNull(application, "application");
    return PSClassicPipelineImporter.importFromApplication(application);
  }

  @Override
  public String toJson(PipelineIrDocument document) throws PSPipelineIrException {
    return PSPipelineIrJsonCodec.toJson(document);
  }

  @Override
  public PipelineIrDocument fromJson(String json) throws PSPipelineIrException {
    return PSPipelineIrJsonCodec.fromJson(json);
  }

  @Override
  public void save(PipelineIrDocument document) throws PSPipelineIrException {
    store.save(document);
  }

  @Override
  public Optional<PipelineIrDocument> load(String appName) throws PSPipelineIrException {
    return store.load(appName);
  }

  @Override
  public boolean exists(String appName) throws PSPipelineIrException {
    return store.exists(appName);
  }

  /** Exposed for diagnostics / tests. */
  public Path getStoreBaseDir() {
    return store.getBaseDir();
  }
}
