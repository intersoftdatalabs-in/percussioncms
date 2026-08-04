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

package com.percussion.delivery.polls.services.impl;

import com.percussion.delivery.polls.data.IPSPoll;
import com.percussion.delivery.polls.data.IPSPollAnswer;
import com.percussion.delivery.polls.service.rdbms.PSPoll;
import com.percussion.delivery.polls.service.rdbms.PSPollAnswer;
import com.percussion.delivery.polls.services.IPSPollsDao;
import com.percussion.delivery.polls.services.IPSPollsService;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

// REFACTORED: CP-JAVA11
/**
 * Default {@link IPSPollsService} implementation. Looks up polls via the supplied {@link
 * IPSPollsDao} and reconciles incoming vote maps against the stored answers before persisting.
 */
public class PSPollsService implements IPSPollsService {
  private IPSPollsDao pollsDao;

  /**
   * Spring-injected constructor.
   *
   * @param pollsDao the polls DAO to delegate persistence to, not {@code null}.
   */
  @Autowired
  public PSPollsService(IPSPollsDao pollsDao) {
    this.pollsDao = pollsDao;
  }

  @Override
  public IPSPoll findPoll(String pollName) {
    return pollsDao.find(pollName);
  }

  @Override
  public void savePoll(String pollName, String pollQuestion, Map<String, Boolean> pollAnswers) {
    var poll = pollsDao.findByQuestion(pollQuestion);
    if (poll == null) {
      poll = pollsDao.createEmptyPoll();
    }
    poll.setPollName(pollName);
    poll.setPollQuestion(pollQuestion);
    var dbPollAnswers = poll.getPollAnswers();
    if (dbPollAnswers == null) {
      dbPollAnswers = new HashSet<>();
      updateAnswers(dbPollAnswers, pollAnswers, (PSPoll) poll);
      poll.setPollAnswers(dbPollAnswers);
    } else {
      updateAnswers(dbPollAnswers, pollAnswers, (PSPoll) poll);
    }
    pollsDao.save(poll);
  }

  /**
   * Updates poll answers in the database poll answers set. Adds new answers or increments count for
   * existing answers.
   *
   * @param dbPollAnswers Set of poll answers from DB, may be empty but not {@code null}.
   * @param pollAnswers Map of answer text to boolean (true if selected), not {@code null}.
   * @param poll The poll entity, not {@code null}.
   */
  private void updateAnswers(
      Set<IPSPollAnswer> dbPollAnswers, Map<String, Boolean> pollAnswers, PSPoll poll) {
    for (var pollAnswer : pollAnswers.entrySet()) {
      var found = false;
      for (var dbPollAnswer : dbPollAnswers) {
        if (dbPollAnswer.getAnswer().equalsIgnoreCase(pollAnswer.getKey())) {
          if (pollAnswer.getValue()) {
            dbPollAnswer.setCount(dbPollAnswer.getCount() + 1);
          }
          found = true;
          break;
        }
      }
      if (!found && pollAnswer.getValue()) {
        var newPollAnswer = (PSPollAnswer) pollsDao.createEmptyAnswer();
        newPollAnswer.setAnswer(pollAnswer.getKey());
        newPollAnswer.setCount(1);
        newPollAnswer.setPoll(poll);
        dbPollAnswers.add(newPollAnswer);
      }
    }
  }

  @Override
  public IPSPoll findPollByQuestion(String pollQuestion) {
    return pollsDao.findByQuestion(pollQuestion);
  }
}
