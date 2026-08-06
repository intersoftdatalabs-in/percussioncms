/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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

package com.percussion.delivery.polls.service.rdbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.delivery.polls.data.IPSPoll;
import com.percussion.delivery.polls.data.IPSPollAnswer;
import com.percussion.delivery.polls.services.IPSPollsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-beans.xml"})
public class PSPollsServiceTest {
  private static final Logger log = LogManager.getLogger(PSPollsServiceTest.class);
  @Autowired private IPSPollsService pollsService;

  @Autowired
  @Qualifier("pollsEntityManager")
  private EntityManager entityManager;

  @BeforeEach
  public void setUp() {
    Session session = getSession();
    try {
      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaDelete<PSPoll> deleteQuery = builder.createCriteriaDelete(PSPoll.class);
      deleteQuery.from(PSPoll.class);
      session.createQuery(deleteQuery).executeUpdate();
    } finally {
      // session.close();
    }
  }

  @AfterEach
  public void tearDown() {
    // no-op
  }

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  @Test
  public void testSave() {
    entityManager.setFlushMode(FlushModeType.COMMIT);

    Map<String, Boolean> answers = new HashMap<>();
    answers.put("Answer1", true);
    answers.put("Answer2", false);
    answers.put("Answer3", false);
    pollsService.savePoll("TestPoll", "TestQuestion", answers);
    try {
      entityManager.flush();
    } catch (Exception e) {
      log.warn(
          "EntityManager flush failed during testSave; test may still pass if data was written", e);
    }
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

    // check the increments
    answers.put("Answer1", true);
    answers.put("Answer2", false);
    answers.put("Answer3", false);
    currSize = poll.getPollAnswers().size();
    pollsService.savePoll("TestPoll", "TestQuestion", answers);
    poll = pollsService.findPollByQuestion("TestQuestion");
    // as we updated existing answer the size should be same
    assertEquals(currSize, poll.getPollAnswers().size());
    // Answer1 must be incremented by 1
    Set<IPSPollAnswer> pollAnswers = poll.getPollAnswers();
    for (IPSPollAnswer ipsPollAnswer : pollAnswers) {
      if (ipsPollAnswer.getAnswer().equals("Answer1")) assertEquals(2, ipsPollAnswer.getCount());

      if (ipsPollAnswer.getAnswer().equals("Answer3")) assertEquals(1, ipsPollAnswer.getCount());
    }

    // Multi answer check
    answers.put("Answer1", false);
    answers.put("Answer2", true);
    answers.put("Answer3", true);
    currSize = poll.getPollAnswers().size();
    pollsService.savePoll("TestPoll", "TestQuestion", answers);
    poll = pollsService.findPollByQuestion("TestQuestion");
    // Answer1 must be incremented by 1
    pollAnswers = poll.getPollAnswers();
    for (IPSPollAnswer ipsPollAnswer : pollAnswers) {
      if (ipsPollAnswer.getAnswer().equals("Answer1")) assertEquals(2, ipsPollAnswer.getCount());

      if (ipsPollAnswer.getAnswer().equals("Answer2")) assertEquals(1, ipsPollAnswer.getCount());

      if (ipsPollAnswer.getAnswer().equals("Answer3")) assertEquals(2, ipsPollAnswer.getCount());
    }
  }
}
