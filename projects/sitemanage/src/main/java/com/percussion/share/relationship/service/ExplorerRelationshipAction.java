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
package com.percussion.share.relationship.service;

import com.percussion.share.relationship.data.PSExplorerRelationshipEdge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Outcome of listing, adding, or removing one Explorer relationship. Never null. */
public final class ExplorerRelationshipAction {

  public enum Status {
    LISTED,
    REMOVED,
    CREATED,
    BAD_REQUEST,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT
  }

  private final Status status;
  private final String message;
  private final List<PSExplorerRelationshipEdge> edges;

  private ExplorerRelationshipAction(
      Status status, String message, List<PSExplorerRelationshipEdge> edges) {
    this.status = status;
    this.message = message == null ? "" : message;
    this.edges = edges == null ? List.of() : List.copyOf(edges);
  }

  public static ExplorerRelationshipAction listed(List<PSExplorerRelationshipEdge> edges) {
    return new ExplorerRelationshipAction(Status.LISTED, "", edges);
  }

  public static ExplorerRelationshipAction removed() {
    return new ExplorerRelationshipAction(Status.REMOVED, "", Collections.emptyList());
  }

  public static ExplorerRelationshipAction created(PSExplorerRelationshipEdge edge) {
    return new ExplorerRelationshipAction(
        Status.CREATED, "", edge == null ? List.of() : List.of(edge));
  }

  public static ExplorerRelationshipAction of(Status status, String message) {
    return new ExplorerRelationshipAction(status, message, new ArrayList<>());
  }

  public Status getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public List<PSExplorerRelationshipEdge> getEdges() {
    return edges;
  }
}
