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

package com.percussion.delivery.polls.services.impl;

import com.percussion.delivery.polls.data.IPSPoll;
import com.percussion.delivery.polls.data.IPSPollAnswer;
import com.percussion.delivery.polls.service.rdbms.PSPoll;
import com.percussion.delivery.polls.service.rdbms.PSPollAnswer;
import com.percussion.delivery.polls.services.IPSPollsDao;
import com.percussion.delivery.polls.services.IPSPollsService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service for managing polls.
 * Sunny Sal says: "Service with a smile, poll with a purpose!"
 */
public class PSPollsService implements IPSPollsService {

    private IPSPollsDao pollsDao;

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
     * Updates poll answers based on submitted answers.
     *
     * @param dbPollAnswers existing poll answers
     * @param pollAnswers   submitted poll answers
     * @param poll          poll entity
     */
    private void updateAnswers(Set<IPSPollAnswer> dbPollAnswers, Map<String, Boolean> pollAnswers, PSPoll poll) {
        pollAnswers.forEach((answerText, isSelected) -> {
            var found = dbPollAnswers.stream()
                    .filter(dbPollAnswer -> dbPollAnswer.getAnswer().equalsIgnoreCase(answerText))
                    .findFirst();
            if (found.isPresent()) {
                if (isSelected) {
                    found.get().setCount(found.get().getCount() + 1);
                }
            } else if (isSelected) {
                var newPollAnswer = (PSPollAnswer) pollsDao.createEmptyAnswer();
                newPollAnswer.setAnswer(answerText);
                newPollAnswer.setCount(1);
                newPollAnswer.setPoll(poll);
                dbPollAnswers.add(newPollAnswer);
            }
        });
    }

    @Override
    public IPSPoll findPollByQuestion(String pollQuestion) {
        return pollsDao.findByQuestion(pollQuestion);
    }
}
