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

// REFACTORED: CP-JAVA11
package com.percussion.rx.publisher.jsf.nodes;

import com.percussion.rx.jsf.PSEditableNodeContainer;
import com.percussion.rx.jsf.PSNodeBase;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.publisher.IPSDeliveryType;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This node displays a collection of Delivery Types.
 */
public class PSDeliveryTypeContainerNode extends PSEditableNodeContainer 
{
   public static final String DELIVERY_TYPE_LIST = "pub-design-deliverytypes-views";
   
   /**
    * The node title
    */
   public static final String NODE_TITLE = "Delivery Types";
   
   /**
    * Constructs an instance.
    *
    */
   public PSDeliveryTypeContainerNode() 
   {
      super(NODE_TITLE, DELIVERY_TYPE_LIST);
   }

   @Override
   /**
    * Gets the child nodes for all delivery types.
    * @return list of child nodes
    */
   @Override
   public List<? extends PSNodeBase> getChildren() throws PSNotFoundException {
      if (m_children == null) {
         var dtypes = getAllDeliveryTypes();
         for (var dtype : dtypes) {
            var node = new PSDeliveryTypeNode(dtype);
            addNode(node);
         }
      }
      return super.getChildren();
   }

   /**
    * @return all Delivery Types in ascending order. Never <code>null</code>
    * may be empty.
    */
   /**
    * Gets all Delivery Types in ascending order.
    * @return sorted list of delivery types
    */
   private List<IPSDeliveryType> getAllDeliveryTypes() {
      var psvc = getPublisherService();
      var dtypes = psvc.findAllDeliveryTypes();
      dtypes.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
      return dtypes;
   }

   // see base
   @Override
   /**
    * Finds a delivery type by name.
    * @param name delivery type name
    * @return true if found, false otherwise
    */
   @Override
   protected boolean findObjectByName(String name) {
      var pub = getPublisherService();
      try {
         pub.loadDeliveryType(name);
         return true;
      } catch (PSNotFoundException e) {
         return false;
      }
   }

   @Override
   /**
    * Gets all delivery type names.
    * @return set of names
    */
   @Override
   public Set<Object> getAllNames() {
      var names = new HashSet<>();
      for (var dtype : getPublisherService().findAllDeliveryTypes()) {
         names.add(dtype.getName());
      }
      return names;
   }

   /**
    * Convenience method to access publisher service.
    * @return the publisher service object. Not <code>null</code>.
    */
   /**
    * Gets the publisher service.
    * @return publisher service, never null
    */
   private IPSPublisherService getPublisherService() {
      return PSPublisherServiceLocator.getPublisherService();
   }
   
   /**
    * Action to create a new Delivery Type, and add it to the tree.
    * @return the perform action for the Delivery Type node, which will 
    * navigate to the editor.
    */
   /**
    * Creates a new Delivery Type and adds it to the tree.
    * @return perform action for the new node
    */
   public String create() throws PSNotFoundException {
      var dtype = getPublisherService().createDeliveryType();
      dtype.setName(getUniqueName("DeliveryType", false));
      getPublisherService().saveDeliveryType(dtype);
      var node = new PSDeliveryTypeNode(dtype);
      return node.editNewNode(this, node);
   }

   @Override
   /**
    * Returns to the delivery type list view.
    */
   @Override
   public String returnToListView() {
      return DELIVERY_TYPE_LIST;
   }

   @Override
   /**
    * Gets the help topic for this node.
    */
   @Override
   public String getHelpTopic() {
      return "DeliveryTypeList";
   }

}
