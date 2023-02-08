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
package com.percussion.pso.effects;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.relationship.IPSEffect;
import com.percussion.relationship.IPSExecutionContext;
import com.percussion.relationship.PSEffectResult;
import com.percussion.relationship.annotation.PSHandlesEffectContext;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequestValidationException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import com.percussion.webservices.system.IPSSystemWs;
import com.percussion.webservices.system.PSSystemWsLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Map.Entry;
import java.util.Set;


@PSHandlesEffectContext()
public class PSEffectLoggingEffect implements IPSEffect
      
{
	
	/**
	    * Get the extension definition.
	    *
	    * @return the extension definition, never <code>null</code>.
	    */
	   public IPSExtensionDef getExtensionDef()
	   {
	      return (IPSExtensionDef) m_def.get();
	   }

	   /**
	    * Get the extension code root.
	    *
	    * @return the extension code root, never <code>null</code>.
	    */
	   public File getCodeRoot()
	   {
	      return (File) m_codeRoot.get();
	   }

	
	 /**
	    * Logger for this class
	    */

	   private static final Logger log = LogManager.getLogger(PSEffectLoggingEffect.class);

	   protected static IPSSystemWs sws = null;
	   protected static IPSGuidManager gmgr = null;
	   protected static IPSContentWs cws = null; 

	   /**
	    * Initialize service pointers. 
	    */
	   protected static void initServices()
	   {
	      if(sws == null)
	      {
	         sws = PSSystemWsLocator.getSystemWebservice(); 
	         gmgr = PSGuidManagerLocator.getGuidMgr(); 
	         cws = PSContentWsLocator.getContentWebservice(); 
	      }
	   }
   /**
    * Default constructor.
    */
   public PSEffectLoggingEffect()
   {
	   	super();
   }

   /**
    * Saves references to the provided extension definition and code root,
    * which might be of use in the effect implementation.
    *
    * See <code>IPSExtension</code> for description.
    */
   public void init(IPSExtensionDef def, File codeRoot)
      throws PSExtensionException
   {
      if (def == null || codeRoot == null)
         throw new IllegalArgumentException("def and codeRoot cannot be null");

      m_def.set(def);
      m_codeRoot.set(codeRoot);
      m_name = def.getRef().toString();
   }


   
       public void recover(Object[] params, IPSRequestContext req, IPSExecutionContext exCtx, PSExtensionProcessingException ex,
	         PSEffectResult result) throws PSExtensionProcessingException
	   { //Nothing to do here      
	      result.setSuccess(); 
	   }

