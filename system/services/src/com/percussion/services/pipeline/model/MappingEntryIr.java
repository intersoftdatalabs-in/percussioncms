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

package com.percussion.services.pipeline.model;

import java.util.Objects;

/** One document ↔ backend mapping row. */
public class MappingEntryIr {

  public static final String BACKEND_KIND_COLUMN = "COLUMN";
  public static final String BACKEND_KIND_EXTENSION = "EXTENSION";
  public static final String BACKEND_KIND_OTHER = "OTHER";

  private String documentField;
  private String backend;
  private String backendKind = BACKEND_KIND_OTHER;

  public String getDocumentField() {
    return documentField;
  }

  public void setDocumentField(String documentField) {
    this.documentField = documentField;
  }

  public String getBackend() {
    return backend;
  }

  public void setBackend(String backend) {
    this.backend = backend;
  }

  public String getBackendKind() {
    return backendKind;
  }

  public void setBackendKind(String backendKind) {
    this.backendKind = backendKind != null ? backendKind : BACKEND_KIND_OTHER;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MappingEntryIr that)) {
      return false;
    }
    return Objects.equals(documentField, that.documentField)
        && Objects.equals(backend, that.backend)
        && Objects.equals(backendKind, that.backendKind);
  }

  @Override
  public int hashCode() {
    return Objects.hash(documentField, backend, backendKind);
  }
}
