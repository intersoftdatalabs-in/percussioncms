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
import java.util.Optional;

/**
 * Pipeline intermediate representation (IR) service: classic import, JSON codec, native load/save.
 *
 * <p>Slice A foundation only — no execute / SQL runtime / hooks.
 */
public interface IPSPipelineIrService {

  /**
   * Import classic application XML stream into IR.
   *
   * @param classicXml not closed by the service
   */
  PipelineIrDocument importClassicXml(InputStream classicXml) throws PSPipelineIrException;

  /** Import an already-materialized objectstore application into IR. */
  PipelineIrDocument importClassicApplication(PSApplication application)
      throws PSPipelineIrException;

  /** Encode IR as JSON text. */
  String toJson(PipelineIrDocument document) throws PSPipelineIrException;

  /** Decode IR from JSON text. */
  PipelineIrDocument fromJson(String json) throws PSPipelineIrException;

  /**
   * Persist native IR by {@code document.app.name}.
   *
   * <p>Storage is the file-backed store configured for this service instance (design-time IR
   * documents, separate from classic objectstore XML apps).
   */
  void save(PipelineIrDocument document) throws PSPipelineIrException;

  /**
   * Load native IR by application name.
   *
   * @return empty when not found
   */
  Optional<PipelineIrDocument> load(String appName) throws PSPipelineIrException;

  /** @return true when native IR exists for the name */
  boolean exists(String appName) throws PSPipelineIrException;
}
