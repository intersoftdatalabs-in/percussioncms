/*
 *     Percussion CMS
 *     Copyright (C) 1999-2021 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.taxonomy.repository;

import com.percussion.taxonomy.domain.Node_editor;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;

import java.util.Collection;

public class HibernateNode_editorDAO extends HibernateDaoSupport implements Node_editorDAO {

    public Node_editor getNode_editor(int id) {
        return (Node_editor) getHibernateTemplate().get(Node_editor.class, new Integer(id));
    }

    public Collection getAllNode_editors() {
        //Optional: Add order by to query
        return getHibernateTemplate().find("from Node_editor nod");
    }

    public void saveNode_editor(Node_editor node_editor) {
        getHibernateTemplate().saveOrUpdate(node_editor);
    }

    public void removeNode_editor(Node_editor node_editor) {
        getHibernateTemplate().delete(node_editor);
    }
    
    public void removeNode_editors(Collection<Node_editor> node_editors) {
        getHibernateTemplate().deleteAll(node_editors);
    }
}