	   public void test(Object[] params, IPSRequestContext req, IPSExecutionContext exCtx, PSEffectResult result)
	         throws PSExtensionProcessingException, PSParameterMismatchException
	   { 
		   
		 
		   String context = "";
			  switch (exCtx.getContextType()) {
			  	case IPSExecutionContext.RS_CHECKIN: context="CHECKIN"; break;
				case IPSExecutionContext.RS_CHECKOUT: context="CHECKOUT"; break;
				case IPSExecutionContext.RS_CONSTRUCTION: context="CONSTRUCTION"; break;
				case IPSExecutionContext.RS_DESTRUCTION: context="DESTRUCTION"; break;
				//case IPSExecutionContext.RS_ENDPOINT_DEPENDENT: context="ENDPOINT_DEPENDENT"; break;
				//case IPSExecutionContext.RS_ENDPOINT_OWNER: context="ENDPOINT_OWNER"; break;
				//case IPSExecutionContext.RS_POST_CHECKOUT: context="POST_CHECKOUT"; break;
				//case IPSExecutionContext.RS_POST_CONSTRUCTION: context="POST_CONSTRUCTION"; break;
				//case IPSExecutionContext.RS_POST_DESTRUCTION: context="POST_DESTRUCTION"; break;
				//case IPSExecutionContext.RS_POST_UPDATE: context="POST_UPDATE"; break;
				case IPSExecutionContext.RS_POST_WORKFLOW: context="POST_WORKFLOW"; break;
				//case IPSExecutionContext.RS_PRE_CHECKIN: context="PRE_CHECKIN"; break;
				case IPSExecutionContext.RS_PRE_CLONE: context="PRE_CLONE"; break;
				//case IPSExecutionContext.RS_PRE_DESTRUCTION: context="PRE_DESTRUCTION"; break;
				//case IPSExecutionContext.RS_PRE_UPDATE: context="PRE_UPDATE"; break;
				case IPSExecutionContext.RS_PRE_WORKFLOW: context="PRE_WORKFLOW"; break;
				case IPSExecutionContext.RS_UPDATE: context="UPDATE"; break;
				default: context="UNKOWN";
			  }
			  
			  String sourceFolderId = req.getParameter("sys_moveSourceFolderId");
			  String targetFolderId = req.getParameter("sys_moveTargetFolderId");
			  
			  log.debug("Move source folder id is ="+sourceFolderId);
			  log.debug("Move target folder id is ="+targetFolderId);
			  
			  PSRelationship current = exCtx.getCurrentRelationship(); 
			  PSRelationship orig =  exCtx.getOriginatingRelationship();
			  Set<PSRelationship> processed = (Set<PSRelationship>)exCtx.getProcessedRelationships();
			  
			  String o="EFFECT Context="+context+"\n";
			  o+="Current Relationship\n";
			  if (current!=null) {
				  o+=outputRelationship(current);
			  }
			  o+="Original Relationship\n";
			  if (orig!=null) {
				  o+=outputRelationship(orig);
			  }
			  o+="Processed Relationships\n";
			  int i=0;
			  for (PSRelationship rel : processed) {
				 o+="Pos:"+i++;
				 o+=outputRelationship(rel);  
			  }
			  
			  // check original folders of dependent
			  // if item is in no folder item is added
			  // if item is in folder possibly will be a move
			  
			  // if following can just replicate add and removing of items
			  
			  log.debug(o);
			  
			  
			  
			  if(exCtx.isConstruction() && current.getConfig().getCategory().equals("rs_folder")) {
				  int dependent=current.getDependent().getId();
				  int owner=current.getOwner().getId();
				  log.debug("Setting private object "+"Added:"+dependent);
				  req.setPrivateObject("Added:"+dependent, owner);
			  }
			  
			  if(exCtx.isDestruction() && current.getConfig().getCategory().equals("rs_folder")) {
				  int dependent=current.getDependent().getId();
				  int owner=current.getOwner().getId();
				  log.debug("Getting private object "+"Added:"+dependent);
				  Object obj = req.getPrivateObject("Added:"+dependent);
				  if (obj != null) {
					  int newFolder = Integer.valueOf(obj.toString());
					  log.debug("Detected item moved from folder "+owner+" to folder" + newFolder);
				  }
			  }
			
			  if(current != null && current.getDependent().getId()==503 && exCtx.isConstruction()) {
				  String msg="Cannot Move Item";
				   String[] args = {m_name, msg};
				   	result.setError(new PSRequestValidationException(1104,m_name) );
			         /*
			         result.setError(req.getUserLocale(),
			            IPSExtensionErrors.EFFECT_VALIDATE_MESSAGE, args);
				   */
			  } else {
				  result.setSuccess();  
			  }
			
			  
	   }
	   

	   

   /**
    * @see IPSEffect#attempt(Object[], IPSRequestContext, IPSExecutionContext, PSEffectResult)
    */
   public void attempt(Object[] params, IPSRequestContext req, IPSExecutionContext exCtx, PSEffectResult result)
         throws PSExtensionProcessingException, PSParameterMismatchException
   {
	  
	  result.setSuccess();
	   
   }
   
   private String outputRelationship(PSRelationship rel) {
	   String o="";
	   if(rel.getConfig()!= null) {
	   o+="    Relationship config Category"+rel.getConfig().getCategory()+"\n";
	   o+="    Relationship config Name"+rel.getConfig().getName()+"\n";
	   o+="    Relationship config Label"+rel.getConfig().getLabel()+"\n";
	   o+="    Relationship config Type"+rel.getConfig().getType()+"\n";
	   }
	   o+="    "+rel.getDescription()+"\n";
	   o+="    Owner="+outputItem(rel.getOwner())+"\n";
	   o+="    Dependent="+outputItem(rel.getDependent())+"\n";
	   o+="    Dependent Object Type="+rel.getDependentObjectType()+"\n";
	   o+="    Relationship Properties:\n";
	   
	   for(Entry<String, String> entry : rel.getAllProperties().entrySet()) {
		   o+="      "+entry.getKey()+":"+entry.getValue()+"\n";
	   }
	   o+="\n";
	   return o;
   }
   
   private String outputItem(PSLocator loc) {
	   String o=loc.getId()+":"+loc.getRevision()+"\n";
	   return o;
   }
   
   /**
    * This holds the definition for this extension, initialized in
   * {#link init(IPSExtensionDef, File)}, never changed or <code>null</code>
   * after that.
   */
  private ThreadLocal m_def = new ThreadLocal();

  /**
   * This holds the 'root' directory for this extension. When installed, all
   * files are installed relative to this location. Files can be loaded from
   * anywhere under this directory and no where else (by default, the actual
   * security policy may vary). This object could be used to load a property
   * file when executing the Effect. Initialized in
   * {#link init(IPSExtensionDef, File)}, never changed or <code>null</code>
   * after that.
   */
  private ThreadLocal m_codeRoot = new ThreadLocal();

  /**
   * Name of the effect as registered. Initialized in the init() method,
   * never <code>null</code> or empty after that.
   */
  protected String m_name = "";
}
