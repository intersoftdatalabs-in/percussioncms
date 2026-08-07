package com.percussion.taxonomy.repository;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.taxonomy.domain.Attribute;
import com.percussion.taxonomy.domain.Language;
import com.percussion.taxonomy.domain.Node;
import com.percussion.taxonomy.domain.Value;
import com.percussion.taxonomy.validation.UrlValidator;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * This DAO is used only to saving/updating the Node.java Domain Object
 *
 * @author rxengineer
 */
@Repository
@Transactional
public class HibernateValueDAO implements ValueDAO {

  private static final Logger log = LogManager.getLogger(HibernateValueDAO.class);

  @Autowired private SessionFactory sessionFactory;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static final int MAX_FIELD_LENGTH = 255;
  public static final int MAX_URL_FIELD_LENGTH = 2048;

  public static final String NODE_ID_PARAM = "nodeID";
  public static final String NODE_ATTR_PARAM_PREFIX = "attr_";
  public static final String NODE_SELECTABLE_PARAM = "isNodeSelectable";

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  public Value getValue(int id) {
    Session session = sessionFactory.getCurrentSession();
    return session.find(Value.class, id);
  }

  public Collection<Value> getAllValues() {
    Session session = sessionFactory.getCurrentSession();
    return session.createQuery("from Value val", Value.class).list();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void saveValue(Value value) {
    Session session = sessionFactory.getCurrentSession();
    session.merge(value);
  }

  public Map<String, String> saveValuesFromParams(
      Map<String, String[]> params,
      Collection<Attribute> attributes,
      Node node,
      int langID,
      String user_name) {
    Map<String, String> values = null;
    try {
      Session session = sessionFactory.getCurrentSession();
      HibernateValueCallback valueSetter =
          new HibernateValueCallback(params, attributes, node, langID, user_name);
      values = valueSetter.doInHibernate(session);
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return values;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void removeValue(Value value) {
    Session session = sessionFactory.getCurrentSession();
    session.remove(value);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  public static String getParamNameFor(Attribute attribute) {
    return NODE_ATTR_PARAM_PREFIX + String.valueOf(attribute.getId());
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // Private class to facilitate the complex value saving process
  private class HibernateValueCallback {

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String username = null;

    private boolean setNotLeaf = false;

    private Map<String, String[]> params = null;

    private Collection<Attribute> attributes = null;

    private Node node = null;

    // Default to english
    private int langID = 1;

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public HibernateValueCallback(
        Map<String, String[]> params,
        Collection<Attribute> attributes,
        Node node,
        int langID,
        String username) {
      this.node = node;
      this.setNotLeaf = (node != null ? node.getNot_leaf() : this.setNotLeaf);
      this.params = params;
      this.attributes = attributes;
      this.langID = langID;
      this.username = username;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public HashMap<String, String> getErrors(
        Attribute attribute, String param_value, boolean newNode) {

      HashMap<String, String> ret = new HashMap<String, String>();

      if (param_value == null) {
        // we to not require for multi values
        if (!attribute.getIs_multiple() && attribute.getIs_required()) {
          ret.put(
              (newNode ? "child" : "regular") + attribute.getId(),
              " Is required and cannot be blank." + error_message_suffix(newNode));
        }
      } else {
        if (attribute.getIs_percussion_item() && !is_valid_url(param_value)) {
          ret.put(
              (newNode ? "child" : "regular") + attribute.getId(),
              " The URL entered was not valid." + error_message_suffix(newNode));
        } else if (attribute.getIs_percussion_item()
            && param_value.length() >= MAX_URL_FIELD_LENGTH) {
          ret.put(
              (newNode ? "child" : "regular") + attribute.getId(),
              " Exceeded the "
                  + MAX_URL_FIELD_LENGTH
                  + " character limit for URLs."
                  + error_message_suffix(newNode));
        } else if ((!attribute.getIs_percussion_item())
            && (param_value.length() >= MAX_FIELD_LENGTH)) {
          // TODO this should be a config var
          ret.put(
              (newNode ? "child" : "regular") + attribute.getId(),
              " Exceeded the "
                  + MAX_FIELD_LENGTH
                  + " character limit."
                  + error_message_suffix(newNode));
        }
      }
      return ret;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<String, String> doInHibernate(Session session) throws HibernateException {

      Map<String, String> errors = null;
      Collection<Value> forGarbage = new HashSet<Value>();

      if (attributes != null) {
        NodeInfo nodeInfo = this.getNodeInfo(session, this.node, this.params);

        if (nodeInfo != null) {

          Node node = nodeInfo.getNode();
          boolean newNode = nodeInfo.isNew();

          errors = new HashMap<String, String>();

          for (Attribute attribute : attributes) {
            errors.putAll(
                this.saveAttribute(
                    session, nodeInfo, this.username, attribute, this.params, forGarbage));
          }

          if (!errors.isEmpty()) {

            if (newNode) {
              for (Value v : forGarbage) {
                session.remove(v);
              }
              session.remove(node);
            }

          } else {
            // Update our Node if there were no errors
            node.setNot_leaf(this.setNotLeaf);

            // I'm doing this here vs. creating a new Attribute and value bc this entire domain
            // design is brittle
            // and horrible...the UI is directly tied to the autogenerated id's of the attribute
            // domain objects...BAD
            node.setIsNodeSelectable(this.isSelectable(this.params));

            session.merge(node);
          }
        }
      }
      return (errors == null || errors.isEmpty() ? null : errors);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////

    private NodeInfo getNodeInfo(Session session, Node node, Map<String, String[]> params) {
      boolean isNew = false;
      NodeInfo nodeInfo = null;

      if (node != null) {

        int nodeId = node.getId();
        isNew = (nodeId <= 0);

        if (isNew) {
          // Update our Node before saving values linked to it
          session.merge(node);
        }
        nodeInfo = new NodeInfo(node, isNew);
      }
      return nodeInfo;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////

    private Map<String, String> saveAttribute(
        Session session,
        NodeInfo nodeInfo,
        String username,
        Attribute attribute,
        Map<String, String[]> params,
        Collection<Value> forGarbage) {
      Map<String, String> errors = new HashMap<String, String>();
      Collection<String> updatedValues = this.getUpdatedValuesFor(attribute, params);
      Map<String, Value> currentValues =
          this.getCurrentValuesFor(session, nodeInfo.getNode().getId(), attribute);

      if (attribute.getIs_multiple()) {

        // *********************************** MULTI VALUES *********************************** //
        errors.putAll(
            this.saveAttrMultiValue(
                session,
                nodeInfo.getNode(),
                nodeInfo.isNew(),
                username,
                attribute,
                updatedValues,
                currentValues,
                forGarbage));

      } else {

        // *********************************** SINGLE VALUES *********************************** //
        errors.putAll(
            this.saveAttrSingleValue(
                session,
                nodeInfo.getNode(),
                nodeInfo.isNew(),
                username,
                attribute,
                StringUtils.trimToNull(params.get(getParamNameFor(attribute))[0]),
                currentValues.values(),
                forGarbage));
      }
      return errors;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////

    private Map<String, String> saveAttrMultiValue(
        Session session,
        Node node,
        boolean newNode,
        String userName,
        Attribute attribute,
        Collection<String> newValues,
        Map<String, Value> currentValues,
        Collection<Value> forGarbage) {
      Map<String, String> errors = new HashMap<String, String>();

      // process values
      for (Object obj : newValues.toArray()) {

        String s = (String) obj;

        if (currentValues.containsKey(s)) {
          // if value exists don't update DB but remove from
          // db_values because we will delete remaining at end of
          // loop
          currentValues.remove(s);

        } else if (!getErrors(attribute, s, newNode).isEmpty()) {

          errors.putAll(getErrors(attribute, s, newNode));

        } else {
          // passed validation create new value
          Value value = new Value();
          value.setAttribute(attribute);
          value.setNode(node);
          value.setLang(
              session
                  .createQuery("select l from Language l where l.id = :langId", Language.class)
                  .setParameter("langId", langID)
                  .uniqueResult());
          value.setCreated_by_id(userName);
          value.setCreated_at(new Timestamp(System.currentTimeMillis()));
          value.setName(s);
          session.merge(value);

          forGarbage.add(value);
        }
      }
      // remove existing values not in params
      for (Value v : currentValues.values()) {
        session.remove(v);
      }
      return errors;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////

    private Map<String, String> saveAttrSingleValue(
        Session session,
        Node node,
        boolean newNode,
        String userName,
        Attribute attribute,
        String newValue,
        Collection<Value> currentValues,
        Collection<Value> forGarbage) {
      Map<String, String> errors = new HashMap<String, String>();
      boolean new_value = false;

      // build our new or existing value object
      Value value = null;

      if (currentValues.size() == 1) {
        // find existing value...
        value = currentValues.iterator().next();

        // if we are modifing we need modified_by and modified_at

      } else if (currentValues.size() == 0) {

        // we won't create an object if the param value is null
        new_value = true;

        if (newValue != null) {
          value = new Value();
          value.setAttribute(attribute);
          value.setNode(node);
          value.setLang(
              session
                  .createQuery("select l from Language l where l.id = :langId", Language.class)
                  .setParameter("langId", langID)
                  .uniqueResult());
          value.setCreated_by_id(userName);
          value.setCreated_at(new Timestamp(System.currentTimeMillis()));
        }
      } else {
        throw new HibernateException("multi values for single value node");
      }

      errors = getErrors(attribute, newValue, newNode);

      // so if there are no errors we can make the DB changes
      if (errors.isEmpty()) {
        if (newValue == null) {
          if (!new_value) {
            session.remove(value);
          }
        } else {
          value.setName(newValue);
          session.merge(value);
          forGarbage.add(value);
        }
      }
      return errors;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////

    private Map<String, Value> getCurrentValuesFor(
        Session session, int nodeID, Attribute attribute) {
      Collection<Value> rawValues =
          session
              .createQuery(
                  "select v from Value v, Node n where v in elements(n.values) and n.id = "
                      + nodeID
                      + " and v.attribute.id = "
                      + attribute.getId()
                      + " and v.lang.id = "
                      + langID,
                  Value.class)
              .list();

      Map<String, Value> currentValues = new HashMap<String, Value>();

      for (Value v : rawValues) {
        // note we don't handle non unique values here
        currentValues.put(v.getName(), v);
      }
      return currentValues;
    }

    private Collection<String> getUpdatedValuesFor(
        Attribute attribute, Map<String, String[]> params) {
      Set<String> updatedValues = new HashSet<String>();

      String paramName = getParamNameFor(attribute);

      if (!StringUtils.isBlank(paramName) && params.containsKey(paramName)) {
        for (String s : params.get(paramName)) {

          String cleanedString = StringUtils.trimToNull(s);

          if (cleanedString != null) {
            updatedValues.add(cleanedString);
          }
        }
      }
      return updatedValues;
    }

    private boolean isSelectable(Map<String, String[]> params) {
      boolean isSelectable = true;

      if (params != null && params.containsKey(NODE_SELECTABLE_PARAM)) {
        String[] rawData = params.get(NODE_SELECTABLE_PARAM);

        if (rawData != null && rawData.length == 1) {
          isSelectable = Boolean.valueOf(rawData[0]);
        }
      }
      return isSelectable;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  private static class NodeInfo {

    private Node node = null;
    private boolean isNew = false;

    public NodeInfo(Node node, boolean isNew) {
      this.node = node;
      this.isNew = isNew;
    }

    public boolean isNew() {
      return this.isNew;
    }

    public Node getNode() {
      return this.node;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  private static String error_message_suffix(boolean newNode) {
    return (newNode ? StringUtils.EMPTY : "  The previous value for this field was restored.");
  }

  private static boolean is_valid_url(String url) {
    // add some extra url checking
    String[] schemes = {"http", "https"};
    UrlValidator urlValidator = new UrlValidator(schemes);
    return urlValidator.isValid(url);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////

}
