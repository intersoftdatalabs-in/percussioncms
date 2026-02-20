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
package com.percussion.cms.objectstore;

import com.percussion.cms.PSCmsException;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRelationshipSet;

/**
 * Compatibility interface for relationship processors. Default implementations throw a {@link
 * com.percussion.cms.PSCmsException} so that existing code using proxies compiles while specific
 * processor implementations are progressively migrated.
 */
public interface IPSRelationshipProcessor {
  default void add(
      String componentType, String relationshipType, java.util.List<?> children, PSKey targetParent)
      throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("add not implemented"));
  }

  default void add(String relationshipType, java.util.List<?> children, PSLocator targetParent)
      throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("add not implemented"));
  }

  default void move(
      String relationshipType, PSKey sourceParent, java.util.List<?> children, PSKey targetParent)
      throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("move not implemented"));
  }

  default void copy(String relationshipType, java.util.List<?> children, PSKey targetParent)
      throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("copy not implemented"));
  }

  default void delete(String relationshipType, PSKey sourceParent, java.util.List<?> children)
      throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("delete not implemented"));
  }

  default PSComponentSummary[] getChildren(String componentType, PSKey parent)
      throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("getChildren not implemented"));
  }

  default PSComponentSummary[] getChildren(
      String componentType, String relationshipType, PSKey parent) throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("getChildren not implemented"));
  }

  default PSComponentSummary[] getParents(
      String componentType, String relationshipType, PSKey parent) throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("getParents not implemented"));
  }

  default void delete(PSKey sourceParent, java.util.List<?> children) throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("delete not implemented"));
  }

  default PSRelationshipSet getRelationships(
      String relationshipType, PSLocator locator, boolean owner) throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("getRelationships not implemented"));
  }

  default void move(
      String relationshipType,
      PSLocator sourceParent,
      java.util.List<?> children,
      PSLocator targetParent)
      throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("move not implemented"));
  }

  default PSRelationshipSet getRelationships(PSRelationshipFilter filter) throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("getRelationships not implemented"));
  }

  default PSComponentSummaries getSummaries(PSRelationshipFilter filter, boolean owner)
      throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("getSummaries not implemented"));
  }

  default void save(PSRelationshipSet relationships) throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("save not implemented"));
  }

  default void delete(PSRelationshipSet relationships) throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("delete not implemented"));
  }

  default PSRelationshipConfig getConfig(String relationshipTypeName) throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("getConfig not implemented"));
  }

  default PSComponentSummary getSummaryByPath(
      String componentType, String path, String relationshipTypeName) throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("getSummaryByPath not implemented"));
  }

  default String[] getRelationshipOwnerPaths(
      String componentType, PSLocator locator, String relationshipTypeName) throws PSCmsException {
    throw new PSCmsException(
        new UnsupportedOperationException("getRelationshipOwnerPaths not implemented"));
  }

  default boolean isDescendent(
      String componentType, PSLocator parent, PSLocator child, String relationshipTypeName)
      throws PSCmsException {
    throw new PSCmsException(new UnsupportedOperationException("isDescendent not implemented"));
  }

  default PSKey[] getDescendentsLocators(
      String componentType, String relationshipType, PSKey parent) throws PSCmsException {
    throw new PSCmsException(
        new UnsupportedOperationException("getDescendentsLocators not implemented"));
  }
}
