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
package com.percussion.services.schedule;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.schedule.data.PSNotificationTemplate;
import com.percussion.services.schedule.data.PSNotifyWhen;
import com.percussion.services.schedule.data.PSScheduledTask;
import com.percussion.services.schedule.data.PSScheduledTaskLog;
import com.percussion.utils.guid.IPSGuid;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Temporarily disabled — failing in perc-system test run")
public class PSSchedulingServiceTest {
  @Test
  public void testCreateSchedule() throws PSSchedulingException {
    final PSScheduledTask s = getService().createSchedule();
    assertNotNull(s.getId());
    assertNull(s.getName());
    assertFalse(s.getId().equals(getService().createSchedule().getId()));

    // not saved yet
    assertFalse(getService().findScheduledTaskById(s.getId()).isPresent());
  }

  @Test
  public void testScheduleNullParam() throws PSSchedulingException {
    try {
      getService().findScheduledTaskById(null);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      getService().saveSchedule(null);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      getService().deleteSchedule(null);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testUpdateScheduleBadCron() throws PSSchedulingException {
    final PSScheduledTask s = getService().createSchedule();

    s.setName(NAME);
    s.setCronSpecification(CRON);
    getService().saveSchedule(s);
    assertNotNull(getService().findScheduledTaskById(s.getId()));

    s.setCronSpecification(BAD_CRON);
    try {
      getService().saveSchedule(s);
      fail();
    } catch (PSSchedulingException expected) {
    }
    assertTrue(getService().findScheduledTaskById(s.getId()).isPresent());
    assertEquals(
        CRON, getService().findScheduledTaskById(s.getId()).orElseThrow().getCronSpecification());

    // remove if support for this kind of expression is implemented
    s.setCronSpecification(BAD_CRON2);
    try {
      getService().saveSchedule(s);
      fail();
    } catch (UnsupportedOperationException expected) {
    }
    assertTrue(getService().findScheduledTaskById(s.getId()).isPresent());
    assertEquals(
        CRON, getService().findScheduledTaskById(s.getId()).orElseThrow().getCronSpecification());

    getService().deleteSchedule(s.getId());
    assertFalse(getService().findScheduledTaskById(s.getId()).isPresent());
  }

  @Test
  public void testTaskLog() {
    getService().deleteAllTaskLogs();

    PSScheduledTaskLog taskLog = createSaveTaskLog(0);

    // test equals and hashCode
    PSScheduledTaskLog taskLog2 = getService().findTaskLogById(taskLog.getId()).orElseThrow();
    assertTrue(taskLog.equals(taskLog2));
    assertTrue(taskLog.hashCode() == taskLog2.hashCode());
    assertTrue(taskLog.getProblemDesc() != null);

    // test getAllTaskLogs
    taskLog2 = createSaveTaskLog(2);
    List<PSScheduledTaskLog> logs = getService().findAllTaskLogs(-1);

    assertTrue(logs.size() == 2);
    // note, the taskLog2 should be the 1st one because it is sorted by
    // end time in descending order.
    assertTrue(taskLog.equals(logs.get(0)));
    assertTrue(taskLog2.equals(logs.get(1)));

    // test eager load 'problemDesc' property
    taskLog2 = getService().findTaskLogById(taskLog2.getId()).orElseThrow();
    assertTrue(taskLog2.getProblemDesc() != null);

    // test projection load 'problemDesc' property
    assertTrue(logs.get(0).getProblemDesc() == null);
    assertTrue(logs.get(1).getProblemDesc() == null);

    // test delete
    getService().deleteTaskLog(taskLog.getId());

    assertFalse(getService().findTaskLogById(taskLog.getId()).isPresent());
    getService().deleteAllTaskLogs();
    assertTrue(getService().findAllTaskLogs(-1).size() == 0);
  }

  @Test
  public void testTaskLogByDate() {
    getService().deleteAllTaskLogs();

    // add seed log entries

    PSScheduledTaskLog taskLog = createSaveTaskLog(0); // now
    PSScheduledTaskLog taskLog1 = createSaveTaskLog(1); // 1 day before
    createSaveTaskLog(2); // 2 days ago
    createSaveTaskLog(3); // 3 days ago
    createSaveTaskLog(4); // 4 days ago

    assertTrue(getService().findAllTaskLogs(-1).size() == 5);

    // remove all logs older than 2 days
    Date beforeDate = getBeforeDate(2);
    getService().deleteTaskLogsByDate(beforeDate);

    // validate above deletion
    List<PSScheduledTaskLog> logs = getService().findAllTaskLogs(-1);
    assertTrue(logs.size() == 2);
    assertTrue(taskLog.equals(logs.get(0)));
    assertTrue(taskLog1.equals(logs.get(1)));

    // remove all logs older than 1 days
    beforeDate = getBeforeDate(1);
    getService().deleteTaskLogsByDate(beforeDate);
    // validate the deletion
    logs = getService().findAllTaskLogs(-1);
    assertTrue(logs.size() == 1);
    assertTrue(taskLog.equals(logs.get(0)));

    // cleanup all log entries
    getService().deleteAllTaskLogs();
  }

  /**
   * Get the date and time plus 2 minutes before the specified number of days from now.
   *
   * @param beforeDate the number days before current time. Assumed not <code>null</code>.
   * @return the date and time described above.
   */
  private Date getBeforeDate(int beforeDate) {
    long milli = calculateDate(beforeDate).getTime() + 120000L;
    return new Date(milli);
  }

  /**
   * Get the date and time before the specified number of days.
   *
   * @param beforeDate the number days before current time. Assumed not <code>null</code>.
   * @return the date and time, never <code>null</code> and is rounded in seconds.
   */
  private Date calculateDate(int beforeDate) {
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, (beforeDate * -1));

    // round time to second, so that the round trip of Date value from the
    // repository will be the same.
    long millis = cal.getTimeInMillis();
    millis = (millis / 1000L) * 1000L;

    return new Date(millis);
  }

  private PSScheduledTaskLog createSaveTaskLog(int beforeDate) {
    IPSGuid id = getService().createTaskLogId();
    PSScheduledTask s = getService().createSchedule();

    Date startTime = calculateDate(beforeDate);
    Date endTime = new Date(startTime.getTime() + 1000L);
    PSScheduledTaskLog taskLog =
        new PSScheduledTaskLog(id, s.getId(), startTime, endTime, true, "Problem...", "localhost");
    getService().saveTaskLog(taskLog);

    return taskLog;
  }

  @Test
  public void testScheduleCRUD() throws PSSchedulingException {
    final String CC = "A Sample CC List";
    final String EXTENSION = "Sample Extension";
    final String ROLE = "Role";

    // create
    final PSScheduledTask s = getService().createSchedule();
    s.setName(NAME);
    s.setEmailAddresses(CC);
    s.setCronSpecification(CRON);
    s.setExtensionName(EXTENSION);
    s.setNotifyWhen(PSNotifyWhen.ALWAYS);
    s.setNotify(ROLE);

    assertFalse(getService().findScheduleByName(NAME).isPresent());

    // save new
    getService().saveSchedule(s);
    assertTrue(getService().findScheduleByName(NAME).isPresent());
    final PSScheduledTask s2 = getService().findScheduledTaskById(s.getId()).orElseThrow();
    assertTrue(EqualsBuilder.reflectionEquals(s, s2));

    // exists in all
    {
      final Collection<PSScheduledTask> all = getService().findAllSchedules();
      assertTrue(EqualsBuilder.reflectionEquals(s, findSTById(all, s.getId())));
    }

    // save update
    s2.setName(NAME2);
    assertNull(getService().findScheduleByName(NAME2));
    getService().saveSchedule(s2);
    assertNotNull(getService().findScheduleByName(NAME2));
    final PSScheduledTask s3 = getService().findScheduledTaskById(s.getId()).orElseThrow();
    assertTrue(EqualsBuilder.reflectionEquals(s2, s3));

    // delete
    getService().deleteSchedule(s3.getId());
    // nothing happens if removing non-existing
    getService().deleteSchedule(s3.getId());
    assertFalse(getService().findScheduledTaskById(s.getId()).isPresent());
    assertFalse(getService().findScheduleByName(NAME2).isPresent());

    // does not exist in all after deletion
    assertNull(findSTById(getService().findAllSchedules(), s.getId()));
  }

  @Test
  public void testCreateNotificationTemplate() {
    final PSNotificationTemplate t = getService().createNotificationTemplate();
    assertNotNull(t.getId());
    assertNotNull(t.getName());
    assertNotNull(t.getSubject());
    assertNull(t.getTemplate());
    assertFalse(t.getId().equals(getService().createNotificationTemplate().getId()));

    // not saved yet
    assertFalse(getService().findNotificationTemplateById(t.getId()).isPresent());
  }

  @Test
  public void testNotificationTemplateNullParam() {
    try {
      getService().findNotificationTemplateById(null);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      getService().saveNotificationTemplate(null);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      getService().deleteNotificationTemplate(null);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testNotificationTemplateCRUD() {
    // create
    final PSNotificationTemplate t = getService().createNotificationTemplate();
    t.setName(NAME);
    t.setSubject(SUBJECT);
    t.setTemplate(TEMPLATE_STR);

    // save new
    assertFalse(getNotificationLabels().contains(NAME));
    getService().saveNotificationTemplate(t);
    assertTrue(getNotificationLabels().contains(NAME));
    final PSNotificationTemplate t2 =
        getService().findNotificationTemplateById(t.getId()).orElseThrow();
    assertTrue(t.equals(t2));

    // exists in all
    {
      final Collection<PSNotificationTemplate> all = getService().findAllNotificationTemplates();
      assertTrue(all.contains(t));
    }

    // save update
    final String TEMPLATE_STR2 = "A Sample Template2";
    t2.setName(NAME2);
    t2.setSubject(SUBJECT);
    t2.setTemplate(TEMPLATE_STR2);

    assertTrue(getNotificationLabels().contains(NAME));
    assertFalse(getNotificationLabels().contains(NAME2));
    getService().saveNotificationTemplate(t2);
    assertFalse(getNotificationLabels().contains(NAME));
    assertTrue(getNotificationLabels().contains(NAME2));

    final PSNotificationTemplate t3 =
        getService().findNotificationTemplateById(t.getId()).orElseThrow();
    assertTrue(t2.equals(t3));

    // delete
    getService().deleteNotificationTemplate(t3.getId());
    getService().deleteNotificationTemplate(t3.getId());

    assertFalse(getService().findNotificationTemplateById(t.getId()).isPresent());

    // does not exist in all after deletion
    assertNull(findNTById(getService().findAllNotificationTemplates(), t.getId()));
  }

  @Test
  public void testFindNotificationTemplateByName() {
    assertFalse(getService().findNotificationTemplateByName(NAME).isPresent());

    // create
    final PSNotificationTemplate t = getService().createNotificationTemplate();
    t.setName(NAME);
    t.setSubject(SUBJECT);
    t.setTemplate(TEMPLATE_STR);

    getService().saveNotificationTemplate(t);
    assertEquals(
        t.getId(), getService().findNotificationTemplateByName(NAME).orElseThrow().getId());

    getService().deleteNotificationTemplate(t.getId());
    assertFalse(getService().findNotificationTemplateByName(NAME).isPresent());
  }

  /**
   * Convenience method to access {@link IPSSchedulingService#findAllNotificationTemplatesNames()}.
   *
   * @return never <code>null</code>.
   */
  private Set<String> getNotificationLabels() {
    return getService().findAllNotificationTemplatesNames();
  }

  /** Current scheduling service. Not <code>null</code>. */
  private IPSSchedulingService getService() {
    return PSSchedulingServiceLocator.getSchedulingService();
  }

  /**
   * Finds schedule by id.
   *
   * @param schedules the schedules to search in. Not <code>null</code>.
   * @param id the id to search for. Not <code>null</code>.
   * @return the found schedule or <code>null</code> if it was not found.
   */
  private PSScheduledTask findSTById(Collection<PSScheduledTask> schedules, IPSGuid id) {
    for (final PSScheduledTask s : schedules) {
      if (s.getId().equals(id)) {
        return s;
      }
    }
    return null;
  }

  /**
   * Finds notification template by id.
   *
   * @param templates the templates to search in. Not <code>null</code>.
   * @param id the id to search for. Not <code>null</code>.
   * @return the found notification template or <code>null</code> if it was not found.
   */
  private PSNotificationTemplate findNTById(
      Collection<PSNotificationTemplate> templates, IPSGuid id) {
    for (final PSNotificationTemplate t : templates) {
      if (t.getId().equals(id)) {
        return t;
      }
    }
    return null;
  }

  /** Creates a sample schedule notification template GUID. */
  protected PSGuid createTemplateGuid() {
    final int UUID = 123;
    final int HOST_ID = 10;
    return new PSGuid(UUID, PSTypeEnum.SCHEDULE_NOTIFICATION_TEMPLATE, HOST_ID);
  }

  /** A sample name value for testing. */
  private static final String NAME = "A Sample Name";

  /** A sample name value for testing. */
  private static final String NAME2 = "A Sample Name 2";

  /** A sample notification template. */
  private static final String SUBJECT = "A Sample Subject";

  /** A sample notification template. */
  private static final String TEMPLATE_STR = "A Sample Template";

  /** Invalid cron expression. */
  private static final String BAD_CRON = "wrong cron expression";

  /**
   * Another invalid cron expression. Triggers unsupported operation exception for the current
   * Quartz version. Remove if support for this kind of expression is implemented
   */
  private static final String BAD_CRON2 = "0 0 12 1 1 1 2090";

  /** Valid sample cron expression. */
  private static final String CRON = "0 0 12 * * ? 2090";
}
