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

package com.percussion.delivery.comments.service.rdbms;

import com.percussion.delivery.comments.dao.IPSCommentsDao;
import com.percussion.delivery.comments.services.IPSCommentsService;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test configuration for PSCommentsServiceTest. Provides mock beans and necessary configuration for
 * running tests.
 */
@Configuration
public class PSCommentsServiceTestConfig {

  @Bean
  public SessionFactory sessionFactory() {
    return Mockito.mock(SessionFactory.class);
  }

  @Bean
  public IPSCommentsDao commentsDao() {
    return Mockito.mock(IPSCommentsDao.class);
  }

  @Bean
  public IPSCommentsService commentService() {
    return new PSCommentsService(commentsDao());
  }
}
