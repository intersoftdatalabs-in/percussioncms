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
package com.percussion.pso.finder;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.data.PSInternalRequestCallException;
import com.percussion.pso.utils.SimplifyParameters;
import com.percussion.server.PSInternalRequest;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSSlotContentFinder;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.impl.finder.PSBaseSlotContentFinder;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.utils.request.PSRequestInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
  * This class is a rewrite of the out of the box Legacy "finder".  That version did not 
  * accept request parameters which meant it either needed to be used with static URL or 
  * invoked multiple times for each set of possible parameters.  To do the latter, you 
  * would have needed a slot registration for each permutation.
  * 
  * @author Erich Heard
  * @version 6.0
  */
public class PSParameterizedLegacyAutoSlotContentFinder extends PSBaseSlotContentFinder	implements IPSSlotContentFinder {
 private static final Logger log = LogManager.getLogger( PSParameterizedLegacyAutoSlotContentFinder.class.getName( ) );
 
 /**
   * This method returns a set of SlotItems retrieved from a legacy XML 
   * query resource.  The resource output should conform to sys_AssemblerInfo.dtd, 
   * but only the content ID (linkurl/@contentid) and the template ID 
   * (linkurl/@variantid) are of import to this routine.  
   */
	protected Set<SlotItem> getSlotItems(IPSAssemblyItem item, IPSTemplateSlot slot, Map<String, Object> params )	throws RepositoryException, PSFilterException {
  log.debug( "initializing. . ." );
  //TreeSet<SlotItem> hits = new TreeSet<SlotItem>( new PSBaseSlotContentFinder.SlotItemOrder( ) );
  Set<SlotItem> hits = new LinkedHashSet<SlotItem>(); 
  Map<String, String> args = slot.getFinderArguments( );
  String resource = this.getValue( args, params, "resource", null );
  if(StringUtils.isBlank( resource ) ) 
  {
      String emsg = "The resource parameter is required for a legacy auto slot content finder";
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
  }
  Map<String,String> resourceArgs = new HashMap<String,String>(); 
  resourceArgs.putAll(args); 
  resourceArgs.putAll(SimplifyParameters.simplifyMap(params)); 
  
  
//  StringBuilder url = new StringBuilder( resource );
//  
//  if( params != null ) {
//   if( !params.isEmpty( ) ) {
//    url.append( "?" );
//    Iterator<String> keys = params.keySet( ).iterator( );
//    
//    while( keys.hasNext( ) ) {
//     String key = keys.next( );
//     String[ ] values = ( String[ ] )params.get( key );
//     if( values == null || values.length == 0 ) continue;
//     
//     url.append( key );
//     url.append( "=" );
//     url.append( values[ 0 ] );
//     if( keys.hasNext( ) ) url.append( "&" );
//    }  //  while( keys.hasNext( ) )
//   }  //  if( !params.isEmpty( ) )
//  }  //  if( params != null )
//  
//  resource = url.toString( );
//  _logger.debug( "URL is \"" + resource + "\"." );
  
  IPSCmsObjectMgr mgr = PSCmsObjectMgrLocator.getObjectManager( );
  PSRequest req = ( PSRequest )PSRequestInfo.getRequestInfo( "PSREQUEST" );
  //  NOTE:  Passing the parameter map "params" here does not seem to work.  I tried 
  //         setting the flag argument to both true and false to no avail.  It certainly 
  //         contains the correct information.  This looks to be due to the values of 
  //         the params Map being String arrays rather than Strings.
  PSInternalRequest iReq = PSServer.getInternalRequest( resource, req, resourceArgs, false, null );
  ArrayList<Integer> id = new ArrayList<Integer>( );
  
  try {
   Document document = iReq.getResultDoc( );
   NodeList nodes = document.getElementsByTagName( "linkurl" );
   
   for( int i=0; i<nodes.getLength( ); i++ ) {
    Element e = ( Element )nodes.item( i );
    //  NOTE:  Shouldn't folder ID be used here so that - in the case of link snippets - the 
    //         proper link is built?  I suspect as is it will default the first folder it 
    //         encounters.
    String templateId = e.getAttribute( "variantid" );
    String contentId = e.getAttribute( "contentid" );
    PSGuid guid = new PSGuid( PSTypeEnum.TEMPLATE, templateId );
    
    log.debug( "found node for CID # {} and template # {}.", contentId, templateId );
    id.clear( );
    id.add( Integer.parseInt( contentId ) );
    
    PSComponentSummary summary = mgr.loadComponentSummaries( id ).get( 0 );
    PSLegacyGuid legacyGuid = new PSLegacyGuid( summary.getCurrentLocator( ) );
    
    log.debug( "new GUID is {} \nlegacy GUID is {}", guid.toString( ), legacyGuid.toString( ) );
    hits.add( new PSBaseSlotContentFinder.SlotItem( legacyGuid, guid, i ) );
   }  //  for( int i=0; i<nodes.getLength( ); i++ )
   
   log.debug( "found {} items for legacy slot.", hits.size() );
  } catch( PSInternalRequestCallException __ie ) {
   log.error(__ie.getMessage());
   log.debug(__ie.getMessage(), __ie);
   throw new RepositoryException( __ie.getMessage( ) );
  }  //  try
  
  return hits;
	}

	public Type getType( ) {
  return com.percussion.services.assembly.IPSSlotContentFinder.Type.AUTOSLOT;
	}
}
