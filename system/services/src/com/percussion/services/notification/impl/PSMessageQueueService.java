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
package com.percussion.services.notification.impl;

import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.server.PSRequest;
import com.percussion.services.jms.IPSQueueSender;
import com.percussion.services.notification.IPSMessageQueueListener;
import com.percussion.services.notification.IPSMessageQueueService;
import com.percussion.utils.request.PSRequestInfo;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * Dispatches to an implementation of {@link IPSMessageQueueListener} based
 * on the instance class of the message.
 *
 * @author adamgent
 * @see #addListener(Class, IPSMessageQueueListener)
 */
public final class PSMessageQueueService implements MessageListener, IPSMessageQueueService
{

   /**
    * Logger to use, never <code>null</code>.
    */
   private static final Logger ms_logger = LogManager.getLogger(
         PSMessageQueueService.class);

   private IPSQueueSender m_queueSender;
   private ConcurrentHashMap<String, IPSMessageQueueListener<?>> queueMap = new ConcurrentHashMap<>();

   /**
    * Associates a Class with a single listener replacing any existing listener bound for that message
    * class.
    *
    * @param <T> The type of message.
    * @param messageType objects of this type will be sent to
    *  listener {@link IPSMessageQueueListener#onMessage(Serializable)}, not null.
    * @param listener the listener that will be called for objects of messageType, not null.
    * {@inheritDoc}
    */
   public <T extends Serializable> void addListener(Class<T> messageType, IPSMessageQueueListener<T> listener) {
      notNull(listener);
      queueMap.put(messageType.getCanonicalName(), listener);
   }

   /**
    * {@inheritDoc}
    */
   public <T extends Serializable> void removeListener(Class<T> messageType) {
      queueMap.remove(messageType.getCanonicalName());
   }

   /**
    * {@inheritDoc}
    */

   public <T extends Serializable> Optional<IPSMessageQueueListener<T>> getListener(Class<T> messageType) {
      notNull(messageType);
      IPSMessageQueueListener<T> listener = (IPSMessageQueueListener<T>) queueMap.get(messageType.getCanonicalName());
      return Optional.ofNullable(listener);
   }

   /**
    * {@inheritDoc}
    */
   public <T extends Serializable> void sendMessage(T message, Integer priority)
   {
      if (priority == null) {
         getQueueSender().sendMessage(message);
      }
      else {
         getQueueSender().sendMessage(message, priority);
      }
   }

   /**
    * {@inheritDoc}
    */

   public void onMessage(Message message)
   {
      try
      {
         PSRequest req = PSRequest.getContextForRequest();
         PSRequestInfo.initRequestInfo((Map) null);
         PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_PSREQUEST, req);

         if (message instanceof ObjectMessage)
         {
            ObjectMessage om = (ObjectMessage) message;
            Serializable object;
            try {
               // JMS deserialization - validate message type (CWE-502)
               object = om.getObject();
               if (object == null) {
                  ms_logger.error("Received null message from queue");
                  return;
               }
            } catch (JMSException e) {
               ms_logger.error("Failed to deserialize message from queue: {}", e.getMessage());
               return;
            }
            String name = object.getClass().getCanonicalName();
            IPSMessageQueueListener<Serializable> ql = (IPSMessageQueueListener<Serializable>)
               queueMap.get(name);
            if (ql == null) {
               ms_logger.error("No listener for type: " + name);
            }
            else {
               try {
                  Serializable queueMessage = om.getObject();
                  if (queueMessage != null) {
                     ql.onMessage(queueMessage);
                  } else {
                     ms_logger.error("Queue message is null for listener: {}", name);
                  }
               } catch (JMSException e) {
                  ms_logger.error("Failed to deserialize message for listener {}: {}", name, e.getMessage());
               }
            }
         }
      }
      catch (Exception e)
      {
         ms_logger.error("Cannot handle jms message", e);
      }
      finally
      {
         PSRequestInfo.resetRequestInfo();

         try
         {
            message.acknowledge();
         }
         catch (JMSException e)
         {
            ms_logger.error("Problem acknowledging message", e);
         }
      }
   }

   /**
    *  See setter.
    * @return never null.
    */
   public IPSQueueSender getQueueSender()
   {
      return m_queueSender;
   }

   /**
    * Return all registered message types as Class objects by resolving the canonical names
    * stored in the internal map. Non-loadable classes are ignored.
    */
   public Set<Class<? extends Serializable>> getRegisteredMessageTypes() {
      Set<Class<? extends Serializable>> types = new java.util.HashSet<>();
      for (String name : queueMap.keySet()) {
         try {
            Class<?> clazz = Class.forName(name);
            if (Serializable.class.isAssignableFrom(clazz)) {

               Class<? extends Serializable> sc = (Class<? extends Serializable>) clazz;
               types.add(sc);
            }
         } catch (ClassNotFoundException e) {
            ms_logger.warn("Registered message type not resolvable: {}", name);
         }
      }
      return types;
   }
   /**
    * The queue wrapper to send messages.
    * @param queueSender never null.
    */
   public void setQueueSender(IPSQueueSender queueSender)
   {
      m_queueSender = queueSender;
   }

}
