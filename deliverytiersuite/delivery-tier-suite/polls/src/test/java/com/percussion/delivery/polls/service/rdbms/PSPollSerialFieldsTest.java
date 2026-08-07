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
package com.percussion.delivery.polls.service.rdbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.delivery.polls.data.IPSPollAnswer;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link PSPoll} / {@link PSPollAnswer} after dropping Java {@link
 * Serializable} (issue #2042 / replace {@code @SuppressWarnings("serial")}).
 */
public class PSPollSerialFieldsTest {

  @Test
  @DisplayName("PSPoll is not Serializable so Set pollAnswers needs no serial suppress")
  void psPollIsNotSerializable() {
    assertFalse(Serializable.class.isAssignableFrom(PSPoll.class));
    assertFalse(new PSPoll() instanceof Serializable);
  }

  @Test
  @DisplayName("PSPollAnswer is not Serializable (owning poll back-ref is a JPA association)")
  void psPollAnswerIsNotSerializable() {
    assertFalse(Serializable.class.isAssignableFrom(PSPollAnswer.class));
    assertFalse(new PSPollAnswer() instanceof Serializable);
  }

  @Test
  @DisplayName("setPollAnswers retains the caller Set instance for Hibernate collection semantics")
  void setPollAnswersRetainsInstance() {
    PSPoll poll = new PSPoll();
    Set<IPSPollAnswer> answers = new HashSet<>();
    PSPollAnswer answer = new PSPollAnswer();
    answer.setAnswer("Yes");
    answer.setCount(1);
    answers.add(answer);

    poll.setPollAnswers(answers);

    assertSame(answers, poll.getPollAnswers());
    assertEquals(1, poll.getPollAnswers().size());
    assertTrue(poll.getPollAnswers().contains(answer));
  }

  @Test
  @DisplayName("pollAnswers association round-trips answer text and count")
  void pollAnswersRoundTrip() {
    PSPoll poll = new PSPoll();
    poll.setPollName("n");
    poll.setPollQuestion("q?");

    PSPollAnswer answer = new PSPollAnswer();
    answer.setAnswer("Maybe");
    answer.setCount(3);
    answer.setPoll(poll);

    Set<IPSPollAnswer> answers = new HashSet<>();
    answers.add(answer);
    poll.setPollAnswers(answers);

    IPSPollAnswer stored = poll.getPollAnswers().iterator().next();
    assertEquals("Maybe", stored.getAnswer());
    assertEquals(3, stored.getCount());
    assertSame(poll, answer.getPoll());
  }
}
