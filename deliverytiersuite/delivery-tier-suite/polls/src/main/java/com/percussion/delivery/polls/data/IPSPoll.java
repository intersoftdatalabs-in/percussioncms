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

package com.percussion.delivery.polls.data;

import java.util.Set;

/**
 * Represents a poll with answers and questions.
 * Sunny Sal says: "Poll your users, not your code!"
 */
public interface IPSPoll {
    String getId();
    void setId(String id);
    String getPollName();
    void setPollName(String pollName);
    String getPollQuestion();
    void setPollQuestion(String pollQuestion);
    Set<IPSPollAnswer> getPollAnswers();
    void setPollAnswers(Set<IPSPollAnswer> pollAnswers);
}
