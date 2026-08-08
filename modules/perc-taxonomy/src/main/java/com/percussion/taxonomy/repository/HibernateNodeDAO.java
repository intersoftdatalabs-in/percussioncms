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

import com.percussion.design.objectstore.PSLocator;
import com.percussion.services.contentmgr.impl.IPSContentRepository;
import com.percussion.services.contentmgr.impl.PSContentInternalLocator;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.taxonomy.domain.Attribute;
import com.percussion.taxonomy.domain.Node;
import com.percussion.taxonomy.domain.Node_editor;
import com.percussion.taxonomy.domain.Node_status;
import com.percussion.taxonomy.domain.Related_node;
import com.percussion.taxonomy.domain.Value;
import com.percussion.taxonomy.web.AbstractTaxonEditorController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class HibernateNodeDAO implements NodeDAO {

  @Autowired private SessionFactory sessionFactory;

  private <T> List<T> executeQuery(String queryString, Class<T> resultClass) {
    Session session = sessionFactory.getCurrentSession();
    return session.createQuery(queryString, resultClass).list();
  }

  private <T> List<T> executeQuery(
      String queryString, Class<T> resultClass, Map<String, ?> substitutions) {
    Session session = sessionFactory.getCurrentSession();
    Query<T> query = session.createQuery(queryString, resultClass);
    if (substitutions != null) {
      for (Map.Entry<String, ?> entry : substitutions.entrySet()) {
        query.setParameter(entry.getKey(), entry.getValue());
      }
    }
    return query.list();
  }

  private void executeUpdate(String queryString) {
    Session session = sessionFactory.getCurrentSession();
    Query<?> query = session.createQuery(queryString);
    query.executeUpdate();
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Node getNode(int nodeID, int langID) {

    String queryString = "select distinct n from Node n ";
    queryString += "left join fetch n.taxonomy t ";
    queryString += "left join fetch n.nodeEditors ne ";
    queryString += "left join fetch n.relatedNodesForNodeId rn ";
    queryString += "left join fetch n.values v ";
    queryString += "join fetch v.attribute a ";
    queryString += "left join fetch a.attribute_langs al ";
    queryString += "join fetch al.language ";
    queryString += "join fetch v.lang ";
    queryString += "where ";
    queryString += "n.id = " + nodeID + " ";
    queryString += "and al.language.id = " + langID + " ";
    queryString += "and v.lang.id = " + langID + "order by n.id";

    return executeQuery(queryString, Node.class).iterator().next();
  }

  public Collection<Node> getAllNodes(int taxID, int langID) {

    String queryString = "select distinct n from Node n ";
    queryString += "left join fetch n.taxonomy ";
    queryString += "left join fetch n.nodeEditors ne ";
    queryString += "left join fetch n.relatedNodesForNodeId rn ";
    queryString += "join fetch n.values v ";
    queryString += "join fetch v.attribute a ";
    queryString += "left join fetch a.attribute_langs al ";
    queryString += "join fetch al.language ";
    queryString += "join fetch v.lang ";
    queryString += "where ";
    queryString += "n.taxonomy.id = " + taxID + " ";
    queryString += "and al.language.id = " + langID + " ";
    queryString += "and v.lang.id = " + langID + "order by n.id";

    return executeQuery(queryString, Node.class);
  }

  public Collection<Node> getNodesFromSearch(
      int taxID, int langID, String search_string, boolean exclude_disabled) {
    Session session = sessionFactory.getCurrentSession();

    String queryString = "select distinct n from Node n ";
    queryString += "left join fetch n.taxonomy ";
    queryString += "left join fetch n.nodeEditors ne ";
    queryString += "left join fetch n.relatedNodesForNodeId rn ";
    queryString += "join fetch n.values v ";
    queryString += "join fetch v.attribute a ";
    queryString += "left join fetch a.attribute_langs al ";
    queryString += "join fetch al.language ";
    queryString += "join fetch v.lang ";
    queryString += "where ";
    queryString += "n.taxonomy.id = " + taxID + " ";

    if (exclude_disabled) {
      queryString += "and n.Not_leaf = ? ";
      queryString += "and n.node_status.id = " + Node_status.ACTIVE + " ";
    }

    queryString += "and al.language.id = " + langID + " ";
    queryString += "and v.lang.id = " + langID + " ";
    queryString += "and lower(v.Name)  like ? order by n.id";

    Query<Node> query = session.createQuery(queryString, Node.class);
    int paramIndex = 0;

    if (exclude_disabled) {
      query.setParameter(paramIndex++, false);
    }

    query.setParameter(
        paramIndex++, "%" + StringUtils.lowerCase(StringUtils.trimToEmpty(search_string)) + "%");

    return query.list();
  }

  // Better SQL would eliminate the need for this function
  private Collection<Object[]> concatNames(Collection<Object[]> raw) {

    Collection<Object[]> refined = new ArrayList<Object[]>();

    // Cache for int node_id, int parent_id, String name, boolean Not Leaf,
    // int node_status.id
    Object[] last_node = new Object[5];
    last_node[0] = -1;
    String current_name = "";

    for (Iterator<Object[]> itr = raw.iterator(); itr.hasNext(); ) {
      Object[] current_node = itr.next();

      if (last_node[0].equals(-1)) {
        last_node = current_node;
      }

      if (((Integer) current_node[0]).equals(((Integer) last_node[0])) && itr.hasNext()) {

        current_name += (String) current_node[2] + " (";

      } else if (!((Integer) current_node[0]).equals(((Integer) last_node[0])) && itr.hasNext()) {

        // Save last_node
        last_node[2] = current_name + ")";

        // fix parens
        if (StringUtils.countMatches(last_node[2].toString(), "(")
            > StringUtils.countMatches(last_node[2].toString(), ")")) {

          last_node[2] = last_node[2].toString().replace(" ()", ")");

        } else {
          last_node[2] = last_node[2].toString().replace(" ()", "");
        }

        refined.add(last_node);

        // reset last node to current
        last_node = current_node;
        current_name = (String) current_node[2] + " (";

      } else if (((Integer) current_node[0]).equals(((Integer) last_node[0])) && !itr.hasNext()) {

        // Save last_node
        current_name += (String) current_node[2] + " (";
        last_node[2] = current_name + ")";

        // fix parens
        if (StringUtils.countMatches(last_node[2].toString(), "(")
            > StringUtils.countMatches(last_node[2].toString(), ")")) {

          last_node[2] = last_node[2].toString().replace(" ()", ")");

        } else {
          last_node[2] = last_node[2].toString().replace(" ()", "");
        }
        refined.add(last_node);

      } else if (!((Integer) current_node[0]).equals(((Integer) last_node[0])) && !itr.hasNext()) {

        // Save last_node
        last_node[2] = current_name + ")";

        // fix parens
        if (StringUtils.countMatches(last_node[2].toString(), "(")
            > StringUtils.countMatches(last_node[2].toString(), ")")) {

          last_node[2] = last_node[2].toString().replace(" ()", ")");

        } else {
          last_node[2] = last_node[2].toString().replace(" ()", "");
        }

        refined.add(last_node);

        // save current node
        refined.add(current_node);
      }
    }
    return (refined);
  }

  /** Return nodeID, parentID, and name of all nodes for a given taxonomy */
  public Collection<Object[]> getAllNodeNames(int taxonomyID, int langID) {
    String queryString =
        "select n.id, n.parent.id, v.Name, n.isNodeSelectable, "
            + "n.node_status.id from Node n join n.values v where v.attribute.Is_node_name > 0 "
            + "and n.taxonomy.id = "
            + taxonomyID
            + " and v.lang.id = "
            + langID
            + " order by v.node.id, v.attribute.Is_node_name";

    return concatNames(executeQuery(queryString, Object[].class));
  }

  /** Return nodeID, parentID, and name of all nodes for a given taxonomy */
  public Collection<Object[]> getSomeNodeNames(Collection<Integer> ids, int langID) {
    String queryString =
        "select n.id, n.parent.id, v.Name, n.Not_leaf, "
            + "n.node_status.id from Node n join n.values v where v.attribute.Is_node_name > 0 "
            + "and n.id in ("
            + StringUtils.join(ids.toArray(), ',')
            + ") and v.lang.id = "
            + langID
            + " order by v.node.id, v.attribute.Is_node_name";

    return concatNames(executeQuery(queryString, Object[].class));
  }

  /** Return nodes for ides */
  public Collection<Node> getSomeNodes(Collection<Integer> ids) {
    String queryString =
        "select n from Node n where n.id in(" + StringUtils.join(ids.toArray(), ',') + ")";

    return executeQuery(queryString, Node.class);
  }

  /** Return all values associated with a given node */
  public Collection<Value> getValuesForNode(int nodeID, int langID) {
    String queryString =
        "select v from Value v, Node n where v in elements(n.values) and n.id = "
            + nodeID
            + " and v.lang.id = "
            + langID;
    return executeQuery(queryString, Value.class);
  }

  /** Return all values associated with a given node and attribute combo */
  public Collection<Value> getSpecificValuesForNode(int nodeID, int attrID, int langID) {
    String queryString =
        "select v from Value v, Node n where v in elements(n.values) and n.id = "
            + nodeID
            + " and v.lang.id = "
            + langID
            + " and v.attribute.id = "
            + attrID;

    return executeQuery(queryString, Value.class);
  }

  /** Return all nodes 'related to' the given node */
  public Collection<Related_node> getRelatedNodes(int nodeID) {
    String queryString =
        "select r from Related_node r where r.node.id = :nodeId and r.relationship.id = 1";
    Session session = sessionFactory.getCurrentSession();
    return (Collection<Related_node>)
        session.createQuery(queryString, Related_node.class).setParameter("nodeId", nodeID).list();
  }

  /** Return all related nodes 'that reference' the given node */
  public Collection<Related_node> getRelatedNodeReferences(int nodeID) {
    String queryString =
        "select r from Related_node r where r.related_node.id = :nodeId and r.relationship.id = 1";
    Session session = sessionFactory.getCurrentSession();
    return (Collection<Related_node>)
        session.createQuery(queryString, Related_node.class).setParameter("nodeId", nodeID).list();
  }

  /** Return all nodes 'similar to' the given node */
  public Collection<Related_node> getSimilarNodes(int nodeID) {
    String queryString =
        "select r from Related_node r where r.node.id = :nodeId and r.relationship.id = 2";
    Session session = sessionFactory.getCurrentSession();
    return (Collection<Related_node>)
        session.createQuery(queryString, Related_node.class).setParameter("nodeId", nodeID).list();
  }

  /** Return all child nodes of the given node */
  public Collection<Node> getChildNodes(int nodeID) {

    String queryString =
        "select n from Node n left join fetch n.nodeEditors ne where n.parent.id = " + nodeID;

    return executeQuery(queryString, Node.class);
  }

  /** Return all NodeEditors for the given node */
  public Collection<Node_editor> getNodeEditors(int nodeID) {

    String queryString = "select ne from Node_editor ne where ne.node.id = " + nodeID;

    return executeQuery(queryString, Node_editor.class);
  }

  /** Return a nodeName for the given node */
  public Collection<String> getNodeName(int nodeID, int langID) {

    String queryString =
        "select v.Name from Value v where v.node.id = "
            + nodeID
            + " and v.attribute.Is_node_name > 0 and v.lang.id = "
            + langID
            + " order by v.node.id, v.attribute.Is_node_name";

    return executeQuery(queryString, String.class);
  }

  public Map<String, String> deleteNodeAndFriends(int nodeID, int taxonomyID) {

    String queryString = "select n.id from Node n where n.parent.id = " + nodeID;

    Map<String, String> errors = null;
    List<Integer> result = executeQuery(queryString, Integer.class);

    if (result.size() == 0) {

      queryString =
          "delete from Related_node rn where rn.node.id ="
              + nodeID
              + " or rn.related_node.id = "
              + nodeID;
      executeUpdate(queryString);
      queryString = "delete from Value v where v.node.id =" + nodeID;
      executeUpdate(queryString);
      queryString = "delete from Node_editor e where e.node.id =" + nodeID;
      executeUpdate(queryString);
      queryString = "delete from Node n where n.id =" + nodeID;
      executeUpdate(queryString);

    } else {
      errors = new HashMap<String, String>();
      errors.put(
          AbstractTaxonEditorController.ACTION_ERROR,
          "Taxon ID=" + nodeID + " has children and cannot be deleted.");
    }
    return errors;
  }

  /** Return all titles for all nodes */
  public Collection<Object[]> getTitlesForNodes(int taxonomyID, int languageID) {

    String queryString =
        "select v.node.id, al.Name, v.Name from Node n, "
            + "Value v, Attribute a,  Attribute_lang al  where n.taxonomy.id = "
            + taxonomyID
            + " and "
            + "v.node.id = n.id and v.lang.id = "
            + languageID
            + " and v.attribute.id = a.id and "
            + "a.taxonomy.id = "
            + taxonomyID
            + " and al.attribute.id = v.attribute.id and al.language.id = "
            + languageID
            + " order by v.node.id";

    return executeQuery(queryString, Object[].class);
  }

  /** Change the parent of a node */
  public void changeParent(int nodeID, int newParentID) {

    Session session = sessionFactory.getCurrentSession();
    String queryString = "select n from Node n where n.id = :nodeId or n.id = :newParentId";

    Collection<Node> nodes =
        session
            .createQuery(queryString, Node.class)
            .setParameter("nodeId", nodeID)
            .setParameter("newParentId", newParentID)
            .list();

    Node[] tmp = nodes.toArray(new Node[nodes.size()]);

    if (tmp[0].getId() == nodeID) {

      tmp[0].setParent(tmp[1]);
      session.merge(tmp[0]);

    } else {
      tmp[1].setParent(tmp[0]);
      session.merge(tmp[1]);
    }
  }

  public void saveNode(Node node) {
    if (node != null) {
      Session session = sessionFactory.getCurrentSession();
      session.merge(node);
    }
  }

  public void removeNode(Node node) {
    Session session = sessionFactory.getCurrentSession();
    session.remove(node);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////

  public Collection<Node> findNodesByAttribute(Attribute attribute) {
    Session session = sessionFactory.getCurrentSession();
    Collection<Node> nodes = null;
    Query<Node> query =
        session
            .createNamedQuery("findNodesByAttribute", Node.class)
            .setParameter("attribute", attribute);
    nodes = query.list();

    return nodes;
  }

  public List<PSLocator> findItemsUsingNode(
      String table, String column, Node node, int maxItems, boolean remove) {
    Session session = sessionFactory.getCurrentSession();

    List<PSLocator> locators = new ArrayList<PSLocator>();
    String testId = Integer.toString(node.getId());
    org.hibernate.query.NativeQuery<?> query =
        session.createNativeQuery(
            "SELECT CONTENTID,REVISIONID,"
                + column
                + " from "
                + table
                + " where "
                + column
                + " like '%"
                + testId
                + "%'",
            Object[].class);
    if (maxItems > 0) query.setMaxResults(maxItems);

    var resultList = query.list();
    int updateCount = 0;
    for (var row : resultList) {
      Object[] rowArray = (Object[]) row;
      int id = (Integer.parseInt(rowArray[0].toString()));
      int revision = (Integer.parseInt(rowArray[1].toString()));
      String itemList = rowArray[2].toString();

      String[] splitArray = StringUtils.split(itemList, " ,");
      List<String> splitList = new ArrayList<>(Arrays.asList(splitArray));
      if (splitList.contains(testId)) {
        PSLocator locator = new PSLocator(id, revision);
        PSLegacyGuid guid = new PSLegacyGuid(locator);

        locators.add(new PSLocator(id, revision));
        if (remove) {
          IPSContentRepository rep = PSContentInternalLocator.getLegacyRepository();

          splitList.remove(testId);
          String newString = StringUtils.join(splitList, ',');
          org.hibernate.query.NativeQuery<?> updateQuery =
              session.createNativeQuery(
                  "UPDATE "
                      + table
                      + " SET "
                      + column
                      + "= :newstring where CONTENTID = :content_id and REVISIONID = :revision");
          updateQuery.setParameter("newstring", newString);
          updateQuery.setParameter("content_id", id);
          updateQuery.setParameter("revision", revision);
          updateQuery.executeUpdate();
          if (++updateCount % 20 == 0) {
            session.flush();
            session.clear();
          }
          rep.evict(Collections.singletonList(guid));
        }
      }
    }

    return locators;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////

}
