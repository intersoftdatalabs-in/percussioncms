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
  /** Workflow role names an admin may use when restricting a transition. */
  private List<String> roles = new ArrayList<>();

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

  /** Workflow role names, never {@code null}. */
  public List<String> getRoles() {
    return roles;
  }

  /** @param roles workflow role names; {@code null} clears the list */
  public void setRoles(List<String> roles) {
    this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
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
    /**
     * Approvals required on a regular transition. Omitted for aging edges. A negative value is the
     * each-role sentinel and is not writable from the approvals surface.
     */
    private Integer approvalsRequired;
    /**
     * Whether this regular transition is the default from its step ({@code DEFAULTTRANSITION}).
     * Omitted on aging edges. At most one regular edge from a step is true after a successful
     * mark.
     */
    private Boolean defaultTransition;
    /**
     * Whether every workflow role may fire this regular transition. {@code true} is allow-all.
     * {@code false} means {@link #allowedRoles} is the enforced list. Omitted on aging edges.
     */
    private Boolean allowAllRoles;
    /**
     * Role names that may fire this transition when {@link #allowAllRoles} is {@code false}.
     * Omitted while the transition still allows every role, and omitted on aging edges.
     */
    private List<String> allowedRoles;
    /** True when this edge is an aging transition, not a regular workflow transition. */
    private boolean aging;
    /** Minutes on an absolute or repeated aging transition. Omitted for regular edges. */
    private Long intervalMinutes;
    /**
     * Aging type name ({@code ABSOLUTE}, {@code REPEATED}, or {@code SYSTEM_FIELD}). Omitted on
     * regular edges. Repeated creates set {@code REPEATED}. Omitted create type stays {@code
     * ABSOLUTE}.
     */
    private String agingType;

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

    /** Stored approval count on a regular transition, or {@code null} on an aging edge. */
    public Integer getApprovalsRequired() {
      return approvalsRequired;
    }

    /** @param approvalsRequired stored {@code TRANSITIONAPPROVALSREQUIRED}, including zero */
    public void setApprovalsRequired(Integer approvalsRequired) {
      this.approvalsRequired = approvalsRequired;
    }

    /** Stored default flag on a regular transition, or {@code null} on an aging edge. */
    public Boolean getDefaultTransition() {
      return defaultTransition;
    }

    /** @param defaultTransition stored {@code DEFAULTTRANSITION}; {@code null} omits the field */
    public void setDefaultTransition(Boolean defaultTransition) {
      this.defaultTransition = defaultTransition;
    }

    /** {@code true} when every role may fire this regular transition; {@code null} on aging. */
    public Boolean getAllowAllRoles() {
      return allowAllRoles;
    }

    /** @param allowAllRoles stored allow-all flag; {@code null} omits the field */
    public void setAllowAllRoles(Boolean allowAllRoles) {
      this.allowAllRoles = allowAllRoles;
    }

    /** Enforced role names when the transition is restricted, or {@code null} when allow-all. */
    public List<String> getAllowedRoles() {
      return allowedRoles;
    }

    /** @param allowedRoles enforced names; {@code null} omits the field */
    public void setAllowedRoles(List<String> allowedRoles) {
      this.allowedRoles = allowedRoles != null ? new ArrayList<>(allowedRoles) : null;
    }

    public boolean isAging() {
      return aging;
    }

    public void setAging(boolean aging) {
      this.aging = aging;
    }

    public Long getIntervalMinutes() {
      return intervalMinutes;
    }

    public void setIntervalMinutes(Long intervalMinutes) {
      this.intervalMinutes = intervalMinutes;
    }

    public String getAgingType() {
      return agingType;
    }

    public void setAgingType(String agingType) {
      this.agingType = agingType;
    }
  }
}
