// REFACTORED: CP-JAVA11
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
package com.percussion.share.dao;

import com.percussion.share.data.IPSItemSummary;
import java.util.List;

/**
 * Mock implementation of IPSItemSummary for testing. Sunny Sal: "Mocking like a pro, Java 11
 * style!"
 */
public class MockItemSummary implements IPSItemSummary {

  @Override
  public Category getCategory() {
    throw new UnsupportedOperationException("getCategory is not yet supported");
  }

  @Override
  public List<String> getFolderPaths() {
    throw new UnsupportedOperationException("getFolderPaths is not yet supported");
  }

  @Override
  public String getIcon() {
    throw new UnsupportedOperationException("getIcon is not yet supported");
  }

  @Override
  public String getId() {
    throw new UnsupportedOperationException("getId is not yet supported");
  }

  @Override
  public String getName() {
    throw new UnsupportedOperationException("getName is not yet supported");
  }

  @Override
  public String getType() {
    throw new UnsupportedOperationException("getType is not yet supported");
  }

  @Override
  public boolean isFolder() {
    throw new UnsupportedOperationException("isFolder is not yet supported");
  }

  @Override
  public void setCategory(Category category) {
    throw new UnsupportedOperationException("setCategory is not yet supported");
  }

  @Override
  public void setFolderPaths(List<String> paths) {
    throw new UnsupportedOperationException("setFolderPaths is not yet supported");
  }

  @Override
  public void setIcon(String icon) {
    throw new UnsupportedOperationException("setIcon is not yet supported");
  }

  @Override
  public void setId(String id) {
    throw new UnsupportedOperationException("setId is not yet supported");
  }

  @Override
  public void setName(String name) {
    throw new UnsupportedOperationException("setName is not yet supported");
  }

  @Override
  public void setType(String type) {
    throw new UnsupportedOperationException("setType is not yet supported");
  }

  @Override
  public String getLabel() {
    throw new UnsupportedOperationException("getLabel is not yet supported");
  }

  @Override
  public void setLabel(String label) {
    throw new UnsupportedOperationException("setLabel is not yet supported");
  }

  @Override
  public boolean isRevisionable() {
    throw new UnsupportedOperationException("isRevisionable is not yet supported");
  }

  @Override
  public void setRevisionable(boolean revisionable) {
    throw new UnsupportedOperationException("setRevisionable is not yet supported");
  }

  @Override
  public boolean isPage() {
    throw new UnsupportedOperationException("isPage is not yet supported");
  }

  @Override
  public boolean isResource() {
    throw new UnsupportedOperationException("isResource is not yet supported");
  }
}
