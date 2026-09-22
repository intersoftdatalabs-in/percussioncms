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

package com.percussion.rest.test.apibridge;

import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.rest.workflows.IWorkflowsAdaptor;
import com.percussion.rest.workflows.WorkflowCreate;
import com.percussion.rest.workflows.WorkflowGraph;
import com.percussion.rest.workflows.WorkflowStepWrite;
import com.percussion.rest.workflows.WorkflowSummary;
import com.percussion.rest.workflows.WorkflowUpdate;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring test stub for {@link IWorkflowsAdaptor}. Required for ApplicationContext load after
 * constructor injection on {@code WorkflowsResource}.
 */
@Component
@Lazy
public class TestWorkflowsAdaptor implements IWorkflowsAdaptor {

  @Override
  public List<NamedObjectRef> getAllowedContentTypes(URI baseUri, String idOrName) {
    return List.of();
  }

  @Override
  public List<NamedObjectRef> setAllowedContentTypes(
      URI baseUri, String idOrName, List<NamedObjectRef> allowedContentTypes) {
    return allowedContentTypes != null ? allowedContentTypes : List.of();
  }

  @Override
  public WorkflowSummary createWorkflow(URI baseUri, WorkflowCreate body) {
    WorkflowSummary summary = new WorkflowSummary();
    summary.setWorkflowName(body != null && body.getName() != null ? body.getName().trim() : "");
    summary.setWorkflowDescription(body != null ? body.getDescription() : null);
    summary.setDefaultWorkflow(false);
    return summary;
  }

  @Override
  public WorkflowSummary updateWorkflow(URI baseUri, String idOrName, WorkflowUpdate body) {
    WorkflowSummary summary = new WorkflowSummary();
    summary.setWorkflowName(idOrName != null ? idOrName.trim() : "");
    summary.setWorkflowDescription(body != null ? body.getDescription() : null);
    summary.setDefaultWorkflow(false);
    return summary;
  }

  @Override
  public void deleteWorkflow(URI baseUri, String idOrName) {
    // No-op stub; CXF Spring context tests only assert bean wiring, not delete behavior.
  }

  @Override
  public WorkflowSummary createWorkflowStep(URI baseUri, String idOrName, WorkflowStepWrite body) {
    return updateWorkflow(baseUri, idOrName, null);
  }

  @Override
  public WorkflowGraph getWorkflowGraph(URI baseUri, String idOrName) {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName(idOrName != null ? idOrName.trim() : "");
    graph.setPackaged(false);
    graph.setNodes(List.of(new WorkflowGraph.Node("Draft")));
    return graph;
  }

  @Override
  public WorkflowSummary updateWorkflowStep(
      URI baseUri, String idOrName, String stepName, WorkflowStepWrite body) {
    return updateWorkflow(baseUri, idOrName, null);
  }
}
