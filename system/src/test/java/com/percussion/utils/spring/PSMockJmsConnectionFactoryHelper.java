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
package com.percussion.utils.spring;

import com.percussion.utils.jndi.PSNamingContextHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Create the connection factory for the mock JMS system. This bean registers
 * the created factory as <em>jms/ConnectionFactory</em> using Mockito-created
 * mock objects.
 *
 * @author dougrand
 *
 */
public class PSMockJmsConnectionFactoryHelper
{

   /**
    * Logger used for publisher service
    */
   private static final Logger ms_log = LogManager.getLogger(PSMockJmsConnectionFactoryHelper.class);

   /**
    * Property binding for the initial JMS connection factory JNDI name.
    * This constant must be the same as the "jndiName" property specified by
    * "sys_jmsConnectionFactory" bean in beans.xml
    */
   private static final String JMS_CONNECTION_FACTORY = "java:comp/env/jms/ConnectionFactory";

   /**
    * Mock connection factory instance
    */
   private static final ConnectionFactory ms_jmsConnectionFactory = mock(ConnectionFactory.class);

   /**
    * Map to store created mock destinations
    */
   private static final Map<String, Object> ms_destinations = new HashMap<>();

   /**
    * Destinations to configure, set from the spring configuration.
    */
   private Map<String, String> m_destinations = null;

   /**
    * The context helper, set in the ctor
    */
   private PSNamingContextHelper m_helper = null;

   /**
    * Create the instance
    *
    * @param helper the jndi helper, never <code>null</code>.
    * @throws NamingException on error binding to JNDI
    */
   public PSMockJmsConnectionFactoryHelper(PSNamingContextHelper helper)
   throws NamingException {
      if (helper == null)
      {
         throw new IllegalArgumentException("helper may not be null");
      }
      m_helper = helper;
      m_helper.addBareBinding(JMS_CONNECTION_FACTORY, ms_jmsConnectionFactory);
   }

   /**
    * @return the destinations map
    */
   public Map<String, String> getDestinations()
   {
      return m_destinations;
   }

   /**
    * @param destinations the destinations to set, where key is JNDI name and value is "queue" or "topic"
    * @throws JMSException on error creating destination
    * @throws NamingException on error binding to JNDI
    */
   public void setDestinations(Map<String, String> destinations)
         throws JMSException, NamingException
   {
      m_destinations = destinations;
      // Add destinations to JNDI. The key of each entry is the jndi
      // name, the value is the string "topic" or "queue"
      for (Map.Entry<String, String> destination : m_destinations.entrySet())
      {
         String jndiname = destination.getKey();
         String type = destination.getValue();
         Object destinationObject;

         if (type.equals("topic"))
         {
            destinationObject = mock(Topic.class);
            ms_destinations.put(jndiname, destinationObject);
            m_helper.addBareBinding(jndiname, destinationObject);
         }
         else if (type.equals("queue"))
         {
            destinationObject = mock(Queue.class);
            ms_destinations.put(jndiname, destinationObject);
            m_helper.addBareBinding(jndiname, destinationObject);
         }
         else
         {
            ms_log.warn("Unknown type found " + type);
         }
      }
   }
}
