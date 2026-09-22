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

package com.percussion.apibridge;

import com.percussion.rest.workflows.WorkflowGraph;
import com.percussion.services.workflow.data.PSAgingTransition;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransitionBase;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/** Projects workflow states and transitions into a read-only {@link WorkflowGraph}. */
public final class WorkflowGraphProjector {

  private WorkflowGraphProjector() {}

  public static WorkflowGraph project(
      String workflowName, boolean packaged, boolean defaultWorkflow, List<PSState> states) {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName(workflowName);
    graph.setPackaged(packaged);
    graph.setDefaultWorkflow(defaultWorkflow);

    List<PSState> ordered = new ArrayList<>();
    if (states != null) {
      for (PSState state : states) {
        if (state != null && StringUtils.isNotBlank(state.getName())) {
          ordered.add(state);
        }
      }
    }
    ordered.sort(Comparator.comparingLong(PSState::getStateId).thenComparing(PSState::getName));

    Map<Long, String> namesById = new LinkedHashMap<>();
    List<WorkflowGraph.Node> nodes = new ArrayList<>();
    Set<String> seenNames = new LinkedHashSet<>();
    for (PSState state : ordered) {
      String name = state.getName().trim();
      namesById.put(state.getStateId(), name);
      if (seenNames.add(name.toLowerCase(Locale.ROOT))) {
        nodes.add(new WorkflowGraph.Node(name));
      }
    }

    List<WorkflowGraph.Edge> edges = new ArrayList<>();
    Set<String> seenEdges = new LinkedHashSet<>();
    for (PSState state : ordered) {
      collect(state.getName(), state.getTransitions(), namesById, nodes, seenNames, edges, seenEdges);
      collect(
          state.getName(),
          safeAging(state),
          namesById,
          nodes,
          seenNames,
          edges,
          seenEdges);
    }
    graph.setNodes(nodes);
    graph.setEdges(edges);
    return graph;
  }

  private static List<PSAgingTransition> safeAging(PSState state) {
    try {
      List<PSAgingTransition> aging = state.getAgingTransitions();
      return aging != null ? aging : List.of();
    } catch (RuntimeException ex) {
      return List.of();
    }
  }

  private static void collect(
      String fromName,
      List<? extends PSTransitionBase> transitions,
      Map<Long, String> namesById,
      List<WorkflowGraph.Node> nodes,
      Set<String> seenNames,
      List<WorkflowGraph.Edge> edges,
      Set<String> seenEdges) {
    if (transitions == null || StringUtils.isBlank(fromName)) {
      return;
    }
    String from = fromName.trim();
    for (PSTransitionBase transition : transitions) {
      if (transition == null) {
        continue;
      }
      String to = namesById.get(transition.getToState());
      if (to == null) {
        to = "state-" + transition.getToState();
        if (seenNames.add(to.toLowerCase(Locale.ROOT))) {
          nodes.add(new WorkflowGraph.Node(to));
        }
      }
      String label = StringUtils.defaultIfBlank(transition.getLabel(), "").trim();
      if (label.isEmpty()) {
        label = "to " + to;
      }
      String key = (from + "|" + label + "|" + to).toLowerCase(Locale.ROOT);
      if (!seenEdges.add(key)) {
        continue;
      }
      WorkflowGraph.Edge edge = new WorkflowGraph.Edge();
      edge.setFrom(from);
      edge.setTo(to);
      edge.setLabel(label);
      edges.add(edge);
    }
  }
}
