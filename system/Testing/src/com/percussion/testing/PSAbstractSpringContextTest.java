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

package com.percussion.testing;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.utils.annotations.IgnoreInWebAppSpringContext;

import com.percussion.utils.testing.SpringContextTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.WebApplicationContext;

import javax.naming.Context;
import javax.naming.InitialContext;

//TODO: The spring tests need moved to a new jar.

@Tag({IntegrationTest.class, SpringContextTest.class})
//@WebAppConfiguration("file:../modules/perc-distribution-tree/target/distribution/jetty/base/webapps/Rhythmyx/WEB-INF")
@ContextConfiguration(classes = {PSSpringContextTestConfig.class})
@DisabledInWebAppSpringContext
public class PSAbstractSpringContextTest {

    @Autowired
    protected WebApplicationContext wac;

    @Autowired
    protected ConfigurableApplicationContext ctx;

    @BeforeEach
    public  void setContext(){
        PSBaseServiceLocator.setCtx(ctx);
    }
    @BeforeEach Class
    public static void setupJndi() throws Exception {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.osjava.sj.memory.MemoryContextFactory");
        System.setProperty("org.osjava.sj.jndi.shared", "true");
        InitialContext ic = new InitialContext();

        ic.createSubcontext("java:/comp/env/jdbc");
        ic.createSubcontext("java:comp/env/jms");
        ic.createSubcontext("java:comp/env/queue");
      //  ic.bind("java:comp/env/jms/ConnectionFactory", PSMockJmsConnectionFactoryHelper.getMs_mockFactory());
    }
}
