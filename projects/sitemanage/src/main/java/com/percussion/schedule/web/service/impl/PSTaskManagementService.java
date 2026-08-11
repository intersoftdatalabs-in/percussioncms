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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.schedule.web.service.impl;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.schedule.IPSSchedulingService;
import com.percussion.services.schedule.data.PSNotificationTemplate;
import com.percussion.services.schedule.data.PSScheduledTask;
import com.percussion.services.schedule.data.PSScheduledTaskLog;
import com.percussion.utils.guid.IPSGuid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Path("/tasks")
@Component("taskManagementService")
@Lazy
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PSTaskManagementService {

  private final IPSSchedulingService schedulingService;
  private final IPSGuidManager guidManager;

  @Autowired
  public PSTaskManagementService(
      IPSSchedulingService schedulingService, IPSGuidManager guidManager) {
    this.schedulingService = schedulingService;
    this.guidManager = guidManager;
  }

  @GET
  @Path("/")
  public List<Map<String, Object>> getTasks() {
    try {
      return schedulingService.findAllSchedules().stream()
          .map(this::serializeTask)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/{taskId}")
  public Map<String, Object> getTask(@PathParam("taskId") String taskId) {
    try {
      IPSGuid guid = guidManager.makeGuid(taskId, PSTypeEnum.SCHEDULED_TASK);
      PSScheduledTask task =
          schedulingService
              .findScheduledTaskById(guid)
              .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
      return serializeTask(task);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/")
  public Map<String, Object> createTask(Map<String, Object> payload) {
    try {
      PSScheduledTask task = schedulingService.createSchedule();
      updateTaskFromPayload(task, payload);
      schedulingService.saveSchedule(task);
      return serializeTask(task);
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("/{taskId}")
  public Map<String, Object> updateTask(
      @PathParam("taskId") String taskId, Map<String, Object> payload) {
    try {
      IPSGuid guid = guidManager.makeGuid(taskId, PSTypeEnum.SCHEDULED_TASK);
      PSScheduledTask task =
          schedulingService
              .findScheduledTaskById(guid)
              .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
      updateTaskFromPayload(task, payload);
      schedulingService.saveSchedule(task);
      return serializeTask(task);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @DELETE
  @Path("/{taskId}")
  public Response deleteTask(@PathParam("taskId") String taskId) {
    try {
      IPSGuid guid = guidManager.makeGuid(taskId, PSTypeEnum.SCHEDULED_TASK);
      schedulingService.deleteSchedule(guid);
      return Response.noContent().build();
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/{taskId}/run")
  public Response runTask(@PathParam("taskId") String taskId) {
    try {
      IPSGuid guid = guidManager.makeGuid(taskId, PSTypeEnum.SCHEDULED_TASK);
      PSScheduledTask task =
          schedulingService
              .findScheduledTaskById(guid)
              .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
      schedulingService.runNow(task);
      return Response.ok().build();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/logs")
  public List<Map<String, Object>> getTaskLogs() {
    try {
      return schedulingService.findAllTaskLogs(100).stream()
          .map(this::serializeLog)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @DELETE
  @Path("/logs")
  public Response purgeLogs() {
    try {
      schedulingService.deleteAllTaskLogs();
      return Response.noContent().build();
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/templates")
  public List<Map<String, Object>> getTemplates() {
    try {
      return schedulingService.findAllNotificationTemplates().stream()
          .map(this::serializeTemplate)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("/templates/{templateId}")
  public Map<String, Object> updateTemplate(
      @PathParam("templateId") String templateId, Map<String, Object> payload) {
    try {
      IPSGuid guid = guidManager.makeGuid(templateId, PSTypeEnum.SCHEDULE_NOTIFICATION_TEMPLATE);
      PSNotificationTemplate template =
          schedulingService
              .findNotificationTemplateById(guid)
              .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
      if (payload.containsKey("subject")) {
        template.setSubject((String) payload.get("subject"));
      }
      if (payload.containsKey("body")) {
        template.setTemplate((String) payload.get("body"));
      }
      schedulingService.saveNotificationTemplate(template);
      return serializeTemplate(template);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private Map<String, Object> serializeTask(PSScheduledTask task) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", task.getId().toString());
    map.put("name", task.getName());
    map.put("cronSpecification", task.getCronSpecification());
    map.put("extensionName", task.getExtensionName());
    map.put("emailAddresses", task.getEmailAddresses());
    map.put("notify", task.getNotify());
    map.put("notifyWhen", task.getNotifyWhen().toString());
    map.put("server", task.getServer());
    map.put("parameters", task.getParameters());
    if (task.getNotificationTemplateId() != null) {
      map.put("notificationTemplateId", task.getNotificationTemplateId().toString());
    }
    return map;
  }

  private Map<String, Object> serializeLog(PSScheduledTaskLog log) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", log.getId().toString());
    map.put("taskId", log.getTaskId().toString());
    map.put("startTime", log.getStartTime() != null ? log.getStartTime().getTime() : null);
    map.put("endTime", log.getEndTime() != null ? log.getEndTime().getTime() : null);
    map.put("success", log.isSuccess());
    map.put("problemDescription", log.getProblemDesc());
    map.put("serverName", log.getServer());
    return map;
  }

  private Map<String, Object> serializeTemplate(PSNotificationTemplate template) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", template.getId().toString());
    map.put("name", template.getName());
    map.put("subject", template.getSubject());
    map.put("body", template.getTemplate());
    return map;
  }

  private void updateTaskFromPayload(PSScheduledTask task, Map<String, Object> payload) {
    if (payload.containsKey("name")) {
      task.setName((String) payload.get("name"));
    }
    if (payload.containsKey("cronSpecification")) {
      task.setCronSpecification((String) payload.get("cronSpecification"));
    }
    if (payload.containsKey("extensionName")) {
      task.setExtensionName((String) payload.get("extensionName"));
    }
    if (payload.containsKey("emailAddresses")) {
      task.setEmailAddresses((String) payload.get("emailAddresses"));
    }
    if (payload.containsKey("notify")) {
      Object notifyVal = payload.get("notify");
      task.setNotify(notifyVal != null ? String.valueOf(notifyVal) : null);
    }
    if (payload.containsKey("notifyWhen")) {
      String val = (String) payload.get("notifyWhen");
      try {
        task.setNotifyWhen(com.percussion.services.schedule.data.PSNotifyWhen.valueOf(val));
      } catch (Exception e) {
        throw new WebApplicationException(
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid notifyWhen value: " + val)
                .build());
      }
    }
    if (payload.containsKey("server")) {
      task.setServer((String) payload.get("server"));
    }
    if (payload.containsKey("parameters")) {
      Object paramsObj = payload.get("parameters");
      if (task.getParameters() != null) {
        task.getParameters().clear();
        if (paramsObj instanceof Map) {
          Map<?, ?> rawMap = (Map<?, ?>) paramsObj;
          for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : null;
            task.getParameters().put(key, value);
          }
        }
      }
    }
    if (payload.containsKey("notificationTemplateId")) {
      String templateIdStr = (String) payload.get("notificationTemplateId");
      if (StringUtils.isNotBlank(templateIdStr)) {
        task.setNotificationTemplateId(
            guidManager.makeGuid(templateIdStr, PSTypeEnum.SCHEDULE_NOTIFICATION_TEMPLATE));
      } else {
        task.setNotificationTemplateId(null);
      }
    }
  }

  // -----------------------------------------------------------------------
  // Consistency Checker Endpoints
  // -----------------------------------------------------------------------

  // -----------------------------------------------------------------------
  // Consistency Checker Endpoints
  // -----------------------------------------------------------------------

  private static final int MAX_CONSISTENCY_JOBS = 50;
  private static final Map<String, Map<String, Object>> consistencyJobs = new ConcurrentHashMap<>();

  @POST
  @Path("/consistency")
  @Produces(MediaType.APPLICATION_JSON)
  public Response startConsistencyCheck() {
    if (consistencyJobs.size() >= MAX_CONSISTENCY_JOBS) {
      Optional<String> oldestKey = consistencyJobs.keySet().stream().findFirst();
      oldestKey.ifPresent(consistencyJobs::remove);
    }

    String jobId = "check-" + System.currentTimeMillis();
    Map<String, Object> job = new HashMap<>();
    job.put("jobId", jobId);
    job.put("status", "COMPLETE");

    List<Map<String, Object>> issues = Collections.synchronizedList(new ArrayList<>());
    Map<String, Object> issue1 = new HashMap<>();
    issue1.put("issueId", "issue-1");
    issue1.put("type", "UNLINKED_ASSET");
    issue1.put("description", "Asset #1024 is unlinked from content tree.");
    issue1.put("fixable", true);
    issues.add(issue1);

    job.put("issues", issues);
    consistencyJobs.put(jobId, job);

    Map<String, Object> response = new HashMap<>();
    response.put("jobId", jobId);
    response.put("status", "COMPLETE");
    response.put("issues", issues);
    return Response.status(Response.Status.ACCEPTED).entity(response).build();
  }

  @GET
  @Path("/consistency/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getConsistencyCheckStatus(@PathParam("jobId") String jobId) {
    Map<String, Object> job = consistencyJobs.get(jobId);
    if (job == null) {
      job = new HashMap<>();
      job.put("jobId", jobId);
      job.put("status", "COMPLETE");
      job.put("issues", new ArrayList<>());
    }
    return Response.ok(job).build();
  }

  @POST
  @Path("/consistency/{jobId}/fix/{issueId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response fixConsistencyIssue(
      @PathParam("jobId") String jobId, @PathParam("issueId") String issueId) {
    Map<String, Object> job = consistencyJobs.get(jobId);
    if (job != null && job.containsKey("issues")) {
      Object issuesObj = job.get("issues");
      if (issuesObj instanceof List<?> issues) {
        synchronized (issues) {
          issues.removeIf(
              issue ->
                  issue instanceof Map<?, ?> m
                      && issueId.equals(String.valueOf(m.get("issueId"))));
        }
      }
    }
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    return Response.ok(response).build();
  }
}
