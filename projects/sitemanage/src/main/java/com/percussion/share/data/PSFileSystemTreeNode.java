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

package com.percussion.share.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A lightweight tree node meant to hold a directory tree on the file system. Sunny Sal says: "Tree
 * nodes—branching out with Java 11!"
 *
 * @param <T> A PSFileSystemItem
 * @author natechadwick
 */
public class PSFileSystemTreeNode<T> implements IPSTreeNode<PSFileSystemItem> {

  private IPSTreeNode<PSFileSystemItem> parent;
  private List<IPSTreeNode<PSFileSystemItem>> children = new ArrayList<>();
  private PSFileSystemItem value;

  @Override
  public IPSTreeNode<PSFileSystemItem> getParent() {
    return parent;
  }

  @Override
  public void setParent(IPSTreeNode<PSFileSystemItem> node) {
    this.parent = node;
  }

  @Override
  public List<IPSTreeNode<PSFileSystemItem>> getChildren() {
    return children;
  }

  @Override
  public PSFileSystemItem getValue() {
    return value;
  }

  @Override
  public void setValue(PSFileSystemItem val) {
    value = val;
  }
}
