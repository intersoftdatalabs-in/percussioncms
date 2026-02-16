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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.delivery.polls.service.rdbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.delivery.polls.data.IPSPoll;
import com.percussion.delivery.polls.data.IPSPollAnswer;
import com.percussion.delivery.polls.services.IPSPollsService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.when;

/**
 * Unit test for {@link com.percussion.delivery.polls.services.impl.PSPollsService}.
 *
 * <p>This test was previously an integration test using Spring + Hibernate + an in-memory Derby
 * datasource configured in {@code test-beans.xml}. During migration it was disabled because it
 * required a DB-backed {@code SessionFactory}.
 *
 * <p>To keep coverage without DB wiring, this version tests the service logic using a mocked
 * {@link com.percussion.delivery.polls.services.IPSPollsDao}.
 */
@ExtendWith(MockitoExtension.class)
class PSPollsServiceTest {

  private IPSPollsService pollsService;

  @Mock private com.percussion.delivery.polls.services.IPSPollsDao pollsDao;

  @BeforeEach
  void setUp() {
    pollsService = new com.percussion.delivery.polls.services.impl.PSPollsService(pollsDao);
  }

  @BeforeEach
  void cleanPollsTable() {
    // no-op: previous integration test cleared the DB table; unit test uses in-memory objects
  }

  @Test
  void testSave() {
    var pollEntity = new PSPoll();
    pollEntity.setPollAnswers(new java.util.HashSet<>());
    when(pollsDao.findByQuestion("TestQuestion")).thenReturn(null, pollEntity, pollEntity, pollEntity);
    when(pollsDao.createEmptyPoll()).thenReturn(pollEntity);
    when(pollsDao.createEmptyAnswer()).thenAnswer(inv -> new PSPollAnswer());

    Map<String, Boolean> answers = new HashMap<>();
    answers.put("Answer1", true);
    answers.put("Answer2", false);
    answers.put("Answer3", false);

    pollsService.savePoll("TestPoll", "TestQuestion", answers);

    IPSPoll poll = pollsService.findPollByQuestion("TestQuestion");
    assertNotNull(poll);
    assertEquals("TestPoll", poll.getPollName());
    assertEquals("TestQuestion", poll.getPollQuestion());
    assertEquals(1, poll.getPollAnswers().size());

    // add a different answer
    answers.put("Answer1", false);
    answers.put("Answer2", false);
    answers.put("Answer3", true);
    int currSize = poll.getPollAnswers().size();
    pollsService.savePoll("TestPoll", "TestQuestion", answers);
    poll = pollsService.findPollByQuestion("TestQuestion");
    assertEquals(currSize + 1, poll.getPollAnswers().size());

    // check increments
    answers.put("Answer1", true);
    answers.put("Answer2", false);
    answers.put("Answer3", false);
    currSize = poll.getPollAnswers().size();
    pollsService.savePoll("TestPoll", "TestQuestion", answers);
    poll = pollsService.findPollByQuestion("TestQuestion");
    assertEquals(currSize, poll.getPollAnswers().size());

    Set<IPSPollAnswer> pollAnswers = poll.getPollAnswers();
    for (IPSPollAnswer pollAnswer : pollAnswers) {
      if (pollAnswer.getAnswer().equals("Answer1")) {
        assertEquals(2, pollAnswer.getCount());
      }
      if (pollAnswer.getAnswer().equals("Answer3")) {
        assertEquals(1, pollAnswer.getCount());
      }
    }

    // multi-answer check
    answers.put("Answer1", false);
    answers.put("Answer2", true);
    answers.put("Answer3", true);
    currSize = poll.getPollAnswers().size();
    pollsService.savePoll("TestPoll", "TestQuestion", answers);
    poll = pollsService.findPollByQuestion("TestQuestion");

    pollAnswers = poll.getPollAnswers();
    for (IPSPollAnswer pollAnswer : pollAnswers) {
      if (pollAnswer.getAnswer().equals("Answer1")) {
        assertEquals(2, pollAnswer.getCount());
      }
      if (pollAnswer.getAnswer().equals("Answer2")) {
        assertEquals(1, pollAnswer.getCount());
      }
      if (pollAnswer.getAnswer().equals("Answer3")) {
        assertEquals(2, pollAnswer.getCount());
      }
    }
  }
}
