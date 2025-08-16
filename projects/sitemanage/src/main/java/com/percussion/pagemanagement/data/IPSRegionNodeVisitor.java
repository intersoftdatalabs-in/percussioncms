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
package com.percussion.pagemanagement.data;

/**
 * Visitor pattern for region nodes. Used to avoid casting and decouple visit order from the
 * visitor. The order of the visit is defined by different iterators.
 *
 * @author adamgent
 */
public interface IPSRegionNodeVisitor {

  /**
   * Visit a region code node.
   *
   * @param regionCode the region code node, never {@code null}
   */
  void visit(PSRegionCode regionCode);

  /**
   * Visit a region node.
   *
   * @param region the region node, never {@code null}
   */
  void visit(PSRegion region);
}
