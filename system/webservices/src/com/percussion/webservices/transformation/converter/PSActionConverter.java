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
package com.percussion.webservices.transformation.converter;

import com.percussion.cms.objectstore.IPSDbComponent;
import com.percussion.cms.objectstore.PSAction;
import com.percussion.cms.objectstore.PSActionParameter;
import com.percussion.cms.objectstore.PSActionProperty;
import com.percussion.cms.objectstore.PSActionVisibilityContext;
import com.percussion.cms.objectstore.PSActionVisibilityContexts;
import com.percussion.cms.objectstore.PSChildActions;
import com.percussion.cms.objectstore.PSDbComponentCollection;
import com.percussion.cms.objectstore.PSKey;
import com.percussion.cms.objectstore.PSMenuChild;
import com.percussion.cms.objectstore.PSMenuContext;
import com.percussion.cms.objectstore.PSMenuMode;
import com.percussion.cms.objectstore.PSMenuModeContextMapping;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.webservices.ui.data.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtilsBean;

/**
 * Converts objects between the classes
 * {@link com.percussion.cms.objectstore.PSAction} and
 * {@link com.percussion.webservices.ui.data.PSAction}.
 */
public class PSActionConverter extends PSConverter
{
   /* (non-Javadoc)
    * @see PSConverter#PSConvert(BeanUtilsUtil)
    */
   public PSActionConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);
   }

   /* (non-Javadoc)
    * @see PSConverter#convert(Class, Object)
    */
   @Override
   public Object convert(@SuppressWarnings("unused") Class type, Object value)
   {
      if (value == null)
         return null;

      if (isClientToServer(value))
      {
         com.percussion.webservices.ui.data.PSAction source =
            (com.percussion.webservices.ui.data.PSAction) value;

         return getActionFromClient(source);
      }
      else
      {
         PSAction source = (PSAction) value;

         return getActionFromServer(source);
      }
   }

   /**
    * Gets the webservice (client) Action object from the objectstore object.
    *
    * @param source the to be converted action object, assumed not
    *    <code>null</code>.
    *
    * @return the converted action object, never <code>null</code>.
    */
   private com.percussion.webservices.ui.data.PSAction getActionFromServer(
         PSAction source)
   {
      Long id = new PSDesignGuid(source.getGUID()).getValue();

      com.percussion.webservices.ui.data.PSAction ws = new com.percussion.webservices.ui.data.PSAction();
      ws.setId(id);
      ws.setDescription(source.getDescription());

      // name/label
      ws.setName(source.getName());
      ws.setLabel(source.getLabel());

      // target
      String tgtName = source.getProperties().getProperty(PSAction.PROP_TARGET);
      if (tgtName != null)
      {
         com.percussion.webservices.ui.data.PSAction.Target tgt = new com.percussion.webservices.ui.data.PSAction.Target();
         tgt.setValue(tgtName);
         tgt.setStyle(source.getProperties().getProperty(PSAction.PROP_TARGET_STYLE, ""));
         ws.setTarget(tgt);
      }

      // command
      com.percussion.webservices.ui.data.PSAction.Command command = getCommand(source);
      ws.setCommand(command);

      // usage
      com.percussion.webservices.ui.data.PSActionUsageUsed[] usage = getUsage(source);
      if (usage != null && usage.length > 0) {
         com.percussion.webservices.ui.data.PSAction.Usage usageWrapper = new com.percussion.webservices.ui.data.PSAction.Usage();
         for (com.percussion.webservices.ui.data.PSActionUsageUsed u : usage) {
            com.percussion.webservices.ui.data.PSAction.Usage.Used used = new com.percussion.webservices.ui.data.PSAction.Usage.Used();
            used.setContextId(u.getContextId());
            used.setContextName(u.getContextName());
            used.setUserInterfaceId(u.getUserInterfaceId());
            used.setUserInterfaceName(u.getUserInterfaceName());
            usageWrapper.getUsed().add(used);
         }
         ws.setUsage(usageWrapper);
      }

      // visibilities
      com.percussion.webservices.ui.data.PSActionVisibilitiesContext[] visibilities = getVisibilities(source);
      if (visibilities != null && visibilities.length > 0) {
         com.percussion.webservices.ui.data.PSAction.Visibilities visWrapper = new com.percussion.webservices.ui.data.PSAction.Visibilities();
         for (com.percussion.webservices.ui.data.PSActionVisibilitiesContext v : visibilities) {
            com.percussion.webservices.ui.data.PSAction.Visibilities.Context ctx = new com.percussion.webservices.ui.data.PSAction.Visibilities.Context();
            ctx.setName(v.getName());
            try {
               ctx.setValue(v.getValue());
            } catch (Exception ignore) { }
            visWrapper.getContext().add(ctx);
         }
         ws.setVisibilities(visWrapper);
      }

      // children
      com.percussion.webservices.ui.data.PSActionChildrenChildAction[] childAction = getChildActions(source);
      if (childAction != null && childAction.length > 0) {
         com.percussion.webservices.ui.data.PSAction.Children childrenWrapper = new com.percussion.webservices.ui.data.PSAction.Children();
         for (com.percussion.webservices.ui.data.PSActionChildrenChildAction ca : childAction) {
            com.percussion.webservices.ui.data.PSAction.Children.ChildAction child = new com.percussion.webservices.ui.data.PSAction.Children.ChildAction();
            child.setId(ca.getId());
            child.setName(ca.getName());
            childrenWrapper.getChildAction().add(child);
         }
         ws.setChildren(childrenWrapper);
      }

      // properties
      Property[] properties = getProperties(source);
      if (properties != null && properties.length > 0) {
         com.percussion.webservices.ui.data.PSAction.Properties props = new com.percussion.webservices.ui.data.PSAction.Properties();
         for (Property p : properties) props.getProperty().add(p);
         ws.setProperties(props);
      }

      // other attributes
      ws.setTooltip(source.getProperty(PSAction.PROP_SHORT_DESC));
      ws.setIconPath(source.getProperty(PSAction.PROP_SMALL_ICON));
      ws.setAcceleratorKey(source.getProperty(PSAction.PROP_ACCEL_KEY));
      ws.setMnemonicKey(source.getProperty(PSAction.PROP_MNEM_KEY));
      ws.setLaunchNewWindow(source.getPropertyBoolean(PSAction.PROP_LAUNCH_NEW_WND));
      ws.setClientAction(source.isClientAction());
      ws.setSupportsMultiSelect(source.getPropertyBoolean(PSAction.PROP_MUTLI_SELECT));
      ws.setType(getType(source));
      ws.setSortRank(new java.math.BigInteger(String.valueOf(source.getSortRank())));
      ws.setRefreshHint(getRefreshHint(source));

      return ws;
   }

   /**
    * Creates a refresh type from {@link PSAction#PROP_REFRESH_HINT}
    * property.
    *
    * @param source the action source, assumed not <code>null</code>.
    *
    * @return the created refresh type, never <code>null</code>. Defaults to
    *    {@link #NONE} if the property does not exist.
    */
   private com.percussion.webservices.ui.data.RefreshType getRefreshHint(
         PSAction source)
   {
      String refHint = source.getProperty(PSAction.PROP_REFRESH_HINT);
      if (refHint == null) {
         return com.percussion.webservices.ui.data.RefreshType.NONE;
      }
      try {
         return com.percussion.webservices.ui.data.RefreshType.fromValue(refHint.toLowerCase());
      } catch (IllegalArgumentException e) {
         return com.percussion.webservices.ui.data.RefreshType.NONE;
      }
   }

   /**
    * Constants defined in
    * {@link com.percussion.webservices.ui.data.RefreshType}
    */


   /**
    * Gets WS type from the source action.
    *
    * @param source the source action, assumed not <code>null</code>.
    *
    * @return the WS type, never <code>null</code>.
    */
   private com.percussion.webservices.ui.data.ActionType getType(
      PSAction source)
   {
      com.percussion.webservices.ui.data.ActionType type;
      if (source.isMenuItem())
         type = com.percussion.webservices.ui.data.ActionType.ITEM;
      else if (source.isCascadedMenu())
         type = com.percussion.webservices.ui.data.ActionType.CASCADING;
      else
         type = com.percussion.webservices.ui.data.ActionType.DYNAMIC;

      return type;
   }



   /**
    * Gets a list of unknown properties.
    *
    * @param source the source action contains properties, assumes not
    *   <code>null</code>.
    *
    * @return the properties, it may be <code>null</code> if there is no
    *   unknown properties.
    */
   private Property[] getProperties(PSAction source)
   {
      Iterator props = source.getProperties().iterator();
      List<Property> tgts = new ArrayList<Property>();
      Property tgtProp;
      while (props.hasNext())
      {
         PSActionProperty prop = (PSActionProperty) props.next();
         if (!ms_knownProps.contains(prop.getName()))
         {
            tgtProp = new Property();
            tgtProp.setName(prop.getName());
            tgtProp.setValue(prop.getValue());
         }
      }

      if (tgts.isEmpty())
      {
         return null;
      }
      else
      {
         Property[] result = new Property[tgts.size()];
         tgts.toArray(result);
         return result;
      }
   }

   /**
    * A list of known properties
    */
   private static Set<String> ms_knownProps = new HashSet<String>();
   static
   {
      ms_knownProps.add(PSAction.PROP_ACCEL_KEY);
      ms_knownProps.add(PSAction.PROP_LAUNCH_NEW_WND);
      ms_knownProps.add(PSAction.PROP_MNEM_KEY);
      ms_knownProps.add(PSAction.PROP_MUTLI_SELECT);
      ms_knownProps.add(PSAction.PROP_REFRESH_HINT);
      ms_knownProps.add(PSAction.PROP_SHORT_DESC);
      ms_knownProps.add(PSAction.PROP_SMALL_ICON);
      ms_knownProps.add(PSAction.PROP_TARGET);
      ms_knownProps.add(PSAction.PROP_TARGET_STYLE);
   }

   /**
    * Gets the child actions from a supplied (objectstore) action object.
    *
    * @param source the source object, assumed not <code>null</code>.
    *
    * @return the constructed child actions, never <code>null</code>,
    *    may be empty.
    */
   private com.percussion.webservices.ui.data.PSActionChildrenChildAction[] getChildActions(
      PSAction source)
   {
      List<com.percussion.webservices.ui.data.PSActionChildrenChildAction> tgtChildren =
         new ArrayList<com.percussion.webservices.ui.data.PSActionChildrenChildAction>();

      com.percussion.webservices.ui.data.PSActionChildrenChildAction tgtChild;
      Iterator srcChildren = source.getChildren().iterator();
      for (int i=0; srcChildren.hasNext(); i++)
      {
         PSMenuChild srcChild = (PSMenuChild) srcChildren.next();
         if (srcChild.getState() == IPSDbComponent.DBSTATE_MARKEDFORDELETE)
            continue;

         tgtChild =
            new com.percussion.webservices.ui.data.PSActionChildrenChildAction();
         int childId = Integer.parseInt(srcChild.getChildActionId());
         tgtChild.setId(new PSDesignGuid(
            PSAction.getGuidFromId(childId)).getValue());
         tgtChild.setName(srcChild.getChildActioName());

         tgtChildren.add(tgtChild);
      }

      // convert list to array
      com.percussion.webservices.ui.data.PSActionChildrenChildAction[] result =
         new com.percussion.webservices.ui.data.PSActionChildrenChildAction[tgtChildren.size()];
      tgtChildren.toArray(result);

      return result;
   }


   /**
    * Gets the visibilities object from a supplied (objectstore) action object.
    *
    * @param source the source object, assumed not <code>null</code>.
    *
    * @return the constructed visibilities object, never <code>null</code>,
    *    may be empty.
    */
   private com.percussion.webservices.ui.data.PSActionVisibilitiesContext[] getVisibilities(
      PSAction source)
   {
      List<com.percussion.webservices.ui.data.PSActionVisibilitiesContext> tgtVises =
         new ArrayList<com.percussion.webservices.ui.data.PSActionVisibilitiesContext>();
      PSActionVisibilityContext srcVis;
      com.percussion.webservices.ui.data.PSActionVisibilitiesContext tgtVis;
      Iterator srcVises = source.getVisibilityContexts().iterator();
      while (srcVises.hasNext())
      {
         srcVis = (PSActionVisibilityContext) srcVises.next();

         if (srcVis.getState() == IPSDbComponent.DBSTATE_MARKEDFORDELETE)
            continue;

         if (srcVis.hasValues())
         {
            Iterator values = srcVis.iterator();
            while (values.hasNext())
            {
               tgtVis =
                  new com.percussion.webservices.ui.data.PSActionVisibilitiesContext();
               tgtVis.setName(srcVis.getName());
               tgtVis.setValue((String)values.next());
               tgtVises.add(tgtVis);
            }
         }
         else
         {
            tgtVis =
               new com.percussion.webservices.ui.data.PSActionVisibilitiesContext();
            tgtVis.setName(srcVis.getName());
            tgtVises.add(tgtVis);
         }
      }

      com.percussion.webservices.ui.data.PSActionVisibilitiesContext[] result =
         new com.percussion.webservices.ui.data.PSActionVisibilitiesContext[tgtVises.size()];
      tgtVises.toArray(result);

      return result;
   }

   /**
    * Convert generated PSAction.Visibilities wrapper into PSActionVisibilityContexts
    */
   private PSActionVisibilityContexts getVisibilityContexts(
         com.percussion.webservices.ui.data.PSAction.Visibilities vis)
   {
      PSActionVisibilityContexts tgtCtxs = new PSActionVisibilityContexts();
      if (vis == null || vis.getContext() == null) return tgtCtxs;
      for (com.percussion.webservices.ui.data.PSAction.Visibilities.Context ctx : vis.getContext())
      {
         PSActionVisibilityContext tgt = new PSActionVisibilityContext(ctx.getName());
         if (ctx.getValue() != null)
            tgt.add(ctx.getValue());
         tgtCtxs.add(tgt);
      }
      return tgtCtxs;
   }

   /**
    * Gets the usage object from a supplied (objectstore) action object.
    *
    * @param source the source object, assumed not <code>null</code>.
    *
    * @return the constructed usage object, never <code>null</code>.
    */
   private com.percussion.webservices.ui.data.PSActionUsageUsed[] getUsage(
         PSAction source)
   {
      com.percussion.webservices.ui.data.PSActionUsageUsed[] usage =
         new com.percussion.webservices.ui.data.PSActionUsageUsed[source.getModeUIContexts().size()];

      Iterator modeCtxs = source.getModeUIContexts().iterator();
      PSMenuModeContextMapping mapping;
      PSDesignGuid ctxGuid, modeGuid;
      for (int i=0; modeCtxs.hasNext(); i++)
      {
         mapping = (PSMenuModeContextMapping) modeCtxs.next();

         if (mapping.getState() == IPSDbComponent.DBSTATE_MARKEDFORDELETE)
            continue;

         usage[i] = new com.percussion.webservices.ui.data.PSActionUsageUsed();

         // convert id to GUID
         ctxGuid = PSMenuContext.getGuidFromId(Integer.parseInt(mapping
               .getContextId()));
         modeGuid = PSMenuMode.getGuidFromId(Integer.parseInt(mapping
               .getModeId()));

         usage[i].setContextId(ctxGuid.getValue());
         usage[i].setContextName(mapping.getContextName());
         usage[i].setUserInterfaceId(modeGuid.getValue());
         usage[i].setUserInterfaceName(mapping.getNodeName());
      }

      return usage;
   }

   /**
    * Gets the command object from a supplied (objectstore) action object.
    *
    * @param source the source object, assumed not <code>null</code>.
    *
    * @return the constructed command object, never <code>null</code>.
    */
   private com.percussion.webservices.ui.data.PSAction.Command getCommand(
      PSAction source)
   {
      com.percussion.webservices.ui.data.PSAction.Command cmd = new com.percussion.webservices.ui.data.PSAction.Command();
      com.percussion.webservices.ui.data.PSAction.Command.Parameters paramsWrapper = new com.percussion.webservices.ui.data.PSAction.Command.Parameters();
      com.percussion.webservices.ui.data.PSAction.Command.Parameters.Parameter prm;
      Iterator srcParams = source.getParameters().iterator();
      while (srcParams.hasNext())
      {
         PSActionParameter srcParam = (PSActionParameter) srcParams.next();
         prm = new com.percussion.webservices.ui.data.PSAction.Command.Parameters.Parameter();
         prm.setName(srcParam.getName());
         prm.setValue(srcParam.getValue());
         paramsWrapper.getParameter().add(prm);
      }
      cmd.setParameters(paramsWrapper);
      cmd.setUrl(source.getURL());
      return cmd;
   }

   /**
    * Gets the objectstore Action object from the webservice client object.
    *
    * @param source the to be converted action object, assumed not
    *    <code>null</code>.
    *
    * @return the converted action object, never <code>null</code>.
    */
   private PSAction getActionFromClient(
      com.percussion.webservices.ui.data.PSAction source)
   {
      PSAction target = new PSAction(source.getName(), source.getLabel());

      long actionId = (new PSDesignGuid(source.getId())).longValue();
      PSKey locator = PSAction.createKey(String.valueOf(actionId));
      locator.setPersisted(false);
      target.setLocator(locator);

      target.setSortRank(source.getSortRank().intValue());
      target.setDescription(source.getDescription());
      target.setURL(source.getCommand().getUrl());
      target.setClientAction(source.isClientAction());

      if (source.getType() == com.percussion.webservices.ui.data.ActionType.ITEM)
      {
         target.setMenuType(PSAction.TYPE_MENUITEM);
      }
      else if (source.getType() == com.percussion.webservices.ui.data.ActionType.CASCADING)
      {
         target.setMenuType(PSAction.TYPE_MENU);
      }
      else
      {
         target.setMenuType(PSAction.TYPE_MENU);
         target.setMenuDynamic(true);
      }

      // set url parameters
      if (source.getCommand() != null && source.getCommand().getParameters() != null) {
         for (com.percussion.webservices.ui.data.PSAction.Command.Parameters.Parameter srcParam : source.getCommand().getParameters().getParameter()) {
            target.getParameters().add(new PSActionParameter(srcParam.getName(), srcParam.getValue()));
         }
      }

      // set properties
      addProperty(target, PSAction.PROP_ACCEL_KEY, source.getAcceleratorKey());
      addProperty(target, PSAction.PROP_MNEM_KEY, source.getMnemonicKey());
      addProperty(target, PSAction.PROP_SHORT_DESC, source.getTooltip());
      addProperty(target, PSAction.PROP_SMALL_ICON, source.getIconPath());
      addProperty(target, PSAction.PROP_LAUNCH_NEW_WND, source
            .isLaunchNewWindow() ? PSAction.YES : PSAction.NO);
      addProperty(target, PSAction.PROP_MUTLI_SELECT, source
            .isSupportsMultiSelect() ? PSAction.YES : PSAction.NO);
      if (source.getRefreshHint() != null)
         addProperty(target, PSAction.PROP_REFRESH_HINT, source
               .getRefreshHint().value());

      if (source.getTarget() != null)
      {
         String targetName = source.getTarget().getValue();
         String targetStyle = source.getTarget().getStyle();

         addProperty(target, PSAction.PROP_TARGET, targetName);
         addProperty(target, PSAction.PROP_TARGET_STYLE, targetStyle);
      }
      com.percussion.webservices.ui.data.PSAction.Properties propsWrapper = source.getProperties();
      if (propsWrapper != null && propsWrapper.getProperty() != null)
      {
         for (Property p : propsWrapper.getProperty())
            addProperty(target, p.getName(), p.getValue());
      }

      // done with properties

      PSDbComponentCollection modeCtxs = getModeUIContexts(source.getUsage(),
         actionId);
      target.setModeUIContexts(modeCtxs);

      PSActionVisibilityContexts visCtxs = getVisibilityContexts(source
            .getVisibilities());
      target.setVisibilityContexts(visCtxs);

      // get child (reference) actions
      com.percussion.webservices.ui.data.PSAction.Children childrenWrapper = source.getChildren();
      if (childrenWrapper != null && childrenWrapper.getChildAction() != null && !childrenWrapper.getChildAction().isEmpty())
      {
         PSChildActions children = target.getChildren();
         for (com.percussion.webservices.ui.data.PSAction.Children.ChildAction srcChild : childrenWrapper.getChildAction())
         {
            int childId = PSAction.getIdFromGuid(
               new PSDesignGuid(srcChild.getId()));
            PSMenuChild tgtChild = new PSMenuChild(childId, srcChild
                  .getName(), target.getId());
            children.add(tgtChild);
         }
      }

      return target;
   }

   /**
    * Converts the visibility contexts from the webservice object to
    * objectstore object.
    *
    * @param srcCtxs the to be converted object, it may be <code>null</code>.
    *
    * @return the converted list, never <code>null</code>, but may be empty.
    */
   private PSActionVisibilityContexts getVisibilityContexts(
      com.percussion.webservices.ui.data.PSActionVisibilitiesContext[] srcCtxs)
   {
      if (srcCtxs == null)
         return new PSActionVisibilityContexts();

      // transfer "srcCtxs" into a Map
      Map<String, PSActionVisibilityContext> mapper =
         new HashMap<String, PSActionVisibilityContext>();
      PSActionVisibilityContext tgtCtx;
      for (com.percussion.webservices.ui.data.PSActionVisibilitiesContext srcCtx
            : srcCtxs)
      {
         if (mapper.get(srcCtx.getName()) == null)
         {
            tgtCtx = new PSActionVisibilityContext(srcCtx.getName(), srcCtx
                  .getValue());
            mapper.put(srcCtx.getName(), tgtCtx);
         }
         else
         {
            tgtCtx = mapper.get(srcCtx.getName());
            tgtCtx.add(srcCtx.getValue());
         }
      }

      // transfer the Map into the target list
      PSActionVisibilityContexts tgtCtxs = new PSActionVisibilityContexts();
      for (PSActionVisibilityContext ctx : mapper.values())
      {
         tgtCtxs.add(ctx);
      }

      return tgtCtxs;
   }

   /**
    * Gets the mode-uicontext mapping list from a usage object.
    *
    * @param usage the to be converted object, it may be <code>null</code>.
    * @param actionId the parent id, assumed not <code>null</code>.
    *
    * @return the mapping list, never <code>null</code>, but may be empty.
    */
   private PSDbComponentCollection getModeUIContexts(
         com.percussion.webservices.ui.data.PSActionUsageUsed[] usage,
         Long actionId)
   {
      PSDbComponentCollection modeCtxs = new PSDbComponentCollection(
            PSMenuModeContextMapping.class);
      PSMenuModeContextMapping mapping;
      String sModeId, sContextId;
      PSDesignGuid modeGuid, ctxGuid;
      String sActionId = String.valueOf(actionId);
      for (com.percussion.webservices.ui.data.PSActionUsageUsed used : usage)
      {
         // convert GUID to id
         modeGuid = new PSDesignGuid(used.getUserInterfaceId());
         sModeId = String.valueOf(PSMenuMode.getIdFromGuid(modeGuid));
         ctxGuid = new PSDesignGuid(used.getContextId());
         sContextId = String.valueOf(PSMenuContext.getIdFromGuid(ctxGuid));

         mapping = new PSMenuModeContextMapping(sModeId, sContextId, sActionId);
         mapping.setModeName(used.getUserInterfaceName());
         mapping.setContextName(used.getContextName());
         modeCtxs.add(mapping);
      }
      return modeCtxs;
   }

   /**
    * Convert generated PSAction.Usage wrapper into the mode-uicontext mappings.
    */
   private PSDbComponentCollection getModeUIContexts(
         com.percussion.webservices.ui.data.PSAction.Usage usage,
         Long actionId)
   {
      if (usage == null || usage.getUsed() == null) return new PSDbComponentCollection(PSMenuModeContextMapping.class);
      PSDbComponentCollection modeCtxs = new PSDbComponentCollection(PSMenuModeContextMapping.class);
      PSMenuModeContextMapping mapping;
      String sModeId, sContextId;
      PSDesignGuid modeGuid, ctxGuid;
      String sActionId = String.valueOf(actionId);
      for (com.percussion.webservices.ui.data.PSAction.Usage.Used used : usage.getUsed())
      {
         modeGuid = new PSDesignGuid(used.getUserInterfaceId());
         sModeId = String.valueOf(PSMenuMode.getIdFromGuid(modeGuid));
         ctxGuid = new PSDesignGuid(used.getContextId());
         sContextId = String.valueOf(PSMenuContext.getIdFromGuid(ctxGuid));

         mapping = new PSMenuModeContextMapping(sModeId, sContextId, sActionId);
         mapping.setModeName(used.getUserInterfaceName());
         mapping.setContextName(used.getContextName());
         modeCtxs.add(mapping);
      }
      return modeCtxs;
   }

   /**
    * Adds the supplied property (name and value) to the supplied action object.
    *
    * @param action the action object that contains properties, assumed not
    *    <code>null</code>.
    * @param name the property name, assumed not <code>null</code>.
    * @param value the property value, it may be <code>null</code> or empty if
    *    not to add the property.
    */
   private void addProperty(PSAction action, String name, String value)
   {
      if (value != null && value.trim().length() > 0)
      {
         PSActionProperty prop = new PSActionProperty(name, value);
         action.getProperties().add(prop);
      }
   }
}
