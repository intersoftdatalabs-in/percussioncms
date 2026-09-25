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

package com.percussion.rest.workflows;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only workflow state/transition graph (slice 32). Writes stay on sibling step/transition
 * surfaces.
 */
@XmlRootElement(name = "WorkflowGraph")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Read-only workflow graph (states and transitions)")
public class WorkflowGraph {

  private String workflowName;
  private boolean packaged;
  private boolean defaultWorkflow;
  private List<Node> nodes = new ArrayList<>();
  private List<Edge> edges = new ArrayList<>();

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public boolean isPackaged() {
    return packaged;
  }

  public void setPackaged(boolean packaged) {
    this.packaged = packaged;
  }

  public boolean isDefaultWorkflow() {
    return defaultWorkflow;
  }

  public void setDefaultWorkflow(boolean defaultWorkflow) {
    this.defaultWorkflow = defaultWorkflow;
  }

  public List<Node> getNodes() {
    return nodes;
  }

  public void setNodes(List<Node> nodes) {
    this.nodes = nodes != null ? nodes : new ArrayList<>();
  }

  public List<Edge> getEdges() {
    return edges;
  }

  public void setEdges(List<Edge> edges) {
    this.edges = edges != null ? edges : new ArrayList<>();
  }

  /** One workflow state. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Node {
    private String name;

    public Node() {}

    public Node(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  /** Directed transition between two state names. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Edge {
    private String from;
    private String to;
    private String label;
    private boolean commentRequired;

    public String getFrom() {
      return from;
    }

    public void setFrom(String from) {
      this.from = from;
    }

    public String getTo() {
      return to;
    }

    public void setTo(String to) {
      this.to = to;
    }

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public boolean isCommentRequired() {
      return commentRequired;
    }

    public void setCommentRequired(boolean commentRequired) {
      this.commentRequired = commentRequired;
    }
  }
}
