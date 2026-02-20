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

package com.percussion.activity.service;

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.injectDependencies;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.util.PSStopwatch;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Integration tests for {@link IPSActivityService}. */
@ExtendWith(SpringExtension.class)

class PSActivityServiceTest {
  private static final Logger log = LogManager.getLogger(PSActivityServiceTest.class);
  private boolean hasStarted = false;
  private IPSActivityService activityService;

  @BeforeEach
  void setUp() {
    try {
      if (!hasStarted) {
        injectDependencies(this);
        hasStarted = true;
      }
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  @AfterEach
  void tearDown() {
    // No teardown logic required
  }

  @Test
  void testNewContentActivities() throws Exception {
    var dates = new ArrayList<Date>();
    dates.add(new Date());
    dates.add(new Date());
    var counts = activityService.findNewContentActivities(Collections.emptyList(), dates);
    assertEquals(1, counts.size());
  }

  @Test
  void testPerformance() throws Exception {
    var path = "//Sites/EnterpriseInvestments";
    var ids = activityService.findItemIdsByPath(path, null);

    var beginDate = getDate(2008, 3, 24, 0, 0, 0); // 2008-3-24 00:00:00
    var dates = new ArrayList<Date>();
    dates.add(beginDate);
    dates.add(new Date());
    var sw = new PSStopwatch();

    sw.start();
    activityService.findNewContentActivities(ids, dates);
    sw.stop();
    System.out.println("findNewContentActivities('" + path + "'): " + sw);

    sw.start();
    activityService.findNumberContentActivities(ids, dates, "Public", null);
    sw.stop();
    System.out.println("findNumberContentActivities('" + path + "'): " + sw);

    sw.start();
    activityService.findPublishedItems(ids, dates);
    sw.stop();
    System.out.println("findPublishedItems(ids, dates)('" + path + "'): " + sw);

    sw.start();
    activityService.findPublishedItems(ids);
    sw.stop();
    System.out.println("findPublishedItems(ids)('" + path + "'): " + sw);
  }

  private Date getDate(int year, int month, int date, int hour, int minute, int second) {
    var cal = Calendar.getInstance();
    cal.clear();
    cal.set(year, month - 1, date, hour, minute, second);
    return cal.getTime();
  }

  @Test
  void testNewContentActivities_Negative() {
    var dates = new ArrayList<Date>();
    dates.add(new Date());
    dates.add(new Date());

    // negative test: null dates
    assertThrows(
        Exception.class,
        () -> activityService.findNewContentActivities(Collections.emptyList(), null));

    // negative test: single date
    assertThrows(
        Exception.class,
        () ->
            activityService.findNewContentActivities(
                Collections.emptyList(), Collections.singletonList(new Date())));

    // negative test: null ids
    assertThrows(Exception.class, () -> activityService.findNewContentActivities(null, dates));
  }

  public void setActivityService(IPSActivityService activityService) {
    this.activityService = activityService;
  }
}
