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

package com.percussion.taxonomy.repository;

import com.percussion.taxonomy.domain.Node_status;
import java.util.Collection;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class HibernateNode_statusDAO implements Node_statusDAO {

  @Autowired private SessionFactory sessionFactory;

  public Node_status getNode_status(int id) {
    Session session = sessionFactory.getCurrentSession();
    return session.find(Node_status.class, id);
  }

  public Collection<Node_status> getAllNode_statuss() {
    Session session = sessionFactory.getCurrentSession();
    return session.createQuery("from Node_status nod", Node_status.class).list();
  }

  public void saveNode_status(Node_status node_status) {
    Session session = sessionFactory.getCurrentSession();
    session.merge(node_status);
  }

  public void removeNode_status(Node_status node_status) {
    Session session = sessionFactory.getCurrentSession();
    session.remove(node_status);
  }
}
