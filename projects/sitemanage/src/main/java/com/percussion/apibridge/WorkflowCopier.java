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

import com.percussion.services.workflow.data.PSAgingTransition;
import com.percussion.services.workflow.data.PSAssignedRole;
import com.percussion.services.workflow.data.PSNotification;
import com.percussion.services.workflow.data.PSNotificationDef;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransitionRole;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.services.workflow.data.PSWorkflowRole;
import com.percussion.utils.guid.IPSGuid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xml.sax.SAXException;

/**
 * Deep-copies a {@link PSWorkflow} (states, transitions, roles, notifications) onto a new guid and
 * name. The source object is not mutated. A {@code null} description keeps the source text; a
 * non-null description (including blank) replaces it.
 */
final class WorkflowCopier {

  private WorkflowCopier() {}

  static PSWorkflow copy(PSWorkflow source, String newName, IPSGuid newGuid, String description) {
    if (source == null) {
      throw new IllegalArgumentException("source workflow is required");
    }
    if (newName == null || newName.isBlank()) {
      throw new IllegalArgumentException("new workflow name is required");
    }
    if (newGuid == null) {
      throw new IllegalArgumentException("new workflow guid is required");
    }
    PSWorkflow copy = new PSWorkflow();
    try {
      copy.fromXML(source.toXML());
    } catch (IOException | SAXException e) {
      throw new IllegalStateException("Could not copy workflow " + source.getName(), e);
    }
    copy.setGUID(newGuid);
    copy.setName(newName);
    if (description != null) {
      copy.setDescription(description);
    }
    rebindWorkflowId(copy, newGuid.longValue());
    return copy;
  }

  /** Point every child row at the new workflow id without changing state or transition ids. */
  static void rebindWorkflowId(PSWorkflow workflow, long workflowId) {
    List<PSState> states = workflow.getStates();
    if (states != null) {
      for (PSState state : states) {
        if (state == null) {
          continue;
        }
        state.setWorkflowId(workflowId);
        List<PSTransition> transitions = state.getTransitions();
        if (transitions != null) {
          for (PSTransition transition : transitions) {
            if (transition == null) {
              continue;
            }
            transition.setWorkflowId(workflowId);
            rebindNotifications(transition.getNotifications(), workflowId);
            rebindTransitionRoles(transition.getTransitionRoles(), workflowId);
          }
          state.setTransitions(new ArrayList<>(transitions));
        }
        List<PSAgingTransition> aging = state.getAgingTransitions();
        if (aging != null) {
          for (PSAgingTransition transition : aging) {
            if (transition == null) {
              continue;
            }
            transition.setWorkflowId(workflowId);
            rebindNotifications(transition.getNotifications(), workflowId);
          }
          state.setAgingTransitions(new ArrayList<>(aging));
        }
        List<PSAssignedRole> assigned = state.getAssignedRoles();
        if (assigned != null) {
          for (PSAssignedRole role : assigned) {
            if (role != null) {
              role.setWorkflowId(workflowId);
            }
          }
        }
      }
    }
    List<PSWorkflowRole> roles = workflow.getRoles();
    if (roles != null) {
      for (PSWorkflowRole role : roles) {
        if (role != null) {
          role.setWorkflowId(workflowId);
        }
      }
    }
    List<PSNotificationDef> defs = workflow.getNotificationDefs();
    if (defs != null) {
      for (PSNotificationDef def : defs) {
        if (def != null) {
          def.setWorkflowId(workflowId);
        }
      }
    }
  }

  private static void rebindNotifications(List<PSNotification> notifications, long workflowId) {
    if (notifications == null) {
      return;
    }
    for (PSNotification notification : notifications) {
      if (notification != null) {
        notification.setWorkflowId(workflowId);
      }
    }
  }

  private static void rebindTransitionRoles(List<PSTransitionRole> roles, long workflowId) {
    if (roles == null) {
      return;
    }
    for (PSTransitionRole role : roles) {
      if (role != null) {
        role.setWorkflowId(workflowId);
      }
    }
  }
}
