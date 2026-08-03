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
// REFACTORED: CP-JAVA11

package com.percussion.rx.config.impl;

import com.percussion.rx.config.IPSConfigHandler.ObjectState;
import com.percussion.rx.design.IPSAssociationSet;
import com.percussion.rx.design.impl.PSEditionWrapper;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.publisher.IPSEditionTaskDef;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * The Edition property setter.
 *
 * @author YuBingChen
 */
public class PSEditionSetter extends PSSimplePropertySetter {

  /** Default constructor for use by Spring. */
  public PSEditionSetter() {}

  @Override
  protected boolean applyProperty(
      Object obj,
      ObjectState state,
      List<IPSAssociationSet> aSets,
      String propName,
      Object propValue)
      throws Exception {
    if (!(obj instanceof PSEditionWrapper)) {
      throw new IllegalArgumentException("obj type must be PSEditionWrapper.");
    }

    var wrapper = (PSEditionWrapper) obj;
    if (PRE_TASKS.equals(propName)) {
      setTasks(wrapper, propValue, true);
    } else if (POST_TASKS.equals(propName)) {
      setTasks(wrapper, propValue, false);
    } else {
      super.applyProperty(wrapper.getEdition(), state, aSets, propName, propValue);
    }

    return true;
  }

  @Override
  protected Object getPropertyValue(Object obj, String propName) throws PSNotFoundException {
    var wrapper = (PSEditionWrapper) obj;
    if (PRE_TASKS.equals(propName)) {
      return wrapper.getPreTasks().isEmpty()
          ? Collections.emptyList()
          : convertListTaskToListMap(wrapper.getPreTasks());
    } else if (POST_TASKS.equals(propName)) {
      return wrapper.getPostTasks().isEmpty()
          ? Collections.emptyList()
          : convertListTaskToListMap(wrapper.getPostTasks());
    }
    return super.getPropertyValue(obj, propName);
  }

  /**
   * Converts a list of tasks to a list of maps.
   *
   * @param tasks the list of tasks
   * @return list of maps representing the tasks
   */
  private List<Map<String, Object>> convertListTaskToListMap(List<IPSEditionTaskDef> tasks) {
    var result = new ArrayList<Map<String, Object>>();
    for (var def : tasks) {
      result.add(convertTaskToMap(def));
    }
    return result;
  }

  /**
   * Converts the specified task to a map, which contains all known properties of the task
   * definition.
   *
   * @param taskDef the task, assumed not null
   * @return the converted map, never null, but may be empty
   */
  private Map<String, Object> convertTaskToMap(IPSEditionTaskDef taskDef) {
    var taskMap = new HashMap<String, Object>();
    if (StringUtils.isNotBlank(taskDef.getExtensionName())) {
      taskMap.put(EXT_NAME, taskDef.getExtensionName());
      var params = taskDef.getParams();
      var pairs = new ArrayList<PSPair<String, String>>();
      for (var k : params.keySet()) {
        pairs.add(new PSPair<>(k, params.get(k)));
      }
      taskMap.put(EXT_PARAMS, pairs);
    }
    return taskMap;
  }

  private void setTasks(PSEditionWrapper wrapper, Object propValue, boolean isPreTasks) {
    var srcTasks = new ArrayList<Map>();
    if (propValue instanceof List) {
      srcTasks.addAll((List<Map>) propValue);
    } else if (!(propValue instanceof Map) || !((Map) propValue).isEmpty()) {
      throw new IllegalArgumentException("A list of Edition task type must be List or empty Map.");
    }

    var id = wrapper.getEdition().getGUID();
    var tasks = new ArrayList<IPSEditionTaskDef>();
    // Reverse pre-tasks so that the seq# of 1st task is smallest
    if (isPreTasks) {
      Collections.reverse(srcTasks);
    }

    for (int i = 0; i < srcTasks.size(); i++) {
      var props = srcTasks.get(i);
      if (props.isEmpty()) {
        // Ignore the empty map in a list, which can be created by the following XML section:
        // <property name="preTasks"><propertySet/></property>
        continue;
      }
      int seq = isPreTasks ? (i + 1) * -1 : (i + 1);
      var task = createTask(props, seq, id);
      tasks.add(task);
    }

    if (isPreTasks) {
      wrapper.setPreTasks(tasks);
    } else {
      wrapper.setPostTasks(tasks);
    }
  }

  private IPSEditionTaskDef createTask(Map<String, Object> props, int seq, IPSGuid editionId) {
    var srv = PSPublisherServiceLocator.getPublisherService();
    var task = srv.createEditionTask();
    task.setEditionId(editionId);
    task.setSequence(seq);
    for (var entry : props.entrySet()) {
      var key = entry.getKey();
      var value = entry.getValue();
      if (EXT_NAME.equals(key)) {
        task.setExtensionName(value.toString());
      } else if (CONT_ON.equals(key)) {
        var v = (Boolean) convertValue(value, Boolean.class);
        task.setContinueOnFailure(v);
      } else if (EXT_PARAMS.equals(key)) {
        if (!(value instanceof List)) {
          throw new IllegalArgumentException("The extensionParams property type must be List.");
        }
        var params = (List<PSPair<String, String>>) value;
        for (var pair : params) {
          task.setParam(pair.getFirst(), pair.getSecond());
        }
      }
    }
    return task;
  }

  /** The name of the edition pre-task property. */
  public static final String PRE_TASKS = "preTasks";

  /** The name of the edition post-task property. */
  public static final String POST_TASKS = "postTasks";

  /** Task specific property names. */
  private static final String EXT_NAME = "extensionName";

  private static final String EXT_PARAMS = "extensionParams";
  private static final String CONT_ON = "continueOnFailure";
}
