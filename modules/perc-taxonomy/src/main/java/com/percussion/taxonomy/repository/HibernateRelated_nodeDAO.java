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

import com.percussion.taxonomy.domain.Related_node;
import java.util.Collection;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class HibernateRelated_nodeDAO implements Related_nodeDAO {

  @Autowired private SessionFactory sessionFactory;

  public Related_node getRelated_node(int id) {
    Session session = sessionFactory.getCurrentSession();
    return session.find(Related_node.class, id);
  }

  public Collection<Related_node> getAllRelated_nodes() {
    String queryString = "from Related_node rn left join fetch rn.relationship";
    Session session = sessionFactory.getCurrentSession();
    return session.createQuery(queryString, Related_node.class).list();
  }

  public void saveRelated_node(Related_node related_node) {
    Session session = sessionFactory.getCurrentSession();
    session.merge(related_node);
  }

  public void removeRelated_node(Related_node related_node) {
    Session session = sessionFactory.getCurrentSession();
    session.remove(related_node);
  }
}
