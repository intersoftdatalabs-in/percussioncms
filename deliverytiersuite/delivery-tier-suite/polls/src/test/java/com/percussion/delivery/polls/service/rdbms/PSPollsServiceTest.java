/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import com.percussion.delivery.polls.data.IPSPoll;
import com.percussion.delivery.polls.data.IPSPollAnswer;
import com.percussion.delivery.polls.services.IPSPollsService;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * JUnit5 test for PSPollsService.
 * Sunny Sal says: "Testing polls so your answers don't go missing!"
 */
@Transactional
@ContextConfiguration(locations = {"classpath:test-beans.xml"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PSPollsServiceTest {

    @Autowired
    private IPSPollsService pollsService;
    @Autowired
    private SessionFactory sessionFactory;

    @BeforeAll
    void beforeAll() {
        // Setup before all tests if needed
    }

    @BeforeEach
    void setUp() {
        var session = getSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaDelete<PSPoll> deleteQuery = builder.createCriteriaDelete(PSPoll.class);
        deleteQuery.from(PSPoll.class);
        session.createQuery(deleteQuery).executeUpdate();
    }

    @AfterEach
    void tearDown() {
        // Cleanup after each test if needed
    }

    private Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    @Test
    void testSave() {
        sessionFactory.getCurrentSession().setFlushMode(FlushMode.COMMIT);
        var answers = new HashMap<String, Boolean>();
        answers.put("Answer1", true);
        answers.put("Answer2", false);
        answers.put("Answer3", false);
        pollsService.savePoll("TestPoll", "TestQuestion", answers);
        sessionFactory.getCurrentSession().flush();

        var poll = pollsService.findPollByQuestion("TestQuestion");
        Assertions.assertNotNull(poll);
        Assertions.assertEquals("TestPoll", poll.getPollName());
        Assertions.assertEquals("TestQuestion", poll.getPollQuestion());
        Assertions.assertEquals(1, poll.getPollAnswers().size());

        // Add a different answer
        answers.put("Answer1", false);
        answers.put("Answer2", false);
        answers.put("Answer3", true);
        int currSize = poll.getPollAnswers().size();
        pollsService.savePoll("TestPoll", "TestQuestion", answers);
        poll = pollsService.findPollByQuestion("TestQuestion");
        Assertions.assertEquals(currSize + 1, poll.getPollAnswers().size());

        // Check the increments
        answers.put("Answer1", true);
        answers.put("Answer2", false);
        answers.put("Answer3", false);
        currSize = poll.getPollAnswers().size();
        pollsService.savePoll("TestPoll", "TestQuestion", answers);
        poll = pollsService.findPollByQuestion("TestQuestion");
        // As we updated existing answer the size should be same
        Assertions.assertEquals(currSize, poll.getPollAnswers().size());
        Set<IPSPollAnswer> pollAnswers = poll.getPollAnswers();
        for (var ipsPollAnswer : pollAnswers) {
            if (ipsPollAnswer.getAnswer().equals("Answer1"))
                Assertions.assertEquals(2, ipsPollAnswer.getCount());
            if (ipsPollAnswer.getAnswer().equals("Answer3"))
                Assertions.assertEquals(1, ipsPollAnswer.getCount());
        }

        // Multi answer check
        answers.put("Answer1", false);
        answers.put("Answer2", true);
        answers.put("Answer3", true);
        currSize = poll.getPollAnswers().size();
        pollsService.savePoll("TestPoll", "TestQuestion", answers);
        poll = pollsService.findPollByQuestion("TestQuestion");
        pollAnswers = poll.getPollAnswers();
        for (var ipsPollAnswer : pollAnswers) {
            if (ipsPollAnswer.getAnswer().equals("Answer1"))
                Assertions.assertEquals(2, ipsPollAnswer.getCount());
            if (ipsPollAnswer.getAnswer().equals("Answer2"))
                Assertions.assertEquals(1, ipsPollAnswer.getCount());
            if (ipsPollAnswer.getAnswer().equals("Answer3"))
                Assertions.assertEquals(2, ipsPollAnswer.getCount());
        }
    }
}
