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

import com.percussion.taxonomy.domain.Node_editor;
import java.util.Collection;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class HibernateNode_editorDAO implements Node_editorDAO {

  @Autowired private SessionFactory sessionFactory;

  public Node_editor getNode_editor(int id) {
    Session session = sessionFactory.getCurrentSession();
    return session.find(Node_editor.class, id);
  }

  public Collection<Node_editor> getAllNode_editors() {
    Session session = sessionFactory.getCurrentSession();
    return session.createQuery("from Node_editor nod", Node_editor.class).list();
  }

  public void saveNode_editor(Node_editor node_editor) {
    Session session = sessionFactory.getCurrentSession();
    session.merge(node_editor);
  }

  public void removeNode_editor(Node_editor node_editor) {
    Session session = sessionFactory.getCurrentSession();
    session.remove(node_editor);
  }

  public void removeNode_editors(Collection<Node_editor> node_editors) {
    Session session = sessionFactory.getCurrentSession();
    for (Node_editor editor : node_editors) {
      session.remove(editor);
    }
  }
}
